<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElTag, ElCard, ElDatePicker, ElSelect,
  ElOption, ElProgress
} from 'element-plus'
import { Search, Refresh, Download, Warning } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import type { LoginLog } from '@/types'

const tableData = ref<LoginLog[]>([])
const loading = ref(false)

const searchForm = reactive({
  username: '',
  status: '',
  dateRange: [] as string[]
})

const stats = ref({
  todayCount: 45,
  weekCount: 312,
  failCount: 8,
  failRate: 2.5
})

const statusOptions = [
  { label: '成功', value: 'success' },
  { label: '失败', value: 'failed' }
]

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(() => {
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    tableData.value = [
      { id: '1', userId: '1', username: 'admin', ip: '192.168.1.100', device: 'Chrome/Windows', location: '北京市', loginTime: dayjs().subtract(0, 'hour').format('YYYY-MM-DD HH:mm:ss'), status: 'success' },
      { id: '2', userId: '2', username: 'content_admin', ip: '192.168.1.101', device: 'Safari/MacOS', location: '上海市', loginTime: dayjs().subtract(2, 'hour').format('YYYY-MM-DD HH:mm:ss'), status: 'success' },
      { id: '3', userId: '1', username: 'admin', ip: '10.0.0.1', device: 'Firefox/Linux', location: '广州市', loginTime: dayjs().subtract(5, 'hour').format('YYYY-MM-DD HH:mm:ss'), status: 'failed', failReason: '密码错误' },
      { id: '4', userId: '3', username: 'op_admin', ip: '192.168.1.102', device: 'Chrome/Windows', location: '深圳市', loginTime: dayjs().subtract(1, 'day').format('YYYY-MM-DD HH:mm:ss'), status: 'success' },
      { id: '5', userId: '1', username: 'admin', ip: '203.0.113.50', device: 'Unknown', location: '美国', loginTime: dayjs().subtract(2, 'day').format('YYYY-MM-DD HH:mm:ss'), status: 'failed', failReason: '账号已锁定' }
    ]
    total.value = 5
  } finally {
    loading.value = false
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
  loadData()
}

const handleExport = () => {
  // 导出逻辑
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

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="location" label="登录地点" width="100" />
        <el-table-column prop="device" label="设备" min-width="160" />
        <el-table-column prop="loginTime" label="登录时间" min-width="160" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因" min-width="120">
          <template #default="{ row }">
            <span v-if="row.status === 'failed'" class="fail-reason">
              <el-icon v-if="row.failReason?.includes('异地')" color="#e6a23c"><Warning /></el-icon>
              {{ row.failReason || '-' }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </div>
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
</style>
