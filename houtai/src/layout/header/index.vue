<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar, ElIcon, ElBadge } from 'element-plus'
import { User, Setting, SwitchButton, Bell, Expand, Fold } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const isCollapsed = ref(false)

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
      <el-icon class="collapse-btn">
        <Fold v-if="!isCollapsed" />
        <Expand v-else />
      </el-icon>
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
          <span class="role-tag">
            {{ userStore.userInfo?.role === 'super_admin' ? '超级管理员' :
               userStore.userInfo?.role === 'content_admin' ? '内容管理员' : '运维管理员' }}
          </span>
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
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.3s;

  &:hover {
    color: var(--color-primary);
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

.role-tag {
  font-size: 12px;
  padding: 2px 8px;
  background-color: #ecf5ff;
  color: var(--color-primary);
  border-radius: 4px;
}
</style>
