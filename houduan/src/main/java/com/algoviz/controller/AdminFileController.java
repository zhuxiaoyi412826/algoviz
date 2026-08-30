package com.algoviz.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.FileStorage;
import com.algoviz.mapper.FileStorageMapper;
import com.algoviz.common.util.FileUploadUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "文件管理", description = "文件上传/下载/删除/统计")
public class AdminFileController {

    @Autowired
    private FileStorageMapper fileStorageMapper;

    /**
     * 上传文件（仅后台登录管理员，Sa-Token 鉴权）
     */
    @SaCheckLogin
    @PostMapping("/file/upload")
    @Operation(summary = "上传文件", description = "上传文件并保存文件记录，返回文件 ID 与下载地址")
    public ApiResponse<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        // 上传者身份取自 Sa-Token 登录态（后台管理员），不再信任可伪造的 X-User-Id Header
        String uploaderId = String.valueOf(StpUtil.getLoginId());
        String filePath = FileUploadUtil.saveFile(file);
        String fileType = FileUploadUtil.getFileExtension(file.getOriginalFilename());

        FileStorage fileStorage = new FileStorage();
        fileStorage.setId(UUID.randomUUID().toString());
        fileStorage.setFileName(filePath.substring(filePath.lastIndexOf("/") + 1));
        fileStorage.setOriginalName(file.getOriginalFilename());
        fileStorage.setFileType(fileType);
        fileStorage.setFileSize(file.getSize());
        fileStorage.setFilePath(filePath);
        fileStorage.setStorageType("local");
        fileStorage.setUploaderId(uploaderId);
        fileStorage.setUploaderName("管理员");
        fileStorage.setCreateTime(java.time.LocalDateTime.now().toString());

        fileStorageMapper.insert(fileStorage);

        Map<String, Object> result = new HashMap<>();
        result.put("id", fileStorage.getId());
        result.put("fileName", fileStorage.getFileName());
        result.put("downloadUrl", "/api/file/download/" + fileStorage.getId());
        return ApiResponse.success(result);
    }

    /**
     * 查询文件列表（仅后台登录管理员）
     */
    @SaCheckLogin
    @GetMapping("/file/list")
    @Operation(summary = "查询文件列表", description = "分页查询已上传文件列表")
    public ApiResponse<Map<String, Object>> getFileList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (page - 1) * pageSize;
        List<FileStorage> list = fileStorageMapper.findByPage(offset, pageSize);
        int total = fileStorageMapper.count();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return ApiResponse.success(result);
    }

    /**
     * 下载文件（公开：供页面 img/附件直接访问；文件名已净化防响应头注入）
     */
    @GetMapping("/file/download/{id}")
    @Operation(summary = "下载文件", description = "根据文件 ID 下载文件内容")
    public void downloadFile(@PathVariable String id, HttpServletResponse response) throws IOException {
        FileStorage file = fileStorageMapper.findById(id);
        if (file != null) {
            java.io.File f = new java.io.File(file.getFilePath());
            if (f.exists()) {
                response.setContentType("application/octet-stream");
                // 净化文件名：去掉 CR/LF/引号等控制字符，防响应头注入（CRLF 注入 / 头分裂）
                String originalName = file.getOriginalName() == null ? "download" : file.getOriginalName();
                String safeName = originalName
                        .replaceAll("[\\r\\n\\t]", " ")
                        .replace("\"", "_");
                String encodedName = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replace("+", "%20");
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"" + safeName + "\"; filename*=UTF-8''" + encodedName);
                java.io.FileInputStream fis = new java.io.FileInputStream(f);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    response.getOutputStream().write(buffer, 0, len);
                }
                fis.close();
            }
        }
    }

    /**
     * 删除文件（仅后台登录管理员）
     */
    @SaCheckLogin
    @DeleteMapping("/file/{id}")
    @Operation(summary = "删除文件", description = "根据文件 ID 删除文件记录与物理文件")
    public ApiResponse<Void> deleteFile(@PathVariable String id) throws IOException {
        FileStorage file = fileStorageMapper.findById(id);
        if (file != null) {
            FileUploadUtil.deleteFile(file.getFilePath());
        }
        boolean success = fileStorageMapper.delete(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    /**
     * 查询文件统计（仅后台登录管理员）
     */
    @SaCheckLogin
    @GetMapping("/file/stats")
    @Operation(summary = "查询文件统计", description = "统计文件总数与占用空间")
    public ApiResponse<Map<String, Object>> getFileStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalFiles", fileStorageMapper.count());
        result.put("totalSize", fileStorageMapper.sumFileSize());
        return ApiResponse.success(result);
    }
}
