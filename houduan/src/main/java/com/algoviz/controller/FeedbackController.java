package com.algoviz.controller;

import com.algoviz.entity.Feedback;
import com.algoviz.mapper.FeedbackMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "反馈管理", description = "用户反馈相关接口")
@CrossOrigin(origins = "*")
public class FeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);

    /** 单张图片最大限制 2MB */
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024L;
    /** 支持的图片后缀 */
    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    ));
    /** 多张图片拼接分隔符 (二进制下的 0x00 0x01 0x02 0x03 0x00 + 4字节长度)，
     *  为简化实现，使用 JSON 存储格式: [len1(4B)][data1][len2(4B)][data2]... */
    private static final byte[] MAGIC_HEADER = new byte[]{(byte)0xFA, (byte)0xFB, (byte)0xFC, (byte)0xFD};

    @Autowired
    private FeedbackMapper feedbackMapper;

    @GetMapping
    @Operation(summary = "获取反馈列表", description = "获取所有反馈（管理后台使用，列表不返回图片二进制）")
    public Map<String, Object> getAllFeedbacks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {

        logger.info("获取反馈列表 - 状态: {}, 类型: {}", status, type);

        Map<String, Object> result = new HashMap<>();
        List<Feedback> feedbacks;

        if (status != null && !status.isEmpty()) {
            feedbacks = feedbackMapper.getFeedbacksByStatus(status.toUpperCase());
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
    @Operation(summary = "获取反馈详情", description = "根据ID获取反馈详情（含图片二进制）")
    public Map<String, Object> getFeedbackById(@PathVariable Long id) {
        logger.info("获取反馈详情：{}", id);

        Map<String, Object> result = new HashMap<>();
        Feedback feedback = feedbackMapper.findById(String.valueOf(id));

        if (feedback != null) {
            // 详情不直接返回二进制大字段，images 置空；前端走专用图片接口
            feedback.setImages(null);
            result.put("success", true);
            result.put("feedback", feedback);
        } else {
            result.put("success", false);
            result.put("message", "反馈不存在");
        }

        return result;
    }

    /**
     * 图片接口：返回某条反馈的第 idx 张图片
     * GET /api/feedback/{id}/image/{idx}   idx 从 0 开始
     */
    @GetMapping("/{id}/image/{idx}")
    @Operation(summary = "获取反馈图片", description = "根据反馈ID和图片序号(从0开始)返回单张图片二进制")
    public ResponseEntity<byte[]> getFeedbackImage(
            @PathVariable Long id,
            @PathVariable Integer idx) {
        Feedback feedback = feedbackMapper.findById(String.valueOf(id));
        if (feedback == null || feedback.getImages() == null) {
            return ResponseEntity.notFound().build();
        }
        List<byte[]> imgs = decodeImages(feedback.getImages());
        if (idx < 0 || idx >= imgs.size()) {
            return ResponseEntity.notFound().build();
        }
        byte[] data = imgs.get(idx);
        String contentType = guessContentType(data);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(data.length);
        headers.setCacheControl("public, max-age=86400");
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    /**
     * 图片接口：获取全部图片的数量与访问 URL 列表
     * GET /api/feedback/{id}/images
     */
    @GetMapping("/{id}/images")
    @Operation(summary = "获取反馈图片列表", description = "返回图片数量及每一张的访问URL")
    public Map<String, Object> getFeedbackImages(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        Feedback feedback = feedbackMapper.findById(String.valueOf(id));
        if (feedback == null) {
            result.put("success", false);
            result.put("message", "反馈不存在");
            return result;
        }
        int count = feedback.getImageCount() != null ? feedback.getImageCount() : 0;
        List<String> urls = new ArrayList<>();
        if (feedback.getImages() != null && count == 0) {
            count = decodeImages(feedback.getImages()).size();
        }
        for (int i = 0; i < count; i++) {
            urls.add("/api/feedback/" + id + "/image/" + i);
        }
        result.put("success", true);
        result.put("count", count);
        result.put("urls", urls);
        return result;
    }

    /**
     * 原 JSON 提交（无图片版本，保持向后兼容）
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "提交反馈(JSON)", description = "用户提交纯文本反馈，不包含图片")
    public Map<String, Object> submitFeedbackJson(@RequestBody Map<String, String> body) {
        return doSubmit(body, null);
    }

    /**
     * 多媒体表单提交（支持图片上传）
     * POST /api/feedback  Content-Type: multipart/form-data
     * 字段: userId / username / email / type / title / content / files(多个)
     * 约束：单张图片 <= 2MB，后缀必须是图片格式
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "提交反馈(含图片)", description = "multipart/form-data 提交，可附带多张图片；单张图片不得超过 2MB")
    public Map<String, Object> submitFeedbackWithFiles(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        Map<String, String> body = new HashMap<>();
        body.put("userId", userId);
        body.put("username", username);
        body.put("email", email);
        body.put("type", type);
        body.put("title", title);
        body.put("content", content);

        return doSubmit(body, files);
    }

    private Map<String, Object> doSubmit(Map<String, String> body, List<MultipartFile> files) {
        logger.info("用户提交反馈，含图片? {}", files != null && !files.isEmpty());

        Map<String, Object> result = new HashMap<>();

        try {
            Feedback feedback = new Feedback();
            feedback.setUserId(body.get("userId") != null && !body.get("userId").isEmpty()
                    ? Long.parseLong(body.get("userId")) : null);
            feedback.setUsername(body.get("username"));
            feedback.setEmail(body.get("email"));
            feedback.setType(body.get("type"));
            feedback.setTitle(body.get("title"));
            feedback.setContent(body.get("content"));
            feedback.setStatus("PENDING");
            feedback.setCreateTime(LocalDateTime.now());
            feedback.setUpdateTime(LocalDateTime.now());

            // ---- 处理图片 ----
            if (files != null && !files.isEmpty()) {
                // 1. 校验单张大小
                for (MultipartFile f : files) {
                    if (f == null || f.isEmpty()) continue;
                    if (f.getSize() > MAX_IMAGE_SIZE) {
                        result.put("success", false);
                        result.put("message", "图片【" + f.getOriginalFilename() + "】超过 2MB，请压缩后再上传");
                        return result;
                    }
                    String ext = getFileExt(f.getOriginalFilename());
                    if (!ALLOWED_EXT.contains(ext.toLowerCase())) {
                        result.put("success", false);
                        result.put("message", "图片【" + f.getOriginalFilename() + "】格式不支持，仅允许 " + ALLOWED_EXT);
                        return result;
                    }
                }
                // 2. 读取并编码存储
                List<byte[]> imgList = new ArrayList<>();
                for (MultipartFile f : files) {
                    if (f == null || f.isEmpty()) continue;
                    imgList.add(f.getBytes());
                }
                if (!imgList.isEmpty()) {
                    feedback.setImages(encodeImages(imgList));
                    feedback.setImageCount(imgList.size());
                }
            } else {
                feedback.setImageCount(0);
            }

            feedbackMapper.insert(feedback);

            result.put("success", true);
            result.put("message", "反馈提交成功");
            result.put("id", feedback.getId());
            if (feedback.getImageCount() != null) {
                result.put("imageCount", feedback.getImageCount());
            }

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

    // ================================================================
    // 工具方法
    // ================================================================

    private static String getFileExt(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1) : "";
    }

    private static String guessContentType(byte[] data) {
        if (data == null || data.length < 4) return "image/png";
        if (data.length >= 8
                && data[0] == (byte)0x89 && data[1] == (byte)0x50
                && data[2] == (byte)0x4E && data[3] == (byte)0x47) {
            return "image/png";
        }
        if (data.length >= 3
                && data[0] == (byte)0xFF && data[1] == (byte)0xD8 && data[2] == (byte)0xFF) {
            return "image/jpeg";
        }
        if (data.length >= 6
                && data[0] == (byte)0x47 && data[1] == (byte)0x49 && data[2] == (byte)0x46) {
            return "image/gif";
        }
        if (data.length >= 12
                && (data[0] == (byte)0x52 && data[1] == (byte)0x49
                    && data[2] == (byte)0x46 && data[3] == (byte)0x46)
                && (data[8] == (byte)0x57 && data[9] == (byte)0x45
                    && data[10] == (byte)0x42 && data[11] == (byte)0x50)) {
            return "image/webp";
        }
        if (data.length >= 2
                && data[0] == (byte)0x42 && data[1] == (byte)0x4D) {
            return "image/bmp";
        }
        return "application/octet-stream";
    }

    /**
     * 多张图片编码为单个 byte[]，格式：
     *   [MAGIC_HEADER 4B]
     *   [len1 4B BIG-ENDIAN][data1]
     *   [len2 4B BIG-ENDIAN][data2] ...
     */
    private static byte[] encodeImages(List<byte[]> images) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(MAGIC_HEADER);
        for (byte[] img : images) {
            int len = img.length;
            bos.write((len >>> 24) & 0xFF);
            bos.write((len >>> 16) & 0xFF);
            bos.write((len >>> 8)  & 0xFF);
            bos.write( len         & 0xFF);
            bos.write(img);
        }
        return bos.toByteArray();
    }

    private static List<byte[]> decodeImages(byte[] data) {
        List<byte[]> result = new ArrayList<>();
        if (data == null || data.length < MAGIC_HEADER.length) return result;
        // 检查 MAGIC
        for (int i = 0; i < MAGIC_HEADER.length; i++) {
            if (data[i] != MAGIC_HEADER[i]) return result;
        }
        int pos = MAGIC_HEADER.length;
        while (pos + 4 <= data.length) {
            int len = ((data[pos] & 0xFF) << 24)
                    | ((data[pos+1] & 0xFF) << 16)
                    | ((data[pos+2] & 0xFF) << 8)
                    |  (data[pos+3] & 0xFF);
            pos += 4;
            if (len < 0 || pos + len > data.length) break;
            byte[] img = Arrays.copyOfRange(data, pos, pos + len);
            result.add(img);
            pos += len;
        }
        return result;
    }
}
