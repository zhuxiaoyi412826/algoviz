package com.algoviz.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** 审计日志条目（写入 ES，由 Fluentd 采集；也作为待审核队列元素） */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditLogEntry {
    private String submitId;
    private Long userId;
    private Long problemId;
    private String problemNo;
    private String contentType;    // QUESTION/CODE/COMMENT
    private String language;
    private String title;
    private String content;        // 内容快照（截断）
    private String riskLevel;
    private int totalScore;
    private List<DetectResult.HitDetail> hitDetails;
    private String preCheck;       // BLOCK/PASS
    private String auditStatus;    // blocked/pending/logonly
    private String submitTime;     // yyyy-MM-dd HH:mm:ss
    // 由 ES 拉取时回填
    private String esIndex;
    private String esDocId;
    private String logTime;
}
