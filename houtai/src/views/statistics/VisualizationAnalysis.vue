<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElCard, ElRow, ElCol, ElTable, ElTableColumn } from 'element-plus'
import * as echarts from 'echarts'

const pieRef = ref<HTMLDivElement>()
const barRef = ref<HTMLDivElement>()

const topAlgorithms = ref([
  { rank: 1, name: '快速排序', visits: 2340, favorites: 456, shares: 123 },
  { rank: 2, name: '二分查找', visits: 1890, favorites: 345, shares: 98 },
  { rank: 3, name: '归并排序', visits: 1560, favorites: 289, shares: 87 },
  { rank: 4, name: '深度优先搜索', visits: 1230, favorites: 234, shares: 76 },
  { rank: 5, name: '广度优先搜索', visits: 980, favorites: 189, shares: 65 }
])

onMounted(() => { initCharts() })

const initCharts = () => {
  if (pieRef.value) {
    const chart = echarts.init(pieRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{ type: 'pie', radius: ['40%', '70%'], data: [
        { value: 4230, name: '数据结构', itemStyle: { color: '#409eff' } },
        { value: 3890, name: '排序算法', itemStyle: { color: '#67c23a' } },
        { value: 2100, name: '查找算法', itemStyle: { color: '#e6a23c' } },
        { value: 1560, name: '图算法', itemStyle: { color: '#f56c6c' } },
        { value: 890, name: '动态规划', itemStyle: { color: '#909399' } }
      ]}]
    })
  }
  if (barRef.value) {
    const chart = echarts.init(barRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['数据结构', '排序', '查找', '图', 'DP', '其他'] },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: [4230, 3890, 2100, 1560, 890, 760], itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] } }]
    })
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>可视化使用分析</h2></div>
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="12"><el-card shadow="hover"><template #header><span>模块访问分布</span></template><div ref="pieRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="12"><el-card shadow="hover"><template #header><span>类别访问量</span></template><div ref="barRef" style="height:300px"></div></el-card></el-col>
    </el-row>
    <el-card shadow="hover"><template #header><span>热门算法排名</span></template>
      <el-table :data="topAlgorithms" stripe>
        <el-table-column prop="rank" label="排名" width="80" />
        <el-table-column prop="name" label="算法名称" min-width="150" />
        <el-table-column prop="visits" label="访问量" width="100" sortable />
        <el-table-column prop="favorites" label="收藏数" width="100" sortable />
        <el-table-column prop="shares" label="分享数" width="100" sortable />
      </el-table>
    </el-card>
  </div>
</template>
