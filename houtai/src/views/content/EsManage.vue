<script setup lang="ts">
import { ref, onMounted, reactive, computed, onBeforeUnmount } from 'vue'
import {
  ElCard, ElButton, ElTag, ElDescriptions, ElDescriptionsItem,
  ElMessage, ElMessageBox, ElIcon, ElStatistic, ElRow, ElCol,
  ElInput, ElDivider, ElProgress, ElAlert, ElLink
} from 'element-plus'
import {
  Refresh, Delete, Connection, DataAnalysis, Search,
  Loading, Warning
} from '@element-plus/icons-vue'
import { vectorApi } from '@/api/vector'

const loading = ref(false)
const syncing = ref(false)
const recreating = ref(false)
const deleting = ref(false)

// ES 健康状态
const health = reactive({
  status: 'offline',
  es_url: '',
  index: ''
})

// ES 索引统计
const stats = reactive<any>({
  exists: false,
  count: 0,
  index: 'interview_problems',
  analyzer: 'ik_max_word (索引) / ik_smart (搜索)'
})

// IK 分词测试
const analyzerInput = ref('动态规划入门二叉树遍历')
const analyzerResult = reactive<any>({
  input: '',
  tokens: [] as string[],
  analyzer: '',
  success: false,
  error: ''
})

