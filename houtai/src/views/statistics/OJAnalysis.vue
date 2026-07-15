<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElCard, ElRow, ElCol, ElRadioGroup, ElRadioButton } from 'element-plus'
import * as echarts from 'echarts'

const trendRef = ref<HTMLDivElement>()
const pieRef = ref<HTMLDivElement>()
const langRef = ref<HTMLDivElement>()

onMounted(() => { initCharts() })

const initCharts = () => {
  if (trendRef.value) {
    const chart = echarts.init(trendRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['提交数', '通过数'], bottom: 0 },
      xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
      yAxis: [{ type: 'value', name: '提交数' }, { type: 'value', name: '通过数', splitLine: { show: false } }],
      series: [
        { name: '提交数', type: 'bar', data: [120, 132, 201, 334, 290, 430, 342] },
        { name: '通过数', type: 'line', yAxisIndex: 1, data: [80, 95, 130, 210, 180, 280, 220] }
      ]
    })
  }
  if (pieRef.value) {
    const chart = echarts.init(pieRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{ type: 'pie', radius: ['40%', '70%'], data: [
        { value: 65, name: 'AC', itemStyle: { color: '#67c23a' } },
        { value: 15, name: 'WA', itemStyle: { color: '#f56c6c' } },
        { value: 8, name: 'TLE', itemStyle: { color: '#e6a23c' } },
        { value: 5, name: 'CE', itemStyle: { color: '#909399' } },
        { value: 7, name: '其他', itemStyle: { color: '#c0c4cc' } }
      ]}]
    })
  }
  if (langRef.value) {
    const chart = echarts.init(langRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['Python', 'Java', 'C++', 'JavaScript'] },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: [45, 30, 15, 10], itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] } }]
    })
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>OJ运营分析</h2><el-radio-group><el-radio-button label="7d">近7天</el-radio-button><el-radio-button label="30d">近30天</el-radio-button></el-radio-group></div>
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="12"><el-card shadow="hover"><template #header><span>提交趋势</span></template><div ref="trendRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="12"><el-card shadow="hover"><template #header><span>判题分布</span></template><div ref="pieRef" style="height:300px"></div></el-card></el-col>
    </el-row>
    <el-card shadow="hover"><template #header><span>语言分布</span></template><div ref="langRef" style="height:300px"></div></el-card>
  </div>
</template>
