<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { changelogApi } from '@/api/system'
import type { Changelog } from '@/types'

const TYPE_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: 'new', label: '新增' },
  { value: 'optimize', label: '优化' },
  { value: 'fix', label: '修复' },
  { value: 'urgent', label: '紧急更新' }
]
const TYPE_LABEL: Record<string, string> = Object.fromEntries(
  TYPE_OPTIONS.map(o => [o.value, o.label])
)
const TYPE_TAG_TYPE: Record<string, "success" | "primary" | "warning" | "info" | "danger" | undefined> = {
  new: 'success', optimize: 'primary', fix: 'warning', urgent: 'danger'
}

const tableData = ref<Changelog[]>([])
const loading = ref(false)
const searchForm = reactive({
  type: 'all',
  status: '' as '' | 0 | 1,
  version: ''
})
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(() => {
  loadData()
  window.addEventListener('message', handleEditorMessage)
})

onUnmounted(() => {
  window.removeEventListener('message', handleEditorMessage)
})

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize: pageSize.value }
    if (searchForm.type && searchForm.type !== 'all') params.type = searchForm.type
    if (searchForm.status !== '') params.status = searchForm.status
    if (searchForm.version) params.version = searchForm.version
    // request 拦截器会把 Promise<AxiosResponse<{list,total}>> 解包成 {list,total}
    const res = (await changelogApi.getPage(params)) as unknown as { list: Changelog[]; total: number }
    tableData.value = res?.list || []
    total.value = res?.total ?? 0
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function handleReset() {
  searchForm.type = 'all'
  searchForm.status = ''
  searchForm.version = ''
  page.value = 1
  loadData()
}

/**
 * 新增 / 编辑：跳转到新浏览器 tab（新网页，不是 popup 弹窗）
 * 保存/取消后子 tab 会自动 close 并通过 postMessage 通知父列表自动刷新
 */
function openCreate() {
  const base = (import.meta as any).env?.BASE_URL || '/'
  const path = `${base.replace(/\/$/, '')}/changelog/new`
  // 只传 target='_blank'，浏览器用用户默认设置（新 tab 或新窗口）打开，不要 width/height/popup 特性
  window.open(path, '_blank')
}

function openEdit(id: number) {
  const base = (import.meta as any).env?.BASE_URL || '/'
  const path = `${base.replace(/\/$/, '')}/changelog/edit/${id}`
  window.open(path, '_blank')
}

function handleEditorMessage(ev: MessageEvent) {
  // 同源校验，避免跨域消息
  if (ev.origin !== window.location.origin) return
  const payload = ev.data || {}
  if (payload.__from !== 'changelog-editor') return
  if (payload.type === 'saved') {
    ElMessage.success(payload.message || '保存成功')
    loadData()
  } else if (payload.type === 'canceled') {
    // 取消不强制刷新，但保持默认行为轻量提示即可
  }
}

async function handleDelete(id: number, version: string) {
  try {
    await ElMessageBox.confirm(
      `确定删除版本 ${version} 的更新日志吗？删除后无法恢复。`,
      '删除确认',
      { type: 'warning' }
    )
    await changelogApi.remove(id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* 用户取消 */ }
}

function parseModules(modules: string): string[] {
  try {
    const arr = JSON.parse(modules || '[]')
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>客服工单 · 更新日志</h2>
      <div>
        <el-button :icon="Refresh" @click="loadData" :loading="loading">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增更新日志（新窗口）</el-button>
      </div>
    </div>

    <div class="card-container">
      <div class="filter-bar">
        <el-select v-model="searchForm.type" placeholder="更新类型" style="width:160px">
          <el-option
            v-for="o in TYPE_OPTIONS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>

        <el-select v-model="searchForm.status" placeholder="发布状态" clearable style="width:140px">
          <el-option label="已发布" :value="1" />
          <el-option label="草稿" :value="0" />
        </el-select>

        <el-input
          v-model="searchForm.version"
          placeholder="按版本号搜索"
          clearable
          style="width:200px"
          @keyup.enter="handleSearch"
        />

        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe style="width:100%">
        <el-table-column label="ID" prop="id" width="80" align="center" />
        <el-table-column label="版本号" width="140" align="center">
          <template #default="{ row }">
            <span style="font-weight:600;color:var(--el-color-primary)">{{ row.version }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="TYPE_TAG_TYPE[row.type] ?? 'info'">
              {{ TYPE_LABEL[row.type] ?? row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新摘要" min-width="240" show-overflow-tooltip prop="summary" />
        <el-table-column label="模块标签" width="200">
          <template #default="{ row }">
            <el-tag
              v-for="m in parseModules(row.modules)"
              :key="m"
              type="info"
              effect="plain"
              style="margin-right:4px;margin-bottom:4px;"
            >{{ m }}</el-tag>
            <span v-if="!parseModules(row.modules).length" style="color:#bbb">—</span>
          </template>
        </el-table-column>
        <el-table-column label="发布日期" width="120" align="center" prop="releaseDate" sortable />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" effect="light">已发布</el-tag>
            <el-tag v-else type="info" effect="light">草稿</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170" align="center" prop="updatedAt" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :icon="Edit" @click="openEdit(row.id)">编辑</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row.id, row.version)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          @size-change="page = 1; loadData()"
          @current-change="loadData()"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-header h2 { margin: 0; font-size: 20px; }
.card-container {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  box-shadow: var(--shadow-sm);
}
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
