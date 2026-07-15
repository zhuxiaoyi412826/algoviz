<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElAvatar, ElMessage, ElPagination, ElInput, ElSelect, ElOption, ElMessageBox } from 'element-plus'
import { Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import type { AppUser } from '@/types'

interface User {
  id: number
  username: string
  email: string
  avatar: string
  gender: string
  nickname: string
  createdAt: string
  updatedAt: string
  lastLoginAt: string
}

const tableData = ref<User[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const currentUser = ref<User | null>(null)

const searchForm = reactive({ keyword: '', status: '' })
const page = ref(1); const pageSize = ref(10); const total = ref(0)

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (searchForm.keyword) params.append('keyword', searchForm.keyword)
    
    const response = await fetch(`http://localhost/api/users?${params.toString()}`)
    const data = await response.json()
    
    if (data.success) {
      tableData.value = data.users.map((item: any) => ({
        id: item.id,
        username: item.username,
        email: item.email,
        avatar: item.avatar || '',
        gender: item.gender || '未知',
        nickname: item.nickname || item.username,
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

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  loadData()
}

const handleView = (row: User) => { 
  currentUser.value = row
  detailVisible.value = true 
}

const handleEdit = (row: User) => {
  currentUser.value = row
  detailVisible.value = true
}

const handleDelete = async (row: User) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', { type: 'warning' })
    
    const response = await fetch(`http://localhost/api/users/${row.id}`, {
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
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>用户列表</h2></div>
    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.keyword" placeholder="用户名/邮箱" clearable style="width:180px" @keyup.enter="handleSearch" />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe style="margin-top:16px">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="用户" min-width="200">
          <template #default="{row}">
            <div style="display:flex;align-items:center;gap:10px">
              <el-avatar :size="36" :src="row.avatar" icon="User" />
              <div>
                <div>{{ row.nickname }}</div>
                <div style="font-size:12px;color:#999">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
        <el-table-column prop="gender" label="性别" width="70" />
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
      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total,sizes,prev,pager,next" />
      </div>
    </div>
    <el-dialog v-model="detailVisible" title="用户详情" width="600px">
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="ID">{{ currentUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ currentUser.gender }}</el-descriptions-item>
        <el-descriptions-item label="头像">
          <el-avatar :size="40" :src="currentUser.avatar" icon="User" />
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentUser.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentUser.updatedAt }}</el-descriptions-item>
        <el-descriptions-item label="最后登录" :span="2">{{ currentUser.lastLoginAt || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>.pagination-wrapper{display:flex;justify-content:flex-end;margin-top:16px}</style>
