<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElCard, ElRow, ElCol, ElProgress } from 'element-plus'
import * as echarts from 'echarts'

const chartRef = ref<HTMLDivElement>()

onMounted(() => { initChart() })

const initChart = () => {
  if (!chartRef.value) return
  const chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{ type: 'pie', radius: ['40%', '70%'], data: [
      { value: 4230, name: '数据结构', itemStyle: { color: '#409eff' } },
      { value: 3890, name: '算法', itemStyle: { color: '#67c23a' } },
      { value: 5620, name: 'OJ', itemStyle: { color: '#e6a23c' } },
      { value: 1289, name: 'AI助手', itemStyle: { color: '#909399' } }
    ]}]
  })
}
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>用户行为数据</h2></div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover"><template #header><span>模块访问分布</span></template><div ref="chartRef" style="height:300px"></div></el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover"><template #header><span>访问统计</span></template>
          <el-row :gutter="16">
            <el-col :span="12"><el-progress type="circle" :percentage="30" color="#409eff"><template #default>数据结构 30%</template></el-progress></el-col>
            <el-col :span="12"><el-progress type="circle" :percentage="27" color="#67c23a"><template #default>算法 27%</template></el-progress></el-col>
            <el-col :span="12"><el-progress type="circle" :percentage="40" color="#e6a23c"><template #default>OJ 40%</template></el-progress></el-col>
            <el-col :span="12"><el-progress type="circle" :percentage="9" color="#909399"><template #default>AI助手 9%</template></el-progress></el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
