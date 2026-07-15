<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem,
  ElInput, ElSelect, ElOption, ElMessage, ElMessageBox, ElPagination
} from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import type { User } from '@/types'

const tableData = ref<User[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增管理员')

const searchForm = reactive({
  username: '',
  role: '',
  status: ''
})

const formData = reactive({
  id: '',
  username: '',
  nickname: '',
  email: '',
  phone: '',
  role: 'content_admin' as 'super_admin' | 'content_admin' | 'operation_admin',
  password: ''
})

const formRef = ref()
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const roleOptions = [
  { label: '超级管理员', value: 'super_admin' },
  { label: '内容管理员', value: 'content_admin' },
  { label: '运维管理员', value: 'operation_admin' }
]

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '禁用', value: 'disabled' }
]

const page = ref(1)
const pageSize = ref(10)

onMounted(() => {
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    // 模拟数据
    await new Promise(resolve => setTimeout(resolve, 500))
    tableData.value = [
      { id: '1', username: 'admin', nickname: '超级管理员', email: 'admin@example.com', role: 'super_admin', status: 'active', createTime: '2024-01-01', updateTime: '2024-01-01' },
      { id: '2', username: 'content_admin', nickname: '内容管理员A', email: 'content@example.com', role: 'content_admin', status: 'active', createTime: '2024-01-15', updateTime: '2024-01-15' },
      { id: '3', username: 'op_admin', nickname: '运维管理员', email: 'ops@example.com', role: 'operation_admin', status: 'active', createTime: '2024-02-01', updateTime: '2024-02-01' },
      { id: '4', username: 'content_user', nickname: '内容管理员B', email: 'user@example.com', role: 'content_admin', status: 'disabled', createTime: '2024-03-01', updateTime: '2024-03-01' }
    ]
    total.value = 4
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.role = ''
  searchForm.status = ''
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增管理员'
  Object.keys(formData).forEach(key => {
    (formData as any)[key] = key === 'role' ? 'content_admin' : ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: User) => {
  dialogTitle.value = '编辑管理员'
  Object.assign(formData, row)
  formData.password = ''
  dialogVisible.value = true
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除该管理员吗？', '提示', {
      type: 'warning'
    })
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // 取消
  }
}

const handleResetPassword = async (row: User) => {
  try {
    await ElMessageBox.confirm(`确定要重置 ${row.nickname} 的密码吗？`, '提示', {
      type: 'warning'
    })
    ElMessage.success('密码已重置为: admin123')
  } catch {
    // 取消
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    ElMessage.success(dialogTitle.value + '成功')
    dialogVisible.value = false
    loadData()
  })
}

const getRoleTagType = (role: string) => {
  const map: Record<string, any> = {
    super_admin: 'danger',
    content_admin: 'success',
    operation_admin: 'warning'
  }
  return map[role] || 'info'
}

const getRoleLabel = (role: string) => {
  const map: Record<string, string> = {
    super_admin: '超级管理员',
    content_admin: '内容管理员',
    operation_admin: '运维管理员'
  }
  return map[role] || role
}

const getStatusType = (status: string) => {
  return status === 'active' ? 'success' : 'danger'
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>管理员账户管理</h2>
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增管理员</el-button>
    </div>

    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.username" placeholder="用户名/昵称" clearable style="width: 200px" />
        <el-select v-model="searchForm.role" placeholder="角色" clearable style="width: 140px">
          <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.role)" size="small">
              {{ getRoleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.status === 'active' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" min-width="160">
          <template #default="{ row }">
            {{ row.lastLoginTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handleResetPassword(row)">重置密码</el-button>
            <el-button type="danger" link @click="handleDelete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @closed="formRef?.resetFields()">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" :disabled="!!formData.id" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="formData.nickname" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="formData.role" style="width: 100%">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="formData.password" type="password" show-password :placeholder="formData.id ? '留空则不修改' : ''" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
