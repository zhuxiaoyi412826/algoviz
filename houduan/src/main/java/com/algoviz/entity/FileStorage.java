package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "文件存储实体")
public class FileStorage {
    @Schema(description = "主键ID")
    private String id;
    @Schema(description = "文件名")
    private String fileName;
    @Schema(description = "原始文件名")
    private String originalName;
    @Schema(description = "文件类型")
    private String fileType;
    @Schema(description = "文件大小")
    private Long fileSize;
    @Schema(description = "文件路径")
    private String filePath;
    @Schema(description = "存储类型")
    private String storageType;
    @Schema(description = "存储桶名称")
    private String bucketName;
    @Schema(description = "下载链接")
    private String downloadUrl;
    @Schema(description = "上传者ID")
    private String uploaderId;
    @Schema(description = "上传者名称")
    private String uploaderName;
    @Schema(description = "创建时间")
    private String createTime;

    public FileStorage() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    public String getBucketName() { return bucketName; }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getUploaderId() { return uploaderId; }
    public void setUploaderId(String uploaderId) { this.uploaderId = uploaderId; }
    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}