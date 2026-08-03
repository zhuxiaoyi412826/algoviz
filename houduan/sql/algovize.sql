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
    `avatar`        VARCHAR(500) DEFAULT '👤',
    `gender`        VARCHAR(10)  DEFAULT '未知',
    `nickname`      VARCHAR(100) DEFAULT NULL,
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
INSERT INTO `user` (`username`, `email`, `password`, `avatar`, `gender`, `nickname`) VALUES
('admin',  'admin@example.com',  'admin123', '👨', '男',   '管理员'),
('user1',  'user1@example.com',  'user123',  '👤', '未知', '用户1');

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
