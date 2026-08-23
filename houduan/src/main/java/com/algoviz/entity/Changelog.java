package com.algoviz.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 更新日志实体
 * 版本号 / 更新类型 / 摘要 / 发布日期 / 功能模块(JSON数组) / 详情(Markdown) / 已知问题 / 发布状态
 */
@Schema(description = "更新日志")
public class Changelog {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "版本号，如 v1.2.3")
    private String version;

    @Schema(description = "更新类型：new=新增 optimize=优化 fix=修复 urgent=紧急更新")
    private String type;

    @Schema(description = "更新摘要（卡片顶部一句话）")
    private String summary;

    @Schema(description = "发布日期（YYYY-MM-DD）")
    private LocalDate releaseDate;

    @Schema(description = "功能模块标签（JSON 字符串数组，例 [\"个人中心\",\"商品\"]）")
    private String modules;

    @Schema(description = "完整更新说明，Markdown 格式")
    private String details;

    @Schema(description = "已知问题 / 注意事项，Markdown 格式，可为空")
    private String knownIssues;

    @Schema(description = "红色框标题：已知问题 / 注意事项")
    private String issuesTitle;

    @Schema(description = "发布状态：0=草稿 1=已发布")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public Changelog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public String getModules() { return modules; }
    public void setModules(String modules) { this.modules = modules; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getKnownIssues() { return knownIssues; }
    public void setKnownIssues(String knownIssues) { this.knownIssues = knownIssues; }
    public String getIssuesTitle() { return issuesTitle; }
    public void setIssuesTitle(String issuesTitle) { this.issuesTitle = issuesTitle; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
