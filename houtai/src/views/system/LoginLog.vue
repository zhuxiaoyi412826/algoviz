<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElTag, ElCard, ElDatePicker, ElSelect,
  ElOption, ElProgress, ElInput, ElPagination, ElMessage, ElDialog,
  ElDescriptions, ElDescriptionsItem
} from 'element-plus'
import { Search, Refresh, Download, Warning, View } from '@element-plus/icons-vue'
import request from '@/api/request'
import type { LoginLog } from '@/types'

const tableData = ref<LoginLog[]>([])
const loading = ref(false)

const searchForm = reactive({
  username: '',
  status: '',
  dateRange: [] as string[]
})

const stats = ref({
  todayCount: 0,
  weekCount: 0,
  failCount: 0,
  failRate: 0
})

const statusOptions = [
  { label: '成功', value: 'success' },
  { label: '失败', value: 'failed' }
]

const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const pageSizeOptions = [10, 20, 50, 100]

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<LoginLog | null>(null)

onMounted(() => {
  loadData()
  loadStats()
})

const loadData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: page.value,
      pageSize: pageSize.value
    }
    if (searchForm.username) params.username = searchForm.username
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await request.get('/system/login-log', { params })
    tableData.value = res.list || []
    total.value = res.total || 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const res = await request.get('/system/login-log/stats')
    stats.value.todayCount = res.todayCount || 0
    stats.value.weekCount = res.weekCount || 0
    stats.value.failCount = res.failCount || 0
    stats.value.failRate = res.failRate || 0
  } catch {
    // keep default zeros
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.status = ''
  searchForm.dateRange = []
  page.value = 1
  loadData()
}

const handlePageChange = (val: number) => {
  page.value = val
  loadData()
}

const handlePageSizeChange = (val: number) => {
  pageSize.value = val
  page.value = 1
  loadData()
}

const handleViewDetail = async (row: LoginLog) => {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = row
  try {
    const res = await request.get(`/system/login-log/${row.id}`)
    detailData.value = res
  } catch {
    // keep the row data already available
  } finally {
    detailLoading.value = false
  }
}

const handleExport = async () => {
  try {
    const params: Record<string, any> = {}
    if (searchForm.username) params.username = searchForm.username
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await request.get('/export/logs', { params, responseType: 'blob' })
    const blob = new Blob([res], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `login_logs_${Date.now()}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

const formatStatus = (s: string) => s === 'success' ? '成功' : '失败'
const statusTagType = (s: string) => s === 'success' ? 'success' : 'danger'
const formatTime = (t?: string) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>登录日志</h2>
      <el-button :icon="Download" @click="handleExport">导出</el-button>
    </div>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <span class="stat-label">今日登录</span>
            <span class="stat-value">{{ stats.todayCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <span class="stat-label">本周登录</span>
            <span class="stat-value">{{ stats.weekCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <span class="stat-label">失败次数</span>
            <span class="stat-value warning">{{ stats.failCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <span class="stat-label">失败率</span>
            <el-progress :percentage="stats.failRate" :stroke-width="10" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.username" placeholder="用户名" clearable style="width: 180px" />
        <el-select v-model="searchForm.status" placeholder="登录状态" clearable style="width: 120px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%; margin-top: 16px"
        :row-style="{ padding: '12px 0' }"
        @row-click="handleViewDetail"
      >
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="ip" label="IP地址" width="160" />
        <el-table-column prop="location" label="登录地点" width="120" />
        <el-table-column prop="device" label="设备" min-width="200" show-overflow-tooltip />
        <el-table-column prop="loginTime" label="登录时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.loginTime) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click.stop="handleViewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="pageSizeOptions"
          :max-pagination-count="10"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="登录日志详情" width="640px" destroy-on-close>
      <div v-loading="detailLoading" v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="日志ID" :span="2">
            {{ detailData.id }}
          </el-descriptions-item>
          <el-descriptions-item label="用户ID">
            {{ detailData.userId }}
          </el-descriptions-item>
          <el-descriptions-item label="用户名">
            {{ detailData.username }}
          </el-descriptions-item>
          <el-descriptions-item label="IP地址">
            {{ detailData.ip }}
          </el-descriptions-item>
          <el-descriptions-item label="登录地点">
            {{ detailData.location || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="登录时间">
            {{ formatTime(detailData.loginTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detailData.status)" size="small">
              {{ formatStatus(detailData.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="失败原因" :span="2" v-if="detailData.status === 'failed'">
            <span class="fail-reason">
              <el-icon v-if="detailData.failReason?.includes('异地')" color="#e6a23c"><Warning /></el-icon>
              {{ detailData.failReason || '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="设备" :span="2">
            {{ detailData.device || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.stats-row {
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 8px;

  .stat-label {
    font-size: 14px;
    color: var(--color-text-secondary);
  }

  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: var(--color-text-primary);

    &.warning {
      color: var(--color-warning);
    }
  }
}

.fail-reason {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-danger);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

:deep(.el-table .el-table__row td) {
  padding: 12px 0;
}

:deep(.el-table .cell) {
  line-height: 1.6;
}
</style>
