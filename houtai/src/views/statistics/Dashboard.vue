<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElCard, ElRow, ElCol, ElStatistic, ElRadioGroup, ElRadioButton } from 'element-plus'
import * as echarts from 'echarts'

const trendChartRef = ref<HTMLDivElement>()
const activeTab = ref('7d')

onMounted(() => { initChart() })

const initChart = () => {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['DAU', 'WAU', 'MAU'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
    yAxis: { type: 'value' },
    series: [
      { name: 'DAU', type: 'line', smooth: true, data: [820, 932, 901, 1234, 1290, 1450, 1256], areaStyle: { opacity: 0.3 } },
      { name: 'WAU', type: 'line', smooth: true, data: [3200, 3800, 3500, 4100, 4200, 4500, 4200] },
      { name: 'MAU', type: 'line', smooth: true, data: [12000, 13500, 12800, 14200, 15000, 16000, 15680] }
    ]
  })
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>核心指标看板</h2><el-radio-group v-model="activeTab"><el-radio-button label="7d">近7天</el-radio-button><el-radio-button label="30d">近30天</el-radio-button></el-radio-group></div>
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :xs="24" :sm="12" :lg="6"><el-card shadow="hover"><el-statistic title="日活(DAU)" :value="1256"><template #prefix><span class="stat-up">+12.5%</span></template></el-statistic></el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card shadow="hover"><el-statistic title="周活(WAU)" :value="8942"><template #prefix><span class="stat-up">+8.3%</span></template></el-statistic></el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card shadow="hover"><el-statistic title="月活(MAU)" :value="35680"><template #prefix><span class="stat-down">-2.1%</span></template></el-statistic></el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card shadow="hover"><el-statistic title="OJ通过率" :value="65.2" suffix="%"><template #prefix><span class="stat-up">+3.5%</span></template></el-statistic></el-card></el-col>
    </el-row>
    <el-card shadow="hover"><template #header><span>活跃趋势</span></template><div ref="trendChartRef" style="height:350px"></div></el-card>
  </div>
</template>

<style scoped>
.stat-up { color: #67c23a; font-size: 14px; margin-right: 4px }
.stat-down { color: #f56c6c; font-size: 14px; margin-right: 4px }
</style>
