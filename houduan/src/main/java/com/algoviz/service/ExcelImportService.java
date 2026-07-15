package com.algoviz.service;

import com.algoviz.dto.BatchAddProblemsRequest;
import com.algoviz.dto.BatchAddProblemsResponse;
import com.algoviz.dto.AIGenerateProblemResponse;
import com.algoviz.entity.OJProblem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Excel 题目导入服务
 *
 * Excel 模板列（第一行为表头，从第二行开始是数据）：
 *   必填：题号(problemNo) | 标题(title) | 难度(difficulty)
 *   选填：标签(tags) | 题目描述(description) | 输入格式(inputFormat) | 输出格式(outputFormat)
 *         样例输入(sampleInput) | 样例输出(sampleOutput) | 解题提示(hint) | 代码模板(template)
 *   状态(status) 可选，默认 ACTIVE
 *
 * 难度值：easy / medium / hard
 */
@Service
public class ExcelImportService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelImportService.class);

    @Autowired
    private OJProblemService problemService;

    /**
     * 解析 Excel 并批量入库
     * @param file 上传的 Excel 文件（.xlsx）
     * @param overwriteOnConflict 题号冲突时是否覆盖
     */
    public BatchAddProblemsResponse importFromExcel(MultipartFile file, boolean overwriteOnConflict) {
        BatchAddProblemsResponse response = new BatchAddProblemsResponse();
        List<String> failedReasons = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        if (file == null || file.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("文件为空");
            response.setSuccessCount(0);
            response.setFailedCount(0);
            response.setFailedReasons(failedReasons);
            return response;
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            response.setSuccess(false);
            response.setMessage("仅支持 .xlsx / .xls 格式");
            response.setSuccessCount(0);
            response.setFailedCount(0);
            response.setFailedReasons(failedReasons);
            return response;
        }

        List<AIGenerateProblemResponse.GeneratedProblem> problems = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             Workbook workbook = filename.endsWith(".xlsx") ? new XSSFWorkbook(is) : WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                response.setSuccess(false);
                response.setMessage("Excel 无可用的 Sheet");
                response.setSuccessCount(0);
                response.setFailedCount(0);
                response.setFailedReasons(failedReasons);
                return response;
            }

            // 1. 解析表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                response.setSuccess(false);
                response.setMessage("Excel 第 1 行无表头");
                return response;
            }
            Map<String, Integer> colIndex = new HashMap<>();
            for (Cell cell : headerRow) {
                String colName = getCellString(cell).trim();
                if (!colName.isEmpty()) {
                    colIndex.put(colName, cell.getColumnIndex());
                }
            }
            logger.info("Excel 表头: {}", colIndex.keySet());

            // 校验必填列
            if (!colIndex.containsKey("题号") && !colIndex.containsKey("problemNo")) {
                response.setSuccess(false);
                response.setMessage("Excel 缺少必填列：题号");
                return response;
            }
            if (!colIndex.containsKey("标题") && !colIndex.containsKey("title")) {
                response.setSuccess(false);
                response.setMessage("Excel 缺少必填列：标题");
                return response;
            }

            // 2. 逐行解析
            int totalRows = sheet.getLastRowNum();
            for (int r = 1; r <= totalRows; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                // 跳过全空行
                if (isRowEmpty(row)) continue;

                try {
                    AIGenerateProblemResponse.GeneratedProblem gp = new AIGenerateProblemResponse.GeneratedProblem();
                    gp.setProblemNo(getByCol(row, colIndex, "题号", "problemNo"));
                    gp.setTitle(getByCol(row, colIndex, "标题", "title"));
                    gp.setDifficulty(getByCol(row, colIndex, "难度", "difficulty", "medium"));
                    gp.setTags(getByCol(row, colIndex, "标签", "tags", ""));
                    gp.setDescription(getByCol(row, colIndex, "题目描述", "description", ""));
                    gp.setInputFormat(getByCol(row, colIndex, "输入格式", "inputFormat", ""));
                    gp.setOutputFormat(getByCol(row, colIndex, "输出格式", "outputFormat", ""));
                    gp.setSampleInput(getByCol(row, colIndex, "样例输入", "sampleInput", ""));
                    gp.setSampleOutput(getByCol(row, colIndex, "样例输出", "sampleOutput", ""));
                    gp.setHint(getByCol(row, colIndex, "解题提示", "hint", ""));
                    gp.setTemplate(getByCol(row, colIndex, "代码模板", "template", ""));
                    problems.add(gp);
                } catch (Exception ex) {
                    failedCount++;
                    failedReasons.add("第 " + (r + 1) + " 行解析失败：" + ex.getMessage());
                }
            }

            logger.info("Excel 解析完成：有效 {} 条，无效 {} 条", problems.size(), failedCount);

        } catch (Exception e) {
            logger.error("Excel 解析异常", e);
            response.setSuccess(false);
            response.setMessage("Excel 解析失败：" + e.getMessage());
            response.setSuccessCount(0);
            response.setFailedCount(0);
            response.setFailedReasons(failedReasons);
            return response;
        }

        // 3. 批量入库（复用 batch 逻辑：题号为空自动分配 / 冲突可覆盖）
        if (problems.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Excel 中无有效题目数据");
            response.setSuccessCount(0);
            response.setFailedCount(failedCount);
            response.setFailedReasons(failedReasons);
            return response;
        }

        for (int i = 0; i < problems.size(); i++) {
            AIGenerateProblemResponse.GeneratedProblem gp = problems.get(i);
            try {
                if (gp.getTitle() == null || gp.getTitle().trim().isEmpty()) {
                    failedCount++;
                    failedReasons.add("第 " + (i + 1) + " 道：标题为空");
                    continue;
                }

                // 题号为空 → 自动分配
                if (gp.getProblemNo() == null || gp.getProblemNo().trim().isEmpty()) {
                    gp.setProblemNo(problemService.generateNextProblemNo());
                }

                // 难度归一化
                String diff = gp.getDifficulty();
                if (diff == null || (!diff.equals("easy") && !diff.equals("medium") && !diff.equals("hard"))) {
                    gp.setDifficulty("medium");
                }

                // 描述包装：把样例/输入输出拼进 description
                String fullDescription = buildDescription(gp);
                String fullTemplate = gp.getTemplate() == null ? "" : gp.getTemplate();

                OJProblem entity = new OJProblem();
                entity.setProblemNo(gp.getProblemNo());
                entity.setTitle(gp.getTitle());
                entity.setDifficulty(gp.getDifficulty());
                entity.setTags(gp.getTags() == null ? "" : gp.getTags());
                entity.setDescription(fullDescription);
                entity.setTemplate(fullTemplate);
                entity.setStatus("ACTIVE");
                entity.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                entity.setUpdatedAt(entity.getCreatedAt());
                entity.setSubmissionCount(0);

                // 冲突检查
                OJProblem existing = problemService.getProblemByNo(gp.getProblemNo());
                if (existing != null) {
                    if (overwriteOnConflict) {
                        // 覆盖
                        entity.setId(existing.getId());
                        problemService.updateProblem(entity);
                        logger.info("第 {} 道：题号 {} 覆盖更新", i + 1, gp.getProblemNo());
                    } else {
                        // 重新分配
                        String newNo = problemService.generateNextProblemNo();
                        entity.setProblemNo(newNo);
                        problemService.addProblem(entity);
                        logger.info("第 {} 道：题号冲突，重新分配 {}", i + 1, newNo);
                    }
                } else {
                    problemService.addProblem(entity);
                }
                successCount++;
            } catch (Exception e) {
                failedCount++;
                failedReasons.add("第 " + (i + 1) + " 道「" + (gp.getTitle() == null ? "无标题" : gp.getTitle()) + "」入库失败：" + e.getMessage());
                logger.error("入库失败", e);
            }
        }

        response.setSuccess(failedCount == 0);
        response.setMessage(String.format("导入完成：成功 %d 道，失败 %d 道", successCount, failedCount));
        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setFailedReasons(failedReasons);
        return response;
    }

    // ====== 工具方法 ======

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                // 整数去掉 .0
                double v = cell.getNumericCellValue();
                if (v == Math.floor(v) && !Double.isInfinite(v)) {
                    return String.valueOf((long) v);
                }
                return String.valueOf(v);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return "";
        }
    }

    private String getByCol(Row row, Map<String, Integer> colIndex, String cn, String en) {
        return getByCol(row, colIndex, cn, en, "");
    }

    private String getByCol(Row row, Map<String, Integer> colIndex, String cn, String en, String defaultVal) {
        Integer idx = colIndex.get(cn);
        if (idx == null) idx = colIndex.get(en);
        if (idx == null) return defaultVal;
        Cell cell = row.getCell(idx);
        String val = getCellString(cell).trim();
        return val.isEmpty() ? defaultVal : val;
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK) {
                String v = getCellString(cell).trim();
                if (!v.isEmpty()) return false;
            }
        }
        return true;
    }

    private String buildDescription(AIGenerateProblemResponse.GeneratedProblem gp) {
        StringBuilder sb = new StringBuilder();
        if (gp.getDescription() != null && !gp.getDescription().isEmpty()) {
            sb.append(gp.getDescription());
        }
        if (gp.getInputFormat() != null && !gp.getInputFormat().isEmpty()) {
            sb.append("\n\n**输入格式**\n\n").append(gp.getInputFormat());
        }
        if (gp.getOutputFormat() != null && !gp.getOutputFormat().isEmpty()) {
            sb.append("\n\n**输出格式**\n\n").append(gp.getOutputFormat());
        }
        if (gp.getSampleInput() != null && !gp.getSampleInput().isEmpty()) {
            sb.append("\n\n**样例输入**\n\n```\n").append(gp.getSampleInput()).append("\n```");
        }
        if (gp.getSampleOutput() != null && !gp.getSampleOutput().isEmpty()) {
            sb.append("\n\n**样例输出**\n\n```\n").append(gp.getSampleOutput()).append("\n```");
        }
        if (gp.getHint() != null && !gp.getHint().isEmpty()) {
            sb.append("\n\n**解题提示**\n\n").append(gp.getHint());
        }
        return sb.toString();
    }
}
