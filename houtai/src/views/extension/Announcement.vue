<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElSwitch, ElDatePicker, ElMessage, ElMessageBox, ElPagination } from 'element-plus'
import { Plus, Edit, Delete, Top } from '@element-plus/icons-vue'
import type { Announcement } from '@/types'

const tableData = ref<Announcement[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增公告')

const formData = reactive<Partial<Announcement>>({ id: '', title: '', content: '', type: 'notice', isTop: false, status: 'draft' })
const formRef = ref()
const rules = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }], content: [{ required: true, message: '请输入内容', trigger: 'blur' }] }

const typeOptions = [
  { label: '更新通知', value: 'update' },
  { label: '普通公告', value: 'notice' },
  { label: '活动通知', value: 'activity' }
]

const page = ref(1); const pageSize = ref(10); const total = ref(0)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 500))
    tableData.value = [
      { id: '1', title: '平台更新公告', content: '新增算法可视化功能...', type: 'update', isTop: true, status: 'published', publishTime: '2024-03-15', createTime: '2024-03-14' },
      { id: '2', title: '清明节活动通知', content: '节日期间OJ题目双倍积分...', type: 'activity', isTop: false, status: 'scheduled', publishTime: '2024-04-01', createTime: '2024-03-10' }
    ]
    total.value = 2
  } finally { loading.value = false }
}

const handleAdd = () => { dialogTitle.value = '新增公告'; Object.keys(formData).forEach(k => (formData as any)[k] = ''); formData.isTop = false; formData.status = 'draft'; dialogVisible.value = true }
const handleEdit = (row: Announcement) => { dialogTitle.value = '编辑公告'; Object.assign(formData, row); dialogVisible.value = true }
const handleDelete = async (row: Announcement) => { try { await ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }); ElMessage.success('删除成功'); loadData() } catch {} }
const handleSubmit = async () => { if (!formRef.value) return; await formRef.value.validate(async (valid: boolean) => { if (!valid) return; ElMessage.success(dialogTitle.value + '成功'); dialogVisible.value = false; loadData() }) }
const getTypeLabel = (type: string) => typeOptions.find(t => t.value === type)?.label || type
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>公告管理</h2><el-button type="primary" :icon="Plus" @click="handleAdd">新增公告</el-button></div>
    <div class="card-container">
      <el-table :data="tableData" v-loading="loading" stripe style="margin-top:16px">
        <el-table-column label="置顶" width="70"><template #default="{row}"><el-tag v-if="row.isTop" type="danger" size="small">置顶</el-tag></template></el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="type" label="类型" width="100"><template #default="{row}"><el-tag size="small">{{ getTypeLabel(row.type) }}</el-tag></template></el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="110" />
        <el-table-column prop="status" label="状态" width="100"><template #default="{row}"><el-tag :type="row.status === 'published' ? 'success' : row.status === 'scheduled' ? 'warning' : 'info'" size="small">{{ row.status === 'published' ? '已发布' : row.status === 'scheduled' ? '定时发布' : '草稿' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="200"><template #default="{row}"><el-button type="primary" link @click="handleEdit(row)">编辑</el-button><el-button type="danger" link @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
      <div class="pagination-wrapper"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" /></div>
    </div>
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px"><el-form ref="formRef" :model="formData" :rules="rules" label-width="80px"><el-form-item label="标题" prop="title"><el-input v-model="formData.title" /></el-form-item><el-form-item label="类型"><el-select v-model="formData.type" style="width:100%"><el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item><el-form-item label="内容" prop="content"><el-input v-model="formData.content" type="textarea" :rows="6" /></el-form-item><el-form-item label="置顶"><el-switch v-model="formData.isTop" /></el-form-item><el-form-item label="状态"><el-select v-model="formData.status" style="width:100%"><el-option label="草稿" value="draft" /><el-option label="立即发布" value="published" /><el-option label="定时发布" value="scheduled" /></el-select></el-form-item></el-form><template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template></el-dialog>
  </div>
</template>

<style scoped>.pagination-wrapper{display:flex;justify-content:flex-end;margin-top:16px}</style>
