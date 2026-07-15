<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElCard, ElForm, ElFormItem, ElSelect, ElOption, ElDatePicker, ElButton, ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'

const formData = reactive({ type: 'user', format: 'xlsx' })
const dateRange = ref<string[]>([])

const handleExport = () => {
  ElMessage.success(`正在导出 ${formData.type} 数据，格式：${formData.format}`)
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>数据导出</h2></div>
    <div class="card-container">
      <el-card shadow="hover" style="max-width:600px"><template #header><span>导出配置</span></template>
        <el-form :model="formData" label-width="100px">
          <el-form-item label="数据类型">
            <el-select v-model="formData.type" style="width:100%">
              <el-option label="用户行为数据" value="user" />
              <el-option label="OJ提交记录" value="submission" />
              <el-option label="AI对话记录" value="dialogue" />
              <el-option label="题目数据" value="problem" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
          <el-form-item label="导出格式">
            <el-select v-model="formData.format" style="width:100%">
              <el-option label="Excel (.xlsx)" value="xlsx" />
              <el-option label="CSV (.csv)" value="csv" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" :icon="Download" @click="handleExport">导出数据</el-button></el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>
