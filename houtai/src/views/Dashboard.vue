<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElCard, ElRow, ElCol, ElStatistic } from 'element-plus'
import { User, Document, ChatDotRound, Collection } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const statsData = ref({
  dau: 1256,
  wau: 8942,
  mau: 35680,
  todaySubmissions: 342,
  aiDialogues: 1289,
  problems: 156
})

const trendChartRef = ref<HTMLDivElement>()
const ojStatusChartRef = ref<HTMLDivElement>()
const moduleChartRef = ref<HTMLDivElement>()

onMounted(() => {
  initTrendChart()
  initOJStatusChart()
  initModuleChart()
})

const initTrendChart = () => {
  if (!trendChartRef.value) return

  const chart = echarts.init(trendChartRef.value)
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['DAU', '提交数'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: [
      { type: 'value', name: 'DAU' },
      { type: 'value', name: '提交数', splitLine: { show: false } }
    ],
    series: [
      {
        name: 'DAU',
        type: 'line',
        smooth: true,
        data: [820, 932, 901, 1234, 1290, 1450, 1256],
        areaStyle: { opacity: 0.3 },
        lineStyle: { width: 3 },
        itemStyle: { color: '#409eff' }
      },
      {
        name: '提交数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: [120, 132, 201, 334, 290, 430, 342],
        lineStyle: { width: 3 },
        itemStyle: { color: '#67c23a' }
      }
    ]
  }
  chart.setOption(option)
}

const initOJStatusChart = () => {
  if (!ojStatusChartRef.value) return

  const chart = echarts.init(ojStatusChartRef.value)
  const option = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      data: [
        { value: 65, name: 'AC', itemStyle: { color: '#67c23a' } },
        { value: 15, name: 'WA', itemStyle: { color: '#f56c6c' } },
        { value: 8, name: 'TLE', itemStyle: { color: '#e6a23c' } },
        { value: 5, name: 'CE', itemStyle: { color: '#909399' } },
        { value: 7, name: '其他', itemStyle: { color: '#c0c4cc' } }
      ]
    }]
  }
  chart.setOption(option)
}

const initModuleChart = () => {
  if (!moduleChartRef.value) return

  const chart = echarts.init(moduleChartRef.value)
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['访问量'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['数据结构', '算法', 'OJ', 'AI助手'],
      axisLabel: { interval: 0 }
    },
    yAxis: { type: 'value' },
    series: [{
      name: '访问量',
      type: 'bar',
      data: [4230, 3890, 5620, 1289],
      itemStyle: {
        color: (params: any) => {
          const colors = ['#409eff', '#67c23a', '#e6a23c', '#909399']
          return colors[params.dataIndex]
        },
        borderRadius: [4, 4, 0, 0]
      },
      barWidth: '40%'
    }]
  }
  chart.setOption(option)
}

const quickActions = [
  { icon: Document, title: 'OJ题目管理', desc: '新增/编辑/管理题目', path: '/content/problem' },
  { icon: User, title: '用户管理', desc: '查看用户数据', path: '/user/list' },
  { icon: Collection, title: '数据统计', desc: '查看运营数据', path: '/statistics/dashboard' }
]
</script>

<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="今日活跃用户" :value="statsData.dau">
            <template #prefix>
              <el-icon class="stat-icon" color="#409eff"><User /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-trend up">
            <span>较昨日 +12.5%</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="今日OJ提交" :value="statsData.todaySubmissions">
            <template #prefix>
              <el-icon class="stat-icon" color="#67c23a"><Document /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-trend up">
            <span>较昨日 +8.3%</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="AI对话次数" :value="statsData.aiDialogues">
            <template #prefix>
              <el-icon class="stat-icon" color="#e6a23c"><ChatDotRound /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-trend down">
            <span>较昨日 -3.2%</span>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="OJ题目数" :value="statsData.problems">
            <template #prefix>
              <el-icon class="stat-icon" color="#f56c6c"><Collection /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-trend up">
            <span>较上周 +5</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>数据趋势</span>
              <el-radio-group size="small">
                <el-radio-button label="近7天" />
                <el-radio-button label="近30天" />
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <span>OJ判题分布</span>
          </template>
          <div ref="ojStatusChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <span>模块访问量</span>
          </template>
          <div ref="moduleChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <span>快捷入口</span>
          </template>
          <el-row :gutter="12">
            <el-col v-for="action in quickActions" :key="action.title" :span="12">
              <div class="quick-action" @click="$router.push(action.path)">
                <el-icon :size="24"><component :is="action.icon" /></el-icon>
                <div class="action-info">
                  <span class="action-title">{{ action.title }}</span>
                  <span class="action-desc">{{ action.desc }}</span>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  max-width: 1600px;
  margin: 0 auto;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  :deep(.el-card__body) {
    padding: 20px;
  }
}

.stat-icon {
  font-size: 24px;
  margin-right: 8px;
}

.stat-trend {
  margin-top: 8px;
  font-size: 12px;

  &.up {
    color: #67c23a;
  }

  &.down {
    color: #f56c6c;
  }
}

.charts-row {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.quick-action {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background-color: #ecf5ff;
    transform: translateX(4px);
  }

  .action-info {
    display: flex;
    flex-direction: column;
  }

  .action-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-primary);
  }

  .action-desc {
    font-size: 12px;
    color: var(--color-text-secondary);
  }
}
</style>
