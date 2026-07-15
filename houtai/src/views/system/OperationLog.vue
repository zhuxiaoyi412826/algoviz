<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElDatePicker, ElSelect,
  ElOption, ElInput
} from 'element-plus'
import { Search, Refresh, View } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import type { OperationLog } from '@/types'

const tableData = ref<OperationLog[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const currentDetail = ref<OperationLog | null>(null)

const searchForm = reactive({
  username: '',
  module: '',
  action: '',
  dateRange: [] as string[]
})

const moduleOptions = [
  { label: '用户管理', value: 'user' },
  { label: '内容管理', value: 'content' },
  { label: '系统配置', value: 'system' },
  { label: '数据统计', value: 'statistics' }
]

const actionOptions = [
  { label: '新增', value: 'create' },
  { label: '编辑', value: 'update' },
  { label: '删除', value: 'delete' },
  { label: '查询', value: 'query' }
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
      { id: '1', userId: '1', username: 'admin', module: 'content', action: 'create', detail: '新增OJ题目: #156 二叉树层序遍历', ip: '192.168.1.100', createTime: dayjs().subtract(1, 'hour').format('YYYY-MM-DD HH:mm:ss') },
      { id: '2', userId: '2', username: 'content_admin', module: 'user', action: 'update', detail: '禁用用户: user_123', ip: '192.168.1.101', createTime: dayjs().subtract(3, 'hour').format('YYYY-MM-DD HH:mm:ss') },
      { id: '3', userId: '1', username: 'admin', module: 'system', action: 'update', detail: '修改系统配置: theme.primaryColor = #409eff', ip: '192.168.1.100', createTime: dayjs().subtract(5, 'hour').format('YYYY-MM-DD HH:mm:ss') },
      { id: '4', userId: '3', username: 'op_admin', module: 'content', action: 'delete', detail: '删除测试用例: testcase_456', ip: '192.168.1.102', createTime: dayjs().subtract(1, 'day').format('YYYY-MM-DD HH:mm:ss') },
      { id: '5', userId: '1', username: 'admin', module: 'statistics', action: 'query', detail: '导出数据: user_behavior_2024.csv', ip: '192.168.1.100', createTime: dayjs().subtract(2, 'day').format('YYYY-MM-DD HH:mm:ss') }
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
  searchForm.module = ''
  searchForm.action = ''
  searchForm.dateRange = []
  loadData()
}

const handleViewDetail = (row: OperationLog) => {
  currentDetail.value = row
  detailVisible.value = true
}

const getModuleTagType = (module: string) => {
  const map: Record<string, any> = {
    user: 'success',
    content: 'primary',
    system: 'warning',
    statistics: 'info'
  }
  return map[module] || 'info'
}

const getModuleLabel = (module: string) => {
  const map: Record<string, string> = {
    user: '用户管理',
    content: '内容管理',
    system: '系统配置',
    statistics: '数据统计'
  }
  return map[module] || module
}

const getActionTagType = (action: string) => {
  const map: Record<string, any> = {
    create: 'success',
    update: 'warning',
    delete: 'danger',
    query: 'info'
  }
  return map[action] || 'info'
}

const getActionLabel = (action: string) => {
  const map: Record<string, string> = {
    create: '新增',
    update: '编辑',
    delete: '删除',
    query: '查询'
  }
  return map[action] || action
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>操作日志</h2>
    </div>

    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.username" placeholder="操作人" clearable style="width: 140px" />
        <el-select v-model="searchForm.module" placeholder="模块" clearable style="width: 120px">
          <el-option v-for="item in moduleOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="searchForm.action" placeholder="操作类型" clearable style="width: 100px">
          <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="100">
          <template #default="{ row }">
            <el-tag :type="getModuleTagType(row.module)" size="small">
              {{ getModuleLabel(row.module) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getActionTagType(row.action)" size="small">
              {{ getActionLabel(row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="操作详情" min-width="300" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="createTime" label="操作时间" width="160" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="handleViewDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="操作详情" width="600px">
      <el-descriptions :column="2" border v-if="currentDetail">
        <el-descriptions-item label="操作人">{{ currentDetail.username }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentDetail.ip }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ getModuleLabel(currentDetail.module) }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="getActionTagType(currentDetail.action)" size="small">
            {{ getActionLabel(currentDetail.action) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ currentDetail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="操作详情" :span="2">
          <div class="detail-content">{{ currentDetail.detail }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-content {
  white-space: pre-wrap;
  word-break: break-all;
  padding: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>
