package com.algoviz.controller;

import com.algoviz.dto.interview.InterviewResponse;
import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.OJSolution;
import com.algoviz.entity.OJSolutionComment;
import com.algoviz.service.OJSolutionCommentService;
import com.algoviz.service.OJSolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OJ 题解后台审核接口
 */
@RestController
@RequestMapping("/api/admin/solutions")
@RequiredArgsConstructor
@Tag(name = "OJ题解审核", description = "后台题解/评论审核管理")
public class OJSolutionAdminController {

    private final OJSolutionService solutionService;
    private final OJSolutionCommentService commentService;

    // ==================== 题解审核 ====================

    @Operation(summary = "题解列表（含审核中）")
    @GetMapping
    public InterviewResponse<PageResult<OJSolution>> list(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String auditStatus,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<OJSolution> r = solutionService.adminList(keyword, auditStatus, page, pageSize);
        return InterviewResponse.ok(r);
    }

    @Operation(summary = "审核通过")
    @PutMapping("/{id}/pass")
    public InterviewResponse<String> pass(@PathVariable Long id) {
        solutionService.adminPassAudit(id);
        return InterviewResponse.ok("已通过审核");
    }

    @Operation(summary = "审核驳回（下架）")
    @PutMapping("/{id}/reject")
    public InterviewResponse<String> reject(@PathVariable Long id) {
        solutionService.adminRejectAudit(id);
        return InterviewResponse.ok("已驳回并下架");
    }

    @Operation(summary = "上架/下架题解")
    @PutMapping("/{id}/status")
    public InterviewResponse<String> updateStatus(@PathVariable Long id, @RequestParam String status) {
        solutionService.adminUpdateStatus(id, status);
        return InterviewResponse.ok("状态已更新");
    }

    // ==================== 评论审核 ====================

    @Operation(summary = "评论列表（含审核中）")
    @GetMapping("/comments")
    public InterviewResponse<PageResult<OJSolutionComment>> commentList(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String auditStatus,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<OJSolutionComment> r = commentService.adminList(keyword, auditStatus, page, pageSize);
        return InterviewResponse.ok(r);
    }

    @Operation(summary = "评论审核通过")
    @PutMapping("/comments/{id}/pass")
    public InterviewResponse<String> commentPass(@PathVariable Long id) {
        commentService.adminPassAudit(id);
        return InterviewResponse.ok("评论已通过审核");
    }

    @Operation(summary = "评论审核驳回（隐藏）")
    @PutMapping("/comments/{id}/reject")
    public InterviewResponse<String> commentReject(@PathVariable Long id) {
        commentService.adminRejectAudit(id);
        return InterviewResponse.ok("评论已驳回并隐藏");
    }
}
