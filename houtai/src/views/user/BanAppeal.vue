<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import request from '@/api/request'

/** 封禁申诉记录（对应后端 ban_appeal 表） */
interface BanAppeal {
  id: number
  userId: number | null
  username: string
  email: string
  reason: string
  status: string
  reply: string | null
  handlerId: number | null
  handlerName: string | null
  handleTime: string | null
  createTime: string
  updateTime: string
}

const tableData = ref<BanAppeal[]>([])
const loading = ref(false)
const searchForm = reactive({ status: '', keyword: '' })
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 详情弹窗
const detailVisible = ref(false)
const currentAppeal = ref<BanAppeal | null>(null)

// 处理弹窗
const handleVisible = ref(false)
const handleRow = ref<BanAppeal | null>(null)
const handleForm = reactive({ action: 'approve', reply: '' })
const handling = ref(false)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: String(page.value),
      pageSize: String(pageSize.value)
    }
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.keyword) params.keyword = searchForm.keyword

    const data: any = await request.get('/admin/ban-appeals', { params })
    tableData.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    console.error('加载封禁申诉列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.status = ''
  searchForm.keyword = ''
  page.value = 1
  loadData()
}

const handlePageChange = (currentPage: number) => {
  page.value = currentPage
  loadData()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  page.value = 1
  loadData()
}

// 状态：pending=待处理 approved=已通过（已解封） rejected=已驳回
const statusText = (s: string) => (s === 'approved' ? '已通过' : s === 'rejected' ? '已驳回' : '待处理')
const statusTagType = (s: string): 'success' | 'danger' | 'warning' =>
  s === 'approved' ? 'success' : s === 'rejected' ? 'danger' : 'warning'

const handleView = (row: BanAppeal) => {
  currentAppeal.value = row
  detailVisible.value = true
}

/** 从详情弹窗直接进入处理（关闭详情再开处理弹窗） */
const handleFromDetail = () => {
  const row = currentAppeal.value
  if (!row) return
  detailVisible.value = false
  openHandle(row)
}

const openHandle = (row: BanAppeal) => {
  handleRow.value = row
  handleForm.action = 'approve'
  handleForm.reply = ''
  handleVisible.value = true
}

const submitHandle = async () => {
  if (!handleRow.value) return

  if (handleForm.action === 'approve') {
    try {
      await ElMessageBox.confirm(
        `通过后将自动解除「${handleRow.value.username}」的封禁状态（账号可立即登录），并向其邮箱发送处理结果通知。确定通过吗？`,
        '通过申诉',
        { type: 'warning' }
      )
    } catch {
      return
    }
  }

  handling.value = true
  try {
    await request.put(`/admin/ban-appeals/${handleRow.value.id}`, {
      action: handleForm.action,
      reply: handleForm.reply
    })
    ElMessage.success(handleForm.action === 'approve' ? '已通过申诉并解封该账号' : '已驳回该申诉')
    handleVisible.value = false
    loadData()
  } catch (error) {
    console.error('处理申诉失败:', error)
  } finally {
    handling.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>封禁申诉</h2></div>
    <div class="card-container">
      <div class="filter-bar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="账号 / 邮箱"
          clearable
          style="width:200px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="searchForm.status" placeholder="处理状态" clearable style="width:140px">
          <el-option label="待处理" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="被封禁账号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="email" label="申诉邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="reason" label="申诉理由" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="处理人" width="110">
          <template #default="{row}">{{ row.handlerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="170">
          <template #default="{row}">{{ row.handleTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{row}">
            <el-button type="primary" link @click="handleView(row)">查看理由</el-button>
            <el-button v-if="row.status === 'pending'" type="primary" link @click="openHandle(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total,sizes,prev,pager,next,jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 申诉理由详情 -->
    <el-dialog v-model="detailVisible" title="申诉详情" width="620px">
      <el-descriptions v-if="currentAppeal" :column="2" border>
        <el-descriptions-item label="申诉ID">{{ currentAppeal.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentAppeal.userId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="被封禁账号">{{ currentAppeal.username }}</el-descriptions-item>
        <el-descriptions-item label="申诉邮箱">{{ currentAppeal.email }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(currentAppeal.status)" size="small">{{ statusText(currentAppeal.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentAppeal.createTime }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ currentAppeal.handlerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ currentAppeal.handleTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="currentAppeal" style="margin-top:16px;">
        <div style="font-weight:600; margin-bottom:8px;">申诉理由</div>
        <div class="reason-box">{{ currentAppeal.reason }}</div>
      </div>

      <div v-if="currentAppeal && currentAppeal.reply" style="margin-top:16px;">
        <div style="font-weight:600; margin-bottom:8px;">管理员回复</div>
        <div class="reason-box">{{ currentAppeal.reply }}</div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentAppeal && currentAppeal.status === 'pending'"
          type="primary"
          @click="handleFromDetail"
        >处理该申诉</el-button>
      </template>
    </el-dialog>

    <!-- 处理申诉 -->
    <el-dialog v-model="handleVisible" title="处理申诉" width="520px" :close-on-click-modal="false">
      <div v-if="handleRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="被封禁账号">{{ handleRow.username }}</el-descriptions-item>
          <el-descriptions-item label="申诉邮箱">{{ handleRow.email }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-top:16px;">
          <div style="font-weight:600; margin-bottom:8px;">申诉理由</div>
          <div class="reason-box">{{ handleRow.reason }}</div>
        </div>

        <div style="margin-top:18px;">
          <div style="font-weight:600; margin-bottom:8px;">处理结果 <span style="color:#f56c6c">*</span></div>
          <el-radio-group v-model="handleForm.action">
            <el-radio value="approve">通过（自动解封该账号）</el-radio>
            <el-radio value="reject">驳回</el-radio>
          </el-radio-group>
        </div>

        <div style="margin-top:18px;">
          <div style="font-weight:600; margin-bottom:8px;">回复说明（会邮件通知申诉人，选填）</div>
          <el-input
            v-model="handleForm.reply"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="可填写解封说明或驳回原因"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="handling" @click="submitHandle">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pagination-wrapper { display:flex; justify-content:flex-end; margin-top:16px; }
.reason-box {
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 220px;
  overflow-y: auto;
}
</style>