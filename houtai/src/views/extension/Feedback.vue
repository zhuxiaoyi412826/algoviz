<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElInput, ElSelect, ElOption, ElMessage, ElPagination } from 'element-plus'

interface Feedback {
  id: string
  userId: string
  username: string
  email: string
  type: string
  title: string
  content: string
  status: string
  reply: string
  createTime: string
}

const tableData = ref<Feedback[]>([])
const loading = ref(false)
const replyVisible = ref(false)
const currentFeedback = ref<Feedback | null>(null)
const replyContent = ref('')

const searchForm = reactive({ type: '', status: '' })

const page = ref(1); const pageSize = ref(10); const total = ref(0)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (searchForm.type) params.append('type', searchForm.type)
    if (searchForm.status) params.append('status', searchForm.status.toUpperCase())
    
    const response = await fetch(`http://localhost/api/feedback?${params.toString()}`)
    const data = await response.json()
    
    if (data.success) {
      tableData.value = data.feedbacks.map((item: any) => ({
        id: item.id,
        userId: item.userId || '',
        username: item.username || '',
        email: item.email || '',
        type: item.type || '',
        title: item.title || '',
        content: item.content || '',
        status: item.status || '',
        reply: item.reply || '',
        createTime: item.createTime || ''
      }))
      total.value = data.count
    } else {
      ElMessage.error('加载反馈列表失败')
    }
  } catch (error) {
    console.error('加载反馈列表失败:', error)
    ElMessage.error('加载反馈列表失败')
  } finally { loading.value = false }
}

const handleReply = (row: Feedback) => { 
  currentFeedback.value = row; 
  replyContent.value = ''; 
  replyVisible.value = true 
}

const handleSubmitReply = async () => {
  if (!currentFeedback.value || !replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  
  try {
    const response = await fetch(`http://localhost/api/feedback/${currentFeedback.value.id}/reply`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reply: replyContent.value })
    })
    const data = await response.json()
    
    if (data.success) {
      ElMessage.success('回复成功')
      replyVisible.value = false
      loadData()
    } else {
      ElMessage.error(data.message || '回复失败')
    }
  } catch (error) {
    console.error('回复失败:', error)
    ElMessage.error('回复失败')
  }
}

const handleUpdateStatus = async (row: Feedback, status: string) => {
  try {
    const response = await fetch(`http://localhost/api/feedback/${row.id}/reply`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reply: '问题已处理完成' })
    })
    const data = await response.json()
    
    if (data.success) {
      ElMessage.success('状态更新成功')
      loadData()
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('操作失败')
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>反馈管理</h2></div>
    <div class="card-container">
      <div class="filter-bar">
        <el-select v-model="searchForm.type" placeholder="类型" clearable style="width:120px">
          <el-option label="可视化" value="visualization" />
          <el-option label="OJ" value="oj" />
          <el-option label="AI" value="ai" />
          <el-option label="其他" value="other" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:120px">
          <el-option label="待处理" value="pending" />
          <el-option label="已回复" value="replied" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe style="margin-top:16px">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="title" label="标题" width="150" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{row}">
            <el-tag size="small">{{ row.type === 'visualization' ? '可视化' : row.type === 'oj' ? 'OJ' : row.type === 'ai' ? 'AI' : '其他' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="反馈内容" min-width="300" />
        <el-table-column prop="reply" label="回复" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="row.status === 'REPLIED' ? 'success' : 'info'" size="small">
              {{ row.status === 'REPLIED' ? '已回复' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="160">
          <template #default="{row}">
            <el-button type="primary" link @click="handleReply(row)">回复</el-button>
            <el-button v-if="row.status !== 'REPLIED'" type="success" link @click="handleUpdateStatus(row, 'REPLIED')">完成</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" /></div>
    </div>
    <el-dialog v-model="replyVisible" title="回复反馈" width="500px">
      <div class="feedback-content">
        <div><strong>用户：</strong>{{ currentFeedback?.username }}</div>
        <div><strong>标题：</strong>{{ currentFeedback?.title }}</div>
        <div><strong>反馈内容：</strong>{{ currentFeedback?.content }}</div>
      </div>
      <el-input v-model="replyContent" type="textarea" :rows="4" placeholder="请输入回复内容" style="margin-top:16px" />
      <template #footer>
        <el-button @click="replyVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSubmitReply">发送回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pagination-wrapper{display:flex;justify-content:flex-end;margin-top:16px}
.feedback-content{padding:12px;background:#f5f7fa;border-radius:4px}
</style>
