<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElMessage, ElMessageBox, ElPagination } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import type { Algorithm } from '@/types'

const tableData = ref<Algorithm[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增算法')

const searchForm = reactive({ name: '', category: '', status: '' })
const formData = reactive<Partial<Algorithm>>({ id: '', name: '', category: 'sort', description: '', timeComplexity: '', spaceComplexity: '', pseudocode: '', status: 'enabled' })
const formRef = ref()
const rules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }], category: [{ required: true, message: '请选择分类', trigger: 'change' }] }

const categoryOptions = [
  { label: '排序', value: 'sort' }, { label: '查找', value: 'search' },
  { label: '图论', value: 'graph' }, { label: '动态规划', value: 'dynamic' },
  { label: '树', value: 'tree' }, { label: '其他', value: 'other' }
]

const page = ref(1); const pageSize = ref(10); const total = ref(0)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 500))
    tableData.value = [
      { id: '1', name: '快速排序', category: 'sort', description: '分治排序算法', timeComplexity: 'O(nlogn)', spaceComplexity: 'O(logn)', status: 'enabled', createTime: '2024-01-01', updateTime: '2024-01-01' },
      { id: '2', name: '二分查找', category: 'search', description: '有序数组查找', timeComplexity: 'O(logn)', spaceComplexity: 'O(1)', status: 'enabled', createTime: '2024-01-01', updateTime: '2024-01-01' },
      { id: '3', name: 'Dijkstra', category: 'graph', description: '最短路径算法', timeComplexity: 'O((V+E)logV)', spaceComplexity: 'O(V)', status: 'enabled', createTime: '2024-01-01', updateTime: '2024-01-01' }
    ]
    total.value = 3
  } finally { loading.value = false }
}

const handleAdd = () => { dialogTitle.value = '新增算法'; Object.keys(formData).forEach(k => (formData as any)[k] = ''); formData.status = 'enabled'; dialogVisible.value = true }
const handleEdit = (row: Algorithm) => { dialogTitle.value = '编辑算法'; Object.assign(formData, row); dialogVisible.value = true }
const handleDelete = async (row: Algorithm) => { try { await ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }); ElMessage.success('删除成功'); loadData() } catch {} }
const handleSubmit = async () => { if (!formRef.value) return; await formRef.value.validate(async (valid: boolean) => { if (!valid) return; ElMessage.success(dialogTitle.value + '成功'); dialogVisible.value = false; loadData() }) }
const getCategoryLabel = (cat: string) => categoryOptions.find(c => c.value === cat)?.label || cat
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>算法可视化</h2><el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button></div>
    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.name" placeholder="名称" clearable style="width:150px" />
        <el-select v-model="searchForm.category" placeholder="分类" clearable style="width:120px"><el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        <el-button type="primary" :icon="Search">查询</el-button><el-button :icon="Refresh">重置</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe style="margin-top:16px">
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="category" label="分类" width="100"><template #default="{row}"><el-tag size="small">{{ getCategoryLabel(row.category) }}</el-tag></template></el-table-column>
        <el-table-column prop="timeComplexity" label="时间复杂度" width="120" />
        <el-table-column prop="spaceComplexity" label="空间复杂度" width="120" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="160"><template #default="{row}"><el-button type="primary" link @click="handleEdit(row)">编辑</el-button><el-button type="danger" link @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
      <div class="pagination-wrapper"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" /></div>
    </div>
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px"><el-form ref="formRef" :model="formData" :rules="rules" label-width="100px"><el-form-item label="名称" prop="name"><el-input v-model="formData.name" /></el-form-item><el-form-item label="分类" prop="category"><el-select v-model="formData.category" style="width:100%"><el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item><el-form-item label="时间复杂度"><el-input v-model="formData.timeComplexity" /></el-form-item><el-form-item label="空间复杂度"><el-input v-model="formData.spaceComplexity" /></el-form-item><el-form-item label="描述"><el-input v-model="formData.description" type="textarea" :rows="3" /></el-form-item><el-form-item label="伪代码"><el-input v-model="formData.pseudocode" type="textarea" :rows="4" /></el-form-item></el-form><template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template></el-dialog>
  </div>
</template>

<style scoped>.pagination-wrapper{display:flex;justify-content:flex-end;margin-top:16px}</style>
