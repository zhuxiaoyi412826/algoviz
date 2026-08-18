package com.algoviz.controller;

import com.algoviz.dto.interview.InterviewFrontStats;
import com.algoviz.dto.interview.InterviewResponse;
import com.algoviz.dto.interview.InterviewTagVO;
import com.algoviz.dto.interview.InterviewUserListVO;
import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.InterviewProblem;
import com.algoviz.service.InterviewUserService;
import com.algoviz.service.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 面试题前台接口
 * 路径前缀：/api/interview/user
 */
@Tag(name = "面试题-前台", description = "F1-F17 前台接口")
@RestController
@RequestMapping("/api/interview/user")
@RequiredArgsConstructor
public class InterviewUserController {

    private final InterviewUserService userService;
    private final VectorSearchService vectorSearchService;

    private static Long resolveUserId() {
        try {
            Object u = com.algoviz.utils.UserContextHolder.get();
            if (u != null) {
                java.lang.reflect.Method m = u.getClass().getMethod("getId");
                Object v = m.invoke(u);
                if (v != null) return Long.valueOf(v.toString());
            }
        } catch (Throwable ignored) {}
        return null;
    }

    // ===== F1 列表 =====
    @Operation(summary = "F1 面试题列表")
    @GetMapping("/problems")
    public InterviewResponse<PageResult<InterviewProblem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer onlyFrequent,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return InterviewResponse.ok(userService.listProblems(
                keyword, difficulty, category, tag, onlyFrequent,
                sortBy, order, page, pageSize));
    }

    // ===== F2 按 id 查看详情（自动累加阅读量+记录历史） =====
    @Operation(summary = "F2 按 id 查看详情（Markdown，访问自动累加阅读量）")
    @GetMapping("/problems/{id}")
    public InterviewResponse<InterviewProblem> getById(@PathVariable Long id) {
        InterviewProblem p = userService.getDetailById(id, resolveUserId());
        if (p == null) return InterviewResponse.fail(404, "题目不存在");
        return InterviewResponse.ok(p);
    }

    // ===== F3 按 problemNo 查看详情 =====
    @Operation(summary = "F3 按 problemNo 查看详情（访问自动累加阅读量）")
    @GetMapping("/problems/by-no/{problemNo}")
    public InterviewResponse<InterviewProblem> getByNo(@PathVariable String problemNo) {
        InterviewProblem p = userService.getDetailByNo(problemNo, resolveUserId());
        if (p == null) return InterviewResponse.fail(404, "题目不存在");
        return InterviewResponse.ok(p);
    }

    // ===== F4 标签（按热度） =====
    @Operation(summary = "F4 获取所有标签（按热度排序）")
    @GetMapping("/tags")
    public InterviewResponse<List<InterviewTagVO>> tags() {
        return InterviewResponse.ok(userService.listTags());
    }

    // ===== F5 分类 =====
    @Operation(summary = "F5 获取分类列表")
    @GetMapping("/categories")
    public InterviewResponse<List<String>> categories() {
        return InterviewResponse.ok(userService.listCategories());
    }

    // ===== F6 收藏列表 =====
    @Operation(summary = "F6 收藏列表")
    @GetMapping("/favorites")
    public InterviewResponse<PageResult<InterviewUserListVO>> myFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long uid = resolveUserId();
        if (uid == null) return InterviewResponse.fail(401, "请先登录");
        return InterviewResponse.ok(userService.listFavorites(uid, page, pageSize));
    }

    // ===== F7 浏览历史 =====
    @Operation(summary = "F7 浏览历史列表")
    @GetMapping("/history")
    public InterviewResponse<PageResult<InterviewUserListVO>> myHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long uid = resolveUserId();
        if (uid == null) return InterviewResponse.fail(401, "请先登录");
        return InterviewResponse.ok(userService.listHistory(uid, page, pageSize));
    }

    // ===== F8 删除单条历史 =====
    @Operation(summary = "F8 删除单条浏览历史")
    @DeleteMapping("/history/{problemId}")
    public InterviewResponse<Void> deleteHistory(@PathVariable Long problemId) {
        Long uid = resolveUserId();
        if (uid == null) return InterviewResponse.fail(401, "请先登录");
        boolean ok = userService.deleteHistory(uid, problemId);
        return ok ? InterviewResponse.ok("删除成功") : InterviewResponse.fail("未找到对应历史");
    }

    // ===== F9 清空历史 =====
    @Operation(summary = "F9 清空浏览历史")
    @DeleteMapping("/history/clear")
    public InterviewResponse<Void> clearHistory() {
        Long uid = resolveUserId();
        if (uid == null) return InterviewResponse.fail(401, "请先登录");
        int n = userService.clearHistory(uid);
        return InterviewResponse.ok("清空浏览历史成功，共 " + n + " 条");
    }

    // ===== F10 收藏 =====
    @Operation(summary = "F10 收藏题目")
    @PostMapping("/favorites")
    public InterviewResponse<Void> addFavorite(@RequestBody Map<String, Long> body) {
        Long uid = resolveUserId();
        if (uid == null) return InterviewResponse.fail(401, "请先登录");
        Long pid = body.get("problemId");
        if (pid == null) return InterviewResponse.fail(400, "缺少 problemId");
        boolean ok = userService.addFavorite(uid, pid);
        return ok ? InterviewResponse.ok("收藏成功") : InterviewResponse.fail("题目不存在或已收藏");
    }

    // ===== F11 取消收藏 =====
    @Operation(summary = "F11 取消收藏")
    @DeleteMapping("/favorites/{problemId}")
    public InterviewResponse<Void> removeFavorite(@PathVariable Long problemId) {
        Long uid = resolveUserId();
        if (uid == null) return InterviewResponse.fail(401, "请先登录");
        boolean ok = userService.removeFavorite(uid, problemId);
        return ok ? InterviewResponse.ok("已取消收藏") : InterviewResponse.fail("未找到收藏");
    }

    // ===== F12 清空收藏 =====
    @Operation(summary = "F12 一键清空当前用户所有收藏题目")
    @DeleteMapping("/favorites/clear")
    public InterviewResponse<Void> clearFavorites() {
        Long uid = resolveUserId();
        if (uid == null) return InterviewResponse.fail(401, "请先登录");
        int n = userService.clearFavorites(uid);
        return InterviewResponse.ok("清空收藏成功，共 " + n + " 条");
    }

    // ===== F13 点赞 =====
    @Operation(summary = "F13 题目点赞")
    @PostMapping("/problems/{id}/like")
    public InterviewResponse<Void> like(@PathVariable Long id) {
        Long uid = resolveUserId();
        if (uid == null) return InterviewResponse.fail(401, "请先登录");
        boolean ok = userService.likeProblem(uid, id);
        return ok ? InterviewResponse.ok("操作成功") : InterviewResponse.fail("题目不存在");
    }

    // ===== F14 点踩 =====
    @Operation(summary = "F14 题目点踩")
    @PostMapping("/problems/{id}/dislike")
    public InterviewResponse<Void> dislike(@PathVariable Long id) {
        Long uid = resolveUserId();
        if (uid == null) return InterviewResponse.fail(401, "请先登录");
        boolean ok = userService.dislikeProblem(uid, id);
        return ok ? InterviewResponse.ok("操作成功") : InterviewResponse.fail("题目不存在");
    }

    // ===== F15 是否收藏 =====
    @Operation(summary = "F15 检查某题是否已收藏")
    @GetMapping("/favorites/{problemId}/check")
    public InterviewResponse<Boolean> isFavorite(@PathVariable Long problemId) {
        Long uid = resolveUserId();
        if (uid == null) return InterviewResponse.ok(false);
        return InterviewResponse.ok(userService.isFavorite(uid, problemId));
    }

    // ===== F16 前台统计（无需登录） =====
    @Operation(summary = "F16 前台统计（阅读/收藏/点赞/点踩汇总）")
    @GetMapping("/stats")
    public InterviewResponse<InterviewFrontStats> stats() {
        return InterviewResponse.ok(userService.frontStats());
    }

    // ===== F17 搜索 =====
    @Operation(summary = "F17 题目模糊搜索")
    @GetMapping("/search")
    public InterviewResponse<PageResult<InterviewProblem>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return InterviewResponse.ok(userService.search(keyword, page, pageSize));
    }

    // ===== F18 ES 分词搜索 =====
    @Operation(summary = "F18 ES 分词搜索（IK 分词器，支持高亮）")
    @GetMapping("/es-search")
    public InterviewResponse<List<InterviewProblem>> esSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "30") int topK,
            @RequestParam(required = false) String difficulty) {
        return vectorSearchService.esSearch(query, topK, difficulty);
    }
}
