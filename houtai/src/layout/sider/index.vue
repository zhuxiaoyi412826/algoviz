<script setup lang="ts">
import { computed, inject } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import * as icons from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getRoleAccess, canAccessRoute } from '@/config/rbac-permissions'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const layoutState = inject<any>('layoutState')
const sidebarCollapsed = computed(() => layoutState.sidebarCollapsed.value)
const toggleSidebar = () => layoutState.toggleSidebar()

const getIcon = (iconName: string) => {
  return (icons as any)[iconName] || icons.Odometer
}

// ==================== 完整菜单定义（带 menuGroup 标记） ====================
interface MenuItem {
  path: string
  name: string
  meta: { title: string; icon?: string }
  menuGroup?: string  // 权限分组
  children?: MenuItem[]
}

const fullMenuList: MenuItem[] = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    meta: { title: '首页', icon: 'Odometer' },
    menuGroup: 'dashboard'
  },
  {
    path: '/system',
    name: 'System',
    meta: { title: '系统管理', icon: 'Setting' },
    menuGroup: 'system',
    children: [
      { path: '/system/admin', name: 'AdminManagement', meta: { title: '管理员账户' }, menuGroup: 'system' },
      { path: '/system/login-log', name: 'LoginLog', meta: { title: '登录日志' }, menuGroup: 'system' },
      { path: '/system/operation-log', name: 'OperationLog', meta: { title: '操作日志' }, menuGroup: 'system' },
      { path: '/system/system-config', name: 'SystemConfig', meta: { title: '系统配置' }, menuGroup: 'system' },
      { path: '/system/security', name: 'Security', meta: { title: '安全设置' }, menuGroup: 'system' }
    ]
  },
  {
    path: '/content',
    name: 'Content',
    meta: { title: '内容管理', icon: 'Document' },
    menuGroup: 'content',
    children: [
      { path: '/content/datastructure', name: 'DataStructure', meta: { title: '数据结构可视化' }, menuGroup: 'content' },
      { path: '/content/algorithm', name: 'Algorithm', meta: { title: '算法可视化' }, menuGroup: 'content' },
      { path: '/content/animation-config', name: 'AnimationConfig', meta: { title: '动画参数配置' }, menuGroup: 'content' },
      { path: '/content/problem', name: 'Problem', meta: { title: 'OJ题目管理' }, menuGroup: 'content' },
      { path: '/content/interview-problem', name: 'InterviewProblem', meta: { title: '面试题目管理' }, menuGroup: 'content' },
      { path: '/content/testcase', name: 'TestCase', meta: { title: '测试用例管理' }, menuGroup: 'content' },
      { path: '/content/judge-config', name: 'JudgeConfig', meta: { title: '判题规则配置' }, menuGroup: 'content' },
      { path: '/content/ai-prompt', name: 'AIPrompt', meta: { title: 'AI快捷提示词' }, menuGroup: 'content' },
      { path: '/content/ai-config', name: 'AIConfig', meta: { title: 'AI接口配置' }, menuGroup: 'content' },
      { path: '/content/vector-manage', name: 'VectorManage', meta: { title: '向量数据库' }, menuGroup: 'content' },
      { path: '/content/algorithm-vector', name: 'AlgorithmVector', meta: { title: '算法题目向量管理' }, menuGroup: 'content' },
      { path: '/content/chromadb-monitor', name: 'ChromaDBMonitor', meta: { title: 'ChromaDB实时检测' }, menuGroup: 'content' },
      { path: '/content/qdrant-monitor', name: 'QdrantMonitor', meta: { title: 'Qdrant实时检测' }, menuGroup: 'content' },
      { path: '/content/es-manage', name: 'EsManage', meta: { title: 'ES分词索引' }, menuGroup: 'content' }
    ]
  },
  {
    path: '/audit',
    name: 'Audit',
    meta: { title: '内容审核', icon: 'Lock' },
    menuGroup: 'audit',
    children: [
      { path: '/audit/sensitive-words', name: 'SensitiveWords', meta: { title: '关键词屏蔽' }, menuGroup: 'audit' },
      { path: '/audit/review', name: 'AuditReview', meta: { title: '人工审核' }, menuGroup: 'audit' },
      { path: '/audit/solution-review', name: 'SolutionReview', meta: { title: '题解审核' }, menuGroup: 'audit' }
    ]
  },
  {
    path: '/user',
    name: 'User',
    meta: { title: '用户管理', icon: 'User' },
    menuGroup: 'user',
    children: [
      { path: '/user/list', name: 'UserList', meta: { title: '用户列表' }, menuGroup: 'user' },
      { path: '/user/login-record', name: 'UserLoginRecord', meta: { title: '登录记录' }, menuGroup: 'user' },
      { path: '/user/behavior', name: 'UserBehavior', meta: { title: '行为数据' }, menuGroup: 'user' }
    ]
  },
  {
    path: '/operation',
    name: 'Operation',
    meta: { title: '日志平台', icon: 'Monitor' },
    menuGroup: 'log',
    children: [
      { path: '/operation/monitor', name: 'ServiceMonitor', meta: { title: '服务状态' }, menuGroup: 'operation' },
      { path: '/operation/resource', name: 'ResourceMonitor', meta: { title: '资源占用' }, menuGroup: 'operation' },
      { path: '/operation/alarm', name: 'Alarm', meta: { title: '异常告警' }, menuGroup: 'operation' },
      { path: '/operation/log', name: 'LogManage', meta: { title: '日志管理' }, menuGroup: 'log' }
    ]
  },
  {
    path: '/statistics',
    name: 'Statistics',
    meta: { title: '数据报表', icon: 'DataAnalysis' },
    menuGroup: 'statistics',
    children: [
      { path: '/statistics/dashboard', name: 'StatisticsDashboard', meta: { title: '核心指标' }, menuGroup: 'statistics' },
      { path: '/statistics/oj-analysis', name: 'OJAnalysis', meta: { title: 'OJ运营分析' }, menuGroup: 'statistics' },
      { path: '/statistics/visualization-analysis', name: 'VisualizationAnalysis', meta: { title: '可视化分析' }, menuGroup: 'statistics' },
      { path: '/statistics/export', name: 'DataExport', meta: { title: '数据导出' }, menuGroup: 'statistics' }
    ]
  },
  {
    path: '/order',
    name: 'Order',
    meta: { title: '订单财务', icon: 'ShoppingCart' },
    menuGroup: 'order',
    children: [
      { path: '/order/list', name: 'OrderList', meta: { title: '订单列表' }, menuGroup: 'order' },
      { path: '/order/coin-dashboard', name: 'CoinDashboard', meta: { title: '硬币收入看板' }, menuGroup: 'order' },
      { path: '/order/coin-products', name: 'CoinProductManage', meta: { title: '硬币商品管理' }, menuGroup: 'order' },
      { path: '/order/coin-purchase', name: 'CoinPurchase', meta: { title: '硬币购买记录' }, menuGroup: 'order' }
    ]
  },
  {
    path: '/extension',
    name: 'Extension',
    meta: { title: '客服工单', icon: 'Service' },
    menuGroup: 'extension',
    children: [
      { path: '/extension/announcement', name: 'Announcement', meta: { title: '公告管理' }, menuGroup: 'extension' },
      { path: '/extension/feedback', name: 'Feedback', meta: { title: '反馈管理' }, menuGroup: 'extension' },
      { path: '/extension/changelog', name: 'Changelog', meta: { title: '更新日志' }, menuGroup: 'extension' },
      { path: '/extension/backup', name: 'Backup', meta: { title: '数据备份' }, menuGroup: 'extension' },
      { path: '/extension/third-party', name: 'ThirdParty', meta: { title: '第三方集成' }, menuGroup: 'extension' }
    ]
  }
]

