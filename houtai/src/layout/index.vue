<script setup lang="ts">
import { ref, provide, computed } from 'vue'
import { useRoute } from 'vue-router'
import MainSidebar from './sider/index.vue'
import MainHeader from './header/index.vue'
import Watermark from '@/components/common/Watermark.vue'

const route = useRoute()
const isLoginPage = computed(() => route.path === '/login')

const sidebarCollapsed = ref(false)
const headerCollapsed = ref(false)

const toggleSidebar = () => { sidebarCollapsed.value = !sidebarCollapsed.value }
const toggleHeader = () => { headerCollapsed.value = !headerCollapsed.value }

provide('layoutState', { sidebarCollapsed, headerCollapsed, toggleSidebar, toggleHeader })
</script>

<template>
  <div v-if="isLoginPage">
    <router-view />
  </div>
  <div class="layout-container" :class="{ 'layout-container--sidebar-collapse': sidebarCollapsed }">
    <!-- 全局水印：登录后 layout 常驻期间一直存在；退出登录销毁 layout 时由组件清理 -->
    <Watermark />
    <MainSidebar />
    <div class="layout-main">
      <div class="header-wrapper" :class="{ 'header-wrapper--collapse': headerCollapsed }">
        <MainHeader />
      </div>
      <button class="header-toggle" :class="{ 'header-toggle--collapse': headerCollapsed }" @click="toggleHeader" :title="headerCollapsed ? '展开顶栏' : '收起顶栏'">
        <span class="tri-icon"></span>
      </button>
      <div class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" :key="$route.fullPath" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.layout-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--color-bg-page);
  min-width: 0;
  position: relative;
}

.header-wrapper {
  flex-shrink: 0;
  transition: max-height 0.3s ease, opacity 0.3s ease;
  overflow: hidden;
  max-height: 60px;
  opacity: 1;

  &--collapse {
    max-height: 0;
    opacity: 0;
  }
}

.header-toggle {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 32px;
  height: 20px;
  border: none;
  border-radius: 0 0 8px 8px;
  background-color: #409eff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.35);
  z-index: 15;
  transition: top 0.3s ease, transform 0.3s ease, background-color 0.3s ease;
  padding: 0;
  top: 60px;

  &:hover {
    background-color: #66b1ff;
    box-shadow: 0 2px 10px rgba(64, 158, 255, 0.55);
  }

  .tri-icon {
    display: inline-block;
    width: 0;
    height: 0;
    border-left: 5px solid transparent;
    border-right: 5px solid transparent;
    border-top: 6px solid #fff;
    transition: transform 0.3s ease;
  }

  &--collapse {
    top: 0;
    border-radius: 8px 8px 0 0;

    .tri-icon {
      border-top: none;
      border-bottom: 6px solid #fff;
    }
  }
}

.layout-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  min-height: 0;
  transition: padding 0.3s ease;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
