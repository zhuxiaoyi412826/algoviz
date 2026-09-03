<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElAvatar, ElMessage, ElPagination, ElInput, ElSelect, ElOption, ElMessageBox, ElRadio, ElRadioGroup } from 'element-plus'
import { Search, Refresh, Edit, Delete, Download } from '@element-plus/icons-vue'
import request from '@/api/request'

interface User {
  id: number
  username: string
  email: string
  age: number | null
  gender: number | null
  nickname: string
  avatarUrl: string
  loginStatus: number
  status: number
  createdAt: string
  updatedAt: string
  lastLoginAt: string
}

const tableData = ref<User[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const currentUser = ref<User | null>(null)
const editVisible = ref(false)
const editUser = ref<User | null>(null)
const editStatus = ref<number>(1)

const scrollTopRef = ref<HTMLElement | null>(null)
const tableWrapperRef = ref<HTMLElement | null>(null)
let syncing = false

const syncScroll = (source: 'top' | 'bottom') => {
  if (syncing) return
  syncing = true
  if (source === 'top' && scrollTopRef.value && tableWrapperRef.value) {
    tableWrapperRef.value.scrollLeft = scrollTopRef.value.scrollLeft
  } else if (source === 'bottom' && scrollTopRef.value && tableWrapperRef.value) {
    scrollTopRef.value.scrollLeft = tableWrapperRef.value.scrollLeft
  }
  syncing = false
}

const searchForm = reactive({ keyword: '', gender: '' as string | number, status: '' as string | number, loginStatus: '' as string | number, order: 'desc' })
const page = ref(1); const pageSize = ref(100); const total = ref(0)

onMounted(() => loadData())

onUnmounted(() => {
  syncing = true
})

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.gender !== '' && searchForm.gender != null) params.gender = searchForm.gender
    if (searchForm.status !== '' && searchForm.status != null) params.status = searchForm.status
    if (searchForm.loginStatus !== '' && searchForm.loginStatus != null) params.loginStatus = searchForm.loginStatus
    if (searchForm.order) params.order = searchForm.order
    params.page = String(page.value)
    params.pageSize = String(pageSize.value)

    const data: any = await request.get('/users', { params })

    if (data.success) {
      tableData.value = data.users.map((item: any) => ({
        id: item.id,
        username: item.username,
        email: item.email,
        age: item.age,
        gender: item.gender == null ? null : item.gender,
        nickname: item.nickname || item.username,
        avatarUrl: item.avatarUrl || '',
        loginStatus: item.loginStatus ?? 1,
        status: item.status != null ? item.status : 1,
        createdAt: item.createdAt,
        updatedAt: item.updatedAt,
        lastLoginAt: item.lastLoginAt
      }))
      total.value = data.count
    } else {
      ElMessage.error('加载用户列表失败')
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
  } finally { loading.value = false }
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

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.gender = ''
  searchForm.status = ''
  searchForm.loginStatus = ''
  searchForm.order = 'desc'
  loadData()
}

const handleView = (row: User) => { 
  currentUser.value = row
  detailVisible.value = true 
}

// 性别：1=男 0=女 null=未知
const genderText = (g: number | null | undefined) => (g === 1 ? '男' : g === 0 ? '女' : '未知')
// 账号状态：1=正常 0=封禁 -1=注销
const statusText = (s: number) => (s === 1 ? '正常' : s === 0 ? '封禁' : '注销')
const statusTagType = (s: number): 'success' | 'danger' | 'info' => (s === 1 ? 'success' : s === 0 ? 'danger' : 'info')

const handleEdit = (row: User) => {
  editUser.value = row
  editStatus.value = row.status
  editVisible.value = true
}

const handleSaveEdit = async () => {
  if (!editUser.value) return
  try {
    const data: any = await request.put(`/users/${editUser.value.id}/status`, { status: editStatus.value })
    if (data.success) {
      ElMessage.success('用户状态更新成功')
      editVisible.value = false
      loadData()
    } else {
      ElMessage.error(data.message || '更新失败')
    }
  } catch (error) {
    console.error('更新失败:', error)
    ElMessage.error('更新失败')
  }
}

