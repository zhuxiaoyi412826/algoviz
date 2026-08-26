-- ============================================================================
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
