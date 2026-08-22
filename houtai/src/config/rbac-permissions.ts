/**
 * RBAC 权限矩阵配置
 * 根据 rbac-spec.md 六、权限矩阵定义
 *
 * 菜单组与路由的映射：
 *  系统管理   → /system
 *  内容管理   → /content
 *  内容审核   → /audit
 *  题目辅助   → /content (部分子菜单)
 *  日志平台   → /operation/log (及子菜单)
 *  数据报表   → /statistics
 *  分析配置   → /statistics (部分子菜单)
 *  订单财务   → /order
 *  运营中心   → /operation (不含日志)
 *  客服工单   → /extension/feedback (及子菜单)
 *  我的题目   → /content/my-problems (外部出题人专属)
 *  审计日志   → /system/audit-log (安全审计管理员专属)
 *
 * 所有角色均可访问：首页、个人中心、修改密码
 */

// ==================== 菜单组 → 路由路径映射 ====================
export type MenuGroup =
  | 'dashboard'
  | 'system'        // 系统管理
  | 'content'       // 内容管理
  | 'audit'         // 内容审核
  | 'log'           // 日志平台
  | 'statistics'    // 数据报表+分析配置
  | 'order'         // 订单财务
  | 'operation'     // 运营中心
  | 'customer'      // 客服工单
  | 'my-problems'   // 我的题目
  | 'audit-log'     // 审计日志
  | 'extension'     // 扩展功能（公告等）

// ==================== 角色 → 菜单组权限映射 ====================
// ✅ = 可见, ✏️ = 受限（部分子菜单）, ❌ = 不可见
type Access = 'full' | 'partial' | 'none'

interface RolePermission {
  [group: string]: Access
}

