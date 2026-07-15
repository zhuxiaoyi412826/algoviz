<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElCard, ElRow, ElCol, ElTag, ElProgress, ElButton, ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import type { ServiceStatus } from '@/types'

const services = ref<ServiceStatus[]>([])
const loading = ref(false)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 500))
    services.value = [
      { name: 'Tomcat (前端)', status: 'running', cpu: 45, memory: 62, disk: 38, uptime: '15天 6小时', lastCheck: new Date().toLocaleString() },
      { name: 'Spring Boot (后端)', status: 'running', cpu: 28, memory: 55, disk: 42, uptime: '15天 6小时', lastCheck: new Date().toLocaleString() },
      { name: 'Node.js (CORS代理)', status: 'running', cpu: 12, memory: 35, disk: 20, uptime: '7天 2小时', lastCheck: new Date().toLocaleString() },
      { name: 'MySQL (数据库)', status: 'running', cpu: 18, memory: 48, disk: 55, uptime: '30天 0小时', lastCheck: new Date().toLocaleString() }
    ]
  } finally { loading.value = false }
}

const handleRefresh = () => { loadData(); ElMessage.success('刷新成功') }
const getStatusType = (status: string) => status === 'running' ? 'success' : status === 'error' ? 'danger' : 'warning'
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>服务状态监控</h2><el-button :icon="Refresh" @click="handleRefresh">刷新</el-button></div>
    <el-row :gutter="20">
      <el-col v-for="service in services" :key="service.name" :xs="24" :sm="12" :lg="6" style="margin-bottom:20px">
        <el-card shadow="hover" v-loading="loading">
          <template #header><div style="display:flex;justify-content:space-between;align-items:center"><span>{{ service.name }}</span><el-tag :type="getStatusType(service.status)" size="small">{{ service.status === 'running' ? '运行中' : service.status === 'error' ? '异常' : '已停止' }}</el-tag></div></template>
          <div class="service-item"><span>CPU</span><el-progress :percentage="service.cpu" :color="service.cpu > 80 ? '#f56c6c' : service.cpu > 60 ? '#e6a23c' : '#67c23a'" /></div>
          <div class="service-item"><span>内存</span><el-progress :percentage="service.memory" :color="service.memory > 80 ? '#f56c6c' : service.memory > 60 ? '#e6a23c' : '#67c23a'" /></div>
          <div class="service-item"><span>磁盘</span><el-progress :percentage="service.disk" :color="service.disk > 80 ? '#f56c6c' : service.disk > 60 ? '#e6a23c' : '#67c23a'" /></div>
          <div class="service-info"><span>运行时长：{{ service.uptime }}</span><span>检查时间：{{ service.lastCheck }}</span></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.service-item { display: flex; align-items: center; gap: 12px; margin-bottom: 12px }
.service-item span { width: 40px; font-size: 13px; color: var(--color-text-secondary) }
.service-item :deep(.el-progress) { flex: 1 }
.service-info { display: flex; flex-direction: column; gap: 4px; margin-top: 12px; font-size: 12px; color: var(--color-text-secondary) }
</style>
