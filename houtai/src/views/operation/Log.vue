<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElSelect, ElOption, ElInput, ElDatePicker, ElMessage, ElPagination } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'

const tableData = ref<any[]>([])
const loading = ref(false)
const searchForm = reactive({ type: '', keyword: '', dateRange: [] as string[] })

const typeOptions = [
  { label: '接口日志', value: 'api' },
  { label: '判题日志', value: 'judge' },
  { label: 'AI日志', value: 'ai' },
  { label: '系统日志', value: 'system' }
]

const page = ref(1); const pageSize = ref(20); const total = ref(0)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 500))
    tableData.value = [
      { id: '1', type: 'api', method: 'POST', path: '/api/submit', status: 200, time: '2024-03-15 10:30:00', duration: 156, ip: '192.168.1.100' },
      { id: '2', type: 'api', method: 'GET', path: '/api/problem/list', status: 200, time: '2024-03-15 10:29:00', duration: 45, ip: '192.168.1.101' },
      { id: '3', type: 'judge', method: '-', path: 'Problem #001', status: 200, time: '2024-03-15 10:28:00', duration: 2340, ip: '-' }
    ]
    total.value = 3
  } finally { loading.value = false }
}

const handleSearch = () => loadData()
const handleDownload = () => ElMessage.info('下载功能开发中')
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>日志管理</h2><el-button :icon="Download" @click="handleDownload">下载日志</el-button></div>
    <div class="card-container">
      <div class="filter-bar">
        <el-select v-model="searchForm.type" placeholder="日志类型" clearable style="width:120px"><el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        <el-input v-model="searchForm.keyword" placeholder="关键词" clearable style="width:180px" />
        <el-date-picker v-model="searchForm.dateRange" type="datetimerange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD HH:mm:ss" />
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe style="margin-top:16px" max-height="500">
        <el-table-column prop="time" label="时间" width="160" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="method" label="方法" width="70" />
        <el-table-column prop="path" label="路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" />
        <el-table-column prop="duration" label="耗时(ms)" width="100" />
        <el-table-column prop="ip" label="IP地址" width="140" />
      </el-table>
      <div class="pagination-wrapper"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" /></div>
    </div>
  </div>
</template>

<style scoped>.pagination-wrapper{display:flex;justify-content:flex-end;margin-top:16px}</style>
