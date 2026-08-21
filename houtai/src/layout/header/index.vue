<script setup lang="ts">
import { computed, inject } from 'vue'
import { useRouter } from 'vue-router'
import { ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar, ElIcon, ElBadge, ElTag } from 'element-plus'
import { User, Setting, SwitchButton, Bell } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const layoutState = inject<any>('layoutState')
const sidebarCollapsed = computed(() => layoutState.sidebarCollapsed.value)
const toggleSidebar = () => layoutState.toggleSidebar()

// 角色编码到中文显示的映射
const roleDisplayMap: Record<string, { label: string; level: number }> = {
  'SUPER_ADMIN': { label: '超级管理员', level: 1 },
  'LEVEL1_ADMIN': { label: '一级管理员', level: 2 },
  'PERM_MANAGER': { label: '权限分配管理员', level: 2 },
  'CONTENT_PUBLISHER': { label: '内容发布管理员', level: 2 },
  'CONTENT_AUDITOR': { label: '内容审核管理员', level: 2 },
  'EFK_LOG_ADMIN': { label: 'EFK日志分析管理员', level: 2 },
  'DATA_ANALYST': { label: '数据分析管理员', level: 2 },
  'ORDER_ADMIN': { label: '订单分析管理员', level: 2 },
  'OPS_ADMIN': { label: '运营活动管理员', level: 2 },
  'FINANCE_ADMIN': { label: '财务管理员', level: 2 },
  'CS_ADMIN': { label: '客服管理员', level: 2 },
  'SECURITY_AUDITOR': { label: '安全审计管理员', level: 2 },
  'DS_PUBLISHER': { label: '数据结构发布管理员', level: 3 },
  'ALGO_PUBLISHER': { label: '算法发布管理员', level: 3 },
  'GENERAL_SUB_ADMIN': { label: '通用子管理员', level: 3 },
  'TEST_VALIDATOR': { label: '题目校验测试管理员', level: 3 },
  'KEYWORD_AUDITOR': { label: '关键词审核管理员', level: 3 },
  'SOLUTION_COMMENT_AUDITOR': { label: '题解评论审核管理员', level: 3 },
  'BIZ_LOG_ADMIN': { label: '业务日志管理员', level: 3 },
  'DEV_LOG_ADMIN': { label: '开发日志管理员', level: 3 },
  'SYS_LOG_ADMIN': { label: '系统日志管理员', level: 3 },
  'OPS_LOG_ADMIN': { label: '运维管理员', level: 3 },
  'UV_PV_ANALYST': { label: 'UV/PV数据分析师', level: 3 },
  'RETENTION_ANALYST': { label: '用户留存数据分析师', level: 3 },
  'ORDER_ANALYST_SUB': { label: '订单数据分析师', level: 3 },
  'PRODUCT_ADMIN': { label: '商品管理员', level: 3 },
  'COIN_ADMIN': { label: '金币管理员', level: 3 },
  'EXTERNAL_AUTHOR': { label: '外部出题人', level: 3 }
}

// 获取当前用户的主要角色（取层级最高的角色）
const primaryRoleInfo = computed(() => {
  const roles = userStore.roles
  if (!roles || roles.length === 0) return null

  let highestLevel = 999
  let primaryRole = null

  for (const role of roles) {
    const info = roleDisplayMap[role]
    if (info && info.level < highestLevel) {
      highestLevel = info.level
      primaryRole = { code: role, ...info }
    }
  }

  if (!primaryRole) {
    return { code: roles[0], label: roles[0], level: 999 }
  }

  return primaryRole
})

// 角色标签颜色
const roleTagType = computed(() => {
  const level = primaryRoleInfo.value?.level || 999
  if (level === 1) return 'danger'
  if (level === 2) return 'warning'
  return 'info'
})

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/system/profile')
      break
    case 'settings':
      router.push('/system/system-config')
      break
    case 'logout':
      userStore.logout()
      router.push('/login')
      break
  }
}
</script>

<template>
  <header class="header">
    <div class="header-left">
      <div class="collapse-btn" @click="toggleSidebar" :title="sidebarCollapsed ? '展开菜单' : '收起菜单'">
        <span class="tri-left" v-if="!sidebarCollapsed"></span>
        <span class="tri-right" v-else></span>
      </div>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="$route.meta?.parent">
          {{ $route.meta.parent }}
        </el-breadcrumb-item>
        <el-breadcrumb-item>{{ $route.meta?.title || $route.name }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="header-right">
      <el-badge :value="3" class="header-icon">
        <el-icon :size="20"><Bell /></el-icon>
      </el-badge>
      <el-dropdown @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" :icon="User" />
          <span class="username">{{ userStore.userInfo?.nickname || '管理员' }}</span>
          <el-tag
            v-if="primaryRoleInfo"
            :type="roleTagType"
            size="small"
            effect="dark"
          >
            {{ primaryRoleInfo.label }}
          </el-tag>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
            <el-dropdown-item command="settings" :icon="Setting">系统设置</el-dropdown-item>
            <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped lang="scss">
.header {
  height: 60px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  width: 28px;
  height: 28px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.3s ease;
  padding: 0;
  background: transparent;
  border: none;

  &:hover {
    background-color: #f5f7fa;
    .tri-left, .tri-right {
      border-color: var(--color-primary);
    }
  }

  .tri-left, .tri-right {
    display: inline-block;
    width: 0;
    height: 0;
    border-top: 5px solid transparent;
    border-bottom: 5px solid transparent;
    transition: all 0.3s ease;
  }

  .tri-left {
    border-right: 7px solid #606266;
    border-left: 0;
  }

  .tri-right {
    border-left: 7px solid #606266;
    border-right: 0;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  cursor: pointer;
  padding: 8px;

  &:hover {
    background-color: #f5f7fa;
    border-radius: 4px;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;

  &:hover {
    background-color: #f5f7fa;
  }
}

.username {
  font-size: 14px;
  color: var(--color-text-primary);
}
</style>
