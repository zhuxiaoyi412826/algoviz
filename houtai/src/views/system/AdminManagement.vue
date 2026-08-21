<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem,
  ElInput, ElSelect, ElOption, ElMessage, ElMessageBox, ElPagination
} from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const userStore = useUserStore()

// ==================== 角色层级定义 ====================
// 一级管理员角色（role_level=2）
const LEVEL1_ROLES = [
  { role_code: 'LEVEL1_ADMIN', label: '一级管理员', category: 'level1' },
  { role_code: 'PERM_MANAGER', label: '权限分配管理员', category: 'level1' },
  { role_code: 'CONTENT_PUBLISHER', label: '内容发布管理员', category: 'level1' },
  { role_code: 'CONTENT_AUDITOR', label: '内容审核管理员', category: 'level1' },
  { role_code: 'EFK_LOG_ADMIN', label: 'EFK日志分析管理员', category: 'level1' },
  { role_code: 'DATA_ANALYST', label: '数据分析管理员', category: 'level1' },
  { role_code: 'ORDER_ADMIN', label: '订单分析管理员', category: 'level1' },
  { role_code: 'OPS_ADMIN', label: '运营活动管理员', category: 'level1' },
  { role_code: 'FINANCE_ADMIN', label: '财务管理员', category: 'level1' },
  { role_code: 'CS_ADMIN', label: '客服管理员', category: 'level1' },
  { role_code: 'SECURITY_AUDITOR', label: '安全审计管理员', category: 'level1' }
]

// 二级管理员角色（role_level=3）
const LEVEL2_ROLES = [
  { role_code: 'DS_PUBLISHER', label: '数据结构发布管理员', category: 'level2' },
  { role_code: 'ALGO_PUBLISHER', label: '算法发布管理员', category: 'level2' },
  { role_code: 'GENERAL_SUB_ADMIN', label: '通用子管理员', category: 'level2' },
  { role_code: 'TEST_VALIDATOR', label: '题目校验测试管理员', category: 'level2' },
  { role_code: 'KEYWORD_AUDITOR', label: '关键词审核管理员', category: 'level2' },
  { role_code: 'SOLUTION_COMMENT_AUDITOR', label: '题解评论审核管理员', category: 'level2' },
  { role_code: 'BIZ_LOG_ADMIN', label: '业务日志管理员', category: 'level2' },
  { role_code: 'DEV_LOG_ADMIN', label: '开发日志管理员', category: 'level2' },
  { role_code: 'SYS_LOG_ADMIN', label: '系统日志管理员', category: 'level2' },
  { role_code: 'OPS_LOG_ADMIN', label: '运维管理员', category: 'level2' },
  { role_code: 'UV_PV_ANALYST', label: 'UV/PV数据分析师', category: 'level2' },
  { role_code: 'RETENTION_ANALYST', label: '用户留存数据分析师', category: 'level2' },
  { role_code: 'ORDER_ANALYST_SUB', label: '订单数据分析师', category: 'level2' },
  { role_code: 'PRODUCT_ADMIN', label: '商品管理员', category: 'level2' },
  { role_code: 'COIN_ADMIN', label: '金币管理员', category: 'level2' },
  { role_code: 'EXTERNAL_AUTHOR', label: '外部出题人', category: 'level2' }
]

// 所有角色（用于列表展示）
const ALL_ROLES = [...LEVEL1_ROLES, ...LEVEL2_ROLES]

// ==================== 根据当前用户角色计算可选角色 ====================
const currentUserRoles = computed(() => userStore.roles)

// 判断当前用户是否可以新增管理员
const canAddAdmin = computed(() => {
  // 超级管理员或一级管理员可以新增
  return userStore.isSuperAdmin() || userStore.isLevel1Admin()
})

// 根据当前角色返回可选的角色选项
const roleOptionsForCreate = computed(() => {
  if (userStore.isSuperAdmin()) {
    // 超级管理员可以选择所有一级和二级角色
    return [...LEVEL1_ROLES, ...LEVEL2_ROLES]
  } else if (userStore.isLevel1Admin()) {
    // 一级管理员只能选择二级角色
    return [...LEVEL2_ROLES]
  } else {
    // 二级管理员无权限新增
    return []
  }
})

// ==================== 页面状态 ====================
const tableData = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增管理员')
const isEdit = ref(false)

const searchForm = reactive({
  username: '',
  role: '',
  status: '',
  roleCategory: '' as '' | 'level1' | 'level2'
})

const formData = reactive({
  id: '',
  username: '',
  nickname: '',
  email: '',
  phone: '',
  role: '' as string,
  password: ''
})

const formRef = ref()
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 筛选用的角色选项（全部）
const searchRoleOptions = computed(() => {
  return ALL_ROLES.map(r => ({ label: r.label, value: r.role_code }))
})

