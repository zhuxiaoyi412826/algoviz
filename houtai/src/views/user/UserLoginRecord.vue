<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElTag, ElDatePicker, ElInput, ElPagination } from 'element-plus'
import { Search, Refresh, Warning } from '@element-plus/icons-vue'

const tableData = ref<any[]>([])
const loading = ref(false)
const searchForm = reactive({ userId: '', dateRange: [] as string[], isAbnormal: false })
const page = ref(1); const pageSize = ref(10); const total = ref(0)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 500))
    tableData.value = [
      { id: '1', userId: '1', nickname: '用户A', openid: 'o****1', ip: '192.168.1.100', device: 'Chrome/Windows', location: '北京市', loginTime: '2024-03-15 10:30:00', isAbnormal: false },
      { id: '2', userId: '2', nickname: '用户B', openid: 'o****2', ip: '203.0.113.50', device: 'Safari/MacOS', location: '美国', loginTime: '2024-03-15 08:15:00', isAbnormal: true },
      { id: '3', userId: '1', nickname: '用户A', openid: 'o****1', ip: '10.0.0.1', device: 'Firefox/Linux', location: '广州市', loginTime: '2024-03-14 22:00:00', isAbnormal: false }
    ]
    total.value = 3
  } finally { loading.value = false }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>用户登录记录</h2></div>
    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.userId" placeholder="用户ID/昵称" clearable style="width:150px" />
        <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" />
        <el-button type="primary" :icon="Search">查询</el-button><el-button :icon="Refresh">重置</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe style="margin-top:16px">
        <el-table-column prop="nickname" label="用户" min-width="120" />
        <el-table-column prop="openid" label="OpenID" width="120" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="location" label="地点" width="100" />
        <el-table-column prop="device" label="设备" min-width="160" />
        <el-table-column prop="loginTime" label="登录时间" width="160" />
        <el-table-column prop="isAbnormal" label="异常" width="80"><template #default="{row}"><el-tag v-if="row.isAbnormal" type="warning" size="small"><el-icon><Warning /></el-icon>异地</el-tag><span v-else>-</span></template></el-table-column>
      </el-table>
      <div class="pagination-wrapper"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" /></div>
    </div>
  </div>
</template>

<style scoped>.pagination-wrapper{display:flex;justify-content:flex-end;margin-top:16px}</style>
