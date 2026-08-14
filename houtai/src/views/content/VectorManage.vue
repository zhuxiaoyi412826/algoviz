<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  ElCard, ElButton, ElTag, ElDescriptions, ElDescriptionsItem,
  ElMessage, ElMessageBox, ElIcon, ElStatistic, ElRow, ElCol
} from 'element-plus'
import { Refresh, Delete, Connection, DataAnalysis, Cpu } from '@element-plus/icons-vue'
import { vectorApi } from '@/api/vector'

const loading = ref(false)
const syncing = ref(false)
const clearing = ref(false)

const stats = ref({
  collectionName: '',
  vectorCount: 0,
  modelName: '',
  status: 'offline'
})

const healthStatus = ref({
  status: 'offline',
  vectorCount: 0,
  model: ''
})

const fetchStats = async () => {
  loading.value = true
  try {
    const res = await vectorApi.stats()
    stats.value = res || stats.value
  } catch (e) {
    stats.value.status = 'offline'
  } finally {
    loading.value = false
  }
}

const fetchHealth = async () => {
  try {
    const res = await vectorApi.health()
    healthStatus.value = res || healthStatus.value
  } catch (e) {
    healthStatus.value.status = 'offline'
  }
}

const handleSyncAll = async () => {
  try {
    await ElMessageBox.confirm(
      '确认将数据库中所有面试题重新向量化并同步到 Chroma？此操作可能需要几分钟。',
      '全量同步确认',
      { type: 'warning' }
    )
  } catch {
    return
  }

  syncing.value = true
  try {
    const res = await vectorApi.syncAll()
    ElMessage.success(`同步完成！成功 ${res.success || 0} 条`)
    await fetchStats()
  } catch (e: any) {
    ElMessage.error('同步失败：' + (e.message || '未知错误'))
  } finally {
    syncing.value = false
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm(
      '确认清空向量数据库？此操作不可逆，需要重新同步。',
      '清空确认',
      { type: 'error', confirmButtonText: '确认清空', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  clearing.value = true
  try {
    await vectorApi.clear()
    ElMessage.success('向量库已清空')
    await fetchStats()
  } catch (e: any) {
    ElMessage.error('清空失败：' + (e.message || '未知错误'))
  } finally {
    clearing.value = false
  }
}

const isOnline = () => stats.value.status === 'running' || healthStatus.value.status === 'ok'

onMounted(() => {
  fetchStats()
  fetchHealth()
})
</script>

<template>
  <div class="vector-manage">
    <!-- 服务状态卡片 -->
    <ElRow :gutter="20" class="status-row">
      <ElCol :span="8">
        <ElCard shadow="hover">
          <div class="status-card">
            <ElIcon :size="40" :color="isOnline() ? '#67c23a' : '#f56c6c'">
              <Connection />
            </ElIcon>
            <div class="status-info">
              <div class="status-label">服务状态</div>
              <ElTag :type="isOnline() ? 'success' : 'danger'" size="large">
                {{ isOnline() ? '在线' : '离线' }}
              </ElTag>
            </div>
          </div>
        </ElCard>
      </ElCol>
      <ElCol :span="8">
        <ElCard shadow="hover">
          <div class="status-card">
            <ElIcon :size="40" color="#409eff">
              <DataAnalysis />
            </ElIcon>
            <div class="status-info">
              <div class="status-label">向量数量</div>
              <div class="status-value">{{ stats.vectorCount || healthStatus.vectorCount || 0 }}</div>
            </div>
          </div>
        </ElCard>
      </ElCol>
      <ElCol :span="8">
        <ElCard shadow="hover">
          <div class="status-card">
            <ElIcon :size="40" color="#e6a23c">
              <Cpu />
            </ElIcon>
            <div class="status-info">
              <div class="status-label">嵌入模型</div>
              <div class="status-model">{{ stats.modelName || healthStatus.model || 'BAAI/bge-small-zh-v1.5' }}</div>
            </div>
          </div>
        </ElCard>
      </ElCol>
    </ElRow>

    <!-- 详细信息 -->
    <ElCard shadow="never" class="detail-card">
      <template #header>
        <div class="card-header">
          <span>向量数据库详情</span>
          <ElButton :icon="Refresh" size="small" @click="fetchStats" :loading="loading">刷新</ElButton>
        </div>
      </template>
      <ElDescriptions :column="2" border>
        <ElDescriptionsItem label="Collection 名称">{{ stats.collectionName || 'interview_problems' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="向量总数">{{ stats.vectorCount || 0 }}</ElDescriptionsItem>
        <ElDescriptionsItem label="嵌入模型">{{ stats.modelName || 'BAAI/bge-small-zh-v1.5' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="服务状态">
          <ElTag :type="isOnline() ? 'success' : 'danger'">
            {{ stats.status || 'unknown' }}
          </ElTag>
        </ElDescriptionsItem>
      </ElDescriptions>
    </ElCard>

    <!-- 操作区 -->
    <ElCard shadow="never" class="action-card">
      <template #header>
        <span>操作管理</span>
      </template>
      <div class="action-buttons">
        <ElButton
          type="primary"
          :icon="Refresh"
          size="large"
          :loading="syncing"
          @click="handleSyncAll"
        >
          全量同步面试题到向量库
        </ElButton>
        <ElButton
          type="danger"
          :icon="Delete"
          size="large"
          :loading="clearing"
          @click="handleClear"
        >
          清空向量库
        </ElButton>
      </div>
      <div class="action-tips">
        <p><strong>全量同步</strong>：将 MySQL 中所有面试题转换为向量并存储到 Chroma，建议首次使用或数据大范围更新后执行。</p>
        <p><strong>清空向量库</strong>：删除 Chroma 中所有向量数据，清空后需要重新同步。</p>
        <p><strong>模型说明</strong>：使用 BAAI/bge-small-zh-v1.5 中文嵌入模型，向量维度 512，cosine 相似度检索。</p>
      </div>
    </ElCard>
  </div>
</template>

<style scoped>
.vector-manage {
  padding: 20px;
}

.status-row {
  margin-bottom: 20px;
}

.status-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 0;
}

.status-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.status-label {
  font-size: 13px;
  color: #909399;
}

.status-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.status-model {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  word-break: break-all;
}

.detail-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-card {
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.action-tips {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
}

.action-tips p {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.action-tips p:last-child {
  margin-bottom: 0;
}
</style>
