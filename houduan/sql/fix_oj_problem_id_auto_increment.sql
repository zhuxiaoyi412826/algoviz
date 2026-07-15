-- ============================================================================
-- 把 oj_problem.id 从 VARCHAR 改成 BIGINT 自增
-- 策略：因为业务上 id 没强依赖（前端只用来定位），直接 MODIFY 列类型
-- 幂等：可重复执行
-- ============================================================================

-- 关外键检查
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `oj_problem`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键自增';

-- 开外键检查
SET FOREIGN_KEY_CHECKS = 1;

SELECT 'oj_problem.id 改为自增 BIGINT 完成' AS message;
