<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import {
  ElCard, ElButton, ElTag, ElDescriptions, ElDescriptionsItem,
  ElMessage, ElMessageBox, ElIcon, ElRow, ElCol,
  ElProgress, ElAlert, ElLink
} from 'element-plus'
import {
  Refresh, Delete, Connection, DataAnalysis, Cpu,
  Loading, Warning, Close
} from '@element-plus/icons-vue'
import { algorithmVectorApi } from '@/api/algorithmVector'

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

// ==================== 同步进度 ====================
const progress = reactive<{
  inProgress: boolean
  phase: 'idle' | 'preparing' | 'running' | 'completed' | 'failed'
  processed: number
  failed: number
  total: number
  remaining: number
  percent: number
  message: string
  lastError: string | null
  elapsedSeconds: number | null
  estimatedRemainingSeconds: number | null
}>({
  inProgress: false,
  phase: 'idle',
  processed: 0,
  failed: 0,
  total: 0,
  remaining: 0,
  percent: 0,
  message: '空闲中',
  lastError: null,
  elapsedSeconds: null,
  estimatedRemainingSeconds: null
})

let progressTimer: number | null = null

const fetchProgress = async () => {
  try {
    const snap: any = await algorithmVectorApi.syncProgress()
    if (!snap) return
    Object.assign(progress, {
      inProgress: !!snap.inProgress,
      phase: snap.phase || 'idle',
      processed: snap.processed || 0,
      failed: snap.failed || 0,
      total: snap.total || 0,
      remaining: snap.remaining || 0,
      percent: Math.min(100, snap.percent || 0),
      message: snap.message || '',
      lastError: snap.lastError || null,
      elapsedSeconds: snap.elapsedSeconds ?? null,
      estimatedRemainingSeconds: snap.estimatedRemainingSeconds ?? null
    })

    // 非 inProgress 且 phase=completed/failed 时：最多再保留 10 秒显示，然后停止轮询
    if (!progress.inProgress && progressTimer != null) {
      // 用户能看到完成/失败的信息后再停（给 10 秒停留）
      setTimeout(() => {
        if (!progress.inProgress && progressTimer != null) {
          stopProgressPolling()
        }
      }, 10_000)
    }
  } catch (e) {
    // 静默，下一轮继续重试
  }
}

const startProgressPolling = () => {
  stopProgressPolling()
  fetchProgress()
  progressTimer = window.setInterval(fetchProgress, 1000)
}

