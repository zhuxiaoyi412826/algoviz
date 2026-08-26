-- ============================================================================
-- 迁移说明：OJ 题解/评论审核接口权限注册
--
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
