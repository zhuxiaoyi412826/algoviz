<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const formData = reactive({
  username: '',
  password: '',
  captcha: '',
  captchaId: ''
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
      // 账号密码校验
      if (formData.username !== 'admin' || formData.password !== 'admin123') {
        ElMessage.error('用户名或密码错误，请输入 admin/admin123')
        loading.value = false
        return
      }

      // 模拟登录
      await new Promise(resolve => setTimeout(resolve, 1000))

      const mockToken = 'mock_token_' + Date.now()
      const mockUser = {
        id: '1',
        username: formData.username,
        nickname: '管理员',
        role: 'super_admin' as const,
        status: 'active' as const,
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString()
      }

      userStore.setToken(mockToken)
      userStore.setUserInfo(mockUser)

      ElMessage.success('登录成功')
      router.push('/')
    } catch {
      ElMessage.error('登录失败，请检查用户名和密码')
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
        <span>默认账号: admin / admin123</span>
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
