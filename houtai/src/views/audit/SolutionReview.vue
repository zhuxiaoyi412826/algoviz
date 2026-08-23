<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ElButton, ElTag, ElTable, ElTableColumn,
  ElPagination, ElInput, ElSelect, ElOption, ElTabs, ElTabPane,
  ElDialog, ElEmpty
} from 'element-plus'
import { Refresh, Check, Close, View, Edit } from '@element-plus/icons-vue'
import request from '@/api/request'

const solLoading = ref(false)
const cmtLoading = ref(false)
const activeTab = ref('solutions')

// 题解
const solList = ref<any[]>([])
const solTotal = ref(0)
const solQuery = reactive({ keyword: '', auditStatus: '', page: 1, pageSize: 20 })

// 评论
const cmtList = ref<any[]>([])
const cmtTotal = ref(0)
const cmtQuery = reactive({ keyword: '', auditStatus: '', page: 1, pageSize: 20 })

// 详情弹窗
const detailVisible = ref(false)
const detailData = ref<any>({})

const loadSolutions = async () => {
  solLoading.value = true
  try {
    const r: any = await request.get('/admin/solutions', { params: solQuery })
    solList.value = r?.list || []
    solTotal.value = r?.total || 0
  } catch (e: any) {
    solList.value = []
    solTotal.value = 0
    ElMessage.error('加载题解列表失败：' + (e?.message || '网络错误'))
  } finally { solLoading.value = false }
}

const loadComments = async () => {
  cmtLoading.value = true
  try {
    const r: any = await request.get('/admin/solutions/comments', { params: cmtQuery })
    cmtList.value = r?.list || []
    cmtTotal.value = r?.total || 0
  } catch (e: any) {
    cmtList.value = []
    cmtTotal.value = 0
    ElMessage.error('加载评论列表失败：' + (e?.message || '网络错误'))
  } finally { cmtLoading.value = false }
}

const auditStatusTag = (s: string) => {
  if (s === 'blocked') return 'danger'
  if (s === 'pending') return 'warning'
  if (s === 'passed') return 'success'
  if (s === 'rejected') return 'info'
  if (s === 'none') return ''
  if (s === 'logonly') return ''
  return ''
}

const auditStatusLabel = (s: string) => {
  const map: Record<string, string> = {
    blocked: '已拦截',
    pending: '待审核',
    passed: '已通过',
    rejected: '已驳回',
    none: '无风险',
    logonly: '仅记录'
  }
  return map[s] || s || '-'
}

const passSolution = async (id: number) => {
  await request.put(`/admin/solutions/${id}/pass`)
  ElMessage.success('已通过')
  loadSolutions()
}

const rejectSolution = async (id: number) => {
  await ElMessageBox.confirm('确定驳回该题解？', '提示', { type: 'warning' })
  await request.put(`/admin/solutions/${id}/reject`)
  ElMessage.success('已驳回')
  loadSolutions()
}

const passComment = async (id: number) => {
  await request.put(`/admin/solutions/comments/${id}/pass`)
  ElMessage.success('已通过')
  loadComments()
}

const rejectComment = async (id: number) => {
  await ElMessageBox.confirm('确定驳回该评论？', '提示', { type: 'warning' })
  await request.put(`/admin/solutions/comments/${id}/reject`)
  ElMessage.success('已驳回')
  loadComments()
}

const showDetail = (row: any) => {
  detailData.value = row
  detailVisible.value = true
}

const handleSolPageChange = (page: number) => {
  solQuery.page = page
  loadSolutions()
}

const handleCmtPageChange = (page: number) => {
  cmtQuery.page = page
  loadComments()
}

const handleSolSearch = () => {
  solQuery.page = 1
  loadSolutions()
}

const handleCmtSearch = () => {
  cmtQuery.page = 1
  loadComments()
}

onMounted(() => {
  loadSolutions()
  window.addEventListener('message', handleEditorMessage)
})

onBeforeUnmount(() => {
  window.removeEventListener('message', handleEditorMessage)
})

function handleEditorMessage(ev: MessageEvent) {
  if (ev.origin !== window.location.origin) return
  const payload = ev.data || {}
  if (payload.__from !== 'solution-editor') return
  if (payload.type === 'saved') {
    ElMessage.success(payload.message || '已保存，列表已刷新')
    loadSolutions()
  }
}

function openEdit(row: any) {
  const base = (import.meta as any).env?.BASE_URL || '/'
  const path = `${base.replace(/\/$/, '')}/solution/edit/${row.id}`
  window.open(path, '_blank')
}
</script>

