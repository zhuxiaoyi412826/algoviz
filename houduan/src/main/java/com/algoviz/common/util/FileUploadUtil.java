package com.algoviz.common.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileUploadUtil {
    private static final String UPLOAD_DIR = "./uploads";
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".pdf", ".doc", ".docx", ".txt", ".json", ".csv"};
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    static {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        if (!isAllowedExtension(extension)) {
            throw new IllegalArgumentException("不允许上传此类型的文件");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过50MB");
        }

        // 魔数校验：校验文件头字节与扩展名是否匹配，防止伪装上传可执行脚本
        byte[] bytes = file.getBytes();
        if (!isValidMagicNumber(extension, bytes)) {
            throw new IllegalArgumentException("文件内容与扩展名不匹配（魔数校验失败）");
        }

        String newFileName = UUID.randomUUID().toString() + extension;
        Path targetPath = Paths.get(UPLOAD_DIR, newFileName);

        Files.write(targetPath, bytes);

        return targetPath.toString();
    }

    /**
     * 魔数校验：根据扩展名校验文件头字节，防止攻击者把可执行内容伪装成图片/文档上传
     */
    private static boolean isValidMagicNumber(String ext, byte[] b) {
        if (b == null || b.length < 4) {
            return true; // 过小文件不做魔数强校验
        }
        int b0 = b[0] & 0xFF;
        int b1 = b[1] & 0xFF;
        int b2 = b[2] & 0xFF;
        int b3 = b[3] & 0xFF;
        switch (ext == null ? "" : ext.toLowerCase()) {
            case ".jpg":
            case ".jpeg":
                return b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF;
            case ".png":
                return b0 == 0x89 && b1 == 'P' && b2 == 'N' && b3 == 'G';
            case ".gif":
                return b0 == 'G' && b1 == 'I' && b2 == 'F' && b3 == '8';
            case ".pdf":
                return b0 == '%' && b1 == 'P' && b2 == 'D' && b3 == 'F';
            case ".doc":
                return b0 == 0xD0 && b1 == 0xCF && b2 == 0x11 && b3 == 0xE0; // OLE 复合文档
            case ".docx":
                return b0 == 'P' && b1 == 'K'; // zip 容器（OOXML）
            case ".txt":
            case ".json":
            case ".csv":
                return true; // 纯文本类型不做魔数强校验（内容风险由业务侧兜底）
            default:
                return true;
        }
    }

    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public static boolean isAllowedExtension(String extension) {
        if (extension == null) {
            return false;
        }
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    public static void deleteFile(String filePath) throws IOException {
        if (filePath != null && !filePath.isEmpty()) {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
            }
        }
    }

    public static long getFileSize(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            return Files.size(path);
        }
        return 0;
    }
}