// ==================== 根据角色过滤菜单 ====================
const menuList = computed(() => {
  const roles = userStore.roles
  if (!roles || roles.length === 0) {
    // 未获取角色信息时，仅显示首页
    return [fullMenuList[0]]
  }

  return fullMenuList.filter(item => {
    const group = item.menuGroup || ''
    const access = getRoleAccess(roles, group)

    if (access === 'none') return false

    // 过滤子菜单
    if (item.children && item.children.length > 0) {
      item.children = item.children.filter(child => {
        const childGroup = child.menuGroup || group
        const childAccess = getRoleAccess(roles, childGroup)

        if (childAccess === 'none') return false
        if (childAccess === 'full') return true

        // partial → 检查是否在允许的子菜单列表中
        return canAccessRoute(roles, child.path)
      })

      // 如果所有子菜单都被过滤掉了，且父菜单不是 full 权限，则隐藏父菜单
      if (item.children.length === 0 && access !== 'full') {
        return false
      }
    }

    return true
  })
})

const activeIndex = computed(() => route.path)

const handleSelect = (index: string) => {
  router.push(index)
}
</script>

<template>
  <aside class="sidebar" :class="{ 'sidebar--collapse': sidebarCollapsed }">
    <div class="sidebar-header">
      <div class="logo">
        <el-icon :size="28"><component :is="getIcon('Odometer')" /></el-icon>
        <span v-show="!sidebarCollapsed" class="logo-text">算法可视化平台</span>
      </div>
    </div>
    <el-menu
      :default-active="activeIndex"
      :collapse="sidebarCollapsed"
      :collapse-transition="false"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
      @select="handleSelect"
    >
      <template v-for="item in menuList" :key="item.path">
        <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
          <template #title>
            <el-icon><component :is="getIcon(item.meta.icon || 'Odometer')" /></el-icon>
            <span>{{ item.meta.title }}</span>
          </template>
          <el-menu-item
            v-for="child in item.children"
            :key="child.path"
            :index="child.path"
          >
            {{ child.meta.title }}
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="item.path">
          <el-icon><component :is="getIcon(item.meta.icon || 'Odometer')" /></el-icon>
          <span>{{ item.meta.title }}</span>
        </el-menu-item>
      </template>
    </el-menu>
    <button class="sidebar-toggle" :class="{ 'sidebar-toggle--collapse': sidebarCollapsed }" @click="toggleSidebar" :title="sidebarCollapsed ? '展开菜单' : '收起菜单'">
      <span class="triangle"></span>
    </button>
  </aside>
