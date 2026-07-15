<script setup lang="ts">
import { ref, reactive } from 'vue'
import {
  ElForm, ElFormItem, ElInput, ElSwitch, ElSelect, ElOption,
  ElButton, ElMessage, ElDivider, ElInputNumber
} from 'element-plus'
import { Check } from '@element-plus/icons-vue'

const loading = ref(false)

const rateLimitForm = reactive({
  enableRateLimit: true,
  defaultLimit: 100,
  windowMs: 60,
  banDuration: 30
})

const captchaForm = reactive({
  enableCaptcha: true,
  captchaType: 'slider',
  captchaExpiry: 300,
  maxAttempts: 5
})

const wechatForm = reactive({
  enableWechatLogin: true,
  appId: 'wx*************',
  appSecret: '****************************',
  callbackUrl: 'https://algo.example.com/auth/wechat/callback'
})

const sslForm = reactive({
  enableSSL: true,
  sslCert: '',
  sslKey: '',
  autoRenew: true
})

const securityForm = reactive({
  tokenExpiry: 24,
  refreshTokenExpiry: 168,
  sessionLimit: 3
})

const handleSave = async () => {
  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('保存成功')
  } finally {
    loading.value = false
  }
}

const handleTestConnection = () => {
  ElMessage.success('微信接口连接测试成功')
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>安全设置</h2>
      <el-button type="primary" :icon="Check" :loading="loading" @click="handleSave">保存设置</el-button>
    </div>

    <div class="card-container">
      <el-form label-width="160px" style="max-width: 800px">
        <el-divider content-position="left">访问频率限制</el-divider>
        <el-form-item label="开启频率限制">
          <el-switch v-model="rateLimitForm.enableRateLimit" />
          <span class="form-tip">防止恶意刷接口和暴力破解</span>
        </el-form-item>
        <el-form-item label="默认访问限制">
          <el-input-number v-model="rateLimitForm.defaultLimit" :min="10" :max="1000" />
          <span style="margin-left: 8px">次 /</span>
          <el-input-number v-model="rateLimitForm.windowMs" :min="1" :max="3600" style="margin-left: 8px; width: 120px" />
          <span style="margin-left: 8px">秒</span>
        </el-form-item>
        <el-form-item label="封禁时长">
          <el-input-number v-model="rateLimitForm.banDuration" :min="1" :max="1440" />
          <span style="margin-left: 8px">分钟</span>
        </el-form-item>

        <el-divider content-position="left">验证码设置</el-divider>
        <el-form-item label="开启验证码">
          <el-switch v-model="captchaForm.enableCaptcha" />
        </el-form-item>
        <el-form-item label="验证码类型">
          <el-select v-model="captchaForm.captchaType" style="width: 200px">
            <el-option label="滑动验证码" value="slider" />
            <el-option label="图形验证码" value="image" />
            <el-option label="极验验证" value="geetest" />
          </el-select>
        </el-form-item>
        <el-form-item label="验证码有效期">
          <el-input-number v-model="captchaForm.captchaExpiry" :min="60" :max="3600" />
          <span style="margin-left: 8px">秒</span>
        </el-form-item>
        <el-form-item label="最大尝试次数">
          <el-input-number v-model="captchaForm.maxAttempts" :min="3" :max="10" />
          <span style="margin-left: 8px">次</span>
          <span class="form-tip">超过后需等待验证码过期</span>
        </el-form-item>

        <el-divider content-position="left">微信登录配置</el-divider>
        <el-form-item label="开启微信登录">
          <el-switch v-model="wechatForm.enableWechatLogin" />
        </el-form-item>
        <el-form-item label="AppID">
          <el-input v-model="wechatForm.appId" />
        </el-form-item>
        <el-form-item label="AppSecret">
          <el-input v-model="wechatForm.appSecret" type="password" show-password />
        </el-form-item>
        <el-form-item label="回调地址">
          <el-input v-model="wechatForm.callbackUrl" />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleTestConnection">测试连接</el-button>
        </el-form-item>

        <el-divider content-position="left">SSL证书配置</el-divider>
        <el-form-item label="启用HTTPS">
          <el-switch v-model="sslForm.enableSSL" />
        </el-form-item>
        <el-form-item label="SSL证书">
          <el-input v-model="sslForm.sslCert" type="textarea" :rows="4" placeholder="请粘贴证书内容" />
        </el-form-item>
        <el-form-item label="SSL私钥">
          <el-input v-model="sslForm.sslKey" type="textarea" :rows="4" placeholder="请粘贴私钥内容" />
        </el-form-item>
        <el-form-item label="自动续期">
          <el-switch v-model="sslForm.autoRenew" />
          <span class="form-tip">证书到期前30天自动续期</span>
        </el-form-item>

        <el-divider content-position="left">其他安全设置</el-divider>
        <el-form-item label="JWT密钥">
          <el-input type="password" show-password placeholder="用于生成Token的密钥" />
          <div class="form-tip">建议使用32位以上的随机字符串，定期更换</div>
        </el-form-item>
        <el-form-item label="Token有效期">
          <el-input-number v-model="securityForm.tokenExpiry" :min="1" :max="168" />
          <span style="margin-left: 8px">小时</span>
        </el-form-item>
        <el-form-item label="刷新Token有效期">
          <el-input-number v-model="securityForm.refreshTokenExpiry" :min="1" :max="720" />
          <span style="margin-left: 8px">小时</span>
        </el-form-item>
        <el-form-item label="登录会话限制">
          <el-input-number v-model="securityForm.sessionLimit" :min="1" :max="10" />
          <span style="margin-left: 8px">个</span>
          <span class="form-tip">同一账号最多同时在线会话数</span>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped lang="scss">
.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
