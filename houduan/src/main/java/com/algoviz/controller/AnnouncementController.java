package com.algoviz.controller;

import com.algoviz.entity.Announcement;
import com.algoviz.mapper.AnnouncementMapper;
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
@RequestMapping("/api/announcements")
@Tag(name = "公告管理", description = "公告管理相关接口")
public class AnnouncementController {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementController.class);

    @Autowired
    private AnnouncementMapper announcementMapper;

    @GetMapping
    @Operation(summary = "获取公告列表", description = "获取所有公告（管理后台使用）")
    public Map<String, Object> getAllAnnouncements() {
        logger.info("获取公告列表");
        
        Map<String, Object> result = new HashMap<>();
        List<Announcement> announcements = announcementMapper.getAllAnnouncements();
        
        result.put("success", true);
        result.put("announcements", announcements);
        
        return result;
    }

    @GetMapping("/published")
    @Operation(summary = "获取已发布公告", description = "获取已发布的公告（前端展示使用）")
    public Map<String, Object> getPublishedAnnouncements() {
        logger.info("获取已发布公告");
        
        Map<String, Object> result = new HashMap<>();
        List<Announcement> announcements = announcementMapper.getPublishedAnnouncements();
        
        result.put("success", true);
        result.put("announcements", announcements);
        
        return result;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取公告详情", description = "根据ID获取公告详情")
    public Map<String, Object> getAnnouncementById(@PathVariable Long id) {
        logger.info("获取公告详情：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        Announcement announcement = announcementMapper.findById(String.valueOf(id));
        
        if (announcement != null) {
            result.put("success", true);
            result.put("announcement", announcement);
        } else {
            result.put("success", false);
            result.put("message", "公告不存在");
        }
        
        return result;
    }

    @PostMapping
    @Operation(summary = "创建公告", description = "创建新公告")
    public Map<String, Object> createAnnouncement(@RequestBody Map<String, String> body) {
        logger.info("创建公告");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Announcement announcement = new Announcement();
            announcement.setTitle(body.get("title"));
            announcement.setContent(body.get("content"));
            announcement.setStatus("DRAFT");
            announcement.setSortOrder(announcementMapper.getMaxSortOrder() + 1);
            announcement.setCreateTime(LocalDateTime.now());
            announcement.setUpdateTime(LocalDateTime.now());
            
            announcementMapper.insert(announcement);
            
            result.put("success", true);
            result.put("message", "公告创建成功");
            
        } catch (Exception e) {
            logger.error("创建公告失败", e);
            result.put("success", false);
            result.put("message", "创建公告失败：" + e.getMessage());
        }
        
        return result;
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新公告", description = "更新公告内容")
    public Map<String, Object> updateAnnouncement(@PathVariable Long id, @RequestBody Map<String, String> body) {
        logger.info("更新公告：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Announcement announcement = announcementMapper.findById(String.valueOf(id));
            if (announcement == null) {
                result.put("success", false);
                result.put("message", "公告不存在");
                return result;
            }
            
            announcement.setTitle(body.get("title"));
            announcement.setContent(body.get("content"));
            if (body.get("sortOrder") != null) {
                announcement.setSortOrder(Integer.parseInt(body.get("sortOrder")));
            }
            
            announcementMapper.update(announcement);
            
            result.put("success", true);
            result.put("message", "更新成功");
            
        } catch (Exception e) {
            logger.error("更新公告失败", e);
            result.put("success", false);
            result.put("message", "更新失败：" + e.getMessage());
        }
        
        return result;
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "发布公告", description = "发布公告")
    public Map<String, Object> publishAnnouncement(@PathVariable Long id) {
        logger.info("发布公告：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Announcement announcement = announcementMapper.findById(String.valueOf(id));
            if (announcement == null) {
                result.put("success", false);
                result.put("message", "公告不存在");
                return result;
            }
            
            announcementMapper.updateAnnouncementStatus(id, "PUBLISHED");
            
            result.put("success", true);
            result.put("message", "发布成功");
            
        } catch (Exception e) {
            logger.error("发布公告失败", e);
            result.put("success", false);
            result.put("message", "发布失败：" + e.getMessage());
        }
        
        return result;
    }

    @PutMapping("/{id}/unpublish")
    @Operation(summary = "取消发布", description = "取消公告发布")
    public Map<String, Object> unpublishAnnouncement(@PathVariable Long id) {
        logger.info("取消发布公告：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Announcement announcement = announcementMapper.findById(String.valueOf(id));
            if (announcement == null) {
                result.put("success", false);
                result.put("message", "公告不存在");
                return result;
            }
            
            announcementMapper.updateAnnouncementStatus(id, "DRAFT");
            
            result.put("success", true);
            result.put("message", "已取消发布");
            
        } catch (Exception e) {
            logger.error("取消发布公告失败", e);
            result.put("success", false);
            result.put("message", "取消发布失败：" + e.getMessage());
        }
        
        return result;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除公告", description = "删除公告")
    public Map<String, Object> deleteAnnouncement(@PathVariable Long id) {
        logger.info("删除公告：{}", id);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Announcement announcement = announcementMapper.findById(String.valueOf(id));
            if (announcement == null) {
                result.put("success", false);
                result.put("message", "公告不存在");
                return result;
            }
            
            announcementMapper.deleteById(String.valueOf(id));
            
            result.put("success", true);
            result.put("message", "删除成功");
            
        } catch (Exception e) {
            logger.error("删除公告失败", e);
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        
        return result;
    }
}