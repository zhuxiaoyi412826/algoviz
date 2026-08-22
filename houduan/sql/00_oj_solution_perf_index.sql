-- ================================================================
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
