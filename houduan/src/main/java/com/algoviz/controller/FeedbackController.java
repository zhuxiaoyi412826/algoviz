package com.algoviz.controller;

import com.algoviz.entity.Feedback;
import com.algoviz.mapper.FeedbackMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "反馈管理", description = "用户反馈相关接口")
@CrossOrigin(origins = "*")
public class FeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);

    @Autowired
    private FeedbackMapper feedbackMapper;

    @GetMapping
    @Operation(summary = "获取反馈列表", description = "获取所有反馈（管理后台使用）")
    public Map<String, Object> getAllFeedbacks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        
        logger.info("获取反馈列表 - 状态: {}, 类型: {}", status, type);
        
        Map<String, Object> result = new HashMap<>();
        List<Feedback> feedbacks;

        if (status != null && !status.isEmpty()) {
            feedbacks = feedbackMapper.getFeedbacksByStatus(status);
        } else if (type != null && !type.isEmpty()) {
            feedbacks = feedbackMapper.getFeedbacksByType(type);
        } else {
            feedbacks = feedbackMapper.getAllFeedbacks();
        }

        result.put("success", true);
        result.put("feedbacks", feedbacks);
        result.put("count", feedbacks.size());
        
        return result;
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户反馈", description = "根据用户ID获取反馈列表")
    public Map<String, Object> getFeedbacksByUserId(@PathVariable Long userId) {
        logger.info("获取用户反馈：{}", userId);
        
        Map<String, Object> result = new HashMap<>();
        List<Feedback> feedbacks = feedbackMapper.getFeedbacksByUserId(userId);
        
        result.put("success", true);
        result.put("feedbacks", feedbacks);
        
        return result;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取反馈详情", description = "根据ID获取反馈详情")
    public Map<String, Object> getFeedbackById(@PathVariable Long id) {
        logger.info("获取反馈详情：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        Feedback feedback = feedbackMapper.findById(String.valueOf(id));
        
        if (feedback != null) {
            result.put("success", true);
            result.put("feedback", feedback);
        } else {
            result.put("success", false);
            result.put("message", "反馈不存在");
        }
        
        return result;
    }

    @PostMapping
    @Operation(summary = "提交反馈", description = "用户提交反馈")
    public Map<String, Object> submitFeedback(@RequestBody Map<String, String> body) {
        logger.info("用户提交反馈");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Feedback feedback = new Feedback();
            feedback.setUserId(body.get("userId") != null ? Long.parseLong(body.get("userId")) : null);
            feedback.setUsername(body.get("username"));
            feedback.setEmail(body.get("email"));
            feedback.setType(body.get("type"));
            feedback.setTitle(body.get("title"));
            feedback.setContent(body.get("content"));
            feedback.setStatus("PENDING");
            feedback.setCreateTime(LocalDateTime.now());
            feedback.setUpdateTime(LocalDateTime.now());
            
            feedbackMapper.insert(feedback);
            
            result.put("success", true);
            result.put("message", "反馈提交成功");
            
        } catch (Exception e) {
            logger.error("提交反馈失败", e);
            result.put("success", false);
            result.put("message", "提交反馈失败：" + e.getMessage());
        }
        
        return result;
    }

    @PutMapping("/{id}/reply")
    @Operation(summary = "回复反馈", description = "管理员回复反馈")
    public Map<String, Object> replyFeedback(@PathVariable Long id, @RequestBody Map<String, String> body) {
        logger.info("回复反馈：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        String reply = body.get("reply");
        
        try {
            Feedback feedback = feedbackMapper.findById(String.valueOf(id));
            if (feedback == null) {
                result.put("success", false);
                result.put("message", "反馈不存在");
                return result;
            }
            
            feedbackMapper.updateFeedbackReply(id, reply, "REPLIED");
            
            result.put("success", true);
            result.put("message", "回复成功");
            
        } catch (Exception e) {
            logger.error("回复反馈失败", e);
            result.put("success", false);
            result.put("message", "回复失败：" + e.getMessage());
        }
        
        return result;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除反馈", description = "管理员删除反馈")
    public Map<String, Object> deleteFeedback(@PathVariable Long id) {
        logger.info("删除反馈：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Feedback feedback = feedbackMapper.findById(String.valueOf(id));
            if (feedback == null) {
                result.put("success", false);
                result.put("message", "反馈不存在");
                return result;
            }
            
            feedbackMapper.deleteById(String.valueOf(id));
            
            result.put("success", true);
            result.put("message", "删除成功");
            
        } catch (Exception e) {
            logger.error("删除反馈失败", e);
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/stats")
    @Operation(summary = "反馈统计", description = "获取反馈统计信息")
    public Map<String, Object> getFeedbackStats() {
        logger.info("获取反馈统计");
        
        Map<String, Object> result = new HashMap<>();
        
        int pendingCount = feedbackMapper.countByStatus("PENDING");
        int repliedCount = feedbackMapper.countByStatus("REPLIED");
        
        result.put("success", true);
        result.put("pendingCount", pendingCount);
        result.put("repliedCount", repliedCount);
        
        return result;
    }
}