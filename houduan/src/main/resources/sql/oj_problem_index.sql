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