// ==================== 同步进度 ====================
const progress = reactive<{
  inProgress: boolean
  phase: 'idle' | 'preparing' | 'running' | 'completed' | 'failed'
  taskType: 'vector_sync' | 'es_sync' | string
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
  taskType: 'none',
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
    const snap: any = await vectorApi.syncProgress()
    if (!snap) return
    Object.assign(progress, {
      inProgress: !!snap.inProgress,
      phase: snap.phase || 'idle',
      taskType: snap.taskType || 'none',
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
    if (!progress.inProgress && progressTimer != null) {
      setTimeout(() => {
        if (!progress.inProgress && progressTimer != null) {
          stopProgressPolling()
        }
      }, 10_000)
    }
  } catch (e) {
    // 静默
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

const taskTypeLabel = computed(() => {
  switch (progress.taskType) {
    case 'vector_sync': return '向量同步'
    case 'es_sync': return 'ES 索引同步'
    default: return progress.taskType || '同步任务'
  }
})
// ==================== 进度部分结束 ====================

// 拉取健康状态
const fetchHealth = async () => {
  loading.value = true
  try {
    const res = await vectorApi.esHealth()
    Object.assign(health, res || {})
  } catch (e: any) {
    health.status = 'offline'
  } finally {
    loading.value = false
  }
}

// 拉取索引统计
const fetchStats = async () => {
  try {
    const res = await vectorApi.esStats()
    Object.assign(stats, res || {})
  } catch (e: any) {
    stats.exists = false
    stats.count = 0
  }
}

// 全量同步
const handleSyncAll = async () => {
  try {
    await ElMessageBox.confirm(
      '确认将数据库中所有面试题同步到 Elasticsearch 索引？此操作将分批处理并显示实时进度。',
      '全量同步确认',
      { type: 'warning' }
    )
  } catch {
    return
  }

  syncing.value = true
  try {
    const res = await vectorApi.esSyncAll()
    if (res && res.submitted === false) {
      ElMessage.warning(res.message || '有任务正在执行，不能重复提交')
    } else {
      ElMessage.success(res?.message || '已提交全量 ES 同步，下方将显示实时进度')
    }
    startProgressPolling()
  } catch (e: any) {
    ElMessage.error('提交同步失败：' + (e?.message || '未知错误'))
  } finally {
    syncing.value = false
  }
}

// 重建索引
const handleRecreate = async () => {
  try {
    await ElMessageBox.confirm(
      '重建索引将删除现有索引并重新创建（使用 IK 分词器）。重建后需要重新全量同步数据。是否继续？',
      '重建索引确认',
      { type: 'warning' }
    )
  } catch {
    return
  }

  recreating.value = true
  try {
    await vectorApi.esRecreate()
    ElMessage.success('索引已重建（IK 分词器）')
    await fetchStats()
  } catch (e: any) {
    ElMessage.error('重建失败：' + (e?.message || '未知错误'))
  } finally {
    recreating.value = false
  }
}

// 删除索引
const handleDeleteIndex = async () => {
  try {
    await ElMessageBox.confirm(
      '确认删除整个面试题 ES 索引？此操作不可恢复！',
      '删除索引确认',
      { type: 'error', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }

  deleting.value = true
  try {
    await vectorApi.esDeleteIndex()
    ElMessage.success('索引已删除')
    await fetchStats()
  } catch (e: any) {
    ElMessage.error('删除失败：' + (e?.message || '未知错误'))
  } finally {
    deleting.value = false
  }
}

// 测试 IK 分词
const handleTestAnalyzer = async () => {
  if (!analyzerInput.value.trim()) {
    ElMessage.warning('请输入要测试的文本')
    return
  }
  try {
    const res = await vectorApi.esTestAnalyzer(analyzerInput.value)
    Object.assign(analyzerResult, res || {})
    if (analyzerResult.success) {
      ElMessage.success(`分词成功，共 ${analyzerResult.tokens.length} 个 token`)
    } else {
      ElMessage.error('分词失败：' + (analyzerResult.error || '未知错误'))
    }
  } catch (e: any) {
    ElMessage.error('分词测试失败：' + (e?.message || '未知错误'))
  }
}

const refreshAll = async () => {
  await Promise.all([fetchHealth(), fetchStats()])
}

onMounted(() => {
  refreshAll()
  fetchProgress().then(() => {
    if (progress.inProgress) startProgressPolling()
  })
})

onBeforeUnmount(() => {
  stopProgressPolling()
})
</script>

<template>
  <div class="es-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-icon><DataAnalysis /></el-icon>
          <span>Elasticsearch 分词索引管理</span>
          <el-button :icon="Refresh" :loading="loading" size="small" @click="refreshAll">刷新</el-button>
        </div>
      </template>

      <!-- 服务状态 -->
      <el-descriptions :column="3" border>
        <el-descriptions-item label="服务状态">
          <el-tag :type="health.status === 'ok' ? 'success' : 'danger'">
            {{ health.status === 'ok' ? '● 已连接' : '● 未连接' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="ES 地址">{{ health.es_url || '-' }}</el-descriptions-item>
        <el-descriptions-item label="索引名">{{ health.index || 'interview_problems' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <!-- 索引统计 -->
      <el-row :gutter="20">
        <el-col :span="6">
          <el-statistic title="索引是否存在">
            <!-- el-statistic value 仅支持 Number|Object，文本用默认插槽渲染 -->
            <template #default>
              <el-tag :type="stats.exists ? 'success' : 'info'" size="large">
                {{ stats.exists ? '是' : '否' }}
              </el-tag>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="索引文档数" :value="Number(stats.count) || 0" />
        </el-col>
        <el-col :span="12">
          <el-statistic title="分词器">
            <template #default>
              <span class="analyzer-text">{{ stats.analyzer || 'ik_max_word / ik_smart' }}</span>
            </template>
          </el-statistic>
        </el-col>
      </el-row>

      <el-divider />

      <!-- 操作按钮 -->
      <div class="actions">
        <el-button
          type="primary"
          :icon="Connection"
          :loading="syncing"
          :disabled="health.status !== 'ok' || progress.inProgress"
          @click="handleSyncAll"
        >
          {{ progress.inProgress && progress.taskType === 'es_sync' ? 'ES 同步进行中...' : '全量同步到 ES' }}
        </el-button>
        <el-button
          type="warning"
          :icon="Refresh"
          :loading="recreating"
          :disabled="health.status !== 'ok' || progress.inProgress"
          @click="handleRecreate"
        >
          重建索引（IK）
        </el-button>
        <el-button
          type="danger"
          :icon="Delete"
          :loading="deleting"
          :disabled="!stats.exists || progress.inProgress"
          @click="handleDeleteIndex"
        >
          删除索引
        </el-button>
      </div>
    </el-card>

    <!-- ============= ES 同步进度条（新增） ============= -->
    <el-card
      shadow="never"
      class="progress-card"
      v-show="progress.inProgress || progress.phase === 'completed' || progress.phase === 'failed' || progress.message !== '空闲中'"
    >
      <template #header>
        <div class="progress-header">
          <div class="progress-header-left">
            <el-icon v-if="progress.inProgress" class="spin-icon" :size="18" color="#409eff"><Loading /></el-icon>
            <el-icon v-else-if="progress.phase === 'failed'" :size="18" color="#f56c6c"><Warning /></el-icon>
            <el-icon v-else-if="progress.phase === 'completed'" :size="18" color="#67c23a"><Refresh /></el-icon>
            <span class="progress-title">
              {{ taskTypeLabel }}进度
              <el-tag :type="progress.phase === 'failed' ? 'danger' : (progress.phase === 'completed' ? 'success' : 'primary')" size="small" effect="plain" class="phase-tag">
                {{
                  progress.phase === 'preparing' ? '准备中'
                  : progress.phase === 'running' ? '运行中'
                  : progress.phase === 'completed' ? '完成'
                  : progress.phase === 'failed' ? '失败' : '空闲'
                }}
              </el-tag>
            </span>
          </div>
          <div class="progress-header-right">
            <el-link type="primary" :underline="false" @click="fetchStats" v-if="progress.phase === 'completed'">已完成，点击刷新 ES 文档数</el-link>
          </div>
        </div>
      </template>

      <el-progress
        :percentage="Number(progress.percent.toFixed(1))"
        :status="progressStatusType"
        :stroke-width="22"
        :format="(p: number) => `${p}%`"
        striped
        striped-flow
      />

      <div class="progress-message">
        <span class="msg-label">当前状态</span>
        <span class="msg-text">{{ progress.message || '-' }}</span>
      </div>

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

      <el-alert v-if="progress.phase === 'failed' && progress.lastError"
        type="error"
        :title="'同步失败: ' + progress.lastError"
        show-icon
        class="fail-alert"
      />
    </el-card>

    <!-- IK 分词测试 -->
    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <el-icon><Search /></el-icon>
          <span>IK 分词器测试</span>
        </div>
      </template>

      <el-input
        v-model="analyzerInput"
        placeholder="输入要分词的文本，如「动态规划入门二叉树遍历」"
        @keyup.enter="handleTestAnalyzer"
      >
        <template #append>
          <el-button :icon="Search" @click="handleTestAnalyzer">分词</el-button>
        </template>
      </el-input>

      <div v-if="analyzerResult.tokens.length > 0" class="analyzer-result">
        <div class="result-label">分词结果（{{ analyzerResult.tokens.length }} 个 token）：</div>
        <div class="token-list">
          <el-tag v-for="(token, i) in analyzerResult.tokens" :key="i" class="token-tag">
            {{ token }}
          </el-tag>
        </div>
      </div>
      <div v-else-if="analyzerResult.error" class="error-msg">
        {{ analyzerResult.error }}
      </div>
    </el-card>

    <!-- 说明 -->
    <el-card style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <el-icon><DataAnalysis /></el-icon>
          <span>使用说明</span>
        </div>
      </template>
      <ul class="usage-list">
        <li><strong>IK 分词器</strong>：已安装 IK Analyzer 插件，索引时用 <code>ik_max_word</code>（细粒度切分，最大化召回），搜索时用 <code>ik_smart</code>（粗粒度切分，精确匹配）。</li>
        <li><strong>全量同步</strong>：把 MySQL 所有面试题写入 ES 索引，分批处理并显示实时进度（已导入、剩余、耗时）。</li>
        <li><strong>重建索引</strong>：删除旧索引并重新创建（使用最新 mapping）。修改分词器配置后必须重建。重建后需重新全量同步。</li>
        <li><strong>自动同步</strong>：题目新增/修改/删除时会自动同步到 ES 索引（无需手动操作）。</li>
        <li><strong>前台搜索</strong>：用户在面试题目列表页可点击"ES 分词搜索"按钮进行精确关键词搜索。</li>
        <li><strong>与向量搜索的区别</strong>：向量搜索理解语义（同义词、概念），ES 分词搜索匹配精确关键词（标签、标题、分类）。两者互补。</li>
      </ul>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.es-manage {
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;

  .el-button {
    margin-left: auto;
  }
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* el-statistic 文本值样式（替换默认数值样式，保持视觉一致） */
.analyzer-text {
  display: inline-block;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.6;
  word-break: break-all;
}

.analyzer-result {
  margin-top: 16px;

  .result-label {
    margin-bottom: 8px;
    color: var(--el-text-color-secondary);
    font-size: 14px;
  }

  .token-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .token-tag {
    margin: 0;
  }
}

.error-msg {
  margin-top: 16px;
  color: var(--el-color-danger);
  font-size: 14px;
}

.usage-list {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
  color: var(--el-text-color-regular);

  code {
    background: var(--el-fill-color-light);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Courier New', monospace;
    font-size: 13px;
  }
}

/* ============ 进度条样式 ============ */
.progress-card {
  margin-top: 16px;
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
