package com.algoviz.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 内容审核记录（人工审核完毕 / BLOCK 拦截 后落库） */
@Data
public class ContentAuditRecord {
    private Long id;
    private String submitId;
    private Long userId;
    private Long problemId;
    private String problemNo;
    private String contentType;      // QUESTION/CODE/COMMENT
    private String language;
    private String riskLevel;        // HIGH/MEDIUM/LOW/NONE
    private String hitDetails;       // JSON
    private Integer totalScore;
    private String contentSnapshot;
    private String preCheckStatus;   // BLOCK/PASS
    private String auditStatus;      // pending/pass/reject/blocked/logonly
    private String auditRemark;
    private String auditorId;
    private LocalDateTime auditTime;
    private String esDocId;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;
}