const statusOptions = [
  { label: '启用', value: 'active' },
  { label: '禁用', value: 'disabled' }
]

const page = ref(1)
const pageSize = ref(10)

onMounted(() => {
  loadData()
})

// ==================== 加载数据 ====================
const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/system/admin', {
      params: {
        page: page.value,
        pageSize: pageSize.value
      }
    })
    const payload = res?.data || res
    if (payload?.list) {
      // 给每条记录添加角色分类信息
      tableData.value = payload.list.map((item: any) => {
        const roleInfo = ALL_ROLES.find(r => r.role_code === item.roleCode || r.role_code === item.role)
        return {
          ...item,
          roleCode: item.roleCode || item.role || '',
          roleCategory: roleInfo?.category || 'unknown',
          roleLabel: roleInfo?.label || item.role || '未知角色'
        }
      })
      total.value = payload.total || 0
    } else {
      // 回退：模拟数据
      loadMockData()
    }
  } catch (e) {
    // 接口未就绪时使用模拟数据
    loadMockData()
  } finally {
    loading.value = false
  }
}

const loadMockData = () => {
  const mockUsers = [
    { id: '1', username: 'algovize', nickname: '超级管理员', email: 'algovize@dsaol.asia', roleCode: 'SUPER_ADMIN', roleCategory: 'level1', roleLabel: '超级管理员', status: 'active', createTime: '2024-01-01', updateTime: '2024-01-01', lastLoginTime: '2024-08-20 10:30:00' },
    { id: '2', username: 'lvl1_admin', nickname: '一级管理员A', email: 'lvl1@example.com', roleCode: 'LEVEL1_ADMIN', roleCategory: 'level1', roleLabel: '一级管理员', status: 'active', createTime: '2024-01-15', updateTime: '2024-06-01', lastLoginTime: '2024-08-19 14:20:00' },
    { id: '3', username: 'ds_publisher', nickname: '数据结构发布员', email: 'ds@example.com', roleCode: 'DS_PUBLISHER', roleCategory: 'level2', roleLabel: '数据结构发布管理员', status: 'active', createTime: '2024-02-01', updateTime: '2024-07-15', lastLoginTime: '2024-08-18 09:15:00' },
    { id: '4', username: 'algo_publisher', nickname: '算法发布员', email: 'algo@example.com', roleCode: 'ALGO_PUBLISHER', roleCategory: 'level2', roleLabel: '算法发布管理员', status: 'active', createTime: '2024-03-01', updateTime: '2024-08-01', lastLoginTime: '2024-08-17 16:45:00' },
    { id: '5', username: 'content_auditor', nickname: '内容审核员', email: 'auditor@example.com', roleCode: 'CONTENT_AUDITOR', roleCategory: 'level1', roleLabel: '内容审核管理员', status: 'disabled', createTime: '2024-04-01', updateTime: '2024-08-10', lastLoginTime: '-' },
    { id: '6', username: 'dev_log_admin', nickname: '开发日志管理员', email: 'log@example.com', roleCode: 'DEV_LOG_ADMIN', roleCategory: 'level2', roleLabel: '开发日志管理员', status: 'active', createTime: '2024-05-01', updateTime: '2024-08-15', lastLoginTime: '2024-08-20 08:00:00' }
  ]
  tableData.value = mockUsers
  total.value = mockUsers.length
}

// ==================== 搜索/重置 ====================
const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.role = ''
  searchForm.status = ''
  searchForm.roleCategory = ''
  loadData()
}

// ==================== 新增/编辑 ====================
const handleAdd = () => {
  if (!canAddAdmin.value) {
    ElMessage.warning('您没有新增管理员的权限')
    return
  }
  isEdit.value = false
  dialogTitle.value = '新增管理员'
  Object.keys(formData).forEach(key => {
    (formData as any)[key] = key === 'role' ? (roleOptionsForCreate.value[0]?.role_code || '') : ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑管理员'
  formData.id = row.id
  formData.username = row.username
  formData.nickname = row.nickname || row.realName || ''
  formData.email = row.email || ''
  formData.phone = row.phone || ''
  formData.role = row.roleCode || row.role || ''
  formData.password = ''
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除管理员「${row.nickname || row.username}」吗？`, '提示', {
      type: 'warning'
    })
    await request.delete(`/system/admin/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') {
      // 忽略取消操作
    }
  }
}

