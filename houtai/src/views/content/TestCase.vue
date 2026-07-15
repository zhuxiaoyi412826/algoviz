<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElInputNumber, ElSelect, ElOption, ElMessage, ElMessageBox, ElPagination } from 'element-plus'
import { Plus, Edit, Delete, Upload, Download } from '@element-plus/icons-vue'

const tableData = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const selectedProblem = ref('')

const searchForm = reactive({ problemId: '' })
const formData = reactive({ id: '', problemId: '', input: '', output: '', score: 10, isSample: false })
const formRef = ref()

const problemOptions = [
  { label: 'P001 - 两数之和', value: '1' },
  { label: 'P002 - 二叉树层序遍历', value: '2' },
  { label: 'P003 - 正则表达式匹配', value: '3' }
]

const page = ref(1); const pageSize = ref(10); const total = ref(0)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 500))
    tableData.value = [
      { id: '1', problemId: '1', problemNo: 'P001', input: 'nums = [2,7,11,15], target = 9', output: '[0,1]', score: 10, isSample: true },
      { id: '2', problemId: '1', problemNo: 'P001', input: 'nums = [3,2,4], target = 6', output: '[1,2]', score: 10, isSample: true },
      { id: '3', problemId: '1', problemNo: 'P001', input: 'nums = [3,3], target = 6', output: '[0,1]', score: 30, isSample: false },
      { id: '4', problemId: '2', problemNo: 'P002', input: '[3,9,20,null,null,15,7]', output: '[[3],[9,20],[15,7]]', score: 10, isSample: true }
    ]
    total.value = 4
  } finally { loading.value = false }
}

const handleProblemChange = () => loadData()
const handleAdd = () => { Object.keys(formData).forEach(k => (formData as any)[k] = ''); formData.score = 10; dialogVisible.value = true }
const handleEdit = (row: any) => { Object.assign(formData, row); dialogVisible.value = true }
const handleDelete = async (row: any) => { try { await ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }); ElMessage.success('删除成功'); loadData() } catch {} }
const handleSubmit = async () => { ElMessage.success('保存成功'); dialogVisible.value = false; loadData() }
const handleBatchImport = () => ElMessage.info('批量导入功能开发中')
const handleExport = () => ElMessage.info('导出功能开发中')
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>测试用例管理</h2><div><el-button :icon="Upload" @click="handleBatchImport">批量导入</el-button><el-button :icon="Download" @click="handleExport">导出</el-button><el-button type="primary" :icon="Plus" @click="handleAdd">新增用例</el-button></div></div>
    <div class="card-container">
      <div class="filter-bar">
        <el-select v-model="searchForm.problemId" placeholder="选择题目" clearable style="width:200px" @change="handleProblemChange"><el-option v-for="item in problemOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe style="margin-top:16px">
        <el-table-column prop="problemNo" label="题号" width="80" />
        <el-table-column prop="input" label="输入" min-width="200" show-overflow-tooltip />
        <el-table-column prop="output" label="预期输出" min-width="150" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="80" />
        <el-table-column prop="isSample" label="示例" width="70"><template #default="{row}"><el-tag :type="row.isSample?'success':'info'" size="small">{{ row.isSample?'是':'否' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="160"><template #default="{row}"><el-button type="primary" link @click="handleEdit(row)">编辑</el-button><el-button type="danger" link @click="handleDelete(row)">删除</el-button></template></el-table-column>
      </el-table>
      <div class="pagination-wrapper"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" /></div>
    </div>
    <el-dialog v-model="dialogVisible" title="测试用例" width="600px">
      <el-form ref="formRef" :model="formData" label-width="100px">
        <el-form-item label="题目"><el-select v-model="formData.problemId" style="width:100%"><el-option v-for="item in problemOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="输入用例"><el-input v-model="formData.input" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="预期输出"><el-input v-model="formData.output" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="分值"><el-input-number v-model="formData.score" :min="1" :max="100" /></el-form-item>
        <el-form-item label="设为示例"><el-switch v-model="formData.isSample" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>.pagination-wrapper{display:flex;justify-content:flex-end;margin-top:16px}</style>