const stopProgressPolling = () => {
  if (progressTimer != null) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

const formatSeconds = (sec: number | null): string => {
  if (sec == null || sec < 0) return '计算中...'
  if (sec === 0) return '0秒'
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  const s = sec % 60
  if (h > 0) return `${h}小时${m}分${s}秒`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}

const progressStatusType = computed(() => {
  if (progress.phase === 'failed') return 'exception' as const
  if (progress.phase === 'completed') return 'success' as const
  if (progress.phase === 'preparing') return 'warning' as const
  if (progress.inProgress) return undefined
  // ElProgress status 仅支持 ""/success/exception/warning，空字符串表示默认（运行中蓝色）
  return '' as const
})

// ==================== 基础信息 ====================

const fetchStats = async () => {
  loading.value = true
  try {
    const res = await algorithmVectorApi.stats()
    stats.value = res || stats.value
  } catch (e) {
    stats.value.status = 'offline'
  } finally {
    loading.value = false
  }
}

const fetchHealth = async () => {
  try {
    const res = await algorithmVectorApi.health()
    healthStatus.value = res || healthStatus.value
  } catch (e) {
    healthStatus.value.status = 'offline'
  }
}

const handleSyncAll = async () => {
  try {
    await ElMessageBox.confirm(
      '将数据库中所有算法题重新向量化写入 Qdrant？此操作会分批处理并显示实时进度。',
      '全量同步确认',
      { type: 'warning' }
    )
  } catch {
    return
  }

  syncing.value = true
  try {
    const res = await algorithmVectorApi.syncAll()
    if (res && res.submitted === false) {
      ElMessage.warning(res.message || '有任务正在执行，不能重复提交')
    } else {
      ElMessage.success(res?.message || '已提交全量同步，下方将显示实时进度')
    }
    startProgressPolling()
  } catch (e: any) {
    ElMessage.error('提交同步失败：' + (e.message || '未知错误'))
  } finally {
    syncing.value = false
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm(
      '确认清空算法题向量库？此操作不可逆，需要重新同步。',
      '清空确认',
      { type: 'error', confirmButtonText: '确认清空', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  clearing.value = true
  try {
    await algorithmVectorApi.clear()
    ElMessage.success('算法题向量库已清空')
    await fetchStats()
  } catch (e: any) {
    ElMessage.error('清空失败：' + (e.message || '未知错误'))
  } finally {
    clearing.value = false
  }
}

const handleCancel = async () => {
  try {
    await algorithmVectorApi.cancelSync()
    ElMessage.info('已请求取消同步，正在停止...')
  } catch (e: any) {
    ElMessage.error('取消失败：' + (e.message || '未知错误'))
  }
}

const isOnline = () => stats.value.status === 'running' || healthStatus.value.status === 'ok'

onMounted(() => {
  fetchStats()
  fetchHealth()
  // 首次进入页面检查是否已有进行中的任务（例如刚导入题目后）
  fetchProgress().then(() => {
    if (progress.inProgress) {
      startProgressPolling()
    }
  })
})

onBeforeUnmount(() => {
  stopProgressPolling()
})
</script>

<template>
  <div class="algorithm-vector-manage">
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
              <div class="status-model">{{ stats.modelName || healthStatus.model || 'bge-large-zh-v1.5' }}</div>
            </div>
          </div>
        </ElCard>
      </ElCol>
    </ElRow>

    <!-- 详细信息 -->
    <ElCard shadow="never" class="detail-card">
      <template #header>
        <div class="card-header">
          <span>算法题向量库详情</span>
          <ElButton :icon="Refresh" size="small" @click="fetchStats" :loading="loading">刷新</ElButton>
        </div>
      </template>
      <ElDescriptions :column="2" border>
        <ElDescriptionsItem label="Collection 名称">{{ stats.collectionName || 'algorithm_knowledge' }}</ElDescriptionsItem>
        <ElDescriptionsItem label="向量总数">{{ stats.vectorCount || 0 }}</ElDescriptionsItem>
        <ElDescriptionsItem label="嵌入模型">{{ stats.modelName || 'bge-large-zh-v1.5' }}</ElDescriptionsItem>
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
          :disabled="progress.inProgress"
          @click="handleSyncAll"
        >
          {{ progress.inProgress ? '向量同步进行中...' : '全量同步算法题到向量库' }}
        </ElButton>
        <ElButton
          type="danger"
          :icon="Delete"
          size="large"
          :loading="clearing"
          :disabled="progress.inProgress"
          @click="handleClear"
        >
          清空向量库
        </ElButton>
      </div>
      <div class="action-tips">
        <p><strong>全量同步</strong>：将 MySQL 中所有算法题转换为向量并存储到 Qdrant，建议首次使用或数据大范围更新后执行。</p>
        <p><strong>清空向量库</strong>：删除 Qdrant 中所有算法题向量数据，清空后需要重新同步。</p>
        <p><strong>模型说明</strong>：使用 bge-large-zh-v1.5 中文嵌入模型，向量维度 1024，cosine 相似度检索。</p>
      </div>
    </ElCard>

    <!-- 同步进度卡片 -->
    <ElCard
      shadow="never"
      class="progress-card"
      v-show="progress.inProgress || progress.phase === 'completed' || progress.phase === 'failed' || progress.phase === 'cancelled' || progress.message !== '空闲中'"
    >
      <template #header>
        <div class="progress-header">
          <div class="progress-header-left">
            <ElIcon v-if="progress.inProgress" class="spin-icon" :size="18" color="#409eff"><Loading /></ElIcon>
            <ElIcon v-else-if="progress.phase === 'failed'" :size="18" color="#f56c6c"><Warning /></ElIcon>
            <ElIcon v-else-if="progress.phase === 'completed'" :size="18" color="#67c23a"><Refresh /></ElIcon>
            <ElIcon v-else-if="progress.phase === 'cancelled'" :size="18" color="#e6a23c"><Close /></ElIcon>
            <span class="progress-title">
              向量同步进度
              <ElTag :type="progress.phase === 'failed' ? 'danger' : (progress.phase === 'completed' ? 'success' : (progress.phase === 'cancelled' ? 'warning' : 'primary'))" size="small" effect="plain" class="phase-tag">
                {{
                  progress.phase === 'preparing' ? '准备中'
                  : progress.phase === 'running' ? '运行中'
                  : progress.phase === 'completed' ? '完成'
                  : progress.phase === 'failed' ? '失败'
                  : progress.phase === 'cancelled' ? '已取消' : '空闲'
                }}
              </ElTag>
            </span>
          </div>
          <div class="progress-header-right">
            <ElButton v-if="progress.inProgress" type="danger" text size="small" :icon="Close" @click="handleCancel">
              取消同步
            </ElButton>
            <ElLink v-else-if="progress.phase === 'completed'" type="primary" :underline="false" @click="fetchStats">已完成，点击刷新向量数量</ElLink>
          </div>
        </div>
      </template>

      <!-- 进度条 -->
      <ElProgress
        :percentage="Number(progress.percent.toFixed(1))"
        :status="progressStatusType"
        :stroke-width="22"
        :format="(p: number) => `${p}%`"
        striped
        striped-flow
      />

      <!-- 状态文案 -->
      <div class="progress-message">
        <span class="msg-label">当前状态</span>
        <span class="msg-text">{{ progress.message || '-' }}</span>
      </div>

      <!-- 进度数字面板 -->
      <div class="progress-stats">
        <div class="stat-item">
          <div class="stat-label">已导入</div>
          <div class="stat-value stat-ok">{{ progress.processed }}</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-label">失败</div>
          <div class="stat-value stat-err">{{ progress.failed }}</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-label">总数</div>
          <div class="stat-value">{{ progress.total }}</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-label">剩余</div>
          <div class="stat-value stat-warn">{{ progress.remaining }}</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-label">已用时间</div>
          <div class="stat-value">{{ formatSeconds(progress.elapsedSeconds) }}</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-label">预计剩余</div>
          <div class="stat-value" :class="progress.phase === 'running' ? 'stat-warn' : ''">{{ formatSeconds(progress.estimatedRemainingSeconds) }}</div>
        </div>
      </div>

      <!-- 失败时显示具体错误 -->
      <ElAlert v-if="progress.phase === 'failed' && progress.lastError"
        type="error"
        :title="'同步失败: ' + progress.lastError"
        show-icon
        class="fail-alert"
      />
    </ElCard>
  </div>
</template>

<style scoped>
.algorithm-vector-manage {
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

/* ============ 进度条样式 ============ */
.progress-card {
  margin-bottom: 20px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.progress-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.spin-icon {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

.progress-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.phase-tag {
  margin-left: 2px;
}

.progress-message {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 14px;
  margin-bottom: 18px;
  padding: 10px 14px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 6px;
}
.progress-message .msg-label {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
}
.progress-message .msg-text {
  font-size: 13px;
  color: #303133;
  word-break: break-all;
}

.progress-stats {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 4px;
}
.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  font-variant-numeric: tabular-nums;
}
.stat-ok { color: #67c23a; }
.stat-err { color: #f56c6c; }
.stat-warn { color: #e6a23c; }
.stat-divider {
  width: 1px;
  height: 36px;
  align-self: center;
  background: #ebeef5;
}

.fail-alert {
  margin-top: 14px;
}

@media (max-width: 1024px) {
  .progress-stats {
    grid-template-columns: repeat(3, 1fr);
  }
  .stat-divider:nth-child(7n) { display: none; }
}
</style>
