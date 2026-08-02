package com.algoviz.controller;

import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.Announcement;
import com.algoviz.entity.Feedback;
import com.algoviz.mapper.AnnouncementMapper;
import com.algoviz.mapper.FeedbackMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "扩展功能", description = "公告与反馈管理")
public class AdminExtensionController {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private FeedbackMapper feedbackMapper;

    @GetMapping("/extension/announcement")
    public ApiResponse<Map<String, Object>> getAnnouncementList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Announcement> list = announcementMapper.findByPage(offset, pageSize);
        int total = announcementMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @PostMapping("/extension/announcement")
    public ApiResponse<Announcement> createAnnouncement(@RequestBody Announcement announcement) {
        announcement.setStatus("draft");
        announcementMapper.insert(announcement);
        return ApiResponse.success(announcement);
    }

    @PutMapping("/extension/announcement/{id}")
    public ApiResponse<Announcement> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        announcementMapper.update(announcement);
        return ApiResponse.success(announcementMapper.findById(String.valueOf(id)));
    }

    @DeleteMapping("/extension/announcement/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        boolean success = announcementMapper.deleteById(String.valueOf(id)) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    @GetMapping("/extension/feedback")
    public ApiResponse<Map<String, Object>> getFeedbackList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Feedback> list = feedbackMapper.findByPage(offset, pageSize);
        int total = feedbackMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    @GetMapping("/extension/feedback/{id}")
    public ApiResponse<Feedback> getFeedbackById(@PathVariable String id) {
        return ApiResponse.success(feedbackMapper.findById(id));
    }

    @PutMapping("/extension/feedback/{id}")
    public ApiResponse<Feedback> updateFeedback(@PathVariable Long id, @RequestBody Feedback feedback) {
        feedback.setId(id);
        feedbackMapper.update(feedback);
        return ApiResponse.success(feedbackMapper.findById(String.valueOf(id)));
    }

    @DeleteMapping("/extension/feedback/{id}")
    public ApiResponse<Void> deleteFeedback(@PathVariable String id) {
        boolean success = feedbackMapper.deleteById(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }
}