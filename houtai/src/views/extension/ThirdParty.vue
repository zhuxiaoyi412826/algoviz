<script setup lang="ts">
import { reactive } from 'vue'
import { ElForm, ElFormItem, ElInput, ElSwitch, ElButton, ElMessage, ElDivider } from 'element-plus'
import { Check } from '@element-plus/icons-vue'

const formData = reactive({
  wechatEnabled: true,
  wechatAppId: '',
  wechatAppSecret: '',
  baiduEnabled: false,
  baiduSiteId: '',
  baiduToken: '',
  cdnEnabled: true,
  cdnDomain: '',
  cdnSecret: ''
})

const loading = ref(false)
const handleSave = async () => { loading.value = true; try { await new Promise(r => setTimeout(r, 500)); ElMessage.success('保存成功') } finally { loading.value = false } }
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>第三方集成</h2><el-button type="primary" :icon="Check" :loading="loading" @click="handleSave">保存配置</el-button></div>
    <div class="card-container">
      <el-form label-width="140px" style="max-width:700px">
        <el-divider content-position="left">微信公众号</el-divider>
        <el-form-item label="启用微信登录"><el-switch v-model="formData.wechatEnabled" /></el-form-item>
        <el-form-item label="AppID"><el-input v-model="formData.wechatAppId" :disabled="!formData.wechatEnabled" /></el-form-item>
        <el-form-item label="AppSecret"><el-input v-model="formData.wechatAppSecret" type="password" show-password :disabled="!formData.wechatEnabled" /></el-form-item>

        <el-divider content-position="left">百度统计</el-divider>
        <el-form-item label="启用百度统计"><el-switch v-model="formData.baiduEnabled" /></el-form-item>
        <el-form-item label="站点ID"><el-input v-model="formData.baiduSiteId" :disabled="!formData.baiduEnabled" /></el-form-item>
        <el-form-item label="API Token"><el-input v-model="formData.baiduToken" :disabled="!formData.baiduEnabled" /></el-form-item>

        <el-divider content-position="left">CDN配置</el-divider>
        <el-form-item label="启用CDN"><el-switch v-model="formData.cdnEnabled" /></el-form-item>
        <el-form-item label="CDN域名"><el-input v-model="formData.cdnDomain" :disabled="!formData.cdnEnabled" placeholder="https://cdn.example.com" /></el-form-item>
        <el-form-item label="访问密钥"><el-input v-model="formData.cdnSecret" type="password" show-password :disabled="!formData.cdnEnabled" /></el-form-item>
      </el-form>
    </div>
  </div>
</template>
