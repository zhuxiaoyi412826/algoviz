<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElCard, ElRow, ElCol } from 'element-plus'
import * as echarts from 'echarts'

const cpuRef = ref<HTMLDivElement>()
const memRef = ref<HTMLDivElement>()
const diskRef = ref<HTMLDivElement>()

const resourceData = reactive({
  cpu: 45,
  memory: 62,
  disk: 38,
  dbConnections: 12,
  avgResponseTime: 85
})

onMounted(() => { initCharts() })

const initCharts = () => {
  if (cpuRef.value) {
    const chart = echarts.init(cpuRef.value)
    chart.setOption({
      series: [{ type: 'gauge', radius: '90%', startAngle: 200, endAngle: -20, min: 0, max: 100, splitNumber: 10, itemStyle: { color: '#409eff' }, detail: { formatter: '{value}%' }, data: [{ value: resourceData.cpu }] }]
    })
  }
  if (memRef.value) {
    const chart = echarts.init(memRef.value)
    chart.setOption({
      series: [{ type: 'gauge', radius: '90%', startAngle: 200, endAngle: -20, min: 0, max: 100, splitNumber: 10, itemStyle: { color: '#67c23a' }, detail: { formatter: '{value}%' }, data: [{ value: resourceData.memory }] }]
    })
  }
  if (diskRef.value) {
    const chart = echarts.init(diskRef.value)
    chart.setOption({
      series: [{ type: 'gauge', radius: '90%', startAngle: 200, endAngle: -20, min: 0, max: 100, splitNumber: 10, itemStyle: { color: '#e6a23c' }, detail: { formatter: '{value}%' }, data: [{ value: resourceData.disk }] }]
    })
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>资源占用监控</h2></div>
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="8"><el-card shadow="hover"><template #header><span>CPU使用率</span></template><div ref="cpuRef" style="height:200px"></div></el-card></el-col>
      <el-col :span="8"><el-card shadow="hover"><template #header><span>内存使用率</span></template><div ref="memRef" style="height:200px"></div></el-card></el-col>
      <el-col :span="8"><el-card shadow="hover"><template #header><span>磁盘使用率</span></template><div ref="diskRef" style="height:200px"></div></el-card></el-col>
    </el-row>
    <el-card shadow="hover"><template #header><span>其他指标</span></template>
      <el-row :gutter="20">
        <el-col :span="8"><div class="stat-item"><span>数据库连接数</span><strong>{{ resourceData.dbConnections }}</strong></div></el-col>
        <el-col :span="8"><div class="stat-item"><span>平均响应时间</span><strong>{{ resourceData.avgResponseTime }}ms</strong></div></el-col>
        <el-col :span="8"><div class="stat-item"><span>网络IO</span><strong>125MB/s</strong></div></el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style scoped>
.stat-item { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 20px; background: #f5f7fa; border-radius: 8px }
.stat-item span { font-size: 14px; color: var(--color-text-secondary) }
.stat-item strong { font-size: 24px; color: var(--color-text-primary) }
</style>
