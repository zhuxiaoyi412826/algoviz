package com.algoviz.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 危险代码检测规则 */
@Data
public class DangerousCodeRule {
    private Long id;
    private String ruleCode;
    private String ruleName;
    private String language;    // ALL/JAVA/PYTHON/JS/CPP
    private String ruleType;    // REGEX/KEYWORD
    private String ruleContent;
    private String riskLevel;   // HIGH/MEDIUM/LOW
    private Integer score;
    private Integer enabled;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
