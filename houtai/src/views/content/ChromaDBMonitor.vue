<template>
  <div class="chromadb-monitor">
    <!-- 顶部信息卡片 -->
    <el-row :gutter="20" class="info-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="card card-status">
          <div class="card-icon">🗄️</div>
          <div class="card-label">Collection 名称</div>
          <div class="card-value">{{ info?.name || '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="card card-count">
          <div class="card-icon">📊</div>
          <div class="card-label">向量总数</div>
          <div class="card-value">
            <span v-if="info">{{ info.vectorCount.toLocaleString() }}</span>
            <span v-else>-</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="card card-dim">
          <div class="card-icon">📐</div>
          <div class="card-label">向量维度</div>
          <div class="card-value">
            <span v-if="info && info.dimension">
              {{ info.dimension }}
              <el-tag size="small" type="info" effect="plain" style="margin-left: 8px">bge-small-zh-v1.5</el-tag>
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
            <el-tag v-if="info?.distanceMetric === 'cosine'" type="success" effect="light">Cosine 余弦相似度</el-tag>
            <el-tag v-else-if="info?.distanceMetric === 'l2'" type="warning" effect="light">L2 欧氏距离</el-tag>
            <el-tag v-else-if="info?.distanceMetric === 'ip'" type="primary" effect="light">IP 内积</el-tag>
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
              <code>{{ info?.modelName || '-' }}</code>
            </el-descriptions-item>
            <el-descriptions-item label="持久化路径">
              <code>{{ info?.chromaPath || '-' }}</code>
            </el-descriptions-item>
            <el-descriptions-item label="服务状态" :span="2">
              <el-tag type="success" effect="light" v-if="info">● 运行中</el-tag>
              <el-tag type="danger" effect="light" v-else>● 未连接</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="Metadata" :span="2">
              <pre v-if="info?.metadata" class="meta-pre">{{ JSON.stringify(info.metadata, null, 2) }}</pre>
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
            <el-button type="primary" :disabled="syncStatus?.running" @click="handleSyncAll">
              <el-icon><Upload /></el-icon> {{ syncStatus?.running ? '同步中...' : '全量同步题目' }}
            </el-button>
            <el-button type="success" @click="refreshInfo; loadVectors(currentPage)">
              <el-icon><Refresh /></el-icon> 刷新所有数据
            </el-button>
            <el-button type="warning" @click="handleClear">
              <el-icon><Delete /></el-icon> 清空向量库
            </el-button>
            <!-- 同步进度显示 -->
            <div v-if="syncStatus" class="sync-progress">
              <div v-if="syncStatus.running" class="sync-running">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>后台同步中... 共 {{ syncStatus.total }} 条</span>
              </div>
              <div v-else class="sync-done">
                ✅ {{ syncStatus.message }}
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
          <div style="display: flex; gap: 8px">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索标题/编号/标签/分类"
              clearable
              style="width: 280px"
              @clear="loadVectors(1)"
              @keyup.enter="loadVectors(1)"
            >
              <template #append>
                <el-button @click="loadVectors(1)">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
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
        <el-table-column prop="id" label="向量 ID" width="90" align="center" />
        <el-table-column prop="problemId" label="题目 ID" width="90" align="center" />
        <el-table-column prop="problemNo" label="题目编号" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="primary" effect="plain">{{ row.problemNo }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="题目标题" min-width="220">
          <template #default="{ row }">
            <span :title="row.title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.difficulty === 'easy' ? 'success' : row.difficulty === 'medium' ? 'warning' : 'danger'"
              size="small"
              effect="light"
            >
              {{ difficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="110" align="center" />
        <el-table-column prop="tags" label="标签" min-width="160">
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
        <el-table-column prop="documentPreview" label="向量化文本预览" min-width="260">
          <template #default="{ row }">
            <div style="color: #606266; font-size: 12px; line-height: 1.4">
              {{ row.documentPreview || '-' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="向量数值" width="280" align="center">
          <template #default="{ row }">
            <div v-if="row.vectorValues && row.vectorValues.length > 0" style="font-size: 11px; color: #909399; font-family: monospace; line-height: 1.5">
              <div>{{ row.vectorPreview }}</div>
              <el-button type="primary" link size="small" @click="showVectorDetail(row)">
                查看全部 {{ row.vectorDimension }} 维
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
          题目ID: {{ currentVector.problemId }} | 维度: {{ currentVector.vectorDimension }}
        </div>
        <div class="vector-grid">
          <div
            v-for="(val, idx) in currentVector.vectorValues"
            :key="idx"
            class="vector-cell"
          >
            <span class="cell-idx">{{ idx }}</span>
            <span class="cell-val">{{ val }}</span>
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Upload, Delete, Search, Loading } from '@element-plus/icons-vue'
import { vectorApi } from '@/api/vector'

const info = ref<any>(null)
const loadingInfo = ref(false)
const loadingVectors = ref(false)
const vectorList = ref<any[]>([])
const vectorTotal = ref(0)
const currentPage = ref(1)
const pageSize = ref(50)
const searchKeyword = ref('')

// 向量详情弹窗
const vectorDetailVisible = ref(false)
const currentVector = ref<any>(null)

const showVectorDetail = (row: any) => {
  currentVector.value = row
  vectorDetailVisible.value = true
}

const difficultyLabel = (d: string) => ({ easy: '简单', medium: '中等', hard: '困难' } as any)[d] || d

const splitTags = (tags: string) => {
  if (!tags) return []
  return String(tags).split(/[,，;；|]/).map(s => s.trim()).filter(Boolean)
}

const refreshInfo = async () => {
  loadingInfo.value = true
  try {
    const res = await vectorApi.collectionInfo()
    info.value = res ?? null
  } catch (e: any) {
    ElMessage.error('ChromaDB 连接失败: ' + (e?.message || e))
    info.value = null
  } finally {
    loadingInfo.value = false
  }
}

const loadVectors = async (page: number) => {
  currentPage.value = page
  loadingVectors.value = true
  try {
    const res = await vectorApi.vectors({
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || ''
    })
    const data = res ?? {}
    vectorList.value = data.vectors ?? []
    vectorTotal.value = data.total ?? 0
  } catch (e: any) {
    ElMessage.error('向量列表加载失败: ' + (e?.message || e))
    vectorList.value = []
    vectorTotal.value = 0
  } finally {
    loadingVectors.value = false
  }
}

// 同步进度轮询
const syncStatus = ref<any>(null)
let syncPollTimer: any = null

const pollSyncStatus = async () => {
  try {
    const res = await fetch('/api/vector/admin/sync-status').then(r => r.json())
    const data = res?.data ?? res
    syncStatus.value = data
    if (data?.running) {
      // 继续轮询
      syncPollTimer = setTimeout(pollSyncStatus, 1000)
    } else if (data && !data.running && data.message) {
      // 任务完成
      ElMessage.success(data.message)
      refreshInfo()
      loadVectors(currentPage.value)
      syncStatus.value = null
    }
  } catch {
    // 忽略状态查询错误
  }
}

const handleSyncAll = async () => {
  try {
    await ElMessageBox.confirm(
      '全量同步会重新向量化所有面试题入库（异步后台执行，不阻塞页面）。是否继续？',
      '确认全量同步',
      { type: 'warning', confirmButtonText: '开始同步', cancelButtonText: '取消' }
    )
  } catch { return }

  try {
    const res = await vectorApi.syncAll()
    ElMessage.info(res?.message || '同步任务已提交，后台处理中...')
    // 开始轮询进度
    pollSyncStatus()
  } catch (e: any) {
    ElMessage.error('同步失败: ' + (e?.message || e))
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm(
      '此操作将彻底清空 ChromaDB 向量库！清空后必须重新同步题目才能进行语义搜索。是否继续？',
      '⚠️ 危险操作：清空向量库',
      { type: 'error', confirmButtonText: '我已确认，清空', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' }
    )
  } catch { return }

  try {
    await vectorApi.clear()
    ElMessage.success('向量库已清空')
    refreshInfo()
    loadVectors(1)
  } catch (e: any) {
    ElMessage.error('清空失败: ' + (e?.message || e))
  }
}

onMounted(() => {
  refreshInfo()
  loadVectors(1)
})
</script>

<style scoped lang="scss">
.chromadb-monitor {
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
