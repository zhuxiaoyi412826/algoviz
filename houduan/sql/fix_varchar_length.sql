-- ============================================================================
-- 修复 oj_problem.id 长度不足（UUID 36 字符塞不进 VARCHAR(32)）
-- 把所有可能存 UUID 的 VARCHAR(32) 主键统一扩到 VARCHAR(40)
-- 幂等：可重复执行
-- ============================================================================

-- 题目相关
ALTER TABLE `oj_problem`     MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `test_case`      MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `test_case`      MODIFY COLUMN `problem_id` VARCHAR(40) NOT NULL;

-- 管理员 / 日志
ALTER TABLE `admin`          MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `login_log`      MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `login_log`      MODIFY COLUMN `user_id`    VARCHAR(40) NOT NULL;
ALTER TABLE `operation_log`  MODIFY COLUMN `id`         VARCHAR(40) NOT NULL;
ALTER TABLE `operation_log`  MODIFY COLUMN `user_id`    VARCHAR(40) NOT NULL;

-- 内容相关
ALTER TABLE `data_structure` MODIFY COLUMN `id`          VARCHAR(40) NOT NULL;
ALTER TABLE `algorithm`      MODIFY COLUMN `id`          VARCHAR(40) NOT NULL;
ALTER TABLE `learning_path`  MODIFY COLUMN `id`          VARCHAR(40) NOT NULL;
ALTER TABLE `learning_node`  MODIFY COLUMN `id`          VARCHAR(40) NOT NULL;
ALTER TABLE `learning_node`  MODIFY COLUMN `path_id`     VARCHAR(40) NOT NULL;

-- 支付 / 文件 / 日志
ALTER TABLE `payment_record` MODIFY COLUMN `id`          VARCHAR(40) NOT NULL;
ALTER TABLE `file_storage`   MODIFY COLUMN `id`          VARCHAR(40) NOT NULL;
ALTER TABLE `api_log`        MODIFY COLUMN `id`          VARCHAR(40) NOT NULL;

SELECT '字段长度修复完成' AS message;
