<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElImage } from 'element-plus'
import request from '@/api/request'

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
  imageCount: number
}

const tableData = ref<Feedback[]>([])
const loading = ref(false)
const replyVisible = ref(false)
const detailVisible = ref(false)
const currentFeedback = ref<Feedback | null>(null)
const replyContent = ref('')
const detailImageUrls = ref<string[]>([])

const searchForm = reactive({ type: '', status: '' })

const page = ref(1); const pageSize = ref(10); const total = ref(0)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (searchForm.type) params.type = searchForm.type
    if (searchForm.status) params.status = searchForm.status.toUpperCase()

    const data: any = await request.get('/feedback', { params })

    const list = Array.isArray(data) ? data : (data?.feedbacks || [])
    const count = Array.isArray(data) ? data.length : (data?.count || list.length)

    tableData.value = list.map((item: any) => ({
      id: item.id,
      userId: item.userId || '',
      username: item.username || '',
      email: item.email || '',
      type: item.type || '',
      title: item.title || '',
      content: item.content || '',
      status: item.status || '',
      reply: item.reply || '',
      createTime: item.createTime || '',
      imageCount: item.imageCount ?? 0
    }))
    total.value = count
  } catch (error) {
    console.error('加载反馈列表失败:', error)
  } finally { loading.value = false }
}

const handleDetail = async (row: Feedback) => {
  currentFeedback.value = { ...row }
  detailImageUrls.value = []
  detailVisible.value = true
  try {
    if ((row.imageCount ?? 0) > 0) {
      const data: any = await request.get(`/feedback/${row.id}/images`)
      const urls: string[] = data?.urls || []
      detailImageUrls.value = urls.map((u: string) =>
        u.startsWith('http') ? u : `${u}`
      )
    }
  } catch (e) { console.warn('加载反馈图片失败:', e) }
}

const handleReply = (row: Feedback) => {
  currentFeedback.value = row
  replyContent.value = ''
  replyVisible.value = true
}

