<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem,
  ElInput, ElSelect, ElOption, ElMessage, ElMessageBox, ElPagination,
  ElInputNumber, ElRadioGroup, ElRadio, ElDivider
} from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, Upload, Download, View } from '@element-plus/icons-vue'
import type { OJProblem } from '@/types'

const tableData = ref<OJProblem[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('新增题目')

const searchForm = reactive({
  keyword: '',
  difficulty: '',
  status: ''
})

const formData = reactive<Partial<OJProblem>>({
  id: '',
  problemNo: '',
  title: '',
  difficulty: 'medium',
  tags: [],
  description: '',
  template: {},
  status: 'offline'
})

const formRef = ref()
const currentProblem = ref<OJProblem | null>(null)

const rules = {
  problemNo: [{ required: true, message: '请输入题号', trigger: 'blur' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  description: [{ required: true, message: '请输入题目描述', trigger: 'blur' }]
}

const difficultyOptions = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' }
]

const statusOptions = [
  { label: '上线', value: 'online' },
  { label: '下线', value: 'offline' }
]

const tagOptions = [
  '数组', '链表', '树', '图', '动态规划', '回溯', '贪心', '分治', '排序', '查找', '字符串', '数学'
]

const languageOptions = [
  { label: 'Python', value: 'python' },
  { label: 'Java', value: 'java' },
  { label: 'C++', value: 'cpp' },
  { label: 'JavaScript', value: 'javascript' }
]

const page = ref(1)
const pageSize = ref(10)

onMounted(() => {
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (searchForm.keyword) params.append('keyword', searchForm.keyword)
    if (searchForm.difficulty) params.append('difficulty', searchForm.difficulty)
    
    const response = await fetch(`http://localhost/api/problems?${params.toString()}`)
    const data = await response.json()
    
    if (data.success) {
      tableData.value = data.problems.map((item: any) => ({
        id: item.id,
        problemNo: item.problemNo,
        title: item.title,
        difficulty: item.difficulty,
        tags: item.tags ? item.tags.split(',') : [],
        description: item.description,
        template: { java: item.template || '' },
        status: item.status === 'ACTIVE' ? 'online' : 'offline',
        submissionCount: item.submissionCount || 0,
        acRate: item.acRate || 0,
        createTime: item.createdAt,
        updateTime: item.updatedAt
      }))
      total.value = data.count
    } else {
      ElMessage.error('加载题目失败')
    }
  } catch (error) {
    console.error('加载题目失败:', error)
    ElMessage.error('加载题目失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.difficulty = ''
  searchForm.status = ''
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增题目'
  Object.keys(formData).forEach(key => {
    (formData as any)[key] = key === 'difficulty' ? 'medium' : key === 'tags' ? [] : key === 'template' ? {} : key === 'status' ? 'offline' : ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: OJProblem) => {
  dialogTitle.value = '编辑题目'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleView = (row: OJProblem) => {
  currentProblem.value = row
  detailVisible.value = true
}

const handleDelete = async (row: OJProblem) => {
  try {
    await ElMessageBox.confirm('确定要删除该题目吗？', '提示', { type: 'warning' })
    
    const response = await fetch(`http://localhost/api/problems/${row.id}`, {
      method: 'DELETE'
    })
    const data = await response.json()
    
    if (data.success) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch {}
}

const handleToggleStatus = async (row: OJProblem) => {
  const newStatus = row.status === 'online' ? 'offline' : 'online'
  try {
    await ElMessageBox.confirm(`确定要${newStatus === 'online' ? '上线' : '下线'}该题目吗？`, '提示', { type: 'warning' })
    
    const response = await fetch(`http://localhost/api/problems/${row.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus === 'online' ? 'ACTIVE' : 'INACTIVE' })
    })
    const data = await response.json()
    
    if (data.success) {
      ElMessage.success('操作成功')
      loadData()
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch {}
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    
    try {
      const submitData = {
        problemNo: formData.problemNo,
        title: formData.title,
        difficulty: formData.difficulty,
        tags: (formData.tags || []).join(','),
        description: formData.description,
        template: formData.template?.java || '',
        status: formData.status === 'online' ? 'ACTIVE' : 'INACTIVE'
      }
      
      let response
      if (formData.id) {
        // 更新题目
        response = await fetch(`http://localhost/api/problems/${formData.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(submitData)
        })
      } else {
        // 新增题目
        response = await fetch('http://localhost/api/problems', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(submitData)
        })
      }
      
      const data = await response.json()
      if (data.success) {
        ElMessage.success(dialogTitle.value + '成功')
        dialogVisible.value = false
        loadData()
      } else {
        ElMessage.error(data.message || '操作失败')
      }
    } catch (error) {
      console.error('保存题目失败:', error)
      ElMessage.error('保存题目失败')
    }
  })
}

const handleBatchImport = () => {
  ElMessage.info('批量导入功能开发中')
}

const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

const getDifficultyType = (difficulty: string) => {
  const map: Record<string, any> = { easy: 'success', medium: 'warning', hard: 'danger' }
  return map[difficulty] || 'info'
}

const getDifficultyLabel = (difficulty: string) => {
  const map: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难' }
  return map[difficulty] || difficulty
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>OJ题目管理</h2>
      <div>
        <el-button :icon="Upload" @click="handleBatchImport">批量导入</el-button>
        <el-button :icon="Download" @click="handleExport">导出</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增题目</el-button>
      </div>
    </div>

    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.keyword" placeholder="题号/标题/标签" clearable style="width: 200px" />
        <el-select v-model="searchForm.difficulty" placeholder="难度" clearable style="width: 100px">
          <el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 100px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="problemNo" label="题号" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="difficulty" label="难度" width="80">
          <template #default="{ row }">
            <el-tag :type="getDifficultyType(row.difficulty)" size="small">
              {{ getDifficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="tag in row.tags" :key="tag" size="small" style="margin-right: 4px">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submissionCount" label="提交数" width="80" />
        <el-table-column prop="acRate" label="通过率" width="80">
          <template #default="{ row }">
            <span :class="row.acRate >= 50 ? 'color-success' : row.acRate >= 30 ? 'color-warning' : 'color-danger'">
              {{ row.acRate }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              inline-prompt
              active-value="online"
              inactive-value="offline"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="110" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" @closed="formRef?.resetFields()">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="题号" prop="problemNo">
          <el-input v-model="formData.problemNo" :disabled="!!formData.id" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-radio-group v-model="formData.difficulty">
            <el-radio v-for="item in difficultyOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="formData.tags" multiple placeholder="选择标签" style="width: 100%">
            <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="6" placeholder="支持Markdown格式" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio value="online">上线</el-radio>
            <el-radio value="offline">下线</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="题目详情" width="800px">
      <template v-if="currentProblem">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="题号">{{ currentProblem.problemNo }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ currentProblem.title }}</el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-tag :type="getDifficultyType(currentProblem.difficulty)" size="small">
              {{ getDifficultyLabel(currentProblem.difficulty) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentProblem.status === 'online' ? '上线' : '下线' }}</el-descriptions-item>
          <el-descriptions-item label="提交数">{{ currentProblem.submissionCount }}</el-descriptions-item>
          <el-descriptions-item label="通过率">{{ currentProblem.acRate }}%</el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <el-tag v-for="tag in currentProblem.tags" :key="tag" size="small" style="margin-right: 4px">{{ tag }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">题目描述</el-divider>
        <div class="problem-description">{{ currentProblem.description }}</div>

        <el-divider content-position="left">代码模板</el-divider>
        <el-tabs v-if="Object.keys(currentProblem.template || {}).length > 0">
          <el-tab-pane v-for="lang in languageOptions" :key="lang.value" :label="lang.label" :name="lang.value">
            <pre class="code-block">{{ currentProblem.template?.[lang.value] || '暂无模板' }}</pre>
          </el-tab-pane>
        </el-tabs>
        <el-empty v-else description="暂无代码模板" />
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.problem-description {
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  line-height: 1.8;
}

.code-block {
  padding: 16px;
  background-color: #282c34;
  color: #abb2bf;
  border-radius: 4px;
  overflow-x: auto;
  font-family: 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.5;
}
</style>
