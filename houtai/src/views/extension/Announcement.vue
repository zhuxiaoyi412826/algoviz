<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElSwitch, ElMessage, ElMessageBox, ElPagination } from 'element-plus'
import { Plus, Edit, Delete, Top, Search } from '@element-plus/icons-vue'

interface Announcement {
  id: number | string
  title: string
  content: string
  type: 'update' | 'notice' | 'activity'
  isTop: boolean
  status: 'draft' | 'published'
  sortOrder?: number
  publishTime?: string
  createTime?: string
  updateTime?: string
}

const API_BASE = 'http://localhost/api/announcements'

const tableData = ref<Announcement[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增公告')
const submitting = ref(false)

const formData = reactive<Partial<Announcement>>({
  id: undefined,
  title: '',
  content: '',
  type: 'notice',
  isTop: false,
  status: 'draft'
})
const formRef = ref()
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const typeOptions = [
  { label: '更新通知', value: 'update' },
  { label: '普通公告', value: 'notice' },
  { label: '活动通知', value: 'activity' }
]

const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '立即发布', value: 'published' }
]

const searchKeyword = ref('')

const page = ref(1)
const pageSize = ref(10)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    const response = await fetch(API_BASE)
    const data = await response.json()
    if (data.success) {
      const list: Announcement[] = (data.announcements || []).map((item: any) => ({
        id: item.id,
        title: item.title || '',
        content: item.content || '',
        type: item.type || 'notice',
        isTop: !!item.isTop,
        status: item.status || 'draft',
        sortOrder: item.sortOrder,
        publishTime: item.publishTime,
        createTime: item.createTime,
        updateTime: item.updateTime
      }))
      // 置顶优先，再按发布/创建时间倒序
      list.sort((a, b) => {
        if (a.isTop !== b.isTop) return a.isTop ? -1 : 1
        const ta = new Date(a.publishTime || a.createTime || 0).getTime()
        const tb = new Date(b.publishTime || b.createTime || 0).getTime()
        return tb - ta
      })
      tableData.value = list
    } else {
      ElMessage.error(data.message || '加载公告列表失败')
    }
  } catch (error) {
    console.error('加载公告列表失败:', error)
    ElMessage.error('加载公告列表失败')
  } finally {
    loading.value = false
  }
}

// 关键字过滤（按标题）
const filteredData = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return tableData.value
  return tableData.value.filter(item => item.title.toLowerCase().includes(kw))
})

const total = computed(() => filteredData.value.length)

// 前端分页
const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

const handleSearch = () => {
  page.value = 1
}

const resetForm = () => {
  formData.id = undefined
  formData.title = ''
  formData.content = ''
  formData.type = 'notice'
  formData.isTop = false
  formData.status = 'draft'
}

const handleAdd = () => {
  dialogTitle.value = '新增公告'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: Announcement) => {
  dialogTitle.value = '编辑公告'
  resetForm()
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: Announcement) => {
  try {
    await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '提示', { type: 'warning' })
    const response = await fetch(`${API_BASE}/${row.id}`, { method: 'DELETE' })
    const data = await response.json()
    if (data.success) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        title: formData.title,
        content: formData.content,
        type: formData.type,
        isTop: formData.isTop,
        status: formData.status
      }
      const isEdit = formData.id !== undefined && formData.id !== null
      const url = isEdit ? `${API_BASE}/${formData.id}` : API_BASE
      const method = isEdit ? 'PUT' : 'POST'
      const response = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      })
      const data = await response.json()
      if (data.success) {
        ElMessage.success(dialogTitle.value + '成功')
        dialogVisible.value = false
        loadData()
      } else {
        ElMessage.error(data.message || '操作失败')
      }
    } catch (error) {
      console.error('提交失败:', error)
      ElMessage.error('操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const handlePublish = async (row: Announcement) => {
  try {
    const response = await fetch(`${API_BASE}/${row.id}/publish`, { method: 'PUT' })
    const data = await response.json()
    if (data.success) {
      ElMessage.success('发布成功')
      loadData()
    } else {
      ElMessage.error(data.message || '发布失败')
    }
  } catch (error) {
    console.error('发布失败:', error)
    ElMessage.error('发布失败')
  }
}

const handleUnpublish = async (row: Announcement) => {
  try {
    const response = await fetch(`${API_BASE}/${row.id}/unpublish`, { method: 'PUT' })
    const data = await response.json()
    if (data.success) {
      ElMessage.success('已取消发布')
      loadData()
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    console.error('取消发布失败:', error)
    ElMessage.error('操作失败')
  }
}

const getTypeLabel = (type: string) => typeOptions.find(t => t.value === type)?.label || type

const formatTime = (t?: string) => {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 19)
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>公告管理</h2>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增公告</el-button>
    </div>
    <div class="card-container">
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="请输入标题关键字搜索"
          clearable
          style="width:240px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      </div>
      <el-table :data="pagedData" v-loading="loading" stripe style="margin-top:16px">
        <el-table-column label="置顶" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.isTop" type="danger" size="small">置顶</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'" size="small">
              {{ row.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
            <el-button
              v-if="row.status === 'draft'"
              type="success"
              link
              size="small"
              style="margin-left:8px"
              @click="handlePublish(row)"
            >发布</el-button>
            <el-button
              v-else
              type="warning"
              link
              size="small"
              style="margin-left:8px"
              @click="handleUnpublish(row)"
            >取消发布</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="160">
          <template #default="{ row }">{{ formatTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'draft'" type="success" link @click="handlePublish(row)">发布</el-button>
            <el-button v-else type="warning" link @click="handleUnpublish(row)">取消发布</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total,sizes,prev,pager,next"
        />
      </div>
    </div>
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="formData.type" style="width:100%">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="formData.content" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="formData.isTop" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="formData.status" style="width:100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pagination-wrapper{display:flex;justify-content:flex-end;margin-top:16px}
.filter-bar{display:flex;gap:12px;align-items:center}
</style>
