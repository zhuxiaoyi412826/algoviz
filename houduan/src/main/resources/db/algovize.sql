-- ============================================================================
-- AlgoViz 数据库迁移脚本：SQLite → MySQL 8.0
-- 源库文件: ./algoviz.db
-- 目标库:   algoviz  (账号: zxy / 密码: 见部署环境变量 MYSQL_PASSWORD，禁止硬编码)
-- 字符集:   utf8mb4 / utf8mb4_unicode_ci
-- ============================================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS `algoviz`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `algoviz`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 2. 建表
-- ============================================================================

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `username`        VARCHAR(50)  NOT NULL,
    `email`           VARCHAR(100) NOT NULL,
    `password`        VARCHAR(255) DEFAULT NULL,
    `age`             INT          DEFAULT NULL,
    `gender`          VARCHAR(10)  DEFAULT '未知',
    `nickname`        VARCHAR(100) DEFAULT NULL,
    `avatar_url`      VARCHAR(500) DEFAULT NULL,
    `login_status`    VARCHAR(20)  DEFAULT 'offline',
    `status`          TINYINT      DEFAULT 1 COMMENT '1:正常 0:封禁',
    `coins`           INT          NOT NULL DEFAULT 1000 COMMENT '硬币余额',
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `last_login_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `ai_dialogues`    INT          DEFAULT 0 COMMENT 'AI对话次数',
    `ds_visits`       INT          DEFAULT 0 COMMENT '数据结构访问次数',
    `algo_visits`     INT          DEFAULT 0 COMMENT '算法访问次数',
    `oj_visits`       INT          DEFAULT 0 COMMENT 'OJ访问次数',
    `last_visit_time` DATETIME     DEFAULT NULL COMMENT '最后访问时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_email` (`email`),
    KEY `idx_user_nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 1. 注册时间索引（排序用，最关键，解决 ORDER BY filesort）
ALTER TABLE `user` ADD INDEX `idx_user_created_at` (`created_at`);

-- 2. 组合索引：筛选字段 + 注册时间（让筛选+排序都走索引）
ALTER TABLE `user` ADD INDEX `idx_user_status_created` (`status`, `created_at`);
ALTER TABLE `user` ADD INDEX `idx_user_gender_created` (`gender`, `created_at`);
ALTER TABLE `user` ADD INDEX `idx_user_login_created` (`login_status`, `created_at`);
-- 商品表
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT,
    `product_id`   VARCHAR(64)   NOT NULL,
    `product_name` VARCHAR(200)  NOT NULL,
    `description`  TEXT,
    `price`        INT           NOT NULL COMMENT '单位：分',
    `category`     VARCHAR(50),
    `icon`         VARCHAR(50),
    `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- 订单表
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT,
    `order_id`             VARCHAR(64)  NOT NULL,
    `order_no`             VARCHAR(64)  DEFAULT NULL,
    `product_id`           VARCHAR(64)  NOT NULL,
    `product_id_ref`       BIGINT       DEFAULT NULL COMMENT '关联 product.id',
    `product_name`         VARCHAR(200) NOT NULL,
    `amount`               INT          NOT NULL COMMENT '单位：分',
    `price`                DECIMAL(10,2) DEFAULT NULL,
    `quantity`             INT          DEFAULT 1,
    `user_id`              BIGINT       DEFAULT NULL,
    `status`               VARCHAR(20)  DEFAULT 'PENDING',
    `wechat_trade_no`      VARCHAR(64)  DEFAULT NULL,
    `wechat_transaction_id` VARCHAR(64) DEFAULT NULL,
    `transaction_id`       VARCHAR(64)  DEFAULT NULL,
    `refund_status`        VARCHAR(20)  DEFAULT 'NONE',
    `refund_reason`        VARCHAR(500) DEFAULT NULL,
    `refund_time`          DATETIME     DEFAULT NULL,
    `create_time`          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `pay_time`             DATETIME     DEFAULT NULL,
    `update_time`          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_orders_order_id` (`order_id`),
    UNIQUE KEY `uk_orders_order_no` (`order_no`),
    KEY `idx_orders_user_id` (`user_id`),
    KEY `idx_orders_status` (`status`),
    KEY `idx_orders_refund_status` (`refund_status`),
    KEY `idx_orders_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- OJ题目表
