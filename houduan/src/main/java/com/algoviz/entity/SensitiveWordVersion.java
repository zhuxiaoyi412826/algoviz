package com.algoviz.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 敏感词版本快照（发布后冻结，可回滚） */
@Data
public class SensitiveWordVersion {
    private Long id;
    private Integer versionNo;
    private Integer wordCount;
    private String snapshotJson;
    private String remark;
    private String createdBy;
    private LocalDateTime createTime;
}
