<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElMessage, ElMessageBox, ElPagination } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import type { DataStructure } from '@/types'

const tableData = ref<DataStructure[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增数据结构')

const searchForm = reactive({ name: '', type: '', status: '' })
const formData = reactive<Partial<DataStructure>>({ id: '', name: '', type: 'array', description: '', status: 'enabled' })
const formRef = ref()
const rules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }], type: [{ required: true, message: '请选择类型', trigger: 'change' }] }

const typeOptions = [
  { label: '数组', value: 'array' }, { label: '链表', value: 'linked_list' },
  { label: '树', value: 'tree' }, { label: '图', value: 'graph' },
  { label: '栈', value: 'stack' }, { label: '队列', value: 'queue' }, { label: '哈希表', value: 'hash' }
]

const page = ref(1); const pageSize = ref(10); const total = ref(0)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 500))
    tableData.value = [
      { id: '1', name: '数组', type: 'array', description: '线性表数据结构', status: 'enabled', createTime: '2024-01-01', updateTime: '2024-01-01' },
      { id: '2', name: '链表', type: 'linked_list', description: '链式存储结构', status: 'enabled', createTime: '2024-01-01', updateTime: '2024-01-01' },
      { id: '3', name: '二叉树', type: 'tree', description: '每个节点最多两个子节点', status: 'enabled', createTime: '2024-01-01', updateTime: '2024-01-01' }
    ]
    total.value = 3
  } finally { loading.value = false }
}

const handleAdd = () => { dialogTitle.value = '新增数据结构'; Object.keys(formData).forEach(k => (formData as any)[k] = ''); formData.status = 'enabled'; dialogVisible.value = true }
const handleEdit = (row: DataStructure) => { dialogTitle.value = '编辑数据结构'; Object.assign(formData, row); dialogVisible.value = true }
const handleDelete = async (row: DataStructure) => { try { await ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }); ElMessage.success('删除成功'); loadData() } catch {} }
const handleSubmit = async () => { if (!formRef.value) return; await formRef.value.validate(async (valid: boolean) => { if (!valid) return; ElMessage.success(dialogTitle.value + '成功'); dialogVisible.value = false; loadData() }) }
const getTypeLabel = (type: string) => typeOptions.find(t => t.value === type)?.label || type
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>数据结构可视化</h2><el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button></div>
    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.name" placeholder="名称" clearable style="width:150px" />
        <el-select v-model="searchForm.type" placeholder="类型" clearable style="width:120px"><el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        <el-button type="primary" :icon="Search">查询</el-button><el-button :icon="Refresh">重置</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe style="margin-top:16px">
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="type" label="类型" width="120"><template #default="{row}"><el-tag size="small">{{ getTypeLabel(row.type) }}</el-tag></template></el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status==='enabled'?'success':'danger'" size="small">{{ row.status==='enabled'?'启用':'禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="160"><template #default="{row}"><el-button type="primary" link @click="handleEdit(row)">编辑</el-button><el-button type="danger" link @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
      <div class="pagination-wrapper"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" /></div>
    </div>
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px"><el-form ref="formRef" :model="formData" :rules="rules" label-width="80px"><el-form-item label="名称" prop="name"><el-input v-model="formData.name" /></el-form-item><el-form-item label="类型" prop="type"><el-select v-model="formData.type" style="width:100%"><el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item><el-form-item label="描述"><el-input v-model="formData.description" type="textarea" :rows="3" /></el-form-item><el-form-item label="状态"><el-select v-model="formData.status" style="width:100%"><el-option label="启用" value="enabled" /><el-option label="禁用" value="disabled" /></el-select></el-form-item></el-form><template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template></el-dialog>
  </div>
</template>

<style scoped>.pagination-wrapper{display:flex;justify-content:flex-end;margin-top:16px}</style>