const handleSubmitReply = async () => {
  if (!currentFeedback.value || !replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  try {
    await request.put(`/feedback/${currentFeedback.value.id}/reply`, { reply: replyContent.value })
    ElMessage.success('回复成功')
    replyVisible.value = false
    loadData()
  } catch (error) {
    console.error('回复失败:', error)
  }
}

const handleUpdateStatus = async (row: Feedback) => {
  try {
    await request.put(`/feedback/${row.id}/reply`, { reply: row.reply || '问题已处理完成' })
    ElMessage.success('状态更新成功')
    loadData()
  } catch (error) {
    console.error('更新状态失败:', error)
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
      <el-table :data="tableData" v-loading="loading" stripe style="margin-top:16px" :row-style="{ height: '72px' }">
        <el-table-column prop="username" label="用户名" width="100" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" width="160" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" width="130" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{row}">
            <el-tag size="small">{{ row.type === 'visualization' ? '可视化' : row.type === 'oj' ? 'OJ' : row.type === 'ai' ? 'AI' : '其他' }}</el-tag>
          </template>
        </el-table-column>

        <!-- 反馈内容列：填满剩余空间，固定高度，超出截断 -->
        <el-table-column label="反馈内容" :flex="1">
          <template #default="{ row }">
            <div
              class="content-cell"
              @click="handleDetail(row)"
              title="点击查看详情"
            >
              <div class="content-cell-inner">
                {{ row.content || '' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 图片列：显示缩略图标 + 数量 -->
        <el-table-column label="图片" width="80" align="center">
          <template #default="{ row }">
            <div v-if="(row.imageCount ?? 0) > 0" class="image-badge" @click="handleDetail(row)" title="点击查看图片">
              <el-icon style="vertical-align:-2px"><Picture /></el-icon>
              <span style="margin-left:4px">{{ row.imageCount }}</span>
            </div>
            <span v-else style="color:var(--text-muted, #aaa); font-size:12px">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="reply" label="回复" width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{row}">
            <el-tag :type="row.status === 'REPLIED' ? 'success' : 'info'" size="small">
              {{ row.status === 'REPLIED' ? '已回复' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="150" />
        <el-table-column label="操作" width="180">
          <template #default="{row}">
            <el-button type="primary" link @click="handleDetail(row)">查看</el-button>
            <el-button type="primary" link @click="handleReply(row)">回复</el-button>
            <el-button v-if="row.status !== 'REPLIED'" type="success" link @click="handleUpdateStatus(row)">完成</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper"><el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" /></div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="反馈详情" width="700px" class="feedback-detail-dialog">
      <div v-if="currentFeedback" class="detail-wrap">
        <div class="detail-row"><strong>用户：</strong>{{ currentFeedback.username }}</div>
        <div class="detail-row"><strong>邮箱：</strong>{{ currentFeedback.email }}</div>
        <div class="detail-row"><strong>标题：</strong>{{ currentFeedback.title }}</div>
        <div class="detail-row"><strong>类型：</strong>
          <el-tag size="small">{{ currentFeedback.type === 'visualization' ? '可视化' : currentFeedback.type === 'oj' ? 'OJ' : currentFeedback.type === 'ai' ? 'AI' : '其他' }}</el-tag>
        </div>
        <div class="detail-row"><strong>状态：</strong>
          <el-tag :type="currentFeedback.status === 'REPLIED' ? 'success' : 'info'" size="small">
            {{ currentFeedback.status === 'REPLIED' ? '已回复' : '待处理' }}
          </el-tag>
        </div>
        <div class="detail-row"><strong>时间：</strong>{{ currentFeedback.createTime }}</div>

        <div class="detail-section">
          <div class="detail-section-title">📝 反馈内容</div>
          <div class="detail-content-block">{{ currentFeedback.content }}</div>
        </div>

        <div v-if="detailImageUrls.length > 0" class="detail-section">
          <div class="detail-section-title">🖼️ 反馈图片（{{ detailImageUrls.length }}）</div>
          <div class="detail-images">
            <el-image
              v-for="(u, i) in detailImageUrls"
              :key="i"
              :src="u"
              :preview-src-list="detailImageUrls"
              :initial-index="i"
              fit="cover"
              class="detail-thumb"
              preview-teleported
            />
          </div>
        </div>

        <div v-if="currentFeedback.reply" class="detail-section">
          <div class="detail-section-title">💬 管理员回复</div>
          <div class="detail-reply-block">{{ currentFeedback.reply }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible=false">关闭</el-button>
        <el-button type="primary" @click="detailVisible=false; handleReply(currentFeedback!)" v-if="currentFeedback && currentFeedback.status !== 'REPLIED'">去回复</el-button>
      </template>
    </el-dialog>

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyVisible" title="回复反馈" width="500px">
      <div class="feedback-content">
        <div><strong>用户：</strong>{{ currentFeedback?.username }}</div>
        <div><strong>标题：</strong>{{ currentFeedback?.title }}</div>
        <div><strong>反馈内容：</strong></div>
        <div style="margin-top:6px; padding:8px; background:#fff; border:1px solid #ebeef5; border-radius:4px; max-height:160px; overflow:auto;">
          {{ currentFeedback?.content }}
        </div>
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
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.feedback-content { padding: 12px; background: #f5f7fa; border-radius: 4px; }

/* ========== 反馈内容单元格：填满宽度，固定高度，溢出截断 ========== */
.content-cell {
  cursor: pointer;
  transition: background .15s;
  border-radius: 4px;
  width: 100%;
}
.content-cell:hover {
  background: var(--bg-card-hover, #f0f7ff);
}
.content-cell-inner {
  height: 100%;
  line-height: 1.5;
  font-size: 13px;
  color: var(--text-secondary, #606266);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  line-clamp: 4;
  word-break: break-word;
  padding: 4px 6px;
  width: 100%;
  box-sizing: border-box;
}

.image-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  transition: background .15s;
}
.image-badge:hover { background: #d9ecff; }

/* ========== 详情弹窗 ========== */
.detail-wrap { font-size: 14px; line-height: 1.7; color: #303133; }
.detail-row { margin-bottom: 8px; display: flex; align-items: center; gap: 6px; }

.detail-section { margin-top: 16px; }
.detail-section-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 10px;
}
.detail-content-block {
  background: #fafafa;
  padding: 12px 14px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  white-space: pre-wrap;
  word-break: break-word;
  color: #303133;
  max-height: 300px;
  overflow: auto;
}
.detail-reply-block {
  background: #f0f9eb;
  padding: 12px 14px;
  border-radius: 6px;
  border: 1px solid #e1f3d8;
  color: #67c23a;
  white-space: pre-wrap;
  word-break: break-word;
}
.detail-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
}
.detail-thumb {
  width: 100%;
  height: 140px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  cursor: pointer;
  background: #f5f7fa;
}
</style>
