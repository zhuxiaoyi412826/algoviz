package com.algoviz.controller;

import com.algoviz.dto.interview.PageResult;
import com.algoviz.entity.OJSolution;
import com.algoviz.entity.OJSolutionComment;
import com.algoviz.entity.User;
import com.algoviz.mapper.OJSolutionMapper;
import com.algoviz.mapper.UserMapper;
import com.algoviz.service.OJSolutionCommentService;
import com.algoviz.service.OJSolutionService;
import com.algoviz.common.util.DebugLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OJ 题解用户侧接口
 */
@RestController
@RequestMapping("/api/solutions")
@RequiredArgsConstructor
@Tag(name = "OJ题解", description = "用户题解发布/查看/评论/点赞")
public class OJSolutionController {

    private final OJSolutionService solutionService;
    private final OJSolutionCommentService commentService;
    private final UserMapper userMapper;
    private final OJSolutionMapper solutionMapper;

    /**
     * 根据用户ID获取用户信息
     */
    private User getUserInfo(Long userId) {
        if (userId == null) return null;
        try {
            User user = userMapper.findById(Math.toIntExact(userId));
            if (user == null) {
                DebugLogger.log("OJSolutionController", "用户不存在, userId=" + userId);
            } else {
                DebugLogger.log("OJSolutionController", "获取用户成功, id=" + user.getId() + ", username=" + user.getUsername() + ", avatarUrl=" + user.getAvatarUrl());
            }
            return user;
        } catch (Exception e) {
            DebugLogger.log("OJSolutionController", "获取用户异常, userId=" + userId + ", error=" + e.getMessage());
            return null;
        }
    }

    // ==================== 题解 ====================

    @Operation(summary = "题解列表")
    @GetMapping
    public Map<String, Object> list(@RequestParam Long problemId,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<OJSolution> r = solutionService.listPublished(problemId, page, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", r);
        return result;
    }

    @Operation(summary = "题解详情")
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id,
                                       @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        OJSolution s = solutionService.getDetail(id, userId);
        Map<String, Object> result = new HashMap<>();
        if (s == null) {
            result.put("success", false);
            result.put("message", "题解不存在");
        } else {
            result.put("success", true);
            result.put("data", s);
        }
        return result;
    }

    @Operation(summary = "我的题解（编辑页，返回原文）")
    @GetMapping("/my")
    public Map<String, Object> mySolution(@RequestParam Long problemId,
                                          @RequestHeader("X-User-Id") Long userId) {
        OJSolution s = solutionService.getMySolution(userId, problemId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", s);
        return result;
    }

    @Operation(summary = "发布/更新题解")
    @PostMapping
    public Map<String, Object> publish(@RequestBody OJSolution solution,
                                        @RequestHeader("X-User-Id") Long userId) {
        solution.setUserId(userId);
        // 设置用户冗余信息
        User user = getUserInfo(userId);
        if (user != null) {
            solution.setUsername(user.getUsername());
            String avatar = user.getAvatarUrl();
            solution.setAvatar((avatar != null && !avatar.isEmpty()) ? avatar : "/default-avatar.png");
        } else {
            solution.setUsername("用户" + userId);
            solution.setAvatar("/default-avatar.png");
        }
        OJSolutionService.PublishResult r = solutionService.publish(solution);
        Map<String, Object> result = new HashMap<>();
        result.put("success", r.isSuccess());
        result.put("message", r.getMessage());
        result.put("data", r);
        return result;
    }

    @Operation(summary = "点赞/取消点赞题解")
    @PostMapping("/{id}/like")
    public Map<String, Object> toggleSolutionLike(@PathVariable Long id,
                                                   @RequestHeader("X-User-Id") Long userId) {
        boolean liked = solutionService.toggleLike(userId, "SOLUTION", id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("liked", liked);
        return result;
    }

    // ==================== 评论 ====================

    @Operation(summary = "评论列表（顶层）")
    @GetMapping("/{solutionId}/comments")
    public Map<String, Object> listComments(@PathVariable Long solutionId,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<OJSolutionComment> r = commentService.listTopLevel(solutionId, page, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", r);
        return result;
    }

    @Operation(summary = "子评论列表（扁平，兼容旧版）")
    @GetMapping("/comments/{rootId}/replies")
    public Map<String, Object> listReplies(@PathVariable Long rootId,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "50") int pageSize) {
        PageResult<OJSolutionComment> r = commentService.listReplies(rootId, page, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", r);
        return result;
    }

    @Operation(summary = "子评论完整树（支持二级/三级/N级嵌套，children 数组递归包含）")
    @GetMapping("/comments/{rootId}/replies/tree")
    public Map<String, Object> listRepliesTree(@PathVariable Long rootId) {
        List<OJSolutionComment> tree = commentService.listRepliesTree(rootId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", tree);
        return result;
    }

    @Operation(summary = "发布评论")
    @PostMapping("/{solutionId}/comments")
    public Map<String, Object> publishComment(@PathVariable Long solutionId,
                                               @RequestBody OJSolutionComment comment,
                                               @RequestHeader("X-User-Id") Long userId) {
        comment.setSolutionId(solutionId);
        comment.setUserId(userId);
        
        // 设置用户冗余信息
        User user = getUserInfo(userId);
        if (user != null) {
            comment.setUsername(user.getUsername());
            String avatar = user.getAvatarUrl();
            comment.setAvatar((avatar != null && !avatar.isEmpty()) ? avatar : "/default-avatar.png");
        } else {
            comment.setUsername("用户" + userId);
            comment.setAvatar("/default-avatar.png");
        }
        
        // 从题解中获取 problemId
        OJSolution solution = solutionMapper.selectById(solutionId);
        if (solution != null) {
            comment.setProblemId(solution.getProblemId());
        }
        
        OJSolutionCommentService.PublishResult r = commentService.publish(comment);
        Map<String, Object> result = new HashMap<>();
        result.put("success", r.isSuccess());
        result.put("message", r.getMessage());
        result.put("data", r);
        return result;
    }

    @Operation(summary = "点赞/取消点赞评论")
    @PostMapping("/comments/{id}/like")
    public Map<String, Object> toggleCommentLike(@PathVariable Long id,
                                                   @RequestHeader("X-User-Id") Long userId) {
        boolean liked = commentService.toggleLike(userId, id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("liked", liked);
        return result;
    }
}