export const ROLE_PERMISSIONS: Record<string, RolePermission> = {
  // ---------- 超级管理员 ----------
  SUPER_ADMIN: {
    dashboard: 'full',
    system: 'full',
    content: 'full',
    audit: 'full',
    log: 'full',
    statistics: 'full',
    order: 'full',
    operation: 'full',
    customer: 'full',
    'my-problems': 'none',
    'audit-log': 'full',
    extension: 'full'
  },

  // ---------- 一级管理员 ----------
  LEVEL1_ADMIN: {
    dashboard: 'full',
    system: 'partial',     // ✏️ 受限：不能看审计日志
    content: 'full',
    audit: 'full',
    log: 'full',
    statistics: 'full',
    order: 'full',
    operation: 'full',
    customer: 'full',
    'my-problems': 'none',
    'audit-log': 'none',   // ❌
    extension: 'full'
  },

  // ---------- 权限分配子管理员 ----------
  PERM_MANAGER: {
    dashboard: 'full',
    system: 'partial',      // ✏️ 仅管理员账户管理
    content: 'none',
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 内容发布管理员 ----------
  CONTENT_PUBLISHER: {
    dashboard: 'full',
    system: 'partial',      // ✏️ 仅个人中心
    content: 'full',
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-数据结构发布管理员 ----------
  DS_PUBLISHER: {
    dashboard: 'full',
    system: 'partial',
    content: 'partial',     // ✏️ 仅数据结构相关
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-算法发布管理员 ----------
  ALGO_PUBLISHER: {
    dashboard: 'full',
    system: 'partial',
    content: 'partial',     // ✏️ 仅算法相关
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-通用子管理员 ----------
  GENERAL_SUB_ADMIN: {
    dashboard: 'full',
    system: 'partial',
    content: 'partial',
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 内容审核管理员 ----------
  CONTENT_AUDITOR: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'full',
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-题目校验测试管理员 ----------
  TEST_VALIDATOR: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'partial',       // ✏️ 仅校验
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-关键词审核管理员 ----------
  KEYWORD_AUDITOR: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'partial',       // ✏️ 仅关键词
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-题解评论审核管理员 ----------
  SOLUTION_COMMENT_AUDITOR: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'partial',       // ✏️ 仅题解/评论
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- EFK日志分析管理员 ----------
  EFK_LOG_ADMIN: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'full',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-业务日志管理员 ----------
  BIZ_LOG_ADMIN: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'partial',         // ✏️ 仅业务日志
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-开发日志管理员 ----------
  DEV_LOG_ADMIN: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'partial',         // ✏️ 仅开发日志
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-系统日志管理员 ----------
  SYS_LOG_ADMIN: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'partial',         // ✏️ 仅系统日志
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-运维管理员 ----------
  OPS_LOG_ADMIN: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'partial',         // ✏️ 日志平台/ES配置
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 数据分析管理员 ----------
  DATA_ANALYST: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'partial',         // ✏️ 可检索日志
    statistics: 'full',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-UV/PV数据分析师 ----------
  UV_PV_ANALYST: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'none',
    statistics: 'partial',   // ✏️ 仅UV/PV报表
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-用户留存数据分析师 ----------
  RETENTION_ANALYST: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'none',
    statistics: 'partial',   // ✏️ 仅留存报表
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 二级-订单数据分析师 ----------
  ORDER_ANALYST_SUB: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'none',
    statistics: 'partial',   // ✏️ 仅订单只读分析
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 订单分析管理员 ----------
  ORDER_ADMIN: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'partial',       // ✏️ 订单/商品/金币
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 商品管理员 ----------
  PRODUCT_ADMIN: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'partial',       // ✏️ 仅商品
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 金币管理员 ----------
  COIN_ADMIN: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'partial',       // ✏️ 仅金币
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 运营活动管理员 ----------
  OPS_ADMIN: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'full',      // ✅ 运营中心
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 财务管理员 ----------
  FINANCE_ADMIN: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'partial',       // ✏️ 对账退款
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'none'
  },

  // ---------- 客服管理员 ----------
  CS_ADMIN: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'full',       // ✅ 客服工单
    'my-problems': 'none',
    'audit-log': 'none',
    extension: 'partial'    // ✏️ 反馈管理
  },

  // ---------- 安全审计管理员 ----------
  SECURITY_AUDITOR: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'none',
    'audit-log': 'full',    // ✅ 审计日志
    extension: 'none'
  },

  // ---------- 外部出题人 ----------
  EXTERNAL_AUTHOR: {
    dashboard: 'full',
    system: 'partial',
    content: 'none',
    audit: 'none',
    log: 'none',
    statistics: 'none',
    order: 'none',
    operation: 'none',
    customer: 'none',
    'my-problems': 'full',  // ✅ 我的题目
    'audit-log': 'none',
    extension: 'none'
  }
}

// ==================== 菜单组 → 受限子菜单定义 ====================
// 当角色对某菜单组为 'partial' 时，定义其可见的子菜单 path
export const PARTIAL_MENUS: Record<string, string[]> = {
  // 系统管理 partial → 仅管理员账户（权限分配子管理员）或仅个人中心
  'PERM_MANAGER:system': ['/system/admin'],
  'CONTENT_PUBLISHER:system': [],  // 仅个人中心（不在侧边栏显示）
  'DS_PUBLISHER:system': [],
  'ALGO_PUBLISHER:system': [],
  'CONTENT_AUDITOR:system': [],
  'TEST_VALIDATOR:system': [],
  'KEYWORD_AUDITOR:system': [],
  'SOLUTION_COMMENT_AUDITOR:system': [],
  'EFK_LOG_ADMIN:system': [],
  'BIZ_LOG_ADMIN:system': [],
  'DEV_LOG_ADMIN:system': [],
  'SYS_LOG_ADMIN:system': [],
  'OPS_LOG_ADMIN:system': [],
  'DATA_ANALYST:system': [],
  'UV_PV_ANALYST:system': [],
  'RETENTION_ANALYST:system': [],
  'ORDER_ANALYST_SUB:system': [],
  'ORDER_ADMIN:system': [],
  'PRODUCT_ADMIN:system': [],
  'COIN_ADMIN:system': [],
  'OPS_ADMIN:system': [],
  'FINANCE_ADMIN:system': [],
  'CS_ADMIN:system': [],
  'SECURITY_AUDITOR:system': [],

  // 内容管理 partial
  'DS_PUBLISHER:content': ['/content/datastructure'],
  'ALGO_PUBLISHER:content': ['/content/algorithm'],
  'GENERAL_SUB_ADMIN:content': ['/content/datastructure', '/content/algorithm'],

  // 内容审核 partial
  'TEST_VALIDATOR:content': [],
  'TEST_VALIDATOR:audit': ['/audit/review'],
  'KEYWORD_AUDITOR:audit': ['/audit/sensitive-words'],
  'SOLUTION_COMMENT_AUDITOR:audit': ['/audit/solution-review', '/audit/review'],

  // 日志平台 partial
  'DATA_ANALYST:log': [],
  'BIZ_LOG_ADMIN:log': ['/operation/log'],
  'DEV_LOG_ADMIN:log': ['/operation/log'],
  'SYS_LOG_ADMIN:log': ['/operation/log'],
  'OPS_LOG_ADMIN:log': ['/operation/log'],

  // 数据报表 partial
  'UV_PV_ANALYST:statistics': ['/statistics/dashboard', '/statistics/oj-analysis'],
  'RETENTION_ANALYST:statistics': ['/statistics/dashboard', '/statistics/visualization-analysis'],
  'ORDER_ANALYST_SUB:statistics': ['/statistics/dashboard', '/statistics/oj-analysis'],

  // 订单财务 partial
  'ORDER_ADMIN:order': ['/order/list', '/order/coin-dashboard', '/order/coin-products', '/order/coin-purchase'],
  'PRODUCT_ADMIN:order': ['/order/coin-products'],
  'COIN_ADMIN:order': ['/order/coin-dashboard', '/order/coin-purchase'],
  'FINANCE_ADMIN:order': ['/order/list'],

  // 一级管理员系统管理 partial → 不能看审计日志（但其他都可看）
  'LEVEL1_ADMIN:system': ['/system/admin', '/system/login-log', '/system/operation-log', '/system/system-config', '/system/security'],

  // 客服工单 partial (反馈管理)
  'CS_ADMIN:extension': ['/extension/feedback'],
}

// ==================== 工具函数 ====================

/**
 * 获取角色对某菜单组的访问权限
 * 多角色取并集：只要有一个角色有权限即可访问
 */
export function getRoleAccess(roles: string[], group: string): Access {
  if (roles.length === 0) return 'none'

  let result: Access = 'none'
  for (const role of roles) {
    const perm = ROLE_PERMISSIONS[role]
    if (!perm) continue
    const access = perm[group] || 'none'
    if (access === 'full') return 'full'
    if (access === 'partial') result = 'partial'
  }
  return result
}

/**
 * 判断角色是否可以访问某个路由路径
 */
export function canAccessRoute(roles: string[], path: string): boolean {
  if (roles.length === 0) return false

  // 超级管理员可以访问所有路由
  if (roles.includes('SUPER_ADMIN')) return true

  // 首页和个人中心所有人可访问
  if (path === '/dashboard' || path === '/' || path.startsWith('/system/profile')) return true
  if (path === '/login') return true

  // 判断属于哪个菜单组
  const group = pathToGroup(path)
  if (!group) return true  // 未知路由默认放行（如 error 页）

  const access = getRoleAccess(roles, group)
  if (access === 'none') return false

  if (access === 'full') return true

  // partial → 检查是否在允许的子菜单列表中
  for (const role of roles) {
    const key = `${role}:${group}`
    const allowed = PARTIAL_MENUS[key]
    if (allowed && allowed.length > 0) {
      // 精确匹配或前缀匹配
      if (allowed.some(p => path === p || path.startsWith(p + '/'))) {
        return true
      }
    }
    // partial 但没有定义子菜单列表 → 个人中心等隐藏页面
    // 允许访问 /system/profile 等不在侧边栏的页面
    if (path.startsWith('/system/profile')) return true
  }

  // partial 角色访问的如果是空列表（仅个人中心），则拦截非个人中心路由
  if (group === 'system' && !path.startsWith('/system/profile')) {
    return false
  }

  return false
}

/**
 * 路由路径 → 菜单组
 */
function pathToGroup(path: string): string | null {
  if (path.startsWith('/system')) {
    // 审计日志单独分组
    if (path.startsWith('/system/audit-log')) return 'audit-log'
    return 'system'
  }
  if (path.startsWith('/content')) return 'content'
  if (path.startsWith('/audit')) return 'audit'
  if (path.startsWith('/order')) return 'order'
  if (path.startsWith('/operation')) return 'operation'
  if (path.startsWith('/statistics')) return 'statistics'
  if (path.startsWith('/extension')) {
    // 客服管理员可见反馈
    return 'extension'
  }
  if (path.startsWith('/dashboard') || path === '/') return 'dashboard'
  return null
}
