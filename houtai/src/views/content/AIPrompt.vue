<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElMessage, ElMessageBox, ElPagination } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import type { AIPrompt } from '@/types'

const tableData = ref<AIPrompt[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增提示词')

const searchForm = reactive({ title: '', category: '' })
const formData = reactive<Partial<AIPrompt>>({ id: '', title: '', category: '', content: '', status: 'enabled' })
const formRef = ref()
const rules = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }], content: [{ required: true, message: '请输入内容', trigger: 'blur' }] }

const categoryOptions = [
  { label: '数据结构', value: 'datastructure' },
  { label: '算法原理', value: 'algorithm' },
  { label: 'OJ题目', value: 'oj' },
  { label: '代码实现', value: 'code' }
]

const page = ref(1); const pageSize = ref(10); const total = ref(0)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 500))
    tableData.value = [
      { id: '1', title: '快速排序原理', category: 'algorithm', content: '请详细解释快速排序的原理和实现步骤', usageCount: 156, status: 'enabled', createTime: '2024-01-01' },
      { id: '2', title: '二叉树层序遍历', category: 'code', content: '请提供二叉树层序遍历的Python实现代码', usageCount: 89, status: 'enabled', createTime: '2024-01-01' },
      { id: '3', title: '链表反转', category: 'datastructure', content: '请解释链表反转的思路和代码实现', usageCount: 234, status: 'enabled', createTime: '2024-01-01' }
    ]
    total.value = 3
  } finally { loading.value = false }
}

const handleAdd = () => { dialogTitle.value = '新增提示词'; Object.keys(formData).forEach(k => (formData as any)[k] = ''); formData.status = 'enabled'; dialogVisible.value = true }
const handleEdit = (row: AIPrompt) => { dialogTitle.value = '编辑提示词'; Object.assign(formData, row); dialogVisible.value = true }
const handleDelete = async (row: AIPrompt) => { try { await ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }); ElMessage.success('删除成功'); loadData() } catch {} }
const handleSubmit = async () => { if (!formRef.value) return; await formRef.value.validate(async (valid: boolean) => { if (!valid) return; ElMessage.success(dialogTitle.value + '成功'); dialogVisible.value = false; loadData() }) }
const getCategoryLabel = (cat: string) => categoryOptions.find(c => c.value === cat)?.label || cat
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>AI快捷提示词</h2><el-button type="primary" :icon="Plus" @click="handleAdd">新增提示词</el-button></div>
    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.title" placeholder="标题" clearable style="width:150px" />
        <el-select v-model="searchForm.category" placeholder="分类" clearable style="width:120px"><el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        <el-button type="primary" :icon="Search">查询</el-button><el-button :icon="Refresh">重置</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe style="margin-top:16px">
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column prop="category" label="分类" width="100"><template #default="{row}"><el-tag size="small">{{ getCategoryLabel(row.category) }}</el-tag></template></el-table-column>
        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="usageCount" label="使用次数" width="100" />
        <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status==='enabled'?'success':'danger'" size="small">{{ row.status==='enabled'?'启用':'禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="160"><template #default="{row}"><el-button type="primary" link @click="handleEdit(row)">编辑</el-button><el-button type="danger" link @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
      <div class="pagination-wrapper"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" /></div>
    </div>
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px"><el-form ref="formRef" :model="formData" :rules="rules" label-width="80px"><el-form-item label="标题" prop="title"><el-input v-model="formData.title" /></el-form-item><el-form-item label="分类" prop="category"><el-select v-model="formData.category" style="width:100%"><el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item><el-form-item label="内容" prop="content"><el-input v-model="formData.content" type="textarea" :rows="4" /></el-form-item><el-form-item label="状态"><el-select v-model="formData.status" style="width:100%"><el-option label="启用" value="enabled" /><el-option label="禁用" value="disabled" /></el-select></el-form-item></el-form><template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template></el-dialog>
  </div>
</template>

<style scoped>.pagination-wrapper{display:flex;justify-content:flex-end;margin-top:16px}</style>
