package com.algoviz.controller;

import com.algoviz.dto.AIGenerateProblemResponse;
import com.algoviz.dto.BatchAddProblemsRequest;
import com.algoviz.dto.BatchAddProblemsResponse;
import com.algoviz.entity.OJProblem;
import com.algoviz.service.ExcelImportService;
import com.algoviz.service.MdImportService;
import com.algoviz.service.JsonImportService;
import com.algoviz.service.OJProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/problems")
@Tag(name = "题目管理", description = "OJ题目相关接口")
public class OJProblemController {

    private static final Logger logger = LoggerFactory.getLogger(OJProblemController.class);

    @Autowired
    private OJProblemService problemService;

    @Autowired
    private ExcelImportService excelImportService;

    @Autowired
    private MdImportService mdImportService;

    @Autowired
    private JsonImportService jsonImportService;

    @GetMapping
    @Operation(summary = "获取题目列表", description = "获取所有题目或按条件筛选")
    public Map<String, Object> getProblems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String difficulty) {
        
        logger.info("获取题目列表 - 关键词: {}, 难度: {}", keyword, difficulty);
        
        Map<String, Object> result = new HashMap<>();
        List<OJProblem> problems;

        if (keyword != null && !keyword.isEmpty()) {
            problems = problemService.searchProblems(keyword);
        } else if (difficulty != null && !difficulty.isEmpty()) {
            problems = problemService.getProblemsByDifficulty(difficulty);
        } else {
            problems = problemService.getActiveProblems();
        }

        result.put("success", true);
        result.put("problems", problems);
        result.put("count", problems.size());
        
        return result;
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有题目(含禁用)", description = "获取所有题目，包括已禁用的")
    public Map<String, Object> getAllProblems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String difficulty) {
        logger.info("获取所有题目 - 关键词: {}, 难度: {}", keyword, difficulty);
        
        Map<String, Object> result = new HashMap<>();
        List<OJProblem> problems;

        if (keyword != null && !keyword.isEmpty()) {
            problems = problemService.searchProblems(keyword);
        } else if (difficulty != null && !difficulty.isEmpty()) {
            problems = problemService.getProblemsByDifficulty(difficulty);
        } else {
            problems = problemService.getAllProblems();
        }
        
        result.put("success", true);
        result.put("problems", problems);
        result.put("count", problems.size());
        
        return result;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取题目详情", description = "根据ID获取题目详细信息")
    public Map<String, Object> getProblemById(@PathVariable String id) {
        logger.info("获取题目详情：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        OJProblem problem = problemService.getProblemById(id);
        
        if (problem != null) {
            result.put("success", true);
            result.put("problem", problem);
        } else {
            result.put("success", false);
            result.put("message", "题目不存在");
        }
        
        return result;
    }

    @GetMapping("/by-no/{problemNo}")
    @Operation(summary = "按题号获取题目", description = "根据题号获取题目详细信息")
    public Map<String, Object> getProblemByNo(@PathVariable String problemNo) {
        logger.info("获取题目：{}", problemNo);
        
        Map<String, Object> result = new HashMap<>();
        OJProblem problem = problemService.getProblemByNo(problemNo);
        
        if (problem != null) {
            result.put("success", true);
            result.put("problem", problem);
        } else {
            result.put("success", false);
            result.put("message", "题目不存在");
        }
        
        return result;
    }

    @PostMapping
    @Operation(summary = "添加题目", description = "添加新的OJ题目")
    public Map<String, Object> addProblem(@RequestBody OJProblem problem) {
        logger.info("添加题目：{}", problem.getTitle());
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            problemService.addProblem(problem);
            result.put("success", true);
            result.put("message", "题目添加成功");
        } catch (Exception e) {
            logger.error("添加题目失败", e);
            result.put("success", false);
            result.put("message", "添加题目失败：" + e.getMessage());
        }
        
        return result;
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新题目", description = "更新题目信息")
    public Map<String, Object> updateProblem(@PathVariable String id, @RequestBody OJProblem problem) {
        logger.info("更新题目：{}", id);

        Map<String, Object> result = new HashMap<>();

        try {
            problem.setId(Long.valueOf(id));
            problemService.updateProblem(problem);
            result.put("success", true);
            result.put("message", "题目更新成功");
        } catch (Exception e) {
            logger.error("更新题目失败", e);
            result.put("success", false);
            result.put("message", "更新题目失败：" + e.getMessage());
        }
        
        return result;
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "切换题目状态", description = "仅更新题目上线/下线状态")
    public Map<String, Object> updateStatus(@PathVariable String id, @RequestBody java.util.Map<String, String> body) {
        String status = body.get("status");
        logger.info("切换题目状态：id={}, status={}", id, status);

        Map<String, Object> result = new HashMap<>();
        if (status == null || (!"ACTIVE".equals(status) && !"INACTIVE".equals(status))) {
            result.put("success", false);
            result.put("message", "状态值非法，仅支持 ACTIVE / INACTIVE");
            return result;
        }

        try {
            boolean ok = problemService.updateStatus(id, status);
            result.put("success", ok);
            result.put("message", ok ? "状态切换成功" : "题目不存在");
        } catch (Exception e) {
            logger.error("切换题目状态失败", e);
            result.put("success", false);
            result.put("message", "切换状态失败：" + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除题目", description = "删除指定题目")
    public Map<String, Object> deleteProblem(@PathVariable String id) {
        logger.info("删除题目：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            problemService.deleteProblem(id);
            result.put("success", true);
            result.put("message", "题目删除成功");
        } catch (Exception e) {
            logger.error("删除题目失败", e);
            result.put("success", false);
            result.put("message", "删除题目失败：" + e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/stats")
    @Operation(summary = "获取统计信息", description = "获取题目统计信息")
    public Map<String, Object> getStats() {
        logger.info("获取题目统计");
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("total", problemService.countProblems());
        result.put("easy", problemService.countByDifficulty("easy"));
        result.put("medium", problemService.countByDifficulty("medium"));
        result.put("hard", problemService.countByDifficulty("hard"));

        return result;
    }

    /**
     * 批量添加题目（专供「AI 生成题目 → 批量入库」流程）
     * 流程：前端把 AI 生成的若干道题（含人工二次编辑）→ 一次性提交 → 后端写入数据库
     */
    @PostMapping("/batch")
    @Operation(summary = "批量添加题目", description = "把 AI 生成的若干道题批量入库")
    public BatchAddProblemsResponse batchAddProblems(@RequestBody BatchAddProblemsRequest request) {
        BatchAddProblemsResponse response = new BatchAddProblemsResponse();
        try {
            return batchAddProblemsInternal(request, response);
        } catch (Exception e) {
            logger.error("批量入库方法整体异常", e);
            response.setSuccess(false);
            response.setMessage("批量入库异常：" + e.getMessage());
            response.setSuccessCount(0);
            response.setFailedCount(request.getProblems() == null ? 0 : request.getProblems().size());
            response.setFailedReasons(new java.util.ArrayList<>(java.util.Arrays.asList(e.getMessage())));
            return response;
        }
    }

    private BatchAddProblemsResponse batchAddProblemsInternal(BatchAddProblemsRequest request, BatchAddProblemsResponse response) {
        logger.info("批量添加 {} 道题目", request.getProblems() == null ? 0 : request.getProblems().size());

        List<String> failedReasons = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        if (request.getProblems() == null || request.getProblems().isEmpty()) {
            response.setSuccess(false);
            response.setMessage("题目列表为空");
            response.setSuccessCount(0);
            response.setFailedCount(0);
            response.setFailedReasons(failedReasons);
            return response;
        }

        boolean overwrite = request.getOverwriteOnConflict() != null && request.getOverwriteOnConflict();

        for (int i = 0; i < request.getProblems().size(); i++) {
            AIGenerateProblemResponse.GeneratedProblem gp = request.getProblems().get(i);
            try {
                // 校验必填
                if (gp.getTitle() == null || gp.getTitle().trim().isEmpty()) {
                    failedCount++;
                    failedReasons.add("第 " + (i + 1) + " 道：标题为空");
                    continue;
                }

                // 智能题号策略：
                //   1. 如果 AI 传了题号 + 数据库无冲突 → 用 AI 的
                //   2. 如果题号为空 / 冲突且不覆盖 → 重新生成（最大+1）
                //   3. 如果题号冲突且 overwrite=true → 走更新流程
                String originalNo = gp.getProblemNo();
                OJProblem existing = null;
                String finalNo = null;

                if (originalNo != null && !originalNo.trim().isEmpty()) {
                    existing = problemService.getProblemByNo(originalNo);
                    if (existing != null && !overwrite) {
                        // 题号冲突且不覆盖 → 智能重新分配
                        finalNo = problemService.generateNextProblemNo();
                        logger.info("第 {} 道：题号 {} 冲突，自动分配新题号 {}", i + 1, originalNo, finalNo);
                    } else {
                        finalNo = originalNo;
                    }
                } else {
                    // 题号为空 → 智能生成
                    finalNo = problemService.generateNextProblemNo();
                }
                gp.setProblemNo(finalNo);

                // 组装入库结构
                OJProblem p = new OJProblem();
                if (existing != null) {
                    p.setId(existing.getId());
                }
                // id 不设，让 DB 自增（新增场景）；更新场景用已有 id
                p.setProblemNo(finalNo);
                p.setTitle(gp.getTitle());
                p.setDifficulty(gp.getDifficulty() == null ? "medium" : gp.getDifficulty());
                p.setTags(gp.getTags() == null ? "" : gp.getTags());
                // 把 AI 返回的 description 拼成结构化 Markdown
                p.setDescription(buildFullDescription(gp));
                p.setTemplate(gp.getTemplate() == null ? "" : gp.getTemplate());
                p.setStatus("ACTIVE");
                p.setSubmissionCount(0);
                p.setAcRate(0.0);
                p.setCreatedAt(java.time.LocalDateTime.now().toString());
                p.setUpdatedAt(p.getCreatedAt());

                if (existing != null) {
                    problemService.updateProblem(p);
                } else {
                    problemService.addProblem(p);
                }
                successCount++;

            } catch (Exception e) {
                failedCount++;
                failedReasons.add("第 " + (i + 1) + " 道：" + e.getMessage());
                logger.error("批量入库失败（第 {} 道）", i + 1, e);
            }
        }

        response.setSuccess(successCount > 0);
        response.setMessage("成功 " + successCount + " 道，失败 " + failedCount + " 道");
        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setFailedReasons(failedReasons);
        return response;
    }
    /**
     * 把 AI 生成的字段拼成完整的 Markdown 描述
     */
    private String buildFullDescription(AIGenerateProblemResponse.GeneratedProblem gp) {
        StringBuilder sb = new StringBuilder();
        if (gp.getDescription() != null) sb.append(gp.getDescription()).append("\n\n");
        if (gp.getInputFormat() != null && !gp.getInputFormat().isEmpty()) {
            sb.append("**输入格式**\n\n").append(gp.getInputFormat()).append("\n\n");
        }
        if (gp.getOutputFormat() != null && !gp.getOutputFormat().isEmpty()) {
            sb.append("**输出格式**\n\n").append(gp.getOutputFormat()).append("\n\n");
        }
        if (gp.getSampleInput() != null && !gp.getSampleInput().isEmpty()) {
            sb.append("**样例输入**\n\n```\n").append(gp.getSampleInput()).append("\n```\n\n");
        }
        if (gp.getSampleOutput() != null && !gp.getSampleOutput().isEmpty()) {
            sb.append("**样例输出**\n\n```\n").append(gp.getSampleOutput()).append("\n```\n\n");
        }
        if (gp.getHint() != null && !gp.getHint().isEmpty()) {
            sb.append("**解题提示**\n\n").append(gp.getHint()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Excel 批量导入题目
     * 接收 .xlsx 文件，解析后批量入库
     *
     * @param file 题目 Excel 文件（必填列：题号、标题、难度）
     * @param overwriteOnConflict 题号冲突时是否覆盖（默认 false，自动重新分配）
     */
    @PostMapping("/import-excel")
    @Operation(summary = "Excel 批量导入题目", description = "上传 .xlsx 文件批量入库")
    public BatchAddProblemsResponse importFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "overwriteOnConflict", defaultValue = "false") boolean overwriteOnConflict) {
        logger.info("收到 Excel 导入请求：filename={}, size={} bytes, overwrite={}",
                file.getOriginalFilename(), file.getSize(), overwriteOnConflict);
        return excelImportService.importFromExcel(file, overwriteOnConflict);
    }

    /**
     * Markdown 批量导入题目
     * 接收 .md 文件，解析后批量入库
     */
    @PostMapping("/import-md")
    @Operation(summary = "MD 批量导入题目", description = "上传 .md 文件批量入库")
    public BatchAddProblemsResponse importFromMd(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "overwriteOnConflict", defaultValue = "false") boolean overwriteOnConflict) {
        logger.info("收到 MD 导入请求：filename={}, size={} bytes, overwrite={}",
                file.getOriginalFilename(), file.getSize(), overwriteOnConflict);
        return mdImportService.importFromMd(file, overwriteOnConflict);
    }

    /**
     * JSON 批量导入题目
     * 接收 .json 文件，解析后批量入库
     */
    @PostMapping("/import-json")
    @Operation(summary = "JSON 批量导入题目", description = "上传 .json 文件批量入库")
    public BatchAddProblemsResponse importFromJson(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "overwriteOnConflict", defaultValue = "false") boolean overwriteOnConflict) {
        logger.info("收到 JSON 导入请求：filename={}, size={} bytes, overwrite={}",
                file.getOriginalFilename(), file.getSize(), overwriteOnConflict);
        return jsonImportService.importFromJson(file, overwriteOnConflict);
    }
}