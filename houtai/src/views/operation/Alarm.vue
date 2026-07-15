<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElTag, ElSwitch, ElMessage, ElPagination } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'

const tableData = ref<any[]>([])
const loading = ref(false)

const config = reactive({
  emailEnabled: true,
  smsEnabled: false,
  emailThreshold: 3,
  smsThreshold: 5
})

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 500))
    tableData.value = [
      { id: '1', type: '服务宕机', level: 'critical', content: 'Tomcat服务响应超时超过5分钟', time: '2024-03-15 10:30:00', status: 'resolved' },
      { id: '2', type: '接口超时', level: 'warning', content: '/api/submit 接口响应时间超过30秒', time: '2024-03-15 09:15:00', status: 'resolved' },
      { id: '3', type: '数据库异常', level: 'critical', content: '数据库连接池耗尽', time: '2024-03-14 22:00:00', status: 'resolved' }
    ]
  } finally { loading.value = false }
}

const handleMarkRead = () => { ElMessage.success('已标记为已读'); loadData() }
const getLevelType = (level: string) => level === 'critical' ? 'danger' : level === 'warning' ? 'warning' : 'info'
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>异常告警</h2></div>
    <div class="card-container" style="margin-bottom:20px">
      <el-form inline label-width="120px">
        <el-form-item label="邮件告警"><el-switch v-model="config.emailEnabled" /></el-form-item>
        <el-form-item label="邮件告警阈值"><el-input-number v-model="config.emailThreshold" :min="1" :max="10" :disabled="!config.emailEnabled" /></el-form-item>
        <el-form-item label="短信告警"><el-switch v-model="config.smsEnabled" /></el-form-item>
      </el-form>
    </div>
    <div class="card-container">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="level" label="级别" width="100"><template #default="{row}"><el-tag :type="getLevelType(row.level)" size="small">{{ row.level === 'critical' ? '严重' : '警告' }}</el-tag></template></el-table-column>
        <el-table-column prop="content" label="告警内容" min-width="300" />
        <el-table-column prop="time" label="时间" width="160" />
        <el-table-column prop="status" label="状态" width="100"><template #default="{row}"><el-tag :type="row.status === 'resolved' ? 'success' : 'warning'" size="small">{{ row.status === 'resolved' ? '已处理' : '待处理' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="120"><template #default><el-button type="primary" link @click="handleMarkRead">标记已读</el-button></template></el-table-column>
      </el-table>
      <div class="pagination-wrapper"><el-pagination :total="tableData.length" layout="total,prev,pager,next" /></div>
    </div>
  </div>
</template>

<style scoped>.pagination-wrapper{display:flex;justify-content:flex-end;margin-top:16px}</style>
