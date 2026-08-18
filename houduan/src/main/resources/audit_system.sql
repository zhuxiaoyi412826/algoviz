
-- 关键词屏蔽审核系统建表脚本
-- 依赖: MySQL 8.0 + algoviz 库

-- 1. 敏感词表（工作区，可增删改；发布版本后快照冻结）
CREATE TABLE IF NOT EXISTS sensitive_word (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    word        VARCHAR(128) NOT NULL COMMENT '敏感词',
    category    VARCHAR(32)  NOT NULL DEFAULT 'ABUSE' COMMENT '分类: ABUSE辱骂/POLITICS涉政/ADVERTISING广告/PORN色情/OTHER',
    LEVEL       VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM' COMMENT '等级: HIGH拦截/MEDIUM待审/LOW仅记录',
    match_mode  VARCHAR(16)  NOT NULL DEFAULT 'EXACT' COMMENT '匹配模式: EXACT/FUZZY',
    enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_word (word)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词库（工作区）';

-- 2. 敏感词版本表（每次"发布版本"将当前词库快照冻结存档，支持回滚）
CREATE TABLE IF NOT EXISTS sensitive_word_version (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_no    INT          NOT NULL COMMENT '版本号（递增）',
    word_count    INT          NOT NULL DEFAULT 0 COMMENT '该版本词条数',
    snapshot_json LONGTEXT     COMMENT '词库快照 JSON [{word,category,level}]',
    remark        VARCHAR(256) COMMENT '版本说明',
    created_by    VARCHAR(64)  COMMENT '发布人',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_version (version_no)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词版本快照';

-- 3. 危险代码规则表（正则规则，检测提交代码中的危害模式）
CREATE TABLE IF NOT EXISTS dangerous_code_rule (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code    VARCHAR(64)  NOT NULL COMMENT '规则编码',
    rule_name    VARCHAR(128) COMMENT '规则名称',
    LANGUAGE     VARCHAR(32)  NOT NULL DEFAULT 'ALL' COMMENT '适用语言: ALL/JAVA/PYTHON/JS/CPP',
    rule_type    VARCHAR(32)  NOT NULL DEFAULT 'REGEX' COMMENT '规则类型: REGEX/KEYWORD',
    rule_content TEXT         NOT NULL COMMENT '规则内容（正则或关键词）',
    risk_level   VARCHAR(16)  NOT NULL DEFAULT 'HIGH' COMMENT '风险等级: HIGH/MEDIUM/LOW',
    score        INT          NOT NULL DEFAULT 80 COMMENT '风险分值',
    enabled      TINYINT      NOT NULL DEFAULT 1,
    DESCRIPTION  VARCHAR(256),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rule_code (rule_code)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='危险代码检测规则';

-- 4. 内容审核记录表（人工审核完毕后写入；BLOCK 拦截记录也落此表）
CREATE TABLE IF NOT EXISTS content_audit_record (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    submit_id        VARCHAR(64)  NOT NULL COMMENT '提交唯一标识',
    user_id          BIGINT       COMMENT '提交用户ID',
    problem_id       BIGINT       COMMENT '关联题目ID',
    problem_no       VARCHAR(64)  COMMENT '关联题目编号',
    content_type     VARCHAR(32)  COMMENT '内容类型: QUESTION/CODE/COMMENT',
    LANGUAGE         VARCHAR(32),
    risk_level       VARCHAR(16)  COMMENT 'HIGH/MEDIUM/LOW/NONE',
    hit_details      JSON         COMMENT '命中详情',
    total_score      INT          DEFAULT 0,
    content_snapshot TEXT         COMMENT '内容快照（截断）',
    pre_check_status VARCHAR(16)  COMMENT 'BLOCK/PASS',
    audit_status     VARCHAR(16)  COMMENT 'pending/pass/reject/blocked/logonly',
    audit_remark     VARCHAR(512),
    auditor_id       VARCHAR(64),
    audit_time       DATETIME,
    es_doc_id        VARCHAR(64)  COMMENT '来源 ES 文档ID',
    submit_time      DATETIME,
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_submit_id (submit_id),
    KEY idx_audit_status (audit_status),
    KEY idx_submit_time (submit_time)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核记录';

-- 敏感词（示例库：广告/违规类，等级含义 HIGH=拦截 MEDIUM=待审 LOW=仅记录）
INSERT INTO sensitive_word (word, category, LEVEL) VALUES
('赌博', 'OTHER', 'HIGH'),
('博彩', 'OTHER', 'HIGH'),
('代考', 'ADVERTISING', 'HIGH'),
('代写作业', 'ADVERTISING', 'HIGH'),
('刷单', 'ADVERTISING', 'MEDIUM'),
('外挂', 'OTHER', 'MEDIUM'),
('翻墙', 'OTHER', 'MEDIUM'),
('发票代开', 'ADVERTISING', 'MEDIUM'),
('加微信', 'ADVERTISING', 'LOW'),
('私聊接单', 'ADVERTISING', 'LOW')
ON DUPLICATE KEY UPDATE update_time = update_time;

-- 危险代码规则
INSERT INTO dangerous_code_rule (rule_code, rule_name, LANGUAGE, rule_type, rule_content, risk_level, score, DESCRIPTION) VALUES
('DC-001', 'Java命令执行', 'JAVA', 'REGEX', 'Runtime\\.getRuntime\\(\\)\\.exec', 'HIGH', 95, '检测 Java 执行系统命令'),
('DC-002', 'Java进程构建', 'JAVA', 'REGEX', 'new\\s+ProcessBuilder', 'HIGH', 90, '检测 Java 进程构建器'),
('DC-003', 'Python命令执行', 'PYTHON', 'REGEX', 'os\\.(system|popen)\\s*\\(', 'HIGH', 95, '检测 Python 执行系统命令'),
('DC-004', 'eval动态执行', 'ALL', 'REGEX', '\\beval\\s*\\(', 'HIGH', 85, '检测 eval 动态代码执行'),
('DC-005', '删除根目录', 'ALL', 'REGEX', 'rm\\s+-rf\\s+/', 'HIGH', 100, '检测 rm -rf / 破坏性命令'),
('DC-006', 'Python删目录', 'PYTHON', 'REGEX', 'shutil\\.rmtree', 'HIGH', 85, '检测递归删除目录'),
('DC-007', '反向Shell', 'ALL', 'REGEX', '/bin/(ba)?sh\\s+-i', 'HIGH', 95, '检测反弹Shell特征'),
('DC-008', '死循环攻击', 'ALL', 'REGEX', 'while\\s*\\(\\s*true\\s*\\)\\s*\\{\\s*\\}', 'MEDIUM', 60, '检测空死循环占用CPU'),
('DC-009', '端口扫描', 'ALL', 'REGEX', '(port|PORT)\\s*(_?)+\\d{2,5}.*for.*range', 'MEDIUM', 70, '疑似端口扫描循环'),
('DC-010', '矿池地址', 'ALL', 'REGEX', '(stratum\\+tcp|miner|矿池)', 'HIGH', 90, '挖矿特征')
ON DUPLICATE KEY UPDATE update_time = update_time;
