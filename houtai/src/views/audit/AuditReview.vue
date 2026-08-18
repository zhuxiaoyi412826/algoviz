<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <el-row :gutter="14" class="stat-row">
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="待审核队列 (Redis)" :value="stats.pendingQueue" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="审核记录总数 (MySQL)" :value="stats.totalRecords" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="已通过" :value="byStatus.pass || 0" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <el-statistic title="已驳回 / 已拦截" :value="(byStatus.reject || 0) + (byStatus.blocked || 0)" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="flow-card">
      <el-alert :closable="false" type="info"
                title="流程：题目提交 → 内存检测(高危拦截/中危放行) → 审计日志 → Fluentd → ES → Spring Task 定时拉取(60s) → Redis 待审队列 → 人工审核 → 通过落库 / 驳回下架" />
      <div class="toolbar">
        <el-button type="primary" :icon="Refresh" :loading="pulling" @click="handleManualPull">立即从 ES 拉取</el-button>
        <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
        <span class="tip-text">定时任务每 60 秒自动拉取一次（audit.pull-interval-ms 可配）</span>
      </div>
    </el-card>

    <el-tabs v-model="activeTab">
      <!-- ==================== 待审核 ==================== -->
      <el-tab-pane :label="`待审核 (${pendingTotal})`" name="pending">
        <el-card shadow="never">
          <el-table :data="pendingList" v-loading="pendingLoading" border stripe>
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="expand-box">
                  <div class="expand-title">内容快照</div>
                  <pre class="content-pre">{{ row.content }}</pre>
                  <div class="expand-title">命中详情</div>
                  <el-table :data="row.hitDetails || []" border size="small">
                    <el-table-column prop="type" label="类型" width="120">
                      <template #default="{ row: h }">
                        <el-tag size="small" :type="h.type === 'DANGEROUS_CODE' ? 'danger' : 'warning'">
                          {{ h.type === 'DANGEROUS_CODE' ? '危险代码' : '敏感词' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="ruleName" label="命中项" min-width="120" />
                    <el-table-column prop="hitContent" label="上下文" min-width="140" />
                    <el-table-column prop="score" label="得分" width="70" />
                  </el-table>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="submitId" label="提交ID" min-width="180" show-overflow-tooltip />
            <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="contentType" label="类型" width="100" />
            <el-table-column prop="riskLevel" label="风险" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="levelType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalScore" label="总分" width="75" />
            <el-table-column prop="submitTime" label="提交时间" width="170" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="openReview(row, 'pass')">通过</el-button>
                <el-button type="danger" size="small" @click="openReview(row, 'reject')">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination background layout="total, prev, pager, next" :total="pendingTotal"
                           v-model:current-page="pendingPage" :page-size="pendingPageSize"
                           @current-change="loadPending" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- ==================== 审核记录 ==================== -->
      <el-tab-pane label="审核记录" name="records">
        <el-card shadow="never">
          <div class="toolbar">
            <el-select v-model="recordQuery.auditStatus" placeholder="审核状态" clearable style="width: 140px">
              <el-option v-for="s in ['pass', 'reject', 'blocked']" :key="s" :label="statusText(s)" :value="s" />
            </el-select>
            <el-select v-model="recordQuery.riskLevel" placeholder="风险等级" clearable style="width: 120px">
              <el-option v-for="l in ['HIGH', 'MEDIUM', 'LOW']" :key="l" :label="l" :value="l" />
            </el-select>
            <el-input v-model="recordQuery.keyword" placeholder="提交ID/内容" clearable style="width: 200px"
                      @keyup.enter="loadRecords" />
            <el-button type="primary" :icon="Search" @click="loadRecords">查询</el-button>
          </div>
          <el-table :data="recordList" v-loading="recordLoading" border stripe>
            <el-table-column prop="submitId" label="提交ID" min-width="170" show-overflow-tooltip />
            <el-table-column prop="problemNo" label="题目编号" width="110" />
            <el-table-column prop="contentType" label="类型" width="95" />
            <el-table-column prop="riskLevel" label="风险" width="85">
              <template #default="{ row }">
                <el-tag size="small" :type="levelType(row.riskLevel)">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalScore" label="总分" width="70" />
            <el-table-column prop="preCheckStatus" label="预检" width="85">
              <template #default="{ row }">
                <el-tag size="small" :type="row.preCheckStatus === 'BLOCK' ? 'danger' : 'success'">
                  {{ row.preCheckStatus }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="auditStatus" label="审核结果" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="statusType(row.auditStatus)">{{ statusText(row.auditStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="auditorId" label="审核人" width="90" />
            <el-table-column prop="auditTime" label="审核时间" width="170" />
            <el-table-column prop="contentSnapshot" label="内容快照" min-width="200" show-overflow-tooltip />
          </el-table>
          <div class="pager">
            <el-pagination background layout="total, prev, pager, next" :total="recordTotal"
                           v-model:current-page="recordQuery.page" :page-size="recordQuery.pageSize"
                           @current-change="loadRecords" />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 审核对话框 -->
    <el-dialog v-model="reviewDialog.visible" :title="reviewDialog.result === 'pass' ? '审核通过' : '驳回内容'"
               width="480px">
      <div class="review-tip">
        <p><b>提交ID：</b>{{ reviewDialog.row?.submitId }}</p>
        <p><b>标题：</b>{{ reviewDialog.row?.title || '-' }}</p>
        <p v-if="reviewDialog.result === 'reject'" style="color: #f56c6c">
          驳回后：审核记录落库 + ES 状态回写 + 关联题目自动下架 (INACTIVE)
        </p>
        <p v-else style="color: #67c23a">通过后：审核记录写入 MySQL 数据库</p>
      </div>
      <el-input v-model="reviewDialog.remark" type="textarea" :rows="3" placeholder="审核备注（可选）" />
      <template #footer>
        <el-button @click="reviewDialog.visible = false">取消</el-button>
        <el-button :type="reviewDialog.result === 'pass' ? 'success' : 'danger'" :loading="reviewing"
                   @click="handleReview">确认{{ reviewDialog.result === 'pass' ? '通过' : '驳回' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  ElCard, ElRow, ElCol, ElStatistic, ElTabs, ElTabPane, ElTable, ElTableColumn,
  ElButton, ElTag, ElPagination, ElDialog, ElInput, ElSelect, ElOption, ElAlert, ElMessage
} from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { auditApi } from '@/api/audit'

defineOptions({ name: 'AuditReview' })

const activeTab = ref('pending')

const levelType = (l: string) => ({ HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' } as any)[l] || 'info'
const statusText = (s: string) => ({
  pass: '通过', reject: '驳回', blocked: '系统拦截', pending: '待审核', logonly: '仅记录'
} as any)[s] || s
const statusType = (s: string) => ({
  pass: 'success', reject: 'danger', blocked: 'danger', pending: 'warning', logonly: 'info'
} as any)[s] || 'info'

// ===== 统计 =====
const stats = ref<any>({ pendingQueue: 0, totalRecords: 0, byStatus: {} })
const byStatus = computed(() => stats.value.byStatus || {})

const loadStats = async () => {
  try {
    stats.value = await auditApi.stats()
  } catch { /* 静默 */ }
}

// ===== 待审核 =====
const pendingLoading = ref(false)
const pendingList = ref<any[]>([])
const pendingTotal = ref(0)
const pendingPage = ref(1)
const pendingPageSize = ref(10)

const loadPending = async () => {
  pendingLoading.value = true
  try {
    const r: any = await auditApi.pending({ page: pendingPage.value, pageSize: pendingPageSize.value })
    pendingList.value = r?.list || []
    pendingTotal.value = r?.total || 0
  } finally {
    pendingLoading.value = false
  }
}

// ===== 手动拉取 =====
const pulling = ref(false)
const handleManualPull = async () => {
  pulling.value = true
  try {
    const msg: any = await auditApi.manualPull()
    ElMessage.success(msg || '拉取完成')
    loadPending()
    loadStats()
  } finally {
    pulling.value = false
  }
}

// ===== 审核 =====
const reviewing = ref(false)
const reviewDialog = reactive({
  visible: false,
  result: 'pass' as 'pass' | 'reject',
  remark: '',
  row: null as any
})

const openReview = (row: any, result: 'pass' | 'reject') => {
  reviewDialog.row = row
  reviewDialog.result = result
  reviewDialog.remark = ''
  reviewDialog.visible = true
}

const handleReview = async () => {
  reviewing.value = true
  try {
    const msg: any = await auditApi.review({
      submitId: reviewDialog.row.submitId,
      result: reviewDialog.result,
      remark: reviewDialog.remark
    })
    ElMessage.success(msg || '审核完成')
    reviewDialog.visible = false
    loadPending()
    loadRecords()
    loadStats()
  } finally {
    reviewing.value = false
  }
}

// ===== 审核记录 =====
const recordLoading = ref(false)
const recordList = ref<any[]>([])
const recordTotal = ref(0)
const recordQuery = reactive({ auditStatus: '', riskLevel: '', keyword: '', page: 1, pageSize: 10 })

const loadRecords = async () => {
  recordLoading.value = true
  try {
    const r: any = await auditApi.records(recordQuery)
    recordList.value = r?.list || []
    recordTotal.value = r?.total || 0
  } finally {
    recordLoading.value = false
  }
}

const refreshAll = () => {
  loadStats()
  loadPending()
  loadRecords()
}

onMounted(refreshAll)
</script>

<style scoped>
.page-container { padding: 4px; }
.stat-row { margin-bottom: 14px; }
.flow-card { margin-bottom: 14px; }
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-top: 12px; }
.tip-text { color: #909399; font-size: 13px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.expand-box { padding: 8px 16px; }
.expand-title { font-weight: 600; margin: 10px 0 6px; }
.content-pre { background: #f5f7fa; padding: 10px; border-radius: 4px; white-space: pre-wrap; word-break: break-all; max-height: 240px; overflow: auto; }
.review-tip p { margin: 4px 0; }
</style>
