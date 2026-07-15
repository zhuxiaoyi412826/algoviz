<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElForm, ElFormItem, ElInput, ElInputNumber, ElSwitch, ElSlider, ElButton, ElCard, ElMessage, ElDivider } from 'element-plus'
import { Check } from '@element-plus/icons-vue'

const formData = reactive({
  animationSpeed: 50,
  stepInterval: 1000,
  gifWidth: 800,
  gifHeight: 600,
  gifFps: 10,
  enableHighlight: true,
  highlightColor: '#409eff',
  enableStepControl: true,
  enableAutoPlay: false,
  themeColor: '#409eff',
  nodeColor: '#67c23a',
  edgeColor: '#909399',
  highlightBgColor: '#fef0f0'
})

const loading = ref(false)

const handleSave = async () => {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 500))
    ElMessage.success('保存成功')
  } finally { loading.value = false }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>动画参数配置</h2><el-button type="primary" :icon="Check" :loading="loading" @click="handleSave">保存配置</el-button></div>
    <div class="card-container">
      <el-form label-width="160px" style="max-width:700px">
        <el-divider content-position="left">动画设置</el-divider>
        <el-form-item label="动画速度"><el-slider v-model="formData.animationSpeed" :min="10" :max="100" show-input /></el-form-item>
        <el-form-item label="单步执行间隔"><el-input-number v-model="formData.stepInterval" :min="100" :max="5000" :step="100" /><span style="margin-left:8px">毫秒</span></el-form-item>
        <el-form-item label="开启高亮标注"><el-switch v-model="formData.enableHighlight" /></el-form-item>
        <el-form-item label="高亮颜色"><el-input v-model="formData.highlightColor" style="width:100px" /></el-form-item>
        <el-form-item label="开启步骤控制"><el-switch v-model="formData.enableStepControl" /></el-form-item>
        <el-form-item label="自动播放"><el-switch v-model="formData.enableAutoPlay" /></el-form-item>

        <el-divider content-position="left">GIF导出设置</el-divider>
        <el-form-item label="导出宽度"><el-input-number v-model="formData.gifWidth" :min="320" :max="1920" :step="40" /><span style="margin-left:8px">px</span></el-form-item>
        <el-form-item label="导出高度"><el-input-number v-model="formData.gifHeight" :min="240" :max="1080" :step="40" /><span style="margin-left:8px">px</span></el-form-item>
        <el-form-item label="帧率"><el-input-number v-model="formData.gifFps" :min="5" :max="30" /><span style="margin-left:8px">fps</span></el-form-item>

        <el-divider content-position="left">配色方案</el-divider>
        <el-form-item label="主题色"><el-input v-model="formData.themeColor" style="width:100px" /></el-form-item>
        <el-form-item label="节点颜色"><el-input v-model="formData.nodeColor" style="width:100px" /></el-form-item>
        <el-form-item label="边颜色"><el-input v-model="formData.edgeColor" style="width:100px" /></el-form-item>
        <el-form-item label="高亮背景色"><el-input v-model="formData.highlightBgColor" style="width:100px" /></el-form-item>
      </el-form>
    </div>
  </div>
</template>