const handleDelete = async (row: User) => {
  try {
    await ElMessageBox.confirm('删除后该用户数据将保留但不可见、不可登录，且用户名/邮箱永久不可再注册。确定删除吗？', '逻辑删除确认', { type: 'warning' })

    const data: any = await request.delete(`/users/${row.id}`)

    if (data.success) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch {}
}

const handleExport = async () => {
  try {
    const response = await fetch('/api/users/export/json', {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('adminToken') || ''}` }
    })
    if (!response.ok) {
      throw new Error('导出失败')
    }
    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'users.json'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('用户数据导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败: ' + (error as Error).message)
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>用户列表</h2></div>
    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.keyword" placeholder="用户名/邮箱" clearable style="width:180px" @keyup.enter="handleSearch" />
        <el-select v-model="searchForm.gender" placeholder="性别" clearable style="width:120px">
          <el-option label="男" :value="1" />
          <el-option label="女" :value="0" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="账号状态" clearable style="width:130px">
          <el-option label="正常" :value="1" />
          <el-option label="封禁" :value="0" />
          <el-option label="注销" :value="-1" />
        </el-select>
        <el-select v-model="searchForm.loginStatus" placeholder="登录状态" clearable style="width:130px">
          <el-option label="在线" :value="0" />
          <el-option label="离线" :value="1" />
        </el-select>
        <el-select v-model="searchForm.order" placeholder="注册时间" style="width:140px">
          <el-option label="注册时间倒序" value="desc" />
          <el-option label="注册时间正序" value="asc" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        <el-button type="success" :icon="Download" @click="handleExport" style="margin-left:auto">导出JSON</el-button>
      </div>
      <div class="scroll-top" ref="scrollTopRef" @scroll="syncScroll('top')">
        <div class="scroll-top-inner" style="min-width:1200px; height:1px"></div>
      </div>
      <div class="table-wrapper" ref="tableWrapperRef" @scroll="syncScroll('bottom')">
        <el-table :data="tableData" v-loading="loading" stripe style="min-width:1200px">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="用户" min-width="200">
          <template #default="{row}">
            <div style="display:flex;align-items:center;gap:10px">
              <el-avatar :size="36" :src="row.avatarUrl" icon="User" />
              <div>
                <div>{{ row.nickname }}</div>
                <div style="font-size:12px;color:#999">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
        <el-table-column prop="age" label="年龄" width="70">
          <template #default="{row}">{{ row.age != null ? row.age : '-' }}</template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{row}">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="登录状态" width="90">
          <template #default="{row}">
            <el-tag :type="row.loginStatus === 0 ? 'success' : 'info'" size="small">
              {{ row.loginStatus === 0 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="90">
          <template #default="{row}">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="150" />
        <el-table-column prop="lastLoginAt" label="最后登录" width="150" />
        <el-table-column label="操作" width="180">
          <template #default="{row}">
            <el-button type="primary" link @click="handleView(row)">详情</el-button>
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>
      <div class="pagination-wrapper">
        <el-pagination 
          v-model:current-page="page" 
          v-model:page-size="pageSize" 
          :total="total" 
          :page-sizes="[50, 100, 200, 500]"
          layout="total,sizes,prev,pager,next,jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
    <el-dialog v-model="detailVisible" title="用户详情" width="600px">
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="ID">{{ currentUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ genderText(currentUser.gender) }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ currentUser.age != null ? currentUser.age : '-' }}</el-descriptions-item>
        <el-descriptions-item label="头像">
          <el-avatar :size="40" :src="currentUser.avatarUrl" icon="User" />
        </el-descriptions-item>
        <el-descriptions-item label="登录状态">
          <el-tag :type="currentUser.loginStatus === 0 ? 'success' : 'info'" size="small">
            {{ currentUser.loginStatus === 0 ? '在线' : '离线' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="statusTagType(currentUser.status)" size="small">
            {{ statusText(currentUser.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentUser.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentUser.updatedAt }}</el-descriptions-item>
        <el-descriptions-item label="最后登录" :span="2">{{ currentUser.lastLoginAt || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
    <el-dialog v-model="editVisible" title="编辑用户" width="450px" :close-on-click-modal="false">
      <div v-if="editUser" style="padding:10px 0">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ID">{{ editUser.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ editUser.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ editUser.nickname }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ editUser.email }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderText(editUser.gender) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="editUser.status === -1" style="margin-top:14px;color:#e6a23c;font-size:13px">
          该账号已由用户本人注销，状态不可变更
        </div>
        <div v-else style="margin-top:18px">
          <div style="font-weight:600;margin-bottom:8px">账号状态 <span style="color:#f56c6c">*</span></div>
          <el-radio-group v-model="editStatus">
            <el-radio :value="1"><el-tag type="success" size="small">正常</el-tag></el-radio>
            <el-radio :value="0"><el-tag type="danger" size="small">封禁</el-tag></el-radio>
          </el-radio-group>
        </div>
      </div>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :disabled="editUser?.status === -1" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pagination-wrapper{display:flex;justify-content:flex-end;margin-top:16px}
.scroll-top{overflow-x:auto;overflow-y:hidden;margin-bottom:-1px}
.scroll-top::-webkit-scrollbar{height:8px}
.scroll-top::-webkit-scrollbar-track{background:#f1f1f1}
.scroll-top::-webkit-scrollbar-thumb{background:#c1c1c1;border-radius:4px}
.scroll-top::-webkit-scrollbar-thumb:hover{background:#a8a8a8}
.table-wrapper{overflow-x:auto;margin-top:0}
.table-wrapper::-webkit-scrollbar{height:8px}
.table-wrapper::-webkit-scrollbar-track{background:#f1f1f1}
.table-wrapper::-webkit-scrollbar-thumb{background:#c1c1c1;border-radius:4px}
.table-wrapper::-webkit-scrollbar-thumb:hover{background:#a8a8a8}
</style>
