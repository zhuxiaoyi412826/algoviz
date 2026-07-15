<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import MainSidebar from './sider/index.vue'
import MainHeader from './header/index.vue'

const route = useRoute()
const isLoginPage = computed(() => route.path === '/login')
</script>

<template>
  <div v-if="isLoginPage">
    <router-view />
  </div>
  <div v-else class="layout-container">
    <MainSidebar />
    <div class="layout-main">
      <MainHeader />
      <div class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
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
}

.layout-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
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
