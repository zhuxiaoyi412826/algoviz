<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import {
  ElForm, ElFormItem, ElInput, ElButton, ElAvatar, ElTag, ElCard,
  ElMessage, ElTabs, ElTabPane, ElDescriptions, ElDescriptionsItem
} from 'element-plus'
import { User, Lock, Message, Phone, Calendar, Timer } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const userStore = useUserStore()
const loading = ref(false)

// 个人信息表单
const profileForm = reactive({
  id: '',
  username: '',
  realName: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})

// 修改密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRef = ref()
const passwordRef = ref()

const profileRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }]
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 角色显示映射
const roleDisplayMap: Record<string, { label: string; type: 'danger' | 'warning' | 'info' | 'success' | 'primary' }> = {
  'SUPER_ADMIN': { label: '超级管理员', type: 'danger' },
  'LEVEL1_ADMIN': { label: '一级管理员', type: 'warning' },
  'CONTENT_PUBLISHER': { label: '内容发布管理员', type: 'warning' },
  'CONTENT_AUDITOR': { label: '内容审核管理员', type: 'warning' },
  'EFK_LOG_ADMIN': { label: 'EFK日志分析管理员', type: 'warning' },
  'DATA_ANALYST': { label: '数据分析管理员', type: 'warning' },
  'ORDER_ADMIN': { label: '订单分析管理员', type: 'warning' },
  'OPS_ADMIN': { label: '运营活动管理员', type: 'warning' },
  'FINANCE_ADMIN': { label: '财务管理员', type: 'warning' },
  'CS_ADMIN': { label: '客服管理员', type: 'warning' },
  'SECURITY_AUDITOR': { label: '安全审计管理员', type: 'warning' },
  'PERM_MANAGER': { label: '权限分配管理员', type: 'warning' },
  'DS_PUBLISHER': { label: '数据结构发布管理员', type: 'info' },
  'ALGO_PUBLISHER': { label: '算法发布管理员', type: 'info' },
  'GENERAL_SUB_ADMIN': { label: '通用子管理员', type: 'info' },
  'TEST_VALIDATOR': { label: '题目校验测试管理员', type: 'info' },
  'KEYWORD_AUDITOR': { label: '关键词审核管理员', type: 'info' },
  'SOLUTION_COMMENT_AUDITOR': { label: '题解评论审核管理员', type: 'info' },
  'BIZ_LOG_ADMIN': { label: '业务日志管理员', type: 'info' },
  'DEV_LOG_ADMIN': { label: '开发日志管理员', type: 'info' },
  'SYS_LOG_ADMIN': { label: '系统日志管理员', type: 'info' },
  'OPS_LOG_ADMIN': { label: '运维管理员', type: 'info' },
  'UV_PV_ANALYST': { label: 'UV/PV数据分析师', type: 'info' },
  'RETENTION_ANALYST': { label: '用户留存数据分析师', type: 'info' },
  'ORDER_ANALYST_SUB': { label: '订单数据分析师', type: 'info' },
  'PRODUCT_ADMIN': { label: '商品管理员', type: 'info' },
  'COIN_ADMIN': { label: '金币管理员', type: 'info' },
  'EXTERNAL_AUTHOR': { label: '外部出题人', type: 'info' }
}

const getRoleTags = () => {
  const roles = userStore.roles
  if (!roles || roles.length === 0) return []
  return roles.map(role => ({
    code: role,
    ...roleDisplayMap[role] || { label: role, type: 'info' as const }
  }))
}

// 从后端加载最新的用户信息
const loadUserInfo = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/admin/info')
    const info = res?.userInfo || res
    if (info) {
      profileForm.id = info.id?.toString() || ''
      profileForm.username = info.username || ''
      profileForm.realName = info.realName || info.nickname || ''
      profileForm.nickname = info.nickname || info.realName || ''
      profileForm.email = info.email || ''
      profileForm.phone = info.phone || ''
      profileForm.avatar = info.avatar || ''
    } else {
      // 回退到 store 数据
      fillFromStore()
    }
  } catch (e) {
    // 接口失败时回退到 store
    fillFromStore()
  } finally {
    loading.value = false
  }
}