<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" @tab-change="(t: string) => t === 'comments' && loadComments()">
      <el-tab-pane label="题解审核" name="solutions">
        <div style="display: flex; gap: 10px; margin-bottom: 14px;">
          <el-input v-model="solQuery.keyword" placeholder="搜索标题/用户" clearable style="width: 200px"
                    @clear="handleSolSearch" @keyup.enter="handleSolSearch" />
          <el-select v-model="solQuery.auditStatus" placeholder="审核状态" clearable style="width: 140px"
                     @change="handleSolSearch">
            <el-option label="全部" value="" />
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="passed" />
            <el-option label="已驳回" value="rejected" />
            <el-option label="已拦截" value="blocked" />
            <el-option label="无风险" value="none" />
            <el-option label="仅记录" value="logonly" />
          </el-select>
          <el-button :icon="Refresh" @click="loadSolutions">刷新</el-button>
        </div>
        <el-table :data="solList" v-loading="solLoading" border style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="problemTitle" label="题目" min-width="150" show-overflow-tooltip />
          <el-table-column prop="username" label="作者" width="120" />
          <el-table-column prop="format" label="格式" width="100" />
          <el-table-column label="审核状态" width="100">
            <template #default="{ row }">
              <el-tag :type="auditStatusTag(row.auditStatus)">{{ auditStatusLabel(row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="riskLevel" label="风险" width="80" />
          <el-table-column prop="likeCount" label="👍" width="60" />
          <el-table-column prop="viewCount" label="👁" width="60" />
          <el-table-column prop="commentCount" label="💬" width="60" />
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :icon="View" @click="showDetail(row)">查看</el-button>
              <el-button size="small" type="primary" :icon="Edit" @click="openEdit(row)">编辑题解</el-button>
              <el-button size="small" type="success" :icon="Check" @click="passSolution(row.id)">通过</el-button>
              <el-button size="small" type="danger" :icon="Close" @click="rejectSolution(row.id)">驳回</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无题解数据，请先在用户端发布题解" />
          </template>
        </el-table>
        <div style="margin-top: 14px; display: flex; justify-content: center;">
          <el-pagination background layout="total, prev, pager, next"
                         :total="solTotal" v-model:current-page="solQuery.page"
                         @current-change="handleSolPageChange" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="评论审核" name="comments">
        <div style="display: flex; gap: 10px; margin-bottom: 14px;">
          <el-input v-model="cmtQuery.keyword" placeholder="搜索内容/用户" clearable style="width: 200px"
                    @clear="handleCmtSearch" @keyup.enter="handleCmtSearch" />
          <el-select v-model="cmtQuery.auditStatus" placeholder="审核状态" clearable style="width: 140px"
                     @change="handleCmtSearch">
            <el-option label="全部" value="" />
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="passed" />
            <el-option label="已驳回" value="rejected" />
            <el-option label="已拦截" value="blocked" />
            <el-option label="无风险" value="none" />
            <el-option label="仅记录" value="logonly" />
          </el-select>
          <el-button :icon="Refresh" @click="loadComments">刷新</el-button>
        </div>
        <el-table :data="cmtList" v-loading="cmtLoading" border style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="username" label="评论人" width="120" />
          <el-table-column prop="solutionId" label="题解ID" width="80" />
          <el-table-column label="审核状态" width="100">
            <template #default="{ row }">
              <el-tag :type="auditStatusTag(row.auditStatus)">{{ auditStatusLabel(row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="riskLevel" label="风险" width="80" />
          <el-table-column prop="likeCount" label="👍" width="60" />
          <el-table-column prop="createdAt" label="时间" width="160" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" :icon="Check" @click="passComment(row.id)">通过</el-button>
              <el-button size="small" type="danger" :icon="Close" @click="rejectComment(row.id)">驳回</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无评论数据" />
          </template>
        </el-table>
        <div style="margin-top: 14px; display: flex; justify-content: center;">
          <el-pagination background layout="total, prev, pager, next"
                         :total="cmtTotal" v-model:current-page="cmtQuery.page"
                         @current-change="handleCmtPageChange" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="detailVisible" title="题解详情" width="70%">
      <div v-if="detailData.title">
        <h3>{{ detailData.title }}</h3>
        <p style="color: #909399; font-size: 13px;">作者：{{ detailData.username }} | 格式：{{ detailData.format }} | 复杂度：{{ detailData.complexity }}</p>
        <div v-if="detailData.idea"><h4>思路</h4><div style="white-space: pre-wrap;">{{ detailData.idea }}</div></div>
        <div v-if="detailData.process"><h4>解题过程</h4><div style="white-space: pre-wrap;">{{ detailData.process }}</div></div>
        <div v-if="detailData.code"><h4>代码 ({{ detailData.codeLang }})</h4><pre style="background: #f5f5f5; padding: 12px; border-radius: 4px; overflow-x: auto;">{{ detailData.code }}</pre></div>
        <div v-if="detailData.detectSummary"><h4>检测摘要</h4><div style="color: #f56c6c;">{{ detailData.detectSummary }}</div></div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 4px; }
</style>
