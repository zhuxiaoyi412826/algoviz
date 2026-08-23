<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElDatePicker, ElSelect,
  ElOption, ElInput, ElDescriptions, ElDescriptionsItem, ElPagination
} from 'element-plus'
import { Search, Refresh, View } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { operationLogApi } from '@/api/system'
import type { OperationLog } from '@/types'

const tableData = ref<any[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const currentDetail = ref<any>(null)
const operatorOptions = ref<string[]>([])   // 操作人下拉候选（后端取去重）

const searchForm = reactive({
  username: '',
  module: '',
  action: '',
  dateRange: [] as string[]
})

// 模块下拉（与 AOP 写入的中文模块名一致，OperationLogAspect.MODULE_MAP）
const moduleOptions = [
  { label: '用户管理', value: '用户管理' },
  { label: '内容审核', value: '内容审核' },
  { label: '系统配置', value: '系统配置' },
  { label: '系统监控', value: '系统监控' },
  { label: '商品管理', value: '商品管理' },
  { label: '金币管理', value: '金币管理' },
  { label: '公告管理', value: '公告管理' },
  { label: '反馈管理', value: '反馈管理' },
  { label: '订单管理', value: '订单管理' },
  { label: '支付管理', value: '支付管理' },
  { label: '面试题管理', value: '面试题管理' },
  { label: 'OJ题目管理', value: 'OJ题目管理' },
  { label: 'AI对话', value: 'AI对话' },
  { label: '系统管理', value: '系统管理' }
]

// 操作类型下拉（与 AOP resolveAction / LogOperation action 值一致）
const actionOptions = [
  { label: '新增', value: '新增' },
  { label: '编辑', value: '编辑' },
  { label: '删除', value: '删除' },
  { label: '启用', value: '启用' },
  { label: '禁用', value: '禁用' },
  { label: '重置密码', value: '重置密码' }
]

// 模块→Tag 颜色
const moduleTagType: Record<string, any> = {
  '用户管理': 'success',
  '内容审核': 'primary',
  '系统配置': 'warning',
  '系统监控': 'info',
  '商品管理': 'danger',
  '金币管理': '',
  '公告管理': '',
  '反馈管理': '',
  '订单管理': '',
  '支付管理': '',
  '面试题管理': '',
  'OJ题目管理': ''
}

// 操作类型→Tag 颜色
const actionTagType: Record<string, any> = {
  '新增': 'success',
  '编辑': 'warning',
  '删除': 'danger',
  '启用': 'success',
  '禁用': 'info',
  '重置密码': 'warning'
}

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(async () => {
  // 先加载操作人下拉，再加载列表
  try {
    const ops = await operationLogApi.getOperators()
    operatorOptions.value = Array.isArray(ops) ? ops : []
  } catch {}
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: page.value,
      pageSize: pageSize.value
    }
    if (searchForm.username) params.username = searchForm.username
    if (searchForm.module) params.module = searchForm.module
    if (searchForm.action) params.action = searchForm.action
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await operationLogApi.getList(params)
    tableData.value = res.list || []
    total.value = res.total || 0
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
  page.value = 1
  loadData()
}

/**
 * 点击操作人下拉选择：直接筛选（不需要再点"查询"）
 */
const handleOperatorChange = () => {
  page.value = 1
  loadData()
}

const handleViewDetail = async (row: OperationLog) => {
  try {
    const detail = await operationLogApi.getDetail(row.id)
    // 兼容 createdAt / createTime
    ;(detail as any).createTime = (detail as any).createTime || (detail as any).createdAt
    currentDetail.value = detail
  } catch {
    currentDetail.value = row
  }
  detailVisible.value = true
}

// 时间格式化为中文（YYYY年MM月DD日 HH:mm:ss）
const formatCnTime = (t?: string) => {
  if (!t) return ''
  return dayjs(t).format('YYYY年MM月DD日 HH:mm:ss')
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>操作日志</h2>
    </div>

    <div class="card-container">
      <div class="filter-bar">
        <el-select
          v-model="searchForm.username"
          placeholder="操作人"
          clearable
          filterable
          style="width: 180px"
          @change="handleOperatorChange"
        >
          <el-option label="所有" value="" />
          <el-option
            v-for="name in operatorOptions"
            :key="name"
            :label="name"
            :value="name"
          />
        </el-select>
        <el-select v-model="searchForm.module" placeholder="模块" clearable style="width: 140px">
          <el-option v-for="item in moduleOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="searchForm.action" placeholder="操作类型" clearable style="width: 120px">
          <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          unlink-panels
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="username" label="操作人" width="130" />
        <el-table-column prop="module" label="模块" width="110">
          <template #default="{ row }">
            <el-tag :type="moduleTagType[row.module] || 'info'" size="small" effect="light">
              {{ row.module }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="actionTagType[row.action] || 'info'" size="small" effect="light">
              {{ row.action }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="操作详情" min-width="320" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="createTime" label="操作时间" width="200">
          <template #default="{ row }">
            {{ formatCnTime(row.createTime || row.createdAt) }}
          </template>
        </el-table-column>
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
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadData"
          @size-change="handleSearch"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="操作详情" width="640px">
      <el-descriptions :column="2" border v-if="currentDetail">
        <el-descriptions-item label="操作人">{{ currentDetail.username }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentDetail.ip }}</el-descriptions-item>
        <el-descriptions-item label="所属模块">
          <el-tag :type="moduleTagType[currentDetail.module] || 'info'" size="small" effect="light">
            {{ currentDetail.module }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="actionTagType[currentDetail.action] || 'info'" size="small" effect="light">
            {{ currentDetail.action }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">
          {{ formatCnTime(currentDetail.createTime || currentDetail.createdAt) }}
        </el-descriptions-item>
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
  padding: 10px 12px;
  background-color: #f5f7fa;
  border-radius: 6px;
  line-height: 1.6;
}
</style>