</template>

<style scoped lang="scss">
.sidebar {
  width: 220px;
  height: 100vh;
  background-color: var(--color-bg-sidebar);
  transition: width 0.3s ease;
  overflow-x: hidden;
  flex-shrink: 0;
  position: relative;

  &--collapse {
    width: 64px;
  }
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid #1f2d3d;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  overflow: hidden;

  .logo-text {
    font-size: 16px;
    font-weight: 600;
    white-space: nowrap;
  }
}

.sidebar-toggle {
  position: absolute;
  top: 50%;
  right: -12px;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background-color: #409eff;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4);
  z-index: 10;
  transition: all 0.3s ease;
  padding: 0;

  &:hover {
    background-color: #66b1ff;
    box-shadow: 0 2px 12px rgba(64, 158, 255, 0.6);
    transform: translateY(-50%) scale(1.1);
  }

  &--collapse {
    right: -12px;
    .triangle {
      border-left-color: #fff;
      border-right-color: transparent;
    }
  }

  .triangle {
    display: inline-block;
    width: 0;
    height: 0;
    border-top: 5px solid transparent;
    border-bottom: 5px solid transparent;
    border-left: 6px solid transparent;
    border-right: 6px solid #fff;
    transition: transform 0.3s ease;
  }

  &--collapse .triangle {
    border-left-color: #fff;
    border-right-color: transparent;
  }
}

.el-menu {
  border-right: none;
  background-color: transparent !important;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  &:hover {
    background-color: #263445 !important;
  }
}

:deep(.el-menu-item.is-active) {
  background-color: #263445 !important;
}
</style>
