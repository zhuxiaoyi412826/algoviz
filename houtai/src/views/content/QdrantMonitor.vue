<template>
  <div class="qdrant-monitor">
    <!-- 顶部信息卡片 -->
    <el-row :gutter="20" class="info-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="card card-status">
          <div class="card-icon">🗄️</div>
          <div class="card-label">Collection 名称</div>
          <div class="card-value">{{ info?.collectionName || stats?.collectionName || '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="card card-count">
          <div class="card-icon">📊</div>
          <div class="card-label">向量总数</div>
          <div class="card-value">
            <span v-if="stats">{{ stats.vectorCount.toLocaleString() }}</span>
            <span v-else>-</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="card card-dim">
          <div class="card-icon">📐</div>
          <div class="card-label">向量维度</div>
          <div class="card-value">
            <span v-if="stats && stats.dim">
              {{ stats.dim }}
              <el-tag size="small" type="info" effect="plain" style="margin-left: 8px">bge-large-zh-v1.5</el-tag>
            </span>
            <span v-else>-</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="card card-distance">
          <div class="card-icon">🎯</div>
          <div class="card-label">距离度量</div>
          <div class="card-value">
            <el-tag v-if="(stats?.distance || info?.distance || '').toLowerCase() === 'cosine'" type="success" effect="light">Cosine 余弦相似度</el-tag>
            <el-tag v-else-if="(stats?.distance || info?.distance || '').toLowerCase() === 'l2'" type="warning" effect="light">L2 欧氏距离</el-tag>
            <el-tag v-else-if="(stats?.distance || info?.distance || '').toLowerCase() === 'dot'" type="primary" effect="light">Dot 内积</el-tag>
            <span v-else>-</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行：配置信息 + 操作 -->
    <el-row :gutter="20" style="margin-top: 16px">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span class="section-title">📁 Collection 配置</span>
              <el-button type="primary" link @click="refreshInfo">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </template>
          <el-descriptions :column="2" border size="default">
            <el-descriptions-item label="嵌入模型">
              <code>bge-large-zh-v1.5</code>
            </el-descriptions-item>
            <el-descriptions-item label="Qdrant 服务">
              <code>{{ qdrantUrl }}</code>
            </el-descriptions-item>
            <el-descriptions-item label="服务状态" :span="2">
              <el-tag type="success" effect="light" v-if="online">● 运行中</el-tag>
              <el-tag type="danger" effect="light" v-else>● 离线</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Collection 配置" :span="2">
              <pre v-if="info?.config" class="meta-pre">{{ JSON.stringify(info.config, null, 2) }}</pre>
              <span v-else>-</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <span class="section-title">⚡ 快速操作</span>
          </template>
          <div style="display: flex; flex-direction: column; gap: 12px">
            <el-button type="primary" :disabled="progress.inProgress" @click="handleSyncAll">
              <el-icon><Upload /></el-icon> {{ progress.inProgress ? '同步中...' : '全量同步题目' }}
            </el-button>
            <el-button type="success" @click="refreshAll">
              <el-icon><Refresh /></el-icon> 刷新所有数据
            </el-button>
            <el-button type="warning" @click="handleClear">
              <el-icon><Delete /></el-icon> 清空向量库
            </el-button>
            <!-- 同步进度显示 -->
            <div v-if="progress.inProgress || progress.phase === 'completed' || progress.phase === 'failed' || progress.phase === 'cancelled'" class="sync-progress">
              <div v-if="progress.inProgress" class="sync-running">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>后台同步中... 共 {{ progress.total }} 条</span>
              </div>
              <div v-else class="sync-done">
                <el-icon color="#67c23a" v-if="progress.phase === 'completed'"><CircleCheck /></el-icon>
                <el-icon color="#e6a23c" v-else-if="progress.phase === 'cancelled'"><Close /></el-icon>
                <span style="margin-left: 4px">{{ progress.message }}</span>
              </div>
            </div>
            <el-alert
              title="同步建议"
              type="info"
              :closable="false"
              show-icon
              size="small"
              style="margin-top: 8px"
            >
              同步完成后，下方表格应显示与 MySQL 相同题量的数据。如果向量数为 0，说明同步失败。
            </el-alert>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 向量列表 -->
    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span class="section-title">📋 向量库实际内容（共 {{ vectorTotal }} 条）</span>
          <div style="display: flex; gap: 8px; align-items: center">
            <el-input
              v-model="searchId"
              placeholder="题目ID"
              clearable
              style="width: 110px"
              @clear="loadVectors(1)"
              @keyup.enter="loadVectors(1)"
            />
            <el-input
              v-model="searchKeyword"
              placeholder="标题关键词"
              clearable
              style="width: 150px"
              @clear="loadVectors(1)"
              @keyup.enter="loadVectors(1)"
            />
            <el-select
              v-model="searchCategory"
              placeholder="难度"
              clearable
              style="width: 100px"
              @change="loadVectors(1)"
            >
              <el-option label="简单" value="easy" />
              <el-option label="中等" value="medium" />
              <el-option label="困难" value="hard" />
            </el-select>
            <el-select
              v-model="searchTags"
              placeholder="标签"
              clearable
              filterable
              allow-create
              default-first-option
              style="width: 140px"
              @change="loadVectors(1)"
            >
              <el-option label="数组" value="数组" />
              <el-option label="链表" value="链表" />
              <el-option label="哈希表" value="哈希表" />
              <el-option label="字符串" value="字符串" />
              <el-option label="动态规划" value="动态规划" />
              <el-option label="贪心" value="贪心" />
              <el-option label="排序" value="排序" />
              <el-option label="搜索" value="搜索" />
              <el-option label="树" value="树" />
              <el-option label="图" value="图" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="loadVectors(1)">查询</el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="vectorList"
        v-loading="loadingVectors"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="序号" width="60" align="center"
          :index="(i) => (currentPage - 1) * pageSize + i + 1" />
        <el-table-column prop="_id" label="向量 ID" width="90" align="center">
          <template #default="{ row }">
            <el-tooltip :content="row._id" placement="top">
              <span style="cursor: default">{{ shortId(row._id) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="algorithmId" label="题目 ID" width="90" align="center" />
        <el-table-column prop="category" label="难度" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.category === 'easy' ? 'success' : row.category === 'medium' ? 'warning' : row.category === 'hard' ? 'danger' : 'info'"
              size="small"
              effect="light"
            >
              {{ difficultyLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="t in splitTags(row.tags)"
              :key="t"
              size="small"
              effect="plain"
              style="margin: 2px"
              type="info"
            >{{ t }}</el-tag>
            <span v-if="!row.tags">-</span>
          </template>
        </el-table-column>
        <el-table-column label="向量化文本（标题）" min-width="240">
          <template #default="{ row }">
            <div style="color: #606266; font-size: 12px; line-height: 1.4">
              {{ row.name || '-' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="向量数值" width="280" align="center">
          <template #default="{ row }">
            <div v-if="row.vector && row.vector.length > 0" style="font-size: 11px; color: #909399; font-family: monospace; line-height: 1.5">
              <div>{{ vectorPreview(row.vector) }}</div>
              <el-button type="primary" link size="small" @click="showVectorDetail(row)">
                查看全部 {{ row.vector.length }} 维
              </el-button>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="display: flex; justify-content: flex-end; margin-top: 16px">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[20, 50, 100, 200]"
          :total="vectorTotal"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadVectors(1)"
          @current-change="loadVectors"
        />
      </div>
    </el-card>

    <!-- 向量数值详情弹窗 -->
    <el-dialog
      v-model="vectorDetailVisible"
      title="向量数值详情"
      width="700px"
    >
      <div v-if="currentVector" style="max-height: 500px; overflow-y: auto">
        <div style="margin-bottom: 12px; color: #606266; font-size: 13px">
          题目ID: {{ currentVector.algorithmId }} | 标题: {{ currentVector.name }} | 维度: {{ currentVector.vector?.length }}
        </div>
        <div class="vector-grid">
          <div
            v-for="(val, idx) in currentVector.vector"
            :key="idx"
            class="vector-cell"
          >
            <span class="cell-idx">{{ idx }}</span>
            <span class="cell-val">{{ Number(val).toFixed(6) }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="vectorDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Upload, Delete, Search, Loading, CircleCheck, Close } from '@element-plus/icons-vue'
import { algorithmVectorApi } from '@/api/algorithmVector'

const info = ref<any>(null)
const stats = ref<any>(null)
const loadingVectors = ref(false)
const vectorList = ref<any[]>([])
const vectorTotal = ref(0)
const currentPage = ref(1)
const pageSize = ref(50)
const searchKeyword = ref('')
const searchId = ref('')
const searchCategory = ref('')
const searchTags = ref('')
const qdrantUrl = 'http://127.0.0.1:6333'

// 同步进度（轮询）
const progress = ref<any>({ inProgress: false, phase: 'idle', total: 0, processed: 0, failed: 0, message: '空闲中' })
let syncPollTimer: any = null

const online = computed(() => (stats.value?.status === 'running') || false)

const shortId = (id: string) => {
  if (!id) return '-'
  return id.length > 10 ? id.slice(0, 8) + '...' : id
}

const difficultyLabel = (d: string) => ({ easy: '简单', medium: '中等', hard: '困难' } as any)[d] || d || '-'

const splitTags = (tags: string) => {
  if (!tags) return []
  return String(tags).split(/[,，;；|]/).map(s => s.trim()).filter(Boolean)
}

// 向量数值预览 / 详情
const vectorDetailVisible = ref(false)
const currentVector = ref<any>(null)

const vectorPreview = (v: number[]) => {
  if (!v || v.length === 0) return '-'
  return '[' + v.slice(0, 5).map(x => Number(x).toFixed(4)).join(', ') + ', ...]'
}

const showVectorDetail = (row: any) => {
  currentVector.value = row
  vectorDetailVisible.value = true
}

const refreshInfo = async () => {
  try {
    const [infoRes, statsRes] = await Promise.all([
      algorithmVectorApi.collectionInfo(),
      algorithmVectorApi.stats()
    ])
    info.value = infoRes ?? null
    stats.value = statsRes ?? null
  } catch (e: any) {
    ElMessage.error('Qdrant 连接失败: ' + (e?.message || e))
    info.value = null
    stats.value = null
  }
}

const loadVectors = async (page: number) => {
  currentPage.value = page
  loadingVectors.value = true
  try {
    const res = await algorithmVectorApi.vectors({
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || '',
      algorithmId: searchId.value || '',
      category: searchCategory.value || '',
      tags: searchTags.value || ''
    })
    const data = res ?? {}
    vectorList.value = data.list ?? []
    vectorTotal.value = data.total ?? 0
  } catch (e: any) {
    ElMessage.error('向量列表加载失败: ' + (e?.message || e))
    vectorList.value = []
    vectorTotal.value = 0
  } finally {
    loadingVectors.value = false
  }
}

const refreshAll = () => {
  refreshInfo()
  loadVectors(currentPage.value)
}

// 同步进度轮询（/sync/progress）
const pollSync = async () => {
  try {
    const res = await algorithmVectorApi.syncProgress()
    const data = res ?? {}
    progress.value = {
      inProgress: !!data.inProgress,
      phase: data.phase || 'idle',
      total: data.total || 0,
      processed: data.processed || 0,
      failed: data.failed || 0,
      message: data.message || ''
    }
    if (data.inProgress) {
      syncPollTimer = setTimeout(pollSync, 1000)
    } else if (data.phase === 'completed') {
      ElMessage.success(data.message || '同步完成')
      refreshAll()
    } else if (data.phase === 'cancelled') {
      ElMessage.info(data.message || '已取消')
      refreshAll()
    } else if (data.phase === 'failed') {
      ElMessage.error(data.message || '同步失败')
      refreshAll()
    }
  } catch {
    // 忽略，下轮重试
  }
}

const handleSyncAll = async () => {
  try {
    await ElMessageBox.confirm(
      '全量同步会把所有算法题目标题向量化写入 Qdrant（异步后台执行，不阻塞页面）。是否继续？',
      '确认全量同步',
      { type: 'warning', confirmButtonText: '开始同步', cancelButtonText: '取消' }
    )
  } catch { return }

  try {
    const res = await algorithmVectorApi.syncAll()
    ElMessage.info(res?.message || '同步任务已提交，后台处理中...')
    progress.value = { inProgress: true, phase: 'running', total: 0, message: '正在准备...' }
    pollSync()
  } catch (e: any) {
    ElMessage.error('同步失败: ' + (e?.message || e))
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm(
      '此操作将彻底清空 Qdrant 算法题向量库！清空后必须重新同步。是否继续？',
      '⚠️ 危险操作：清空向量库',
      { type: 'error', confirmButtonText: '我已确认，清空', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' }
    )
  } catch { return }

  try {
    await algorithmVectorApi.clear()
    ElMessage.success('向量库已清空')
    refreshAll()
  } catch (e: any) {
    ElMessage.error('清空失败: ' + (e?.message || e))
  }
}

onMounted(() => {
  refreshInfo()
  loadVectors(1)
})

onBeforeUnmount(() => {
  if (syncPollTimer) clearTimeout(syncPollTimer)
})
</script>

<style scoped lang="scss">
.qdrant-monitor {
  padding: 0;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.info-cards {
  .card {
    text-align: center;
    border-radius: 8px;
    transition: transform 0.2s;

    &:hover {
      transform: translateY(-2px);
    }

    .card-icon {
      font-size: 32px;
      margin-bottom: 8px;
    }

    .card-label {
      font-size: 13px;
      color: #909399;
      margin-bottom: 6px;
    }

    .card-value {
      font-size: 22px;
      font-weight: 700;
      color: #303133;
    }
  }

  .card-count .card-value { color: #409EFF; }
  .card-dim .card-value { color: #67C23A; }
  .card-distance .card-value { font-size: 16px; }
}

.meta-pre {
  margin: 0;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  max-height: 150px;
  overflow: auto;
}

.sync-progress {
  .sync-running {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #409eff;
    font-size: 13px;
  }

  .sync-done {
    display: flex;
    align-items: center;
    color: #67c23a;
    font-size: 13px;
  }
}

/* 向量数值网格 */
.vector-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 4px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 6px;
}

.vector-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  transition: background 0.15s;

  &:hover {
    background: #ecf5ff;
    border-color: #c6e2ff;
  }

  .cell-idx {
    color: #909399;
    font-weight: 600;
    min-width: 24px;
  }

  .cell-val {
    color: #409EFF;
    font-weight: 500;
  }
}
</style>
