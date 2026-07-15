import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', hidden: true }
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

router.beforeEach((to, from, next) => {
  const title = to.meta.title as string
  if (title) {
    document.title = `${title} - 算法可视化平台管理后台`
  }
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next({ path: '/login', replace: true })
    return
  }
  next()
})

export default router
