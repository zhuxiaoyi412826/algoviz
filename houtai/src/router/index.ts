import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { canAccessRoute } from '@/config/rbac-permissions'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', hidden: true }
  },
  // 更新日志独立编辑器：新窗口打开，不包 Layout，纯 Vditor + 保存/取消
  {
    path: '/changelog/new',
    name: 'ChangelogCreateStandalone',
    component: () => import('@/views/extension/ChangelogEdit.vue'),
    meta: { title: '新增更新日志', hidden: true, standalone: true }
  },
  {
    path: '/changelog/edit/:id',
    name: 'ChangelogEditStandalone',
    component: () => import('@/views/extension/ChangelogEdit.vue'),
    meta: { title: '编辑更新日志', hidden: true, standalone: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页', icon: 'Odometer' }
      },
      // 系统管理模块
      {
        path: 'system',
        name: 'System',
        redirect: '/system/admin',
        meta: { title: '系统管理', icon: 'Setting' },
        children: [
          {
            path: 'profile',
            name: 'Profile',
            component: () => import('@/views/system/Profile.vue'),
            meta: { title: '个人中心', hidden: true }
          },
          {
            path: 'admin',
            name: 'AdminManagement',
            component: () => import('@/views/system/AdminManagement.vue'),
            meta: { title: '管理员账户', roles: ['super_admin'] }
          },
          {
            path: 'login-log',
            name: 'LoginLog',
            component: () => import('@/views/system/LoginLog.vue'),
            meta: { title: '登录日志' }
          },
          {
            path: 'operation-log',
            name: 'OperationLog',
            component: () => import('@/views/system/OperationLog.vue'),
            meta: { title: '操作日志' }
          },
          {
            path: 'system-config',
            name: 'SystemConfig',
            component: () => import('@/views/system/SystemConfig.vue'),
            meta: { title: '系统配置' }
          },
          {
            path: 'security',
            name: 'Security',
            component: () => import('@/views/system/Security.vue'),
            meta: { title: '安全设置', roles: ['super_admin'] }
          }
        ]
      },
      // 内容管理模块
      {
        path: 'content',
        name: 'Content',
        redirect: '/content/datastructure',
        meta: { title: '内容管理', icon: 'Document' },
        children: [
          {
            path: 'datastructure',
            name: 'DataStructure',
            component: () => import('@/views/content/DataStructure.vue'),
            meta: { title: '数据结构可视化' }
          },
          {
            path: 'algorithm',
            name: 'Algorithm',
            component: () => import('@/views/content/Algorithm.vue'),
            meta: { title: '算法可视化' }
          },
          {
            path: 'animation-config',
            name: 'AnimationConfig',
            component: () => import('@/views/content/AnimationConfig.vue'),
            meta: { title: '动画参数配置' }
          },
          {
            path: 'problem',
            name: 'Problem',
            component: () => import('@/views/content/Problem.vue'),
            meta: { title: 'OJ题目管理' }
          },
          {
            path: 'interview-problem',
            name: 'InterviewProblem',
            component: () => import('@/views/content/InterviewProblem.vue'),
            meta: { title: '面试题目管理' }
          },
          {
            path: 'testcase',
            name: 'TestCase',
            component: () => import('@/views/content/TestCase.vue'),
            meta: { title: '测试用例管理' }
          },
          {
            path: 'judge-config',
            name: 'JudgeConfig',
            component: () => import('@/views/content/JudgeConfig.vue'),
            meta: { title: '判题规则配置' }
          },
          {
            path: 'ai-prompt',
            name: 'AIPrompt',
            component: () => import('@/views/content/AIPrompt.vue'),
            meta: { title: 'AI快捷提示词' }
          },
          {
            path: 'ai-config',
            name: 'AIConfig',
            component: () => import('@/views/content/AIConfig.vue'),
            meta: { title: 'AI接口配置', roles: ['super_admin'] }
          },
          {
            path: 'vector-manage',
            name: 'VectorManage',
            component: () => import('@/views/content/VectorManage.vue'),
            meta: { title: '向量数据库' }
          },
          {
            path: 'chromadb-monitor',
            name: 'ChromaDBMonitor',
            component: () => import('@/views/content/ChromaDBMonitor.vue'),
            meta: { title: 'ChromaDB实时检测' }
          },
          {
            path: 'es-manage',
            name: 'EsManage',
            component: () => import('@/views/content/EsManage.vue'),
            meta: { title: 'ES分词索引' }
          }
        ]
      },
      // 内容审核模块（关键词屏蔽 + 人工审核）
      {
        path: 'audit',
        name: 'Audit',
        redirect: '/audit/sensitive-words',
        meta: { title: '内容审核', icon: 'Lock' },
        children: [
          {
            path: 'sensitive-words',
            name: 'SensitiveWords',
            component: () => import('@/views/audit/SensitiveWordManage.vue'),
            meta: { title: '关键词屏蔽' }
          },
          {
            path: 'review',
            name: 'AuditReview',
            component: () => import('@/views/audit/AuditReview.vue'),
            meta: { title: '人工审核' }
          },
          {
            path: 'solution-review',
            name: 'SolutionReview',
            component: () => import('@/views/audit/SolutionReview.vue'),
            meta: { title: '题解审核' }
          }
        ]
      },
      // 用户管理模块
      {
        path: 'user',
        name: 'User',
        redirect: '/user/list',
        meta: { title: '用户管理', icon: 'User' },
        children: [
          {
            path: 'list',
            name: 'UserList',
            component: () => import('@/views/user/UserList.vue'),
            meta: { title: '用户列表' }
          },
          {
            path: 'login-record',
            name: 'UserLoginRecord',
            component: () => import('@/views/user/UserLoginRecord.vue'),
            meta: { title: '登录记录' }
          },
          {
            path: 'behavior',
            name: 'UserBehavior',
            component: () => import('@/views/user/UserBehavior.vue'),
            meta: { title: '行为数据' }
          }
        ]
      },
      // 订单管理模块
      {
        path: 'order',
        name: 'Order',
        redirect: '/order/list',
        meta: { title: '订单管理', icon: 'ShoppingCart' },
        children: [
          {
            path: 'list',
            name: 'OrderList',
            component: () => import('@/views/order/OrderList.vue'),
            meta: { title: '订单列表' }
          },
          {
            path: 'coin-dashboard',
            name: 'CoinDashboard',
            component: () => import('@/views/coin/CoinDashboard.vue'),
            meta: { title: '硬币收入看板' }
          },
          {
            path: 'coin-products',
            name: 'CoinProductManage',
            component: () => import('@/views/coin/CoinProductManage.vue'),
            meta: { title: '硬币商品管理' }
          },
          {
            path: 'coin-purchase',
            name: 'CoinPurchase',
            component: () => import('@/views/coin/CoinPurchase.vue'),
            meta: { title: '硬币购买记录' }
          }
        ]
      },
      // 数据统计模块
      {
        path: 'statistics',
        name: 'Statistics',
        redirect: '/statistics/dashboard',
        meta: { title: '数据统计', icon: 'DataAnalysis' },
        children: [
          {
            path: 'dashboard',
            name: 'StatisticsDashboard',
            component: () => import('@/views/statistics/Dashboard.vue'),
            meta: { title: '核心指标' }
          },
          {
            path: 'oj-analysis',
            name: 'OJAnalysis',
            component: () => import('@/views/statistics/OJAnalysis.vue'),
            meta: { title: 'OJ运营分析' }
          },
          {
            path: 'visualization-analysis',
            name: 'VisualizationAnalysis',
            component: () => import('@/views/statistics/VisualizationAnalysis.vue'),
            meta: { title: '可视化分析' }
          },
          {
            path: 'export',
            name: 'DataExport',
            component: () => import('@/views/statistics/DataExport.vue'),
            meta: { title: '数据导出' }
          }
        ]
      },
      // 运维监控模块
      {
        path: 'operation',
        name: 'Operation',
        redirect: '/operation/monitor',
        meta: { title: '运维监控', icon: 'Monitor' },
        children: [
          {
            path: 'monitor',
            name: 'ServiceMonitor',
            component: () => import('@/views/operation/Monitor.vue'),
            meta: { title: '服务状态' }
          },
          {
            path: 'resource',
            name: 'ResourceMonitor',
            component: () => import('@/views/operation/Resource.vue'),
            meta: { title: '资源占用' }
          },
          {
            path: 'alarm',
            name: 'Alarm',
            component: () => import('@/views/operation/Alarm.vue'),
            meta: { title: '异常告警' }
          },
          {
            path: 'log',
            name: 'LogManage',
            component: () => import('@/views/operation/Log.vue'),
            meta: { title: '日志管理' }
          }
        ]
      },
      // 扩展功能模块
      {
        path: 'extension',
        name: 'Extension',
        redirect: '/extension/announcement',
        meta: { title: '扩展功能', icon: 'Extension' },
        children: [
          {
            path: 'announcement',
            name: 'Announcement',
            component: () => import('@/views/extension/Announcement.vue'),
            meta: { title: '公告管理' }
          },
          {
            path: 'feedback',
            name: 'Feedback',
            component: () => import('@/views/extension/Feedback.vue'),
            meta: { title: '反馈管理' }
          },
          {
            path: 'changelog',
            name: 'Changelog',
            component: () => import('@/views/extension/ChangelogList.vue'),
            meta: { title: '更新日志' }
          },
          {
            path: 'backup',
            name: 'Backup',
            component: () => import('@/views/extension/Backup.vue'),
            meta: { title: '数据备份', roles: ['super_admin'] }
          },
          {
            path: 'third-party',
            name: 'ThirdParty',
            component: () => import('@/views/extension/ThirdParty.vue'),
            meta: { title: '第三方集成', roles: ['super_admin'] }
          }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 标记是否已完成首次 session 校验
let sessionValidated = false

router.beforeEach(async (to, from, next) => {
  const title = to.meta.title as string
  if (title) {
    document.title = `${title} - 算法可视化平台管理后台`
  }

  const token = localStorage.getItem('token')

  // 未登录 → 跳登录页
  if (to.path !== '/login' && !token) {
    next({ path: '/login', replace: true })
    return
  }

  // 已在登录页且已登录 → 跳首页
  if (to.path === '/login' && token) {
    next({ path: '/', replace: true })
    return
  }

  // 首次进入（刷新页面）时校验 session 是否仍然有效
  if (token && !sessionValidated && to.path !== '/login') {
    sessionValidated = true
    try {
      const { default: request } = await import('@/api/request')
      const data = await request.get('/admin/info')
      // 同步最新的用户信息到 store（修复刷新后角色丢失问题）
      const { useUserStore } = await import('@/stores/user')
      const userStore = useUserStore()
      const serverUserInfo = data?.userInfo
      const serverRoles = data?.roles || []
      const serverPermissions = data?.permissions || []

      if (serverUserInfo) {
        const adaptedUserInfo = {
          id: serverUserInfo.id?.toString() || '',
          username: serverUserInfo.username || '',
          nickname: serverUserInfo.realName || serverUserInfo.username || '',
          email: serverUserInfo.email || '',
          phone: serverUserInfo.phone || '',
          role: serverRoles[0] || '',
          status: serverUserInfo.status === 1 || serverUserInfo.status === 'active' ? 'active' : 'disabled',
          avatar: serverUserInfo.avatar || '',
          createTime: serverUserInfo.createdAt || '',
          updateTime: serverUserInfo.updatedAt || '',
          lastLoginTime: serverUserInfo.lastLoginTime || ''
        }
        userStore.setUserInfo(adaptedUserInfo, serverRoles, serverPermissions)
        localStorage.setItem('userInfo', JSON.stringify(adaptedUserInfo))
        localStorage.setItem('roles', JSON.stringify(serverRoles))
        localStorage.setItem('permissions', JSON.stringify(serverPermissions))
      }
    } catch {
      // session 已过期或 token 无效 → 清除登录态，跳转登录页
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('roles')
      localStorage.removeItem('permissions')
      ElMessage.warning('登录已过期，请重新登录')
      next({ path: '/login', replace: true })
      return
    }
  }

  // 页面级权限校验：根据角色判断是否可以访问该路由
  if (token && to.path !== '/login') {
    // 从 localStorage 读取角色信息（store 可能还未初始化）
    const rolesStr = localStorage.getItem('roles') || '[]'
    const roles: string[] = JSON.parse(rolesStr)
    if (roles.length > 0 && !canAccessRoute(roles, to.path)) {
      ElMessage.warning('您没有权限访问该页面')
      next({ path: '/dashboard', replace: true })
      return
    }
  }

  next()
})

export default router
