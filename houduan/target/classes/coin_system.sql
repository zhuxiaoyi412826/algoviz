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
