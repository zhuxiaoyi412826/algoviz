package com.algoviz.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 敏感词（工作区） */
@Data
public class SensitiveWord {
    private Long id;
    private String word;
    private String category;   // ABUSE/POLITICS/ADVERTISING/PORN/OTHER
    private String level;      // HIGH拦截 / MEDIUM待审 / LOW仅记录
    private String matchMode;  // EXACT/FUZZY
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
