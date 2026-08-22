package com.algoviz.controller;

import com.algoviz.dto.interview.*;
import com.algoviz.entity.InterviewProblem;
import com.algoviz.service.InterviewProblemAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 面试题后台管理接口
 * 路径前缀：/api/interview/admin
 */
@Tag(name = "面试题-后台管理", description = "B1-B15 后台接口")
@RestController
@RequestMapping("/api/interview/admin")
@RequiredArgsConstructor
public class InterviewProblemAdminController {

    private final InterviewProblemAdminService adminService;
    private final ObjectMapper objectMapper;

    private static String resolveAdminId() {
        try {
            Object u = com.algoviz.common.util.UserContextHolder.get();
            if (u != null) {
                java.lang.reflect.Method m = u.getClass().getMethod("getId");
                Object v = m.invoke(u);
                if (v != null) return v.toString();
            }
        } catch (Throwable ignored) {}
        return "system";
    }

    // ===== B1 列表 =====
    @Operation(summary = "B1 分页查询面试题列表")
    @GetMapping("/problems")
    public InterviewResponse<PageResult<InterviewProblem>> listProblems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer isFrequent,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return InterviewResponse.ok(adminService.list(
                keyword, tag, difficulty, category, status, isFrequent,
                sortBy, order, page, pageSize));
    }

    // ===== B2 详情 =====
    @Operation(summary = "B2 面试题详情")
    @GetMapping("/problems/{id}")
    public InterviewResponse<InterviewProblem> getById(@PathVariable Long id) {
        InterviewProblem p = adminService.getById(id);
        if (p == null) return InterviewResponse.fail(404, "题目不存在");
        return InterviewResponse.ok(p);
    }

    // ===== B3 新增 =====
    @Operation(summary = "B3 手动添加面试题")
    @PostMapping("/problems")
    public InterviewResponse<InterviewProblem> create(@RequestBody InterviewProblemSaveDTO dto) {
        try {
            InterviewProblem p = adminService.create(dto, resolveAdminId());
            return InterviewResponse.ok("添加成功", p);
        } catch (IllegalArgumentException e) {
            return InterviewResponse.fail(400, e.getMessage());
        }
    }

    // ===== B4 修改 =====
    @Operation(summary = "B4 修改面试题")
    @PutMapping("/problems/{id}")
    public InterviewResponse<Void> update(@PathVariable Long id, @RequestBody InterviewProblemSaveDTO dto) {
        try {
            boolean ok = adminService.update(id, dto, resolveAdminId());
            if (!ok) return InterviewResponse.fail(404, "题目不存在");
            return InterviewResponse.ok("修改成功");
        } catch (IllegalArgumentException e) {
            return InterviewResponse.fail(400, e.getMessage());
        }
    }

    // ===== B5 状态 =====
    @Operation(summary = "B5 切换状态")
    @PutMapping("/problems/{id}/status")
    public InterviewResponse<Void> updateStatus(@PathVariable Long id,
                                                @RequestBody Map<String, String> body) {
        String s = body.get("status");
        if (s == null) s = body.get("problemStatus");
        boolean ok = adminService.updateStatus(id, s, resolveAdminId());
        if (!ok) return InterviewResponse.fail(404, "题目不存在");
        return InterviewResponse.ok("状态更新成功");
    }

    // ===== B6 单条逻辑删除 =====
    @Operation(summary = "B6 逻辑删除")
    @DeleteMapping("/problems/{id}")
    public InterviewResponse<Void> logicDelete(@PathVariable Long id) {
        boolean ok = adminService.logicDelete(id, resolveAdminId());
        if (!ok) return InterviewResponse.fail(404, "题目不存在");
        return InterviewResponse.ok("删除成功");
    }

    // ===== B7 批量逻辑删除 =====
    @Operation(summary = "B7 批量逻辑删除")
    @DeleteMapping("/problems/batch")
    public InterviewResponse<Void> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        int n = adminService.batchLogicDelete(ids, resolveAdminId());
        return InterviewResponse.ok("成功删除 " + n + " 条");
    }

    // ===== B8 单条物理删除 =====
    @Operation(summary = "B8 物理删除")
    @DeleteMapping("/problems/real/{id}")
    public InterviewResponse<Void> permanentDelete(@PathVariable Long id) {
        boolean ok = adminService.physicalDelete(id);
        if (!ok) return InterviewResponse.fail(404, "题目不存在");
        return InterviewResponse.ok("物理删除成功");
    }

    // ===== B9 批量物理删除 =====
    @Operation(summary = "B9 批量物理删除")
    @DeleteMapping("/problems/real/batch")
    public InterviewResponse<Void> batchPermanentDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        int n = adminService.batchPhysicalDelete(ids);
        return InterviewResponse.ok("成功物理删除 " + n + " 条");
    }

    // ===== B10 JSON 批量导入 =====
    @Operation(summary = "B10 JSON 批量导入")
    @PostMapping("/problems/batch-import")
    public InterviewResponse<BatchImportResult> batchImport(
            @RequestParam(defaultValue = "false") boolean overwriteOnConflict,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> raw = (List<Map<String, Object>>) body.get("problems");
        if (raw == null) return InterviewResponse.fail(400, "缺少 problems 字段");
        List<InterviewProblemSaveDTO> dtos = raw.stream().map(InterviewProblemAdminController::mapToDto).toList();
        BatchImportResult r = adminService.batchImport(dtos, overwriteOnConflict, resolveAdminId());
        r.setFileName(body.get("fileName") != null ? body.get("fileName").toString() : null);
        return InterviewResponse.ok("导入完成", r);
    }

    // ===== B11 上传 JSON 文件导入 =====
    @Operation(summary = "B11 上传 JSON 文件导入")
    @PostMapping(value = "/problems/import-json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InterviewResponse<BatchImportResult> importJsonFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean overwriteOnConflict) throws Exception {
        if (file == null || file.isEmpty()) return InterviewResponse.fail(400, "请上传 JSON 文件");
        String json = new String(file.getBytes(), StandardCharsets.UTF_8);
        List<InterviewProblemSaveDTO> dtos;
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            dtos = raw.stream().map(InterviewProblemAdminController::mapToDto).toList();
        } catch (Exception e) {
            return InterviewResponse.fail(400, "JSON 格式错误：" + e.getMessage());
        }
        BatchImportResult r = adminService.batchImport(dtos, overwriteOnConflict, resolveAdminId());
        r.setFileName(file.getOriginalFilename());
        return InterviewResponse.ok("导入完成", r);
    }

    // ===== B12 JSON 导出 =====
    @Operation(summary = "B12 JSON 导出")
    @GetMapping(value = "/problems/export-json", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportJson(@RequestParam(required = false) String difficulty,
                           @RequestParam(required = false) String category,
                           HttpServletResponse response) throws Exception {
        List<InterviewProblem> data = adminService.listAllForExport(difficulty, category);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        response.setContentType("application/json;charset=UTF-8");
        String fname = "面试题库_" + java.time.LocalDate.now().toString().replace("-", "") + ".json";
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + new String(fname.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }

    // ===== B13 AI 生成 =====
    @Operation(summary = "B13 AI 生成题目（同步）")
    @PostMapping("/ai/generate")
    public InterviewResponse<List<InterviewProblemSaveDTO>> aiGenerate(@RequestBody Map<String, Object> body) {
        String category = body.get("category") == null ? null : body.get("category").toString();
        String difficulty = body.get("difficulty") == null ? null : body.get("difficulty").toString();
        int num = body.get("num") == null ? 3 : Integer.parseInt(body.get("num").toString());
        return InterviewResponse.ok(adminService.generateByAI(category, difficulty, num));
    }

    // ===== B14 保存 AI 生成 =====
    @Operation(summary = "B14 批量保存 AI 生成结果")
    @PostMapping("/problems/batch-save")
    public InterviewResponse<BatchImportResult> aiBatchSave(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> raw = (List<Map<String, Object>>) body.get("problems");
        if (raw == null) return InterviewResponse.fail(400, "缺少 problems 字段");
        List<InterviewProblemSaveDTO> dtos = raw.stream().map(InterviewProblemAdminController::mapToDto).toList();
        return InterviewResponse.ok(adminService.batchSaveAIGenerated(dtos, resolveAdminId()));
    }

    // ===== B15 后台统计 =====
    @Operation(summary = "B15 后台统计")
    @GetMapping("/stats")
    public InterviewResponse<InterviewAdminStats> adminStats() {
        return InterviewResponse.ok(adminService.adminStats());
    }

    // ===== 工具：Map -> SaveDTO =====
    @SuppressWarnings("unchecked")
    private static InterviewProblemSaveDTO mapToDto(Map<String, Object> m) {
        InterviewProblemSaveDTO d = new InterviewProblemSaveDTO();
        d.setProblemNo(strOrNull(m, "problemNo"));
        d.setTitle(strOrNull(m, "title"));
        d.setDifficulty(strOrNull(m, "difficulty"));
        d.setCategory(strOrNull(m, "category"));
        d.setTags(m.get("tags"));
        d.setDescription(strOrNull(m, "description"));
        d.setInputFormat(strOrNull(m, "inputFormat"));
        d.setOutputFormat(strOrNull(m, "outputFormat"));
        d.setSolution(strOrNull(m, "solution"));
        d.setStatus(strOrNull(m, "status"));
        Object ifr = m.get("isFrequent");
        if (ifr != null) {
            if (ifr instanceof Boolean b) d.setIsFrequent(b ? 1 : 0);
            else d.setIsFrequent(Integer.parseInt(ifr.toString()) == 0 ? 0 : 1);
        }
        return d;
    }

    private static String strOrNull(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }
}
