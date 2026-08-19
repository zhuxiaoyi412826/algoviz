-- =====================================================================
-- OJ 题解 + 评论 + 点赞 系统表
-- 方案A：复用审核链路（AuditDetectService + DFA + Fluentd→ES→Redis）
-- =====================================================================

-- 1. 用户题解表
DROP TABLE IF EXISTS `oj_solution`;
CREATE TABLE `oj_solution` (
    `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `problem_id`       BIGINT         NOT NULL COMMENT '题目ID',
    `problem_title`    VARCHAR(200)   DEFAULT NULL COMMENT '题目标题冗余',
    `user_id`          BIGINT         NOT NULL COMMENT '发布用户ID',
    `username`         VARCHAR(100)   DEFAULT NULL COMMENT '用户名冗余',
    `avatar`           VARCHAR(500)   DEFAULT NULL COMMENT '头像冗余',

    `title`            VARCHAR(200)   NOT NULL COMMENT '题解标题',
    `format`           VARCHAR(100)   DEFAULT NULL COMMENT '解题格式（双指针/DP/回溯等）',
    `idea`             MEDIUMTEXT     COMMENT '思路',
    `process`          MEDIUMTEXT     COMMENT '解题过程',
    `complexity`       VARCHAR(500)   DEFAULT NULL COMMENT '复杂度分析',
    `code_lang`        VARCHAR(50)    DEFAULT 'java' COMMENT '代码语言',
    `code`             MEDIUMTEXT     COMMENT '题解代码',

    `like_count`       INT            DEFAULT 0 COMMENT '点赞数',
    `view_count`       INT            DEFAULT 0 COMMENT '观看数',
    `comment_count`    INT            DEFAULT 0 COMMENT '评论数',

    `is_passed`        TINYINT(1)     DEFAULT 0 COMMENT '是否已AC该题',
    `is_featured`      TINYINT(1)     DEFAULT 0 COMMENT '精选置顶',

    `audit_status`     VARCHAR(20)    DEFAULT 'none' COMMENT '审核状态 none/pending/blocked/passed/rejected',
    `risk_level`       VARCHAR(10)    DEFAULT 'NONE' COMMENT '风险等级 NONE/LOW/MEDIUM/HIGH',
    `detect_summary`   VARCHAR(1000)  DEFAULT NULL COMMENT '检测命中摘要',

    `status`           VARCHAR(20)    DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/HIDDEN/DELETED',
    `created_at`       DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_solution_problem` (`problem_id`, `status`, `created_at` DESC),
    KEY `idx_solution_user` (`user_id`, `created_at` DESC),
    KEY `idx_solution_audit` (`audit_status`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ用户题解表';

-- 2. 题解评论表（多级评论 parent_id + root_id）
DROP TABLE IF EXISTS `oj_solution_comment`;
CREATE TABLE `oj_solution_comment` (
    `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `solution_id`      BIGINT         NOT NULL COMMENT '题解ID',
    `problem_id`       BIGINT         DEFAULT NULL COMMENT '题目ID冗余（后台按题目筛选）',
    `user_id`          BIGINT         NOT NULL COMMENT '评论用户ID',
    `username`         VARCHAR(100)   DEFAULT NULL COMMENT '用户名冗余',
    `avatar`           VARCHAR(500)   DEFAULT NULL COMMENT '头像冗余',

    `parent_id`        BIGINT         DEFAULT 0 COMMENT '父评论ID，0=顶层评论',
    `root_id`          BIGINT         DEFAULT 0 COMMENT '顶层评论ID（子评论同属一个root）',
    `reply_to_user_id`   BIGINT       DEFAULT NULL COMMENT '回复目标用户ID',
    `reply_to_username`  VARCHAR(100) DEFAULT NULL COMMENT '回复目标用户名',

    `content`          MEDIUMTEXT     NOT NULL COMMENT '评论正文',

    `like_count`       INT            DEFAULT 0 COMMENT '点赞数',

    `audit_status`     VARCHAR(20)    DEFAULT 'none' COMMENT '审核状态',
    `risk_level`       VARCHAR(10)    DEFAULT 'NONE' COMMENT '风险等级',
    `detect_summary`   VARCHAR(1000)  DEFAULT NULL COMMENT '检测命中摘要',

    `status`           VARCHAR(20)    DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/HIDDEN/DELETED',
    `created_at`       DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_comment_solution` (`solution_id`, `root_id`, `created_at`),
    KEY `idx_comment_problem` (`problem_id`),
    KEY `idx_comment_user` (`user_id`),
    KEY `idx_comment_audit` (`audit_status`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ题解评论表';

-- 3. 点赞去重表
DROP TABLE IF EXISTS `oj_solution_like`;
CREATE TABLE `oj_solution_like` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL COMMENT '点赞用户ID',
    `target_type`   VARCHAR(20)  NOT NULL COMMENT 'SOLUTION/COMMENT',
    `target_id`     BIGINT       NOT NULL COMMENT '目标ID',
    `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ题解/评论点赞去重表';