DROP TABLE IF EXISTS `oj_problem`;
CREATE TABLE `oj_problem` (
    `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键自增',
    `problem_no`       VARCHAR(32)    NOT NULL,
    `title`            VARCHAR(200)   NOT NULL,
    `difficulty`       VARCHAR(20)    DEFAULT 'easy',
    `tags`             VARCHAR(500)   DEFAULT NULL,
    `description`      TEXT,
    `template`         MEDIUMTEXT,
    `status`           VARCHAR(20)    DEFAULT 'ACTIVE',
    `submission_count` INT            DEFAULT 0,
    `ac_rate`          DECIMAL(5,2)   DEFAULT 0.00,
    `created_at`       DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oj_problem_no` (`problem_no`),
    KEY `idx_oj_problem_difficulty` (`difficulty`),
    KEY `idx_oj_problem_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ题目表';

-- 测试用例表
DROP TABLE IF EXISTS `test_case`;
CREATE TABLE `test_case` (
    `id`         VARCHAR(32) NOT NULL,
    `problem_id` VARCHAR(32) NOT NULL,
    `input`      TEXT        NOT NULL,
    `output`     TEXT        NOT NULL,
    `score`      INT         DEFAULT 100,
    `is_sample`  TINYINT(1)  DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_test_case_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OJ测试用例表';

-- 提交记录表
DROP TABLE IF EXISTS `submission`;
CREATE TABLE `submission` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `submission_id` VARCHAR(50)   DEFAULT NULL,
    `user_id`       BIGINT        DEFAULT NULL,
    `username`      VARCHAR(100)  DEFAULT NULL,
    `problem_id`    BIGINT        DEFAULT NULL,
    `problem_title` VARCHAR(200)  DEFAULT NULL,
    `code`          MEDIUMTEXT,
    `language`      VARCHAR(50),
    `result`        VARCHAR(50)   DEFAULT NULL,
    `status`        VARCHAR(20)   DEFAULT 'PENDING',
    `runtime`       INT           DEFAULT NULL COMMENT '单位 ms',
    `memory`        INT           DEFAULT NULL COMMENT '单位 KB',
    `error_message` TEXT,
    `judge_log`     TEXT,
    `create_time`   DATETIME      DEFAULT NULL,
    `submit_time`   DATETIME      DEFAULT NULL,
    `judge_time`    DATETIME      DEFAULT NULL,
    `update_time`   DATETIME      DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_submission_submission_id` (`submission_id`),
    KEY `idx_submission_problem_id` (`problem_id`),
    KEY `idx_submission_user_id` (`user_id`),
    KEY `idx_submission_status` (`status`),
    KEY `idx_submission_submit_time` (`submit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提交记录表';



-- 登录日志表
DROP TABLE IF EXISTS `login_log`;
CREATE TABLE `login_log` (
    `id`          VARCHAR(32)  NOT NULL,
    `user_id`     VARCHAR(32)  NOT NULL,
    `username`    VARCHAR(50)  NOT NULL,
    `ip`          VARCHAR(64)  NOT NULL,
    `device`      VARCHAR(200) DEFAULT NULL,
    `location`    VARCHAR(200) DEFAULT NULL,
    `login_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `status`      VARCHAR(20)  DEFAULT 'success',
    `fail_reason` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_login_log_user_id` (`user_id`),
    KEY `idx_login_log_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- 操作日志表
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
    `id`         VARCHAR(32)  NOT NULL,
    `user_id`    VARCHAR(32)  NOT NULL,
    `username`   VARCHAR(50)  NOT NULL,
    `module`     VARCHAR(50)  NOT NULL,
    `action`     VARCHAR(100) NOT NULL,
    `detail`     TEXT,
    `ip`         VARCHAR(64)  DEFAULT NULL,
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_op_log_user_id` (`user_id`),
    KEY `idx_op_log_module` (`module`),
    KEY `idx_op_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 系统配置表
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
    `key`          VARCHAR(100) NOT NULL,
    `value`        TEXT,
    `type`         VARCHAR(20)  DEFAULT 'string',
    `label`        VARCHAR(100) DEFAULT NULL,
    `description`  VARCHAR(500) DEFAULT NULL,
    `config_group` VARCHAR(50)  DEFAULT 'basic',
    PRIMARY KEY (`key`),
    KEY `idx_system_config_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 数据结构配置表
DROP TABLE IF EXISTS `data_structure`;
CREATE TABLE `data_structure` (
    `id`          VARCHAR(32)  NOT NULL,
    `name`        VARCHAR(100) NOT NULL,
    `type`        VARCHAR(50)  NOT NULL,
    `description` TEXT,
    `status`      VARCHAR(20)  DEFAULT 'enabled',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据结构配置表';

-- 算法配置表
DROP TABLE IF EXISTS `algorithm`;
CREATE TABLE `algorithm` (
    `id`               VARCHAR(32)  NOT NULL,
    `name`             VARCHAR(100) NOT NULL,
    `category`         VARCHAR(50)  NOT NULL,
    `description`      TEXT,
    `time_complexity`  VARCHAR(50)  DEFAULT NULL,
    `space_complexity` VARCHAR(50)  DEFAULT NULL,
    `pseudocode`       MEDIUMTEXT,
    `status`           VARCHAR(20)  DEFAULT 'enabled',
    `created_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_algorithm_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='算法配置表';

-- AI提示词表
DROP TABLE IF EXISTS `ai_prompt`;
CREATE TABLE `ai_prompt` (
    `id`          VARCHAR(32)  NOT NULL,
    `title`       VARCHAR(200) NOT NULL,
    `category`    VARCHAR(50)  NOT NULL,
    `content`     MEDIUMTEXT   NOT NULL,
    `usage_count` INT          DEFAULT 0,
    `status`      VARCHAR(20)  DEFAULT 'enabled',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_ai_prompt_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI提示词表';

-- 小程序用户表
DROP TABLE IF EXISTS `app_user`;
CREATE TABLE `app_user` (
    `id`              VARCHAR(32)  NOT NULL,
    `openid`          VARCHAR(64)  NOT NULL,
    `nickname`        VARCHAR(100) DEFAULT NULL,
    `avatar`          VARCHAR(500) DEFAULT NULL,
    `bind_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `status`          VARCHAR(20)  DEFAULT 'active',
    `ds_visits`       INT          DEFAULT 0,
    `algo_visits`     INT          DEFAULT 0,
    `oj_visits`       INT          DEFAULT 0,
    `ai_dialogues`    INT          DEFAULT 0,
    `last_visit_time` DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_user_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小程序用户表';

-- 统计表
DROP TABLE IF EXISTS `statistics`;
CREATE TABLE `statistics` (
    `id`             VARCHAR(32) NOT NULL,
    `date`           DATE        NOT NULL,
    `dau`            INT         DEFAULT 0,
    `wau`            INT         DEFAULT 0,
    `mau`            INT         DEFAULT 0,
    `ds_visits`      INT         DEFAULT 0,
    `algo_visits`    INT         DEFAULT 0,
    `oj_submissions` INT         DEFAULT 0,
    `oj_ac_rate`     DECIMAL(5,2) DEFAULT 0.00,
    `ai_dialogues`   INT         DEFAULT 0,
    `created_at`     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_statistics_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统计表';

-- 公告表
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `title`       VARCHAR(200)  NOT NULL,
    `content`     TEXT,
    `type`        VARCHAR(20)   DEFAULT 'notice',
    `is_top`      TINYINT(1)    DEFAULT 0,
    `sort_order`  INT           DEFAULT 0,
    `status`      VARCHAR(20)   DEFAULT 'draft',
    `publish_time` DATETIME     DEFAULT NULL,
    `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_announcement_status` (`status`),
    KEY `idx_announcement_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

-- 反馈表
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       DEFAULT NULL,
    `username`   VARCHAR(100) DEFAULT NULL,
    `email`      VARCHAR(255) DEFAULT NULL,
    `user_nickname` VARCHAR(100) DEFAULT NULL,
    `type`       VARCHAR(50)  DEFAULT 'other',
    `title`      VARCHAR(200) DEFAULT NULL,
    `content`    TEXT         NOT NULL,
    `images`     TEXT,
    `status`     VARCHAR(20)  DEFAULT 'pending',
    `reply`      TEXT,
    `reply_time` DATETIME     DEFAULT NULL,
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_feedback_user_id` (`user_id`),
    KEY `idx_feedback_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈表';

-- 支付记录表
DROP TABLE IF EXISTS `payment_record`;
CREATE TABLE `payment_record` (
    `id`              VARCHAR(32)  NOT NULL,
    `order_id`        VARCHAR(64)  NOT NULL,
    `product_id`      VARCHAR(64)  NOT NULL,
    `product_name`    VARCHAR(200) NOT NULL,
    `amount`          INT          NOT NULL COMMENT '单位：分',
    `payment_method`  VARCHAR(20)  DEFAULT 'wechat',
    `transaction_id`  VARCHAR(64)  DEFAULT NULL,
    `status`          VARCHAR(20)  DEFAULT 'pending',
    `refund_status`   VARCHAR(20)  DEFAULT 'none',
    `refund_reason`   VARCHAR(500) DEFAULT NULL,
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `pay_time`        DATETIME     DEFAULT NULL,
    `refund_time`     DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_payment_order_id` (`order_id`),
    KEY `idx_payment_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- 文件存储表
DROP TABLE IF EXISTS `file_storage`;
CREATE TABLE `file_storage` (
    `id`            VARCHAR(32)  NOT NULL,
    `file_name`     VARCHAR(255) NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `file_type`     VARCHAR(50)  DEFAULT NULL,
    `file_size`     BIGINT       DEFAULT 0,
    `file_path`     VARCHAR(500) NOT NULL,
    `storage_type`  VARCHAR(20)  DEFAULT 'local',
    `bucket_name`   VARCHAR(100) DEFAULT NULL,
    `download_url`  VARCHAR(500) DEFAULT NULL,
    `uploader_id`   VARCHAR(32)  DEFAULT NULL,
    `uploader_name` VARCHAR(100) DEFAULT NULL,
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_file_uploader` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件存储表';

-- API日志表
DROP TABLE IF EXISTS `api_log`;
CREATE TABLE `api_log` (
    `id`            VARCHAR(32)  NOT NULL,
    `api_path`      VARCHAR(255) NOT NULL,
    `http_method`   VARCHAR(10)  NOT NULL,
    `status_code`   INT          DEFAULT 200,
    `response_time` INT          DEFAULT 0 COMMENT '单位 ms',
    `client_ip`     VARCHAR(64)  DEFAULT NULL,
    `request_body`  MEDIUMTEXT,
    `response_body` MEDIUMTEXT,
    `error_message` TEXT,
    `user_id`       VARCHAR(32)  DEFAULT NULL,
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_api_log_path` (`api_path`),
    KEY `idx_api_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API日志表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 3. 初始数据
-- ============================================================================

-- 用户测试数据
INSERT INTO `user` (`username`, `email`, `password`, `age`, `gender`, `nickname`, `avatar_url`, `login_status`, `status`) VALUES
('user1',  'user1@example.com',  'user123',  NULL, '未知', '用户1', 'https://i.pravatar.cc/150?u=2', 'offline', 1);

-- 注：旧 admin 表已弃用，管理员统一使用 sys_user (RBAC) 体系
-- 超级管理员账号：algovize / algovize123（Argon2id 加密，见 sys_user 种子数据）

-- 系统配置
INSERT INTO `system_config` (`key`, `value`, `type`, `label`, `description`, `config_group`) VALUES
('site_title',         '算法可视化平台', 'string',  '网站标题', '网站标题配置',     'basic'),
('theme_mode',         'light',         'string',  '主题模式', '默认主题模式',     'theme'),
('api_cors_enabled',   'true',          'boolean', 'CORS开关', '是否启用CORS代理', 'api'),
('rate_limit_enabled', 'true',          'boolean', '频率限制', '是否启用频率限制', 'security');

-- 商品测试数据
INSERT INTO `product` (`product_id`, `product_name`, `description`, `price`, `category`, `icon`) VALUES
('notes-basic',        '算法基础笔记',         '包含常见数据结构和算法的详细笔记，适合初学者',  2990,  'notes',    '📝'),
('notes-advanced',     '高级算法笔记',         '包含高级算法和复杂数据结构的深入解析',            4990,  'notes',    '📚'),
('notes-md',           'MD算法笔记',           'Markdown格式的算法学习笔记，适合快速查阅和学习',1,     'notes',    '📘'),
('notes-javase',       'JavaSE笔记',           'JavaSE核心知识点总结，包含面向对象、集合框架等',  1,     'notes',    '☕'),
('tutorial-basic',     '算法入门教程',         '零基础入门算法，包含视频讲解和实战练习',          9990,  'tutorial', '🎓'),
('tutorial-advanced',  '算法进阶教程',         '针对面试和竞赛的高级算法教程',                    14990, 'tutorial', '🚀'),
('project-basic',      '算法可视化项目',       '基于AlgoVize的算法可视化项目源码',                19990, 'project',  '💻'),
('project-advanced',   'AI辅助算法项目',       '结合AI技术的智能算法分析系统',                    29990, 'project',  '🌟');

-- OJ题目数据（来自运行中的 SQLite 数据库）
INSERT INTO `oj_problem` (`id`, `problem_no`, `title`, `difficulty`, `tags`, `description`, `template`, `status`, `submission_count`, `ac_rate`, `created_at`, `updated_at`) VALUES
('1', '1',     '单链表的选择排序',          'easy',   '链表',                  '给定一个无序单链表的头节点head，实现单链表的选择排序。要求额外空间复杂度为O(1)。**解题提示**选择排序的核心是从未排序的部分中找到最小值，然后放在排好序部分的尾部。具体过程：1. 遍历链表，找到整个链表的最小值节点，作为新的头节点；2. 每次从未排序的部分中找到最小值节点，将其从未排序部分删除，然后连接到排好序部分的尾部；3. 重复步骤2，直到所有节点都排序完成。时间复杂度O(N²)，空间复杂度O(1)。', 'public Node selectionSort(Node head) { Node tail = null; Node smallPre = null; Node cur = head; Node small = null; while (cur != null) { small = cur; smallPre = getSmallestPreNode(cur); if (smallPre != null) { small = smallPre.next; smallPre.next = small.next;   } cur = cur == small ? cur.next : cur; if (tail == null) {  head = small;  } else {  tail.next = small; } tail = small; }return head;', 'ACTIVE', 0, 0.00, '2026-06-11 00:34:56', '2026-06-11 00:34:56'),
('2', '1001',  '两数之和',                 'easy',   '数组,哈希表',         '给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出和为目标值 target 的那两个整数，并返回它们的数组下标。', 'class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // 在此处编写你的代码\n        return new int[0];\n    }\n}', 'ACTIVE', 0, 0.00, '2026-06-11 00:34:56', '2026-06-11 00:34:56'),
('3', '1002',  '反转链表',                 'easy',   '链表,递归',           '将两个升序链表合并为一个新的升序链表并返回。新链表是通过拼接给定的两个链表的所有节点组成的。', 'class Solution {\n    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {\n        // 在此处编写你的代码\n        return null;\n    }\n}', 'ACTIVE', 0, 0.00, '2026-06-11 00:34:56', '2026-06-11 00:34:56'),
('4', '2001',  '买卖股票最佳时机',         'medium', '数组,动态规划',       '给定一个数组 prices ，它的第 i 个元素 prices[i] 表示一支给定股票第 i 天的价格。你只能选择某一天买入这只股票，并选择在未来的某一个不同的日子卖出该股票。设计一个算法来计算你所能获取的最大利润。', 'class Solution {\n    public int maxProfit(int[] prices) {\n        // 在此处编写你的代码\n        return 0;\n    }\n}', 'ACTIVE', 0, 0.00, '2026-06-11 00:34:56', '2026-06-11 00:34:56'),
('5', '2002',  '全排列',                   'medium', '回溯',                '给定一个不含重复数字的数组 nums ，返回其所有可能的全排列。你可以按任意顺序返回答案。', 'class Solution {\n    public List<List<Integer>> permute(int[] nums) {\n        // 在此处编写你的代码\n        return new ArrayList<>();\n    }\n}', 'ACTIVE', 0, 0.00, '2026-06-11 00:34:56', '2026-06-11 00:34:56'),
('6', '3001',  '最长公共子序列',           'hard',   '动态规划',           '给定两个字符串 text1 和 text2，返回这两个字符串的最长公共子序列的长度。', 'class Solution {\n    public int longestCommonSubsequence(String text1, String text2) {\n        // 在此处编写你的代码\n        return 0;\n    }\n}', 'ACTIVE', 0, 0.00, '2026-06-11 00:34:56', '2026-06-11 00:34:56'),
('7', '3002',  '滑动窗口最大值',           'hard',   '队列,滑动窗口',       '给你一个整数数组 nums，有一个大小为 k 的滑动窗口从数组的最左侧移动到数组的最右侧。你只可以看到在滑动窗口内的 k 个数字。滑动窗口每次只向右移动一位。返回滑动窗口中的最大值。', 'class Solution {\n    public int[] maxSlidingWindow(int[] nums, int k) {\n        // 在此处编写你的代码\n        return new int[0];\n    }\n}', 'ACTIVE', 0, 0.00, '2026-06-11 00:34:56', '2026-06-11 00:34:56');


-- ============================================================================
-- 4. VARCHAR 长度修复（适配 UUID 36 字符）
-- 把所有可能存 UUID 的 VARCHAR(32) 主键统一扩到 VARCHAR(40)
-- 幂等：可重复执行
-- ============================================================================

-- 题目相关
ALTER TABLE `test_case`      MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `test_case`      MODIFY COLUMN `problem_id` VARCHAR(40) NOT NULL;

-- 日志
ALTER TABLE `login_log`      MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `login_log`      MODIFY COLUMN `user_id`    VARCHAR(40) NOT NULL;
ALTER TABLE `operation_log`  MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `operation_log`  MODIFY COLUMN `user_id`    VARCHAR(40) NOT NULL;

-- 内容相关
ALTER TABLE `data_structure` MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `algorithm`      MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `ai_prompt`      MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `app_user`       MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `statistics`     MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;

-- 支付 / 文件 / 日志
ALTER TABLE `payment_record` MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `file_storage`   MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `api_log`        MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;

SELECT 'VARCHAR 长度修复完成' AS message;

-- ============================================================================
-- 5. 验证
-- ============================================================================
SELECT 'algoviz 数据库迁移完成' AS message;
SELECT COUNT(*) AS user_count      FROM `user`;
SELECT COUNT(*) AS product_count   FROM `product`;
SELECT COUNT(*) AS oj_problem_cnt  FROM `oj_problem`;

-- ============================================================================
-- 6. user 表字段变更：avatar → age，新增 avatar_url / login_status / status
-- ============================================================================

-- 6.1 将 avatar 字段替换为 age
ALTER TABLE `user` CHANGE COLUMN `avatar` `age` INT DEFAULT NULL COMMENT '年龄';

-- 6.2 新增 avatar_url（头像地址）
ALTER TABLE `user` ADD COLUMN `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL' AFTER `nickname`;

-- 6.3 新增 login_status（登录状态：online/offline）
ALTER TABLE `user` ADD COLUMN `login_status` VARCHAR(20) DEFAULT 'offline' COMMENT '登录状态' AFTER `avatar_url`;

-- 6.4 新增 status（账号状态：1正常 0封禁）
ALTER TABLE `user` ADD COLUMN `status` TINYINT DEFAULT 1 COMMENT '1:正常 0:封禁' AFTER `login_status`;

-- 6.5 批量更新所有用户的头像URL（格式：https://i.pravatar.cc/150?u={id}）
UPDATE `user` SET `avatar_url` = CONCAT('https://i.pravatar.cc/150?u=', `id`), `updated_at` = NOW() WHERE `avatar_url` IS NULL OR `avatar_url` = '';

SELECT 'user 表字段变更完成' AS message;

-- ============================================================================
-- 7. 面试题目模块：5 张表 + 示例数据
-- ============================================================================

-- 7.1 面试题目主表
CREATE TABLE IF NOT EXISTS `interview_problem` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT    COMMENT '主键自增',
    `problem_no`        VARCHAR(32)     NOT NULL                   COMMENT '题目编号，唯一（如 MS001）',
    `title`             VARCHAR(255)    NOT NULL                   COMMENT '题目标题',
    `difficulty`        VARCHAR(20)     NOT NULL DEFAULT 'medium'  COMMENT '难度：easy / medium / hard',
    `tags`              VARCHAR(500)    DEFAULT NULL               COMMENT '标签，逗号分隔（如：数组,哈希表,双指针）',
    `category`          VARCHAR(50)     DEFAULT NULL               COMMENT '分类：数组/链表/树/图/动态规划/数学/其他',
    `description`       MEDIUMTEXT                                  COMMENT '题目描述（Markdown）',
    `input_format`      TEXT           DEFAULT NULL                COMMENT '输入格式（Markdown）',
    `output_format`     TEXT           DEFAULT NULL                COMMENT '输出格式（Markdown）',
    `solution`          MEDIUMTEXT     NOT NULL                    COMMENT '参考答案/题解（Markdown）',
    `status`            VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE'   COMMENT '状态：ACTIVE=上线 / INACTIVE=下线',
    `is_frequent`       TINYINT(1)     DEFAULT 0                   COMMENT '是否高频：1=是 0=否',
    `is_deleted`        TINYINT(1)     DEFAULT 0                   COMMENT '逻辑删除：1=已删除 0=正常',
    `view_count`        INT            DEFAULT 0                   COMMENT '累计浏览数（阅读量）',
    `like_count`        INT            DEFAULT 0                   COMMENT '点赞数（冗余计数）',
    `dislike_count`     INT            DEFAULT 0                   COMMENT '点踩数（冗余计数）',
    `created_by`        VARCHAR(64)    DEFAULT NULL                COMMENT '创建人（管理员ID）',
    `updated_by`        VARCHAR(64)    DEFAULT NULL                COMMENT '最后更新人',
    `created_at`        DATETIME       DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `updated_at`        DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_interview_problem_no` (`problem_no`),
    KEY `idx_interview_filter` (`status`, `category`, `difficulty`, `is_deleted`),
    KEY `idx_interview_difficulty` (`difficulty`),
    KEY `idx_interview_is_frequent` (`is_frequent`),
    KEY `idx_interview_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试题目表';

-- 7.2 用户收藏记录表
CREATE TABLE IF NOT EXISTS `interview_favorite` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT    COMMENT '主键',
    `user_id`      BIGINT        NOT NULL                   COMMENT '用户ID（关联 user.id）',
    `problem_id`   BIGINT        NOT NULL                   COMMENT '题目ID（关联 interview_problem.id）',
    `problem_no`   VARCHAR(32)   DEFAULT NULL               COMMENT '冗余：题目编号',
    `collect_time` DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '收藏时间',
    `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    `updated_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_problem` (`user_id`, `problem_id`),
    KEY `idx_favorite_user_collect` (`user_id`, `collect_time`),
    KEY `idx_favorite_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试题目收藏表';

-- 7.3 用户浏览历史记录表
CREATE TABLE IF NOT EXISTS `interview_history` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT    COMMENT '主键',
    `user_id`     BIGINT        NOT NULL                   COMMENT '用户ID（关联 user.id）',
    `problem_id`  BIGINT        NOT NULL                   COMMENT '题目ID（关联 interview_problem.id）',
    `view_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '浏览时间',
    `created_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    `updated_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_history_user_view` (`user_id`, `view_time`),
    KEY `idx_history_problem_id` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试题目浏览历史表';

-- 7.4 用户点赞点踩记录表
CREATE TABLE IF NOT EXISTS `interview_like` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT    COMMENT '主键',
    `user_id`     BIGINT        NOT NULL                   COMMENT '用户ID（关联 user.id）',
    `problem_id`  BIGINT        NOT NULL                   COMMENT '题目ID（关联 interview_problem.id）',
    `type`        VARCHAR(10)   NOT NULL                   COMMENT '类型：like=点赞 / dislike=点踩',
    `created_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    `updated_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_problem` (`user_id`, `problem_id`),
    KEY `idx_like_problem_id` (`problem_id`),
    KEY `idx_like_user_type` (`user_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试题目点赞点踩表';

-- 7.5 面试题目标签字典表
CREATE TABLE IF NOT EXISTS `interview_tag` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(50)   NOT NULL                   COMMENT '标签名（唯一）',
    `category`   VARCHAR(50)   DEFAULT NULL               COMMENT '标签分类',
    `use_count`  INT           DEFAULT 0                   COMMENT '使用次数',
    `sort_order` INT           DEFAULT 0                   COMMENT '排序值（小在前）',
    `created_at` DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试题目标签字典';

-- 7.6 初始化示例面试题（仅当表空时插入，保证幂等）
INSERT INTO `interview_problem` (`problem_no`, `title`, `difficulty`, `tags`, `category`, `description`, `input_format`, `output_format`, `solution`, `status`, `is_frequent`)
SELECT * FROM (
    SELECT
        'MS001' AS problem_no,
        '两数之和' AS title,
        'easy' AS difficulty,
        '数组,哈希表' AS tags,
        '数组' AS category,
        '## 题目描述\n给定一个整数数组 `nums` 和一个整数目标值 `target`，请你在该数组中找出 **和为目标值** 的那 **两个** 整数，并返回它们的数组下标。' AS description,
        '## 输入格式\n- 第一行：整数 n（数组长度）\n- 第二行：n 个整数（数组 nums）\n- 第三行：整数 target' AS input_format,
        '## 输出格式\n两个整数（数组下标），空格分隔' AS output_format,
        '## 题解\n使用哈希表，时间复杂度 O(n)，空间 O(n)。\n\n```java\npublic int[] twoSum(int[] nums, int target) {\n    Map<Integer, Integer> map = new HashMap<>();\n    for (int i = 0; i < nums.length; i++) {\n        if (map.containsKey(target - nums[i])) {\n            return new int[]{map.get(target - nums[i]), i};\n        }\n        map.put(nums[i], i);\n    }\n    return new int[0];\n}\n```' AS solution,
        'ACTIVE' AS status,
        1 AS is_frequent
    UNION ALL
    SELECT 'MS002', '反转链表', 'easy', '链表,递归,迭代', '链表',
        '## 题目描述\n给你单链表的头节点 `head`，请你反转链表，并返回反转后的链表。',
        '## 输入格式\n- 第一行：整数 n（节点数）\n- 第二行：n 个整数，按顺序为链表节点值',
        '## 输出格式\n反转后链表的节点值，空格分隔',
        '## 题解\n### 方法一：迭代（双指针）\n```java\npublic ListNode reverseList(ListNode head) {\n    ListNode prev = null, cur = head;\n    while (cur != null) {\n        ListNode nxt = cur.next;\n        cur.next = prev;\n        prev = cur;\n        cur = nxt;\n    }\n    return prev;\n}\n```',
        'ACTIVE', 1
    UNION ALL
    SELECT 'MS003', 'LRU 缓存', 'medium', '链表,哈希表,设计,双向链表', '设计',
        '## 题目描述\n请你设计并实现一个满足 **LRU (最近最少使用)** 缓存约束的数据结构。',
        '## 输入格式\n见题目描述', '## 输出格式\n见题目描述',
        '## 题解\n核心：HashMap + 手写双向链表，保证 O(1) 访问与更新。',
        'ACTIVE', 1
    UNION ALL
    SELECT 'MS004', '二叉树的层序遍历', 'medium', '树,广度优先搜索,队列,二叉树', '树',
        '## 题目描述\n给你二叉树的根节点 `root`，返回其节点值的 **层序遍历**（即逐层地，从左到右访问所有节点）。',
        '## 输入格式\n以数组形式表示二叉树（层序），null 使用 -1 表示',
        '## 输出格式\n每一行代表一层，数字空格分隔',
        '## 题解\n```java\nList<List<Integer>> levelOrder(TreeNode root) {\n    // 队列 BFS，每次处理一层\n}\n```',
        'ACTIVE', 0
    UNION ALL
    SELECT 'MS005', '接雨水', 'hard', '栈,数组,双指针,动态规划,单调栈', '数组',
        '## 题目描述\n给定 n 个非负整数表示每个宽度为 1 的柱子的高度图，计算按此排列的柱子，下雨之后能接多少雨水。',
        '## 输入格式\n- 第一行：整数 n\n- 第二行：n 个整数 height',
        '## 输出格式\n一个整数（接水量）',
        '## 题解\n双指针 O(n) O(1)：\n```java\npublic int trap(int[] h) {\n    int l = 0, r = h.length - 1, maxL = 0, maxR = 0, ans = 0;\n    while (l < r) {\n        if (h[l] < h[r]) { maxL = Math.max(maxL, h[l]); ans += maxL - h[l++]; }\n        else { maxR = Math.max(maxR, h[r]); ans += maxR - h[r--]; }\n    }\n    return ans;\n}\n```',
        'ACTIVE', 1
) t WHERE NOT EXISTS (SELECT 1 FROM `interview_problem` LIMIT 1);

-- 7.7 初始化标签字典（仅表空时插入）
INSERT INTO `interview_tag` (`name`, `category`, `use_count`, `sort_order`)
SELECT * FROM (
    SELECT '数组' AS name, '数据结构' AS category, 2 AS use_count, 1 AS sort_order
    UNION ALL SELECT '哈希表', '数据结构', 2, 2
    UNION ALL SELECT '链表', '数据结构', 2, 3
    UNION ALL SELECT '递归', '算法', 1, 10
    UNION ALL SELECT '迭代', '算法', 1, 11
    UNION ALL SELECT '设计', '算法', 1, 12
    UNION ALL SELECT '双向链表', '数据结构', 1, 4
    UNION ALL SELECT '树', '数据结构', 1, 5
    UNION ALL SELECT '广度优先搜索', '算法', 1, 13
    UNION ALL SELECT '队列', '数据结构', 1, 6
    UNION ALL SELECT '二叉树', '数据结构', 1, 7
    UNION ALL SELECT '栈', '数据结构', 1, 8
    UNION ALL SELECT '双指针', '算法', 1, 14
    UNION ALL SELECT '动态规划', '算法', 1, 15
    UNION ALL SELECT '单调栈', '算法', 1, 16
    UNION ALL SELECT '字符串', '数据结构', 0, 9
    UNION ALL SELECT '图', '数据结构', 0, 17
    UNION ALL SELECT '贪心', '算法', 0, 18
    UNION ALL SELECT '回溯', '算法', 0, 19
    UNION ALL SELECT '二分', '算法', 0, 20
    UNION ALL SELECT '数学', '算法', 0, 21
    UNION ALL SELECT '其他', '综合', 0, 999
    UNION ALL SELECT 'Java', 'java必备', 0, 100
    UNION ALL SELECT '泛型', 'java必备', 0, 101
    UNION ALL SELECT '反射', 'java必备', 0, 102
    UNION ALL SELECT '注解', 'java必备', 0, 103
    UNION ALL SELECT '集合', 'java必备', 0, 104
    UNION ALL SELECT '多线程', 'java必备', 0, 105
    UNION ALL SELECT '锁', 'java必备', 0, 106
    UNION ALL SELECT 'JVM', 'java必备', 0, 107
    UNION ALL SELECT '计算机网络', '计算机基础', 0, 200
    UNION ALL SELECT '设计模式', '计算机基础', 0, 201
    UNION ALL SELECT 'Spring', '框架', 0, 300
    UNION ALL SELECT 'SpringBoot', '框架', 0, 301
    UNION ALL SELECT 'MySQL', '数据库', 0, 400
    UNION ALL SELECT 'Redis', '数据库', 0, 401
    UNION ALL SELECT 'Docker', 'Java进阶', 0, 500
    UNION ALL SELECT 'Git', 'Java进阶', 0, 501
    UNION ALL SELECT 'Linux', 'Java进阶', 0, 502
    UNION ALL SELECT 'Nginx', 'Java进阶', 0, 503
    UNION ALL SELECT 'LLM', 'AI', 0, 600
    UNION ALL SELECT 'RAG', 'AI', 0, 601
    UNION ALL SELECT 'Agent', 'AI', 0, 602
) t WHERE NOT EXISTS (SELECT 1 FROM `interview_tag` LIMIT 1);

SET FOREIGN_KEY_CHECKS = 1;

SELECT '面试题目模块 5 张表 + 示例数据初始化完成' AS message;

-- 8 新增货币系统
-- ============================================================================
-- 货币系统（硬币）SQL —— 幂等，可重复执行
-- 不修改 algovize.sql，仅新增表和字段
-- ============================================================================

USE `algoviz`;
SET NAMES utf8mb4;

-- 1. user 表新增 coins 字段（初始 1000 硬币）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'algoviz' AND TABLE_NAME = 'user' AND COLUMN_NAME = 'coins');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `coins` INT NOT NULL DEFAULT 1000 COMMENT ''硬币余额'' AFTER `status`',
    'SELECT ''coins 字段已存在'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 已有用户补充硬币（仅 coins 为 NULL 或未设置的）
UPDATE `user` SET `coins` = 1000 WHERE `coins` IS NULL OR `coins` = 0;

-- 2. 硬币商品表
CREATE TABLE IF NOT EXISTS `coin_product` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `product_id`    VARCHAR(64)   NOT NULL                COMMENT '商品编号',
    `product_name`  VARCHAR(200)  NOT NULL                COMMENT '商品名称',
    `description`   TEXT                                  COMMENT '商品描述',
    `coin_price`    INT           NOT NULL                COMMENT '硬币价格',
    `category`      VARCHAR(50)   DEFAULT 'coin'          COMMENT '分类',
    `icon`          VARCHAR(20)   DEFAULT '🪙'            COMMENT '图标',
    `status`        VARCHAR(20)   DEFAULT 'ACTIVE'        COMMENT 'ACTIVE=上架 INACTIVE=下架',
    `created_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_coin_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='硬币商品表';

-- 3. 硬币购买记录表
CREATE TABLE IF NOT EXISTS `coin_purchase` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT        NOT NULL                COMMENT '用户ID',
    `username`      VARCHAR(100)  DEFAULT NULL            COMMENT '冗余用户名',
    `product_id`    VARCHAR(64)   NOT NULL                COMMENT '商品编号',
    `product_name`  VARCHAR(200)  NOT NULL                COMMENT '商品名称',
    `coin_price`    INT           NOT NULL                COMMENT '消耗硬币数',
    `coin_before`   INT           DEFAULT NULL            COMMENT '购买前余额',
    `coin_after`    INT           DEFAULT NULL            COMMENT '购买后余额',
    `status`        VARCHAR(20)   DEFAULT 'SUCCESS'       COMMENT 'SUCCESS=成功',
    `created_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_coin_purchase_user_id` (`user_id`),
    KEY `idx_coin_purchase_product_id` (`product_id`),
    KEY `idx_coin_purchase_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='硬币购买记录表';

-- 4. 初始化 9 个硬币商品（每次启动覆盖更新，修复乱码）
INSERT INTO `coin_product` (`product_id`, `product_name`, `description`, `coin_price`, `category`, `icon`, `status`)
VALUES
    ('coin-ds-visual',        '数据结构图解手册',  '图文并茂讲解数组/链表/树/图等核心数据结构', 120,  'coin', '📊', 'ACTIVE'),
    ('coin-algo-notes',       '算法基础笔记',      '常见排序/查找/递归算法详细笔记',           100,  'coin', '📝', 'ACTIVE'),
    ('coin-java-notes',       'JavaSE核心笔记',    '面向对象/集合/多线程/IO知识点总结',       150,  'coin', '☕', 'ACTIVE'),
    ('coin-interview100',     '面试必刷100题',     '高频面试题集含详细解析',                   350,  'coin', '🎯', 'ACTIVE'),
    ('coin-algo-tutorial',    '算法入门教程',      '零基础入门含视频讲解和实战',               300,  'coin', '🎓', 'ACTIVE'),
    ('coin-advanced-tutorial','算法进阶教程',      '面试竞赛级高级算法教程',                   500,  'coin', '🚀', 'ACTIVE'),
    ('coin-viz-project',      '可视化项目源码',    'AlgoViz算法可视化项目完整源码',            800,  'coin', '💻', 'ACTIVE'),
    ('coin-ai-project',       'AI辅助算法项目',    '结合AI的智能算法分析系统',                 1000, 'coin', '🌟', 'ACTIVE'),
    ('coin-dp-master',        '动态规划专题',      '从入门到精通DP经典题集',                   280,  'coin', '🧩', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `product_name` = VALUES(`product_name`),
    `description`  = VALUES(`description`),
    `coin_price`   = VALUES(`coin_price`),
    `icon`         = VALUES(`icon`);

SELECT '货币系统初始化完成' AS message;

--9 关键词屏蔽审核系统建表脚本

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

-- 10. 用户题解表
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

--11 ====================================================================
-- RBAC 角色权限体系 DDL + 种子数据
--   后台管理员密码：
--     超级管理员（algovize/algovize123）：Argon2id（本脚本已写生成的 hash）
--     普通管理员：BCrypt（可由 PasswordEncoderUtil.bcryptEncode 生成）
--   前台用户密码：
--     MD5（由 user 表保存）
-- ====================================================================

-- ============================================================
-- 1. 后台管理员用户表 sys_user
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `username`        VARCHAR(64)  NOT NULL COMMENT '登录账号',
    `password`        VARCHAR(128) NOT NULL COMMENT '密码：超级管理员Argon2id(≤128)，普通管理员BCrypt(≤60)',
    `real_name`       VARCHAR(64)  NOT NULL DEFAULT '未设置' COMMENT '真实姓名',
    `email`           VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `phone`           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`          VARCHAR(256) DEFAULT NULL COMMENT '头像 URL',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常 2-封禁',
    `account_type`    TINYINT      NOT NULL DEFAULT 1 COMMENT '账号类型: 1-内部管理员 2-外部出题人',
    `last_login_time` DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`   VARCHAR(45)  DEFAULT NULL COMMENT '最后登录 IP',
    `created_by`      BIGINT       DEFAULT NULL COMMENT '创建人 ID',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_account_type` (`account_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台用户表';


-- ============================================================
-- 2. 角色表 sys_role
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT,
    `role_code`      VARCHAR(64) NOT NULL COMMENT '角色编码',
    `role_name`      VARCHAR(64) NOT NULL COMMENT '角色名称',
    `role_level`     TINYINT     NOT NULL COMMENT '角色层级: 1-超级 2-一级 3-二级',
    `parent_role_id` BIGINT      DEFAULT NULL COMMENT '父角色 ID（NULL=顶级）',
    `data_scope`     TINYINT     NOT NULL DEFAULT 1 COMMENT '数据范围: 1-全部 2-本部门 3-本人',
    `description`    VARCHAR(256) DEFAULT NULL COMMENT '描述',
    `status`         TINYINT     NOT NULL DEFAULT 1 COMMENT '0-禁用 1-正常',
    `is_system`      TINYINT     NOT NULL DEFAULT 0 COMMENT '0-否 1-系统内置',
    `sort_order`     INT         NOT NULL DEFAULT 0,
    `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_role_level` (`role_level`),
    KEY `idx_parent_role` (`parent_role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';


-- ============================================================
-- 3. 菜单表 sys_menu（目录/菜单/按钮）
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `parent_id`  BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单 ID，0=根节点',
    `menu_name`  VARCHAR(64)  NOT NULL,
    `menu_type`  TINYINT      NOT NULL COMMENT '1-目录 2-菜单 3-按钮',
    `path`       VARCHAR(128) DEFAULT NULL,
    `component`  VARCHAR(128) DEFAULT NULL,
    `perms`      VARCHAR(128) DEFAULT NULL COMMENT '权限标识如 content:ds:list',
    `icon`       VARCHAR(64)  DEFAULT NULL,
    `sort_order` INT          NOT NULL DEFAULT 0,
    `visible`    TINYINT      NOT NULL DEFAULT 1 COMMENT '0-隐藏 1-显示',
    `status`     TINYINT      NOT NULL DEFAULT 1,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent_id`),
    KEY `idx_type` (`menu_type`),
    KEY `idx_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';


-- ============================================================
-- 4. 权限点表 sys_permission（按钮级权限）
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `menu_id`     BIGINT       NOT NULL COMMENT '所属菜单 ID',
    `perm_code`   VARCHAR(128) NOT NULL COMMENT '权限编码如 content:ds:add',
    `perm_name`   VARCHAR(64)  NOT NULL,
    `perm_type`   TINYINT      NOT NULL COMMENT '1-新增 2-编辑 3-删除 4-导出 5-审核 6-其他',
    `api_method`  VARCHAR(10)  DEFAULT NULL,
    `api_path`    VARCHAR(256) DEFAULT NULL,
    `status`      TINYINT      NOT NULL DEFAULT 1,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_perm_code` (`perm_code`),
    KEY `idx_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限点表';


-- ============================================================
-- 5-7. 关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT   NOT NULL,
    `role_id`    BIGINT   NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `role_id`    BIGINT   NOT NULL,
    `menu_id`    BIGINT   NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_role` (`role_id`),
    KEY `idx_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT,
    `role_id`       BIGINT   NOT NULL,
    `permission_id` BIGINT   NOT NULL,
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
    KEY `idx_role` (`role_id`),
    KEY `idx_perm` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';


-- ============================================================
-- 8-10. 审计/登录/业务操作日志
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_audit_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL,
    `username`       VARCHAR(64)  NOT NULL,
    `role_name`      VARCHAR(64)  DEFAULT NULL,
    `module`         VARCHAR(64)  NOT NULL,
    `operation`      VARCHAR(128) NOT NULL,
    `method`         VARCHAR(10)  DEFAULT NULL,
    `request_url`    VARCHAR(512) DEFAULT NULL,
    `request_params` TEXT         DEFAULT NULL,
    `response_code`  INT          DEFAULT NULL,
    `ip`             VARCHAR(45)  NOT NULL,
    `location`       VARCHAR(128) DEFAULT NULL,
    `cost_time`      BIGINT       DEFAULT NULL,
    `operation_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_module` (`module`),
    KEY `idx_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

CREATE TABLE IF NOT EXISTS `sys_login_log` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       DEFAULT NULL,
    `username`   VARCHAR(64)  NOT NULL,
    `login_type` TINYINT      NOT NULL COMMENT '1-登录 2-登出',
    `status`     TINYINT      NOT NULL COMMENT '0-失败 1-成功',
    `ip`         VARCHAR(45)  NOT NULL,
    `location`   VARCHAR(128) DEFAULT NULL,
    `browser`    VARCHAR(64)  DEFAULT NULL,
    `os`         VARCHAR(64)  DEFAULT NULL,
    `message`    VARCHAR(256) DEFAULT NULL,
    `login_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL,
    `username`       VARCHAR(64)  NOT NULL,
    `business_type`  VARCHAR(64)  NOT NULL,
    `operation_type` VARCHAR(32)  NOT NULL,
    `target_id`      BIGINT       DEFAULT NULL,
    `target_name`    VARCHAR(128) DEFAULT NULL,
    `before_data`    TEXT         DEFAULT NULL,
    `after_data`     TEXT         DEFAULT NULL,
    `remark`         VARCHAR(256) DEFAULT NULL,
    `operation_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_business` (`business_type`),
    KEY `idx_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务操作日志表';


-- ====================================================================
-- 种子数据：28 个角色
-- ====================================================================
INSERT IGNORE INTO `sys_role` (`id`, `role_code`, `role_name`, `role_level`, `parent_role_id`, `data_scope`, `description`, `is_system`, `sort_order`) VALUES
-- 超级 + 一级
(1,  'SUPER_ADMIN',                '超级管理员',                 1, NULL, 1, '运维应急，全部权限',                                      1, 1),
(2,  'LEVEL1_ADMIN',               '一级管理员',                 2, 1,    1, '日常业务管理员',                                          1, 2),
(3,  'PERM_MANAGER',               '权限分配子管理员',           3, 2,    2, '仅分配账号角色',                                          1, 3),
(4,  'CONTENT_PUBLISHER',          '内容发布管理员',             2, 2,    1, '管理内容发布',                                            1, 4),
(5,  'CONTENT_AUDITOR',            '内容审核管理员',             2, 2,    1, '只做审核，不能新增',                                      1, 5),
(6,  'EFK_LOG_ADMIN',              'EFK日志分析管理员',          2, 2,    1, '日志检索，ES 操作',                                       1, 6),
(7,  'DATA_ANALYST',               '数据分析管理员',             2, 2,    1, '报表 + 看板配置',                                         1, 7),
(8,  'ORDER_ADMIN',                '订单分析管理员',             2, 2,    1, '订单/商品/金币',                                          1, 8),
(9,  'OPS_ADMIN',                  '运营活动管理员',             2, 2,    1, '活动配置',                                                1, 9),
(10, 'FINANCE_ADMIN',              '财务管理员',                 2, 2,    1, '对账退款审核',                                            1, 10),
(11, 'CS_ADMIN',                   '客服管理员',                 2, 2,    1, '申诉工单/禁言解封',                                       1, 11),
(12, 'SECURITY_AUDITOR',           '安全审计管理员',             2, 1,    1, '只读审计',                                                1, 12),
(13, 'EXTERNAL_AUTHOR',            '外部出题人',                 3, NULL, 3, '外部合作出题',                                            1, 13),
-- 内容发布二级
(14, 'DS_PUBLISHER',               '二级-数据结构发布管理员',     3, 4,    3, '仅自己创建的数据结构题目',                                  1, 14),
(15, 'ALGO_PUBLISHER',             '二级-算法发布管理员',         3, 4,    3, '仅自己创建的算法题目',                                     1, 15),
(16, 'GENERAL_SUB_ADMIN',          '二级-通用子管理员',           3, 4,    3, '按需开放模块',                                            1, 16),
-- 内容审核二级
(17, 'TEST_VALIDATOR',             '题目校验测试管理员',         3, 5,    1, '仅校验不可直接上线',                                       1, 17),
(18, 'KEYWORD_AUDITOR',            '二级-关键词审核管理员',       3, 5,    1, '关键词屏蔽审核',                                           1, 18),
(19, 'SOLUTION_COMMENT_AUDITOR',   '二级-题解评论审核管理员',     3, 5,    1, '题解审核、评论审核',                                       1, 19),
-- EFK 二级
(20, 'BIZ_LOG_ADMIN',              '二级-业务日志管理员',         3, 6,    1, '业务日志检索导出',                                         1, 20),
(21, 'DEV_LOG_ADMIN',              '二级-开发日志管理员',         3, 6,    1, '开发日志检索导出',                                         1, 21),
(22, 'SYS_LOG_ADMIN',              '二级-系统日志管理员',         3, 6,    1, '系统日志检索导出',                                         1, 22),
(23, 'OPS_LOG_ADMIN',              '二级-运维管理员',             3, 6,    1, '日志平台/ES 配置',                                        1, 23),
-- 数据分析二级
(24, 'UV_PV_ANALYST',              '二级-UV/PV数据分析师',        3, 7,    1, 'UV/PV 报表',                                              1, 24),
(25, 'RETENTION_ANALYST',          '二级-用户留存数据分析师',     3, 7,    1, '留存报表',                                                1, 25),
(26, 'ORDER_ANALYST_SUB',          '二级-订单数据分析师',         3, 7,    1, '订单只读分析',                                            1, 26),
-- 订单二级
(27, 'PRODUCT_ADMIN',              '商品管理员',                 3, 8,    1, '商品增删改查上下架',                                       1, 27),
(28, 'COIN_ADMIN',                 '金币管理员',                 3, 8,    2, '金币调整与流水',                                           1, 28);


-- ====================================================================
-- 种子数据：超级管理员 algovize / algovize123（Argon2id 加密）
--   密码由 doc/tools/gen_argon2_hash.py 生成
-- ====================================================================
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `email`, `status`, `account_type`)
VALUES (1, 'algovize',
        '$argon2id$v=19$m=65536,t=3,p=1$IOLmhNf9s/Z03JV7O/tJZA$YT/C7YsDrOroO1hD5Ik2cpXkPSja6J0y8xtVdcjbq88',
        '超级管理员', 'algovize@dsaol.asia', 1, 1);

-- 超级管理员 → SUPER_ADMIN 角色
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 11
-- OJ 用户题解 & 评论 性能优化索引脚本
-- 将查询从 秒级（10~20s）降到 毫秒级（< 50ms）
-- 适用数据量：几千~几万条
-- 执行前确认数据库：USE algovize;
-- ================================================================

-- 1) oj_solution：用户侧最常用查询 selectPublishedBrief 的 覆盖索引
--    查询条件：problem_id = ? AND status = 'PUBLISHED' AND audit_status != 'blocked'
--    排序：ORDER BY is_featured DESC, like_count DESC, created_at DESC
--    单列 id/problem_id/status 都不足以让 MySQL 跳过 filesort，这组复合索引直接把 ORDER BY 字段
--    纳入索引尾部，让 InnoDB 直接按索引顺序返回（避免 Using filesort，查询 10 条 0.00x 秒）
ALTER TABLE oj_solution
    ADD INDEX idx_sol_problem_sort (
        problem_id,
        status,
        audit_status,
        is_featured DESC,
        like_count DESC,
        created_at DESC,
        id
    );

-- 2) oj_solution_comment：顶层评论列表的 覆盖索引
--    查询：solution_id = ? AND parent_id = 0 AND status = 'PUBLISHED' AND audit_status != 'blocked'
--    排序：ORDER BY like_count DESC, created_at ASC
ALTER TABLE oj_solution_comment
    ADD INDEX idx_cmt_sol_top_sort (
        solution_id,
        parent_id,
        status,
        audit_status,
        like_count DESC,
        created_at ASC,
        id
    );

-- 3) oj_solution_comment：子评论（回复）查询 索引
--    查询：root_id = ? AND parent_id != 0 AND status = 'PUBLISHED' AND audit_status != 'blocked'
--    排序：ORDER BY created_at ASC
ALTER TABLE oj_solution_comment
    ADD INDEX idx_cmt_root_reply_sort (
        root_id,
        parent_id,
        status,
        audit_status,
        created_at ASC,
        id
    );

-- 4) oj_solution_comment：批量 countRepliesByRootIds 的 GROUP BY 辅助索引
--    查询：root_id IN (...) AND parent_id != 0 AND status = 'PUBLISHED' AND audit_status != 'blocked'
--    聚合：GROUP BY root_id
--    以上 idx_cmt_root_reply_sort 已覆盖，无需重复创建

-- 验证索引是否生效：
-- EXPLAIN
-- SELECT id, problem_id, user_id, title, format, like_count, view_count, comment_count, created_at
-- FROM oj_solution
-- WHERE problem_id = 1 AND status = 'PUBLISHED' AND audit_status != 'blocked'
-- ORDER BY is_featured DESC, like_count DESC, created_at DESC
-- LIMIT 0, 10;
-- 期望输出：key = idx_sol_problem_sort, Extra = Using where（没有 Using filesort / Using temporary）

--12
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
-- 13
-- =============================================
-- OJ 题目表索引优化脚本
-- 用途：优化排序、分页、筛选查询性能
-- =============================================

-- 1. 添加索引：id (主键已自带，不需要额外添加)
-- id 是主键，已经自动有索引

-- 2. 添加索引：created_at (用于时间排序)
-- 这个索引可以加速按创建时间排序的查询
ALTER TABLE oj_problem ADD INDEX idx_created_at (created_at);

-- 3. 添加索引：status (用于状态筛选)
-- 这个索引可以加速按状态筛选的查询
ALTER TABLE oj_problem ADD INDEX idx_status (status);

-- 4. 添加索引：difficulty (用于难度筛选)
-- 这个索引可以加速按难度筛选的查询
ALTER TABLE oj_problem ADD INDEX idx_difficulty (difficulty);

-- 5. 添加复合索引：status + id (用于状态筛选+ID排序)
-- 这个索引可以加速 "WHERE status = 'ACTIVE' ORDER BY id" 的查询
ALTER TABLE oj_problem ADD INDEX idx_status_id (status, id);

-- 6. 添加复合索引：status + created_at (用于状态筛选+时间排序)
-- 这个索引可以加速 "WHERE status = 'ACTIVE' ORDER BY created_at" 的查询
ALTER TABLE oj_problem ADD INDEX idx_status_created_at (status, created_at);

-- 7. 添加全文索引：title + tags (用于关键词搜索)
-- 注意：全文索引需要 MySQL 5.6+ 且使用 InnoDB 引擎
-- 如果你的 MySQL 版本不支持，可以跳过这一步
ALTER TABLE oj_problem ADD FULLTEXT INDEX ft_title_tags (title, tags);

-- 验证索引是否创建成功
-- SHOW INDEX FROM oj_problem;

-- 常用查询的索引使用建议：
-- 
-- 查询场景：                           推荐索引：
-- ---------------------------------------------
-- 按 ID 排序分页                       PRIMARY KEY (id)
-- 按 created_at 排序分页               idx_created_at
-- 按 status 筛选 + 按 ID 排序           idx_status_id
-- 按 status 筛选 + 按 created_at 排序   idx_status_created_at
-- 按 difficulty 筛选                    idx_difficulty
-- 按 title/tags 搜索                   ft_title_tags
-- 复合查询（筛选+排序）                  对应复合索引

-- 性能测试 SQL：
-- EXPLAIN SELECT * FROM oj_problem WHERE status = 'ACTIVE' ORDER BY id LIMIT 20 OFFSET 0;
-- EXPLAIN SELECT * FROM oj_problem WHERE status = 'ACTIVE' ORDER BY created_at LIMIT 20 OFFSET 0;
-- EXPLAIN SELECT * FROM oj_problem ORDER BY id LIMIT 20 OFFSET 0;
-- EXPLAIN SELECT * FROM oj_problem ORDER BY created_at LIMIT 20 OFFSET 0;

-- 14【更新日志表】：对应后台“客服工单 → 更新日志” + 前台 changelog.html
-- 执行前请先确认数据库 algovize 已选择
CREATE TABLE IF NOT EXISTS `changelog` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
  `version`         VARCHAR(32)     NOT NULL                 COMMENT '版本号，如 v1.2.3',
  `type`            VARCHAR(16)     NOT NULL DEFAULT 'new'   COMMENT '更新类型：new=新增 optimize=优化 fix=修复 urgent=紧急更新',
  `summary`         VARCHAR(255)    NOT NULL DEFAULT ''      COMMENT '更新摘要（卡片顶部的一句话）',
  `release_date`    DATE            NOT NULL                 COMMENT '发布日期（YYYY-MM-DD）',
  `modules`         TEXT            NULL                     COMMENT '功能模块标签，JSON 字符串数组：["用户中心","商品页面"]',
  `details`         MEDIUMTEXT      NOT NULL                 COMMENT '完整更新说明，换行 \n 分隔（前端拆成 <li>）',
  `known_issues`    VARCHAR(1000)   NOT NULL DEFAULT ''      COMMENT '已知问题 / 注意事项说明',
  `issues_title`    VARCHAR(32)     NOT NULL DEFAULT '已知问题' COMMENT '红色框标题：已知问题 / 注意事项',
  `status`          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '发布状态：0=草稿(不显示) 1=已发布',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version` (`version`),
  KEY `idx_type_release_date` (`type`, `release_date`),
  KEY `idx_status_release_date` (`status`, `release_date`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='更新日志表';

-- （可选）把 changelog.html 现有的 5 条硬编码记录导入，方便你先看效果
-- 审核后需要的话再执行
 INSERT INTO `changelog` (`version`, `type`, `summary`, `release_date`, `modules`, `details`, `known_issues`, `issues_title`, `status`) VALUES
('v1.2.3', 'new',      '新增个人中心页面，支持用户头像自定义和设置管理', '2024-04-22', '["个人中心","用户设置","头像系统"]', '新增个人中心页面，包含账号信息、订单、产品服务等功能\n支持18个默认头像选择，可切换使用\n支持上传本地图片作为头像\n新增设置功能，可修改姓名、性别、头像、密码\n集成商品页面和支付功能入口', '暂无已知问题', '已知问题', 1),
('v1.2.2', 'optimize', '优化导航栏结构，新增商品展示页面', '2024-04-21', '["导航栏","商品页面","支付系统"]', '导航栏新增商品入口，支持快速访问商品页面\n新增商品分类功能，支持按笔记、教程、项目筛选\n实现支付模态框，支持微信和支付宝二维码展示\n优化页面响应式布局，适配移动端设备', '支付功能正在开发中，暂不支持真实支付', '注意事项', 1),
('v1.2.1', 'new',      '新增数据结构独立页面，优化数据结构选择交互', '2024-04-20', '["数据结构","独立页面","数组可视化"]', '新增数组数据结构独立可视化页面\n数据结构选择区域居中显示，两行四列布局\n点击数据结构按钮跳转到对应独立页面\n新增查询、增加、删除、扩容操作按钮', '部分数据结构独立页面仍在开发中', '已知问题', 1),
('v1.2.0', 'fix',      '修复多个可视化器初始化问题，优化页面加载性能', '2024-04-19', '["可视化器","页面加载","基础组件"]', '修复 Trie 可视化器 setData 方法缺失问题\n修复并查集可视化器初始化错误\n修复线段树可视化器数据加载问题\n优化页面加载速度，减少初始化时间', '暂无已知问题', '已知问题', 1),
('v1.1.9', 'urgent',   '紧急修复暗色模式切换问题', '2024-04-18', '["暗色模式","主题切换"]', '紧急修复主题切换按钮样式异常问题\n修复 LocalStorage 主题保存失效问题\n优化主题切换动画效果', '请刷新页面以获取最新主题设置', '注意事项', 1);



-- 15 迁移说明：OJ 题解/评论审核接口权限注册
-- 背景：
--   OJSolutionAdminController (/api/admin/solutions) 原先未纳入 Sa-Token 登录拦截，
--   未携带 satoken 的请求也能 200 通过。现已：
--     Step1: SaTokenConfig.match 新增 /api/admin/** 强制登录校验
--     Step2: Controller 6+3 个方法加 @SaCheckPermission 注解
--            - 题解审核：oj:solution:audit
--            - 评论审核：oj:solution:comment:audit
--   本脚本（Step3）负责把这两个权限码注册到 DB，并绑定到已创建的审核角色
--   sol_audit_01，使"审核评论管理员"登录后能通过注解校验。
--
-- 执行前提：
--   - 角色已创建：sys_role.role_code = 'sol_audit_01'
--   - 超级管理员（sys_user.id=1）无需执行本脚本，StpInterfaceImpl 返回 ["*"] 自动通配
--
-- 幂等性：所有 INSERT 先 DELETE 去重，可重复执行
-- ============================================================================

USE algoviz;

-- 取审核角色 id（SOLUTION_COMMENT_AUDITOR），用于后续绑定
SET @role_audit_id = (SELECT id FROM sys_role WHERE role_code = 'SOLUTION_COMMENT_AUDITOR' LIMIT 1);

-- ============================================================================
-- 方案 A：走 sys_menu（menu_type=3 按钮级权限）+ sys_role_menu 绑定
--   StpInterfaceImpl.findPermissionCodesByUserId 的 UNION 第二支会读 sys_menu.perms
-- ============================================================================

-- A.1 清理旧记录（幂等）
DELETE FROM sys_role_menu
 WHERE role_id = @role_audit_id
   AND menu_id IN (
       SELECT id FROM sys_menu WHERE perms IN ('oj:solution:audit', 'oj:solution:comment:audit')
   );

DELETE FROM sys_menu WHERE perms IN ('oj:solution:audit', 'oj:solution:comment:audit');

-- A.2 注册按钮级权限菜单（parent_id=0 顶层；如已有 OJ 父菜单可按需调整 parent_id）
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, visible, status, created_at, updated_at)
VALUES
  (0, '题解审核权限', 3, NULL, NULL, 'oj:solution:audit',         NULL, 0, 1, 1, NOW(), NOW()),
  (0, '评论审核权限', 3, NULL, NULL, 'oj:solution:comment:audit', NULL, 0, 1, 1, NOW(), NOW());

-- A.3 绑定到 sol_audit_01 角色
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @role_audit_id, id FROM sys_menu
 WHERE perms IN ('oj:solution:audit', 'oj:solution:comment:audit');

-- ============================================================================
-- 方案 B：走 sys_permission（perm_type=5 审核）+ sys_role_permission 绑定
--   StpInterfaceImpl.findPermissionCodesByUserId 的 UNION 第一支会读 sys_permission.perm_code
--   与方案 A 双保险，任意一张表有权限码都能通过 @SaCheckPermission 校验
-- ============================================================================

-- B.1 清理旧记录（幂等）
DELETE FROM sys_role_permission
 WHERE role_id = @role_audit_id
   AND permission_id IN (
       SELECT id FROM sys_permission WHERE perm_code IN ('oj:solution:audit', 'oj:solution:comment:audit')
   );

DELETE FROM sys_permission WHERE perm_code IN ('oj:solution:audit', 'oj:solution:comment:audit');

-- B.2 注册权限点（perm_type=5 表示审核类；menu_id 关联到方案 A 刚插入的菜单记录）
SET @menu_sol_id = (SELECT id FROM sys_menu WHERE perms = 'oj:solution:audit' LIMIT 1);
SET @menu_cmt_id = (SELECT id FROM sys_menu WHERE perms = 'oj:solution:comment:audit' LIMIT 1);

INSERT INTO sys_permission (menu_id, perm_code, perm_name, perm_type, api_method, api_path, status, created_at, updated_at)
VALUES
  (@menu_sol_id, 'oj:solution:audit',         'OJ题解审核', 5, 'GET/PUT', '/api/admin/solutions',           1, NOW(), NOW()),
  (@menu_cmt_id, 'oj:solution:comment:audit', 'OJ评论审核', 5, 'GET/PUT', '/api/admin/solutions/comments',  1, NOW(), NOW());

-- B.3 绑定到 sol_audit_01 角色
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT @role_audit_id, id FROM sys_permission
 WHERE perm_code IN ('oj:solution:audit', 'oj:solution:comment:audit');

-- ============================================================================
-- 验证查询（执行后应看到 2 行 menu + 2 行 permission + 4 行绑定）
-- ============================================================================

SELECT '== sys_menu ==' AS section;
SELECT id, parent_id, menu_name, menu_type, perms, status FROM sys_menu
 WHERE perms IN ('oj:solution:audit', 'oj:solution:comment:audit');

SELECT '== sys_permission ==' AS section;
SELECT id, perm_code, perm_name, perm_type, status FROM sys_permission
 WHERE perm_code IN ('oj:solution:audit', 'oj:solution:comment:audit');

SELECT '== sys_role_menu (SOLUTION_COMMENT_AUDITOR) ==' AS section;
SELECT rm.role_id, r.role_code, rm.menu_id, m.perms
  FROM sys_role_menu rm
  JOIN sys_role r ON r.id = rm.role_id
  JOIN sys_menu m ON m.id = rm.menu_id
 WHERE r.role_code = 'SOLUTION_COMMENT_AUDITOR';

SELECT '== sys_role_permission (SOLUTION_COMMENT_AUDITOR) ==' AS section;
SELECT rp.role_id, r.role_code, rp.permission_id, p.perm_code
  FROM sys_role_permission rp
  JOIN sys_role r ON r.id = rp.role_id
  JOIN sys_permission p ON p.id = rp.permission_id
 WHERE r.role_code = 'SOLUTION_COMMENT_AUDITOR';

--16
-- 迁移说明：题解/评论审核管理列表查询索引优化
--
-- 背景：
--   后台管理员按 audit_status 筛选评论/题解列表时，COUNT 查询耗时 ~90ms
--   根因：现有索引 (audit_status, created_at) 不含 status 列，
--   MySQL 判定 status != 'DELETED' 时必须回表（随机 I/O），4.6 万行 = 36K 次回表
--
-- 优化：
--   将 status 作为索引尾列加入，变为覆盖索引 (audit_status, created_at, status)
--   - COUNT: 纯索引扫描，零回表
--   - SELECT + ORDER BY created_at DESC + LIMIT: 索引正序读 → created_at DESC，
--     status 从索引取 → 零回表，LIMIT 20 只读 20 条
--   - status 放在末尾不改变索引排序，不影响 ORDER BY 优化
--
-- 幂等：先 DROP IF EXISTS 再 CREATE
-- ============================================================================

-- USE algoviz;  -- 按实际库名修改，或命令行 -D 指定

-- ① oj_solution_comment：评论审核列表覆盖索引
DROP INDEX idx_comment_audit ON oj_solution_comment;
CREATE INDEX idx_comment_audit ON oj_solution_comment (audit_status, created_at DESC, status);

-- ② oj_solution：题解审核列表覆盖索引
DROP INDEX idx_solution_audit ON oj_solution;
CREATE INDEX idx_solution_audit ON oj_solution (audit_status, created_at DESC, status);
-- 17============ ① 存量值迁移：online→0(在线)、其余→1(离线) ============
UPDATE `user`
SET `login_status` = CASE
    WHEN `login_status` IN ('online', '0') THEN 0
    ELSE 1
END;

-- ============ ② 改类型：varchar(20) -> tinyint(1) ============
-- 语义：0 = 在线（已登录），1 = 离线（默认值，新注册用户离线）
ALTER TABLE `user`
    MODIFY COLUMN `login_status` TINYINT(1) NOT NULL DEFAULT 1
    COMMENT '登录状态: 0=在线 1=离线';

-- 索引 idx_user_login_created（含 login_status, created_at）类型变更后自动适配，无需重建

-- ============================================================================
-- 18============ user 表结构升级：gender 整型化 / 手机号预留 / 逻辑删除 / 注销状态
-- 语义约定：
--   gender       TINYINT(1)  : 1=男 0=女，NULL=未知（原 '男'/'女'/'未知' 字符串迁移）
--   phone        VARCHAR(20) : 手机号，预留字段，暂不启用（先不加唯一索引）
--   is_deleted   TINYINT(1)  : 0=正常 1=已逻辑删除；所有业务 SQL 必须带 is_deleted=0
--   status       TINYINT(1)  : 1=正常 0=封禁 -1=注销
-- 唯一索引策略（重要）：
--   uk_user_username / uk_user_email 保持不变 —— 逻辑删除/注销后用户名与邮箱
--   永久占用，新用户注册不可复用（防止冒名注册）。应用层注册查重使用
--   "不过滤 is_deleted" 的查询提前拦截，唯一索引作为并发兜底。
-- ============================================================================

-- ① gender: varchar(10) -> tinyint(1)，'男'->1，'女'->0，'未知'/其他->NULL
UPDATE `user`
SET `gender` = CASE `gender`
    WHEN '男'   THEN '1'
    WHEN '女'   THEN '0'
    ELSE NULL
END;

ALTER TABLE `user`
    MODIFY COLUMN `gender` TINYINT(1) DEFAULT NULL
    COMMENT '性别: 1=男 0=女';

-- ② 新增手机号（预留字段，暂不启用，不建唯一索引）
ALTER TABLE `user`
    ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL
    COMMENT '手机号（预留，暂不启用）' AFTER `email`;

-- ③ 新增逻辑删除字段：0=正常 1=已删除
ALTER TABLE `user`
    ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '逻辑删除: 0=正常 1=已删除' AFTER `status`;

-- ④ status 语义升级：1=正常 0=封禁 -1=注销（先兜底 NULL 数据，再收紧 NOT NULL）
UPDATE `user` SET `status` = 1 WHERE `status` IS NULL;
ALTER TABLE `user`
    MODIFY COLUMN `status` TINYINT(1) NOT NULL DEFAULT 1
    COMMENT '账号状态: 1=正常 0=封禁 -1=注销';

-- ⑤ 索引重建：is_deleted 作为前导等值列，保证所有业务 SQL 带 is_deleted=0 后
--    筛选 + ORDER BY created_at 全部走索引（is_deleted=1 行极少，过滤成本极低）
DROP INDEX `idx_user_created_at`   ON `user`;
CREATE INDEX `idx_user_deleted_created`       ON `user` (`is_deleted`, `created_at`);

DROP INDEX `idx_user_status_created` ON `user`;
CREATE INDEX `idx_user_deleted_status_created` ON `user` (`is_deleted`, `status`, `created_at`);

DROP INDEX `idx_user_gender_created` ON `user`;
CREATE INDEX `idx_user_deleted_gender_created` ON `user` (`is_deleted`, `gender`, `created_at`);

DROP INDEX `idx_user_login_created` ON `user`;
CREATE INDEX `idx_user_deleted_login_created`  ON `user` (`is_deleted`, `login_status`, `created_at`);

-- ⑥ 唯一索引无需改动：uk_user_username / uk_user_email 天然覆盖"含已删除行"的
--    全局唯一性，逻辑删除/注销账号的用户名与邮箱永久不可再注册。

-- ============================================================================
-- 19============ 模块访问统计拆表：user 表 6 列 -> 独立 user_visit_stat 表
-- 拆出字段：
--   ai_dialogues    AI 对话次数      -> user_visit_stat.ai_dialogues
--   ds_visits       数据结构访问次数 -> user_visit_stat.ds_visits
--   algo_visits     算法访问次数     -> user_visit_stat.algo_visits
--   oj_visits       OJ 访问次数      -> user_visit_stat.oj_visits
--   last_login_at   最后登录时间     -> user_visit_stat.last_login_at
--   last_visit_time 最后访问时间     -> user_visit_stat.last_visit_time
-- 设计理由：
--   1) 这 6 列都是高频更新的业务统计计数器，与用户基础资料读写竞争同一行，
--      拆表后互不影响（用户改名/改头像不再被计数器自增阻塞）；
--   2) 新增/调整统计模块只动本表，用户主表无需反复 DDL；
--   3) 访问计数用 INSERT ... ON DUPLICATE KEY UPDATE 原子自增，无读-改-写竞态；
--   4) 用户核心聚合指标（coins 刷题数量 评论数等）保留在 user 主表。
-- 口径约定：
--   - user 与 user_visit_stat 为 1:1（user_id 主键），无 stat 行视为全 0；
--   - 逻辑删除(is_deleted=1)用户的 stat 行保留（审计），但 Dashboard 聚合
--     INNER JOIN user AND u.is_deleted = 0，与既有统计口径一致。
-- ============================================================================

-- ① 新建用户模块访问统计表（1:1，user_id 为主键）
CREATE TABLE IF NOT EXISTS `user_visit_stat` (
    `user_id`         BIGINT   NOT NULL COMMENT '用户ID（user.id，1:1）',
    `ai_dialogues`    INT      NOT NULL DEFAULT 0 COMMENT 'AI对话次数',
    `ds_visits`       INT      NOT NULL DEFAULT 0 COMMENT '数据结构访问次数',
    `algo_visits`     INT      NOT NULL DEFAULT 0 COMMENT '算法访问次数',
    `oj_visits`       INT      NOT NULL DEFAULT 0 COMMENT 'OJ访问次数',
    `last_login_at`   DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_visit_time` DATETIME DEFAULT NULL COMMENT '最后访问时间（任一模块上报时刷新）',
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`),
    KEY `idx_uvs_last_visit` (`last_visit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户模块访问统计表';

-- ② 存量数据迁移（含已逻辑删除用户的历史统计，一并保留用于审计）；
--    INSERT IGNORE 保证幂等，重复执行不会产生重复行
INSERT IGNORE INTO `user_visit_stat`
    (`user_id`, `ai_dialogues`, `ds_visits`, `algo_visits`, `oj_visits`, `last_login_at`, `last_visit_time`)
SELECT `id`, `ai_dialogues`, `ds_visits`, `algo_visits`, `oj_visits`, `last_login_at`, `last_visit_time`
FROM `user`;

-- ③ 迁移核验（执行删列前必须先跑，行数与总和应一致）：
--    SELECT COUNT(*) FROM user_visit_stat;                          -- 应等于 user 表总行数
--    SELECT SUM(ds_visits), SUM(algo_visits), SUM(oj_visits), SUM(ai_dialogues)
--    FROM user_visit_stat;                                          -- 与下面 user 表总和对比
--    SELECT SUM(ds_visits), SUM(algo_visits), SUM(oj_visits), SUM(ai_dialogues) FROM user;

-- ④ 删除 user 主表拆出列（确认 ③ 核验通过后再执行）
ALTER TABLE `user`
    DROP COLUMN `ai_dialogues`,
    DROP COLUMN `ds_visits`,
    DROP COLUMN `algo_visits`,
    DROP COLUMN `oj_visits`,
    DROP COLUMN `last_visit_time`,
    DROP COLUMN `last_login_at`;

-- ⑤ 兜底：为注册流程遗漏初始化的老用户补 stat 行（COUNT 自增接口 upsert 也会自动建行，
--    此句仅为数据完整性兜底，幂等）
INSERT IGNORE INTO `user_visit_stat` (`user_id`)
SELECT `id` FROM `user` WHERE `is_deleted` = 0;

-- 20 ① 新表（1:1，user_id 主键）
CREATE TABLE IF NOT EXISTS `user_visit_stat` (
    `user_id`         BIGINT   NOT NULL COMMENT '用户ID（user.id，1:1）',
    `ai_dialogues`    INT      NOT NULL DEFAULT 0 COMMENT 'AI对话次数',
    `ds_visits`       INT      NOT NULL DEFAULT 0 COMMENT '数据结构访问次数',
    `algo_visits`     INT      NOT NULL DEFAULT 0 COMMENT '算法访问次数',
    `oj_visits`       INT      NOT NULL DEFAULT 0 COMMENT 'OJ访问次数',
    `last_login_at`   DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_visit_time` DATETIME DEFAULT NULL COMMENT '最后访问时间（任一模块上报时刷新）',
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    KEY `idx_uvs_last_visit` (`last_visit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ② 存量迁移（INSERT IGNORE 幂等，含已删除用户一并保留审计）
INSERT IGNORE INTO `user_visit_stat`
    (`user_id`,`ai_dialogues`,`ds_visits`,`algo_visits`,`oj_visits`,`last_login_at`,`last_visit_time`)
SELECT `id`,`ai_dialogues`,`ds_visits`,`algo_visits`,`oj_visits`,`last_login_at`,`last_visit_time` FROM `user`;

-- ③ 核验关卡（只读）：stat 行数=user 行数；四项 SUM 逐列相等

-- ④ 删除 user 表 6 列（③ 通过后再执行，不可逆）
ALTER TABLE `user`
    DROP COLUMN `ai_dialogues`,`ds_visits`,`algo_visits`,`oj_visits`,
    DROP COLUMN `last_visit_time`,`last_login_at`;

-- ⑤ 兜底补 stat 行（幂等）
INSERT IGNORE INTO `user_visit_stat` (`user_id`) SELECT `id` FROM `user` WHERE `is_deleted`=0;