<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElForm, ElFormItem, ElInput, ElInputNumber, ElSwitch, ElButton, ElCard, ElMessage, ElDivider, ElSelect, ElOption, ElAlert } from 'element-plus'
import { Check } from '@element-plus/icons-vue'

const formData = reactive({
  provider: 'deepseek',
  apiUrl: 'https://api.deepseek.com/v1',
  apiKey: 'sk-****************************',
  model: 'deepseek-chat',
  maxTokens: 4096,
  temperature: 0.7,
  requestTimeout: 30000,
  enableStream: true,
  streamSpeed: 50,
  rateLimitEnabled: true,
  rateLimitPerMinute: 20,
  enableSensitiveFilter: true
})

const loading = ref(false)
const handleSave = async () => { loading.value = true; try { await new Promise(r => setTimeout(r, 500)); ElMessage.success('保存成功') } finally { loading.value = false } }
const handleTest = () => ElMessage.success('接口连接测试成功')
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>AI接口配置</h2><div><el-button @click="handleTest">测试连接</el-button><el-button type="primary" :icon="Check" :loading="loading" @click="handleSave">保存配置</el-button></div></div>
    <div class="card-container">
      <el-form label-width="140px" style="max-width:700px">
        <el-divider content-position="left">基础配置</el-divider>
        <el-form-item label="AI服务商"><el-select v-model="formData.provider" style="width:200px"><el-option label="DeepSeek" value="deepseek" /><el-option label="OpenAI" value="openai" /><el-option label="文心一言" value="wenxin" /></el-select></el-form-item>
        <el-form-item label="API地址"><el-input v-model="formData.apiUrl" /></el-form-item>
        <el-form-item label="API密钥"><el-input v-model="formData.apiKey" type="password" show-password /></el-form-item>
        <el-form-item label="模型名称"><el-input v-model="formData.model" /></el-form-item>

        <el-divider content-position="left">请求参数</el-divider>
        <el-form-item label="最大Token数"><el-input-number v-model="formData.maxTokens" :min="100" :max="32768" :step="100" /></el-form-item>
        <el-form-item label="Temperature"><el-input-number v-model="formData.temperature" :min="0" :max="2" :step="0.1" :precision="1" /></el-form-item>
        <el-form-item label="请求超时"><el-input-number v-model="formData.requestTimeout" :min="5000" :max="120000" :step="1000" /><span style="margin-left:8px">毫秒</span></el-form-item>

        <el-divider content-position="left">流式输出</el-divider>
        <el-form-item label="开启流式输出"><el-switch v-model="formData.enableStream" /></el-form-item>
        <el-form-item label="输出速度"><el-input-number v-model="formData.streamSpeed" :min="10" :max="200" :step="10" /><span style="margin-left:8px">字符/秒</span></el-form-item>

        <el-divider content-position="left">限流与安全</el-divider>
        <el-form-item label="启用访问限制"><el-switch v-model="formData.rateLimitEnabled" /></el-form-item>
        <el-form-item label="每分钟限制"><el-input-number v-model="formData.rateLimitPerMinute" :min="1" :max="100" :disabled="!formData.rateLimitEnabled" /><span style="margin-left:8px">次/分钟</span></el-form-item>
        <el-form-item label="敏感词过滤"><el-switch v-model="formData.enableSensitiveFilter" /></el-form-item>
      </el-form>
    </div>
  </div>
</template>
