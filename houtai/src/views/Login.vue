<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const formData = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    loading.value = true
    try {
      const data: any = await request.post('/admin/login', {
        username: formData.username,
        password: formData.password
      })

      // 后端返回格式: { code: 200, data: { token, tokenName, userInfo, roles, permissions, menus, loginId } }
      const payload = data?.data || data
      const token = payload?.token
      const userInfo = payload?.userInfo
      const userRoles = payload?.roles || []
      const userPermissions = payload?.permissions || []

      if (!token) {
        ElMessage.error('登录失败，未获取到 Token')
        return
      }

      userStore.setToken(token)
      if (userInfo) {
        // 适配前端 User 类型：将 sysUser 字段转为前端期望的格式
        const rawStatus = userInfo.status
        const statusValue: 'active' | 'disabled' =
          rawStatus === 1 || rawStatus === 'active' || rawStatus === 'enabled'
            ? 'active'
            : 'disabled'

        const adaptedUserInfo = {
          id: userInfo.id?.toString() || '',
          username: userInfo.username || '',
          nickname: userInfo.realName || userInfo.username || '',
          email: userInfo.email || '',
          phone: userInfo.phone || '',
          role: userRoles[0] || '',
          status: statusValue,
          avatar: userInfo.avatar || '',
          createTime: userInfo.createdAt || '',
          updateTime: userInfo.updatedAt || '',
          lastLoginTime: userInfo.lastLoginTime || ''
        }
        userStore.setUserInfo(adaptedUserInfo, userRoles, userPermissions)
        localStorage.setItem('userInfo', JSON.stringify(adaptedUserInfo))
        localStorage.setItem('roles', JSON.stringify(userRoles))
        localStorage.setItem('permissions', JSON.stringify(userPermissions))
      }

      ElMessage.success('登录成功')
      router.push('/')
    } catch (error: any) {
      // axios 拦截器已处理 401/403 等错误
      const msg = error?.response?.data?.message || error?.message || '登录失败，请检查用户名和密码'
      ElMessage.error(msg)
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>算法可视化平台</h1>
        <p>管理后台</p>
      </div>
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="formData.username"
            placeholder="用户名"
            size="large"
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            native-type="submit"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>默认账号: algovize / algovize123</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  h1 {
    font-size: 28px;
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: var(--color-text-secondary);
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 24px;
  }

  :deep(.el-input__wrapper) {
    padding: 4px 16px;
  }
}

.login-btn {
  width: 100%;
  font-size: 16px;
  letter-spacing: 4px;
}

.login-footer {
  text-align: center;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 16px;
}
</style>