const fillFromStore = () => {
  const info = userStore.userInfo
  if (info) {
    profileForm.id = info.id || ''
    profileForm.username = info.username || ''
    profileForm.realName = info.nickname || ''
    profileForm.nickname = info.nickname || ''
    profileForm.email = info.email || ''
    profileForm.phone = info.phone || ''
    profileForm.avatar = info.avatar || ''
  }
}

const handleSaveProfile = async () => {
  if (!profileRef.value) return
  await profileRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      await request.put(`/system/admin/${profileForm.id}`, {
        realName: profileForm.realName,
        email: profileForm.email,
        phone: profileForm.phone
      })
      // 更新 store
      if (userStore.userInfo) {
        userStore.userInfo.nickname = profileForm.realName
        userStore.userInfo.email = profileForm.email
        userStore.userInfo.phone = profileForm.phone
      }
      ElMessage.success('个人信息保存成功')
    } catch (e: any) {
      ElMessage.error(e?.message || '保存失败')
    }
  })
}

const handleChangePassword = async () => {
  if (!passwordRef.value) return
  await passwordRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      await request.put(`/system/admin/${profileForm.id}`, {
        password: passwordForm.newPassword
      })
      ElMessage.success('密码修改成功，请重新登录')
      // 修改密码后退出登录
      setTimeout(() => {
        userStore.logout()
        window.location.href = '/login'
      }, 1500)
    } catch (e: any) {
      ElMessage.error(e?.message || '密码修改失败')
    }
  })
}

onMounted(() => {
  fillFromStore()
  loadUserInfo()
})
</script>

<template>
  <div class="profile-page">
    <div class="page-header">
      <h2>个人中心</h2>
    </div>

    <!-- 顶部用户信息卡片 -->
    <el-card class="user-card" shadow="hover">
      <div class="user-header">
        <el-avatar :size="80" :icon="User" :src="profileForm.avatar || undefined" />
        <div class="user-meta">
          <div class="user-name">{{ profileForm.realName || profileForm.username }}</div>
          <div class="user-account">@{{ profileForm.username }}</div>
          <div class="user-roles">
            <el-tag
              v-for="role in getRoleTags()"
              :key="role.code"
              :type="role.type"
              size="small"
              effect="dark"
              style="margin-right: 6px"
            >
              {{ role.label }}
            </el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 标签页 -->
    <el-card class="content-card" shadow="never">
      <el-tabs>
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息">
          <el-form
            ref="profileRef"
            :model="profileForm"
            :rules="profileRules"
            label-width="100px"
            style="max-width: 500px; margin-top: 10px"
            v-loading="loading"
          >
            <el-form-item label="用户名">
              <el-input :model-value="profileForm.username" disabled>
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="profileForm.realName" placeholder="请输入姓名">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱">
                <template #prefix><el-icon><Message /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号">
                <template #prefix><el-icon><Phone /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码">
          <el-form
            ref="passwordRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            style="max-width: 500px; margin-top: 10px"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码">
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少6位">
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码">
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 账号信息 -->
        <el-tab-pane label="账号信息">
          <el-descriptions :column="2" border style="margin-top: 10px">
            <el-descriptions-item label="用户ID">{{ profileForm.id }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ profileForm.username }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ profileForm.realName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ profileForm.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ profileForm.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag :type="userStore.userInfo?.status === 'active' ? 'success' : 'danger'" size="small">
                {{ userStore.userInfo?.status === 'active' ? '正常' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              <el-icon style="margin-right: 4px"><Calendar /></el-icon>
              {{ userStore.userInfo?.createTime || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="最后登录">
              <el-icon style="margin-right: 4px"><Timer /></el-icon>
              {{ userStore.userInfo?.lastLoginTime || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.profile-page {
  max-width: 800px;
}

.page-header {
  margin-bottom: 16px;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
}

.user-card {
  margin-bottom: 16px;

  .user-header {
    display: flex;
    align-items: center;
    gap: 20px;
  }

  .user-meta {
    .user-name {
      font-size: 20px;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    .user-account {
      font-size: 14px;
      color: var(--el-text-color-secondary);
      margin-top: 2px;
    }

    .user-roles {
      margin-top: 8px;
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
    }
  }
}

.content-card {
  min-height: 400px;
}
</style>
