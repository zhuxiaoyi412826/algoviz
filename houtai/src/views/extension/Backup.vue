<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElTag, ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Download, Refresh, Delete } from '@element-plus/icons-vue'

const tableData = ref<any[]>([])
const loading = ref(false)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 500))
    tableData.value = [
      { id: '1', name: 'backup_20240315.sql', size: '15.6MB', type: '完整备份', createTime: '2024-03-15 02:00:00', status: 'completed' },
      { id: '2', name: 'backup_20240314.sql', size: '15.4MB', type: '完整备份', createTime: '2024-03-14 02:00:00', status: 'completed' },
      { id: '3', name: 'backup_20240313.sql', size: '15.2MB', type: '完整备份', createTime: '2024-03-13 02:00:00', status: 'completed' }
    ]
  } finally { loading.value = false }
}

const handleCreate = async () => {
  try {
    await ElMessageBox.confirm('确定要创建新的备份吗？', '提示', { type: 'info' })
    ElMessage.success('备份任务已创建，请在后台任务中查看进度')
  } catch {}
}

const handleRestore = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要恢复此备份吗？当前数据将被覆盖！', '警告', { type: 'warning' })
    ElMessage.success('恢复任务已启动，请等待完成')
  } catch {}
}

const handleDownload = (row: any) => ElMessage.info(`正在下载 ${row.name}`)
const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除此备份吗？', '提示', { type: 'warning' })
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>数据备份</h2><el-button type="primary" :icon="Upload" @click="handleCreate">创建备份</el-button></div>
    <div class="card-container">
      <el-table :data="tableData" v-loading="loading" stripe style="margin-top:16px">
        <el-table-column prop="name" label="文件名" min-width="250" />
        <el-table-column prop="size" label="大小" width="100" />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column prop="status" label="状态" width="100"><template #default="{row}"><el-tag :type="row.status === 'completed' ? 'success' : 'warning'" size="small">{{ row.status === 'completed' ? '已完成' : '进行中' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="240"><template #default="{row}"><el-button type="primary" link @click="handleRestore(row)">恢复</el-button><el-button :icon="Download" link @click="handleDownload(row)">下载</el-button><el-button type="danger" link @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
    </div>
  </div>
</template>
