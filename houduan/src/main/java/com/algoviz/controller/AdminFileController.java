package com.algoviz.controller;

import com.algoviz.dto.ApiResponse;
import com.algoviz.entity.FileStorage;
import com.algoviz.mapper.FileStorageMapper;
import com.algoviz.util.FileUploadUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @PostMapping("/file/upload")
    public ApiResponse<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                       @RequestHeader(value = "X-User-Id", required = false) String userId) throws IOException {
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
        fileStorage.setUploaderId(userId != null ? userId : "admin");
        fileStorage.setUploaderName(userId != null ? "用户" : "管理员");
        fileStorage.setCreateTime(java.time.LocalDateTime.now().toString());

        fileStorageMapper.insert(fileStorage);

        Map<String, Object> result = new HashMap<>();
        result.put("id", fileStorage.getId());
        result.put("fileName", fileStorage.getFileName());
        result.put("downloadUrl", "/api/file/download/" + fileStorage.getId());
        return ApiResponse.success(result);
    }

    @GetMapping("/file/list")
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

    @GetMapping("/file/download/{id}")
    public void downloadFile(@PathVariable String id, HttpServletResponse response) throws IOException {
        FileStorage file = fileStorageMapper.findById(id);
        if (file != null) {
            java.io.File f = new java.io.File(file.getFilePath());
            if (f.exists()) {
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=" +
                        new String(file.getOriginalName().getBytes("UTF-8"), "ISO-8859-1"));
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

    @DeleteMapping("/file/{id}")
    public ApiResponse<Void> deleteFile(@PathVariable String id) throws IOException {
        FileStorage file = fileStorageMapper.findById(id);
        if (file != null) {
            FileUploadUtil.deleteFile(file.getFilePath());
        }
        boolean success = fileStorageMapper.delete(id) > 0;
        return success ? ApiResponse.success(null) : ApiResponse.error("删除失败");
    }

    @GetMapping("/file/stats")
    public ApiResponse<Map<String, Object>> getFileStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalFiles", fileStorageMapper.count());
        result.put("totalSize", fileStorageMapper.sumFileSize());
        return ApiResponse.success(result);
    }
}