const handleResetPassword = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要重置「${row.nickname || row.username}」的密码吗？重置后密码为 admin123`, '提示', {
      type: 'warning'
    })
    await request.post(`/system/admin/${row.id}/reset-password`)
    ElMessage.success('密码已重置为: admin123')
  } catch (e: any) {
    if (e !== 'cancel') {
      // 忽略取消操作
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        // 编辑
        const payload: any = {
          nickname: formData.nickname,
          email: formData.email,
          phone: formData.phone,
          role: formData.role
        }
        if (formData.password) {
          payload.password = formData.password
        }
        await request.put(`/system/admin/${formData.id}`, payload)
        ElMessage.success('编辑成功')
      } else {
        // 新增
        await request.post('/system/admin', {
          username: formData.username,
          realName: formData.nickname,
          email: formData.email,
          phone: formData.phone,
          role: formData.role,
          password: formData.password,
          status: 1
        })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || '操作失败'
      ElMessage.error(msg)
    }
  })
}

// ==================== 辅助函数 ====================
const getRoleTagType = (roleCategory: string): 'success' | 'primary' | 'warning' | 'info' | 'danger' => {
  const map: Record<string, 'success' | 'primary' | 'warning' | 'info' | 'danger'> = {
    'level1': 'warning',    // 一级管理员：橙色
    'level2': 'info',       // 二级管理员：灰色
    'unknown': 'info'
  }
  return map[roleCategory] || 'info'
}

const getRoleCategoryTag = (roleCategory: string): { label: string; type: 'success' | 'primary' | 'warning' | 'info' | 'danger' } => {
  const map: Record<string, { label: string; type: 'success' | 'primary' | 'warning' | 'info' | 'danger' }> = {
    'level1': { label: '一级管理员', type: 'warning' },
    'level2': { label: '二级管理员', type: 'info' },
    'unknown': { label: '未知', type: 'danger' }
  }
  return map[roleCategory] || map.unknown
}

const getRoleLabel = (roleCode: string) => {
  const found = ALL_ROLES.find(r => r.role_code === roleCode)
  return found?.label || roleCode
}

const getStatusType = (status: string | number): 'success' | 'danger' => {
  if (status === 'active' || status === 1 || status === 'enabled') return 'success'
  return 'danger'
}

const getStatusLabel = (status: string | number) => {
  if (status === 'active' || status === 1 || status === 'enabled') return '启用'
  return '禁用'
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>管理员账户管理</h2>
      <el-button
        v-if="canAddAdmin"
        type="primary"
        :icon="Plus"
        @click="handleAdd"
      >新增管理员</el-button>
      <el-tooltip v-else content="您没有新增管理员的权限" placement="top">
        <el-button type="primary" :icon="Plus" disabled>新增管理员</el-button>
      </el-tooltip>
    </div>

    <div class="card-container">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input v-model="searchForm.username" placeholder="用户名/昵称" clearable style="width: 200px" />
        <el-select v-model="searchForm.roleCategory" placeholder="管理员级别" clearable style="width: 140px">
          <el-option label="一级管理员" value="level1" />
          <el-option label="二级管理员" value="level2" />
        </el-select>
        <el-select v-model="searchForm.role" placeholder="具体角色" clearable style="width: 180px">
          <el-option v-for="item in searchRoleOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="管理员级别" width="110">
          <template #default="{ row }">
            <el-tag
              :type="getRoleCategoryTag(row.roleCategory).type"
              size="small"
              effect="light"
            >
              {{ getRoleCategoryTag(row.roleCategory).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="具体角色" min-width="180">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.roleCategory)" size="small">
              {{ row.roleLabel || getRoleLabel(row.roleCode) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" min-width="160">
          <template #default="{ row }">
            {{ row.lastLoginTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160">
          <template #default="{ row }">
            {{ row.createTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handleResetPassword(row)">重置密码</el-button>
            <el-button
              type="danger"
              link
              :disabled="row.username === 'algovize'"
              @click="handleDelete(row)"
            >删除</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" :disabled="isEdit" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="formData.nickname" placeholder="显示用的名字" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="formData.email" placeholder="可选" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="formData.phone" placeholder="可选" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="formData.role" style="width: 100%" placeholder="请选择角色">
            <template v-if="userStore.isSuperAdmin()">
              <el-option-group label="一级管理员">
                <el-option
                  v-for="item in LEVEL1_ROLES"
                  :key="item.role_code"
                  :label="item.label"
                  :value="item.role_code"
                />
              </el-option-group>
              <el-option-group label="二级管理员">
                <el-option
                  v-for="item in LEVEL2_ROLES"
                  :key="item.role_code"
                  :label="item.label"
                  :value="item.role_code"
                />
              </el-option-group>
            </template>
            <template v-else-if="userStore.isLevel1Admin()">
              <el-option-group label="二级管理员">
                <el-option
                  v-for="item in LEVEL2_ROLES"
                  :key="item.role_code"
                  :label="item.label"
                  :value="item.role_code"
                />
              </el-option-group>
            </template>
          </el-select>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空则不修改' : '初始密码（建议 admin123）'"
          />
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
}
</style>
