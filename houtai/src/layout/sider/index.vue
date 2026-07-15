<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import * as icons from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const isCollapse = ref(false)

const getIcon = (iconName: string) => {
  return (icons as any)[iconName] || icons.Odometer
}

const menuList = [
  {
    path: '/dashboard',
    name: 'Dashboard',
    meta: { title: '首页', icon: 'Odometer' }
  },
  {
    path: '/system',
    name: 'System',
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      { path: '/system/admin', name: 'AdminManagement', meta: { title: '管理员账户' } },
      { path: '/system/login-log', name: 'LoginLog', meta: { title: '登录日志' } },
      { path: '/system/operation-log', name: 'OperationLog', meta: { title: '操作日志' } },
      { path: '/system/system-config', name: 'SystemConfig', meta: { title: '系统配置' } },
      { path: '/system/security', name: 'Security', meta: { title: '安全设置' } }
    ]
  },
  {
    path: '/content',
    name: 'Content',
    meta: { title: '内容管理', icon: 'Document' },
    children: [
      { path: '/content/datastructure', name: 'DataStructure', meta: { title: '数据结构可视化' } },
      { path: '/content/algorithm', name: 'Algorithm', meta: { title: '算法可视化' } },
      { path: '/content/animation-config', name: 'AnimationConfig', meta: { title: '动画参数配置' } },
      { path: '/content/problem', name: 'Problem', meta: { title: 'OJ题目管理' } },
      { path: '/content/testcase', name: 'TestCase', meta: { title: '测试用例管理' } },
      { path: '/content/judge-config', name: 'JudgeConfig', meta: { title: '判题规则配置' } },
      { path: '/content/ai-prompt', name: 'AIPrompt', meta: { title: 'AI快捷提示词' } },
      { path: '/content/ai-config', name: 'AIConfig', meta: { title: 'AI接口配置' } }
    ]
  },
  {
    path: '/user',
    name: 'User',
    meta: { title: '用户管理', icon: 'User' },
    children: [
      { path: '/user/list', name: 'UserList', meta: { title: '用户列表' } },
      { path: '/user/login-record', name: 'UserLoginRecord', meta: { title: '登录记录' } },
      { path: '/user/behavior', name: 'UserBehavior', meta: { title: '行为数据' } }
    ]
  },
  {
    path: '/statistics',
    name: 'Statistics',
    meta: { title: '数据统计', icon: 'DataAnalysis' },
    children: [
      { path: '/statistics/dashboard', name: 'StatisticsDashboard', meta: { title: '核心指标' } },
      { path: '/statistics/oj-analysis', name: 'OJAnalysis', meta: { title: 'OJ运营分析' } },
      { path: '/statistics/visualization-analysis', name: 'VisualizationAnalysis', meta: { title: '可视化分析' } },
      { path: '/statistics/export', name: 'DataExport', meta: { title: '数据导出' } }
    ]
  },
  {
    path: '/operation',
    name: 'Operation',
    meta: { title: '运维监控', icon: 'Monitor' },
    children: [
      { path: '/operation/monitor', name: 'ServiceMonitor', meta: { title: '服务状态' } },
      { path: '/operation/resource', name: 'ResourceMonitor', meta: { title: '资源占用' } },
      { path: '/operation/alarm', name: 'Alarm', meta: { title: '异常告警' } },
      { path: '/operation/log', name: 'LogManage', meta: { title: '日志管理' } }
    ]
  },
  {
    path: '/extension',
    name: 'Extension',
    meta: { title: '扩展功能', icon: 'Extension' },
    children: [
      { path: '/extension/announcement', name: 'Announcement', meta: { title: '公告管理' } },
      { path: '/extension/feedback', name: 'Feedback', meta: { title: '反馈管理' } },
      { path: '/extension/backup', name: 'Backup', meta: { title: '数据备份' } },
      { path: '/extension/third-party', name: 'ThirdParty', meta: { title: '第三方集成' } }
    ]
  }
]

const activeIndex = computed(() => route.path)

const handleSelect = (index: string) => {
  router.push(index)
}
</script>

<template>
  <aside class="sidebar" :class="{ 'sidebar--collapse': isCollapse }">
    <div class="sidebar-header">
      <div class="logo">
        <el-icon :size="28"><component :is="getIcon('Odometer')" /></el-icon>
        <span v-show="!isCollapse" class="logo-text">算法可视化平台</span>
      </div>
    </div>
    <el-menu
      :default-active="activeIndex"
      :collapse="isCollapse"
      :collapse-transition="false"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
      @select="handleSelect"
    >
      <template v-for="item in menuList" :key="item.path">
        <el-sub-menu v-if="item.children" :index="item.path">
          <template #title>
            <el-icon><component :is="getIcon(item.meta.icon)" /></el-icon>
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
          <el-icon><component :is="getIcon(item.meta.icon)" /></el-icon>
          <span>{{ item.meta.title }}</span>
        </el-menu-item>
      </template>
    </el-menu>
  </aside>
</template>

<style scoped lang="scss">
.sidebar {
  width: 220px;
  height: 100vh;
  background-color: var(--color-bg-sidebar);
  transition: width 0.3s;
  overflow-x: hidden;
  flex-shrink: 0;

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
