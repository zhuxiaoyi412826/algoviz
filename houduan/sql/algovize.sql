-- ============================================================================
-- AlgoViz 数据库迁移脚本：SQLite → MySQL 8.0
-- 源库文件: ./algoviz.db
-- 目标库:   algoviz  (账号: zxy / 密码: 412826)
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
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `username`      VARCHAR(50)  NOT NULL,
    `email`         VARCHAR(100) NOT NULL,
    `password`      VARCHAR(255) DEFAULT NULL,
    `age`           INT          DEFAULT NULL,
    `gender`        VARCHAR(10)  DEFAULT '未知',
    `nickname`      VARCHAR(100) DEFAULT NULL,
    `avatar_url`    VARCHAR(500) DEFAULT NULL,
    `login_status`  VARCHAR(20)  DEFAULT 'offline',
    `status`        TINYINT      DEFAULT 1 COMMENT '1:正常 0:封禁',
    `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `last_login_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_email` (`email`),
    KEY `idx_user_nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

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

-- 管理员表
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
    `id`              VARCHAR(32)  NOT NULL,
    `username`        VARCHAR(50)  NOT NULL,
    `nickname`        VARCHAR(100) NOT NULL,
    `password`        VARCHAR(255) NOT NULL,
    `avatar`          VARCHAR(500) DEFAULT NULL,
    `email`           VARCHAR(100) DEFAULT NULL,
    `phone`           VARCHAR(20)  DEFAULT NULL,
    `role`            VARCHAR(50)  DEFAULT 'content_admin',
    `status`          VARCHAR(20)  DEFAULT 'active',
    `last_login_time` DATETIME     DEFAULT NULL,
    `last_login_ip`   VARCHAR(64)  DEFAULT NULL,
    `created_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

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
('admin',  'admin@example.com',  'admin123', 25, '男',   '管理员', 'https://i.pravatar.cc/150?u=1', 'offline', 1),
('user1',  'user1@example.com',  'user123',  NULL, '未知', '用户1', 'https://i.pravatar.cc/150?u=2', 'offline', 1);

-- 管理员账号（密码: admin123）
INSERT INTO `admin` (`id`, `username`, `nickname`, `password`, `role`, `status`) VALUES
('1', 'admin', '超级管理员', '412826zxyZXY', 'super_admin', 'active');

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

-- 管理员 / 日志
ALTER TABLE `admin`          MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
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
SELECT COUNT(*) AS admin_count     FROM `admin`;

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
