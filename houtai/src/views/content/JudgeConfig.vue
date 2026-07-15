<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElForm, ElFormItem, ElInput, ElInputNumber, ElSwitch, ElButton, ElCard, ElMessage, ElDivider } from 'element-plus'
import { Check } from '@element-plus/icons-vue'

const formData = reactive({
  ac: { enabled: true, description: '答案正确' },
  wa: { enabled: true, description: '答案错误' },
  ce: { enabled: true, description: '编译错误' },
  re: { enabled: true, description: '运行时错误' },
  tle: { enabled: true, description: '超时' },
  mle: { enabled: true, description: '内存超限' },
  timeLimit: 2000,
  memoryLimit: 256,
  enableSpecialJudge: false,
  enablePartialScore: true
})

const loading = ref(false)
const handleSave = async () => { loading.value = true; try { await new Promise(r => setTimeout(r, 500)); ElMessage.success('保存成功') } finally { loading.value = false } }
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>判题规则配置</h2><el-button type="primary" :icon="Check" :loading="loading" @click="handleSave">保存配置</el-button></div>
    <div class="card-container">
      <el-form label-width="160px" style="max-width:700px">
        <el-divider content-position="left">判题状态配置</el-divider>
        <el-form-item v-for="(item, key) in { ac: formData.ac, wa: formData.wa, ce: formData.ce, re: formData.re, tle: formData.tle, mle: formData.mle }" :key="key" :label="`${key.toUpperCase()}状态`">
          <el-switch v-model="item.enabled" /><span style="margin:0 12px">{{ item.description }}</span>
        </el-form-item>

        <el-divider content-position="left">限制配置</el-divider>
        <el-form-item label="时间限制"><el-input-number v-model="formData.timeLimit" :min="100" :max="30000" :step="100" /><span style="margin-left:8px">毫秒</span></el-form-item>
        <el-form-item label="内存限制"><el-input-number v-model="formData.memoryLimit" :min="32" :max="1024" :step="32" /><span style="margin-left:8px">MB</span></el-form-item>

        <el-divider content-position="left">高级配置</el-divider>
        <el-form-item label="开启特判"><el-switch v-model="formData.enableSpecialJudge" /><span class="form-tip">用于特殊输出格式的题目</span></el-form-item>
        <el-form-item label="部分得分"><el-switch v-model="formData.enablePartialScore" /><span class="form-tip">开启后按测试用例比例给分</span></el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>.form-tip{margin-left:12px;font-size:12px;color:var(--color-text-secondary)}</style>
