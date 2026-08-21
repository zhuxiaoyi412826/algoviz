<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { ElCard, ElRow, ElCol, ElStatistic, ElTable, ElTableColumn, ElTag } from 'element-plus'
import { User, Document, ChatDotRound, Collection, View } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const loading = ref(true)
const overview = ref<any>({})
const weekTrend = ref<any[]>([])
const ojDistribution = ref<any[]>([])
const moduleVisits = ref<any[]>([])
const topOJProblems = ref<any[]>([])
const topInterviewProblems = ref<any[]>([])

const trendChartRef = ref<HTMLDivElement>()
const ojStatusChartRef = ref<HTMLDivElement>()
const moduleChartRef = ref<HTMLDivElement>()

const trendDays = ref<'7d' | '30d'>('7d')

const loadData = async () => {
  loading.value = true
  try {
    const data: any = await request.get('/dashboard/stats')
    if (data) {
      overview.value = data.overview || {}
      weekTrend.value = data.weekTrend || []
      ojDistribution.value = data.ojDistribution || []
      moduleVisits.value = data.moduleVisits || []
      topOJProblems.value = data.topOJProblems || []
      topInterviewProblems.value = data.topInterviewProblems || []

      await nextTick()
      initTrendChart()
      initOJStatusChart()
      initModuleChart()
    }
  } catch (e) {
    console.error('加载仪表盘数据失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const formatDate = (dateStr: string) => {
  const d = new Date(dateStr)
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return weekdays[d.getDay()]
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  const data = weekTrend.value
  if (!data.length) return

  const dates = data.map((d: any) => formatDate(d.date))
  const dauData = data.map((d: any) => d.dau || 0)
  const subData = data.map((d: any) => d.submissions || 0)

  const chart = echarts.init(trendChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['DAU', '提交数'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates
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
        data: dauData,
        areaStyle: { opacity: 0.3 },
        lineStyle: { width: 3 },
        itemStyle: { color: '#409eff' }
      },
      {
        name: '提交数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: subData,
        lineStyle: { width: 3 },
        itemStyle: { color: '#67c23a' }
      }
    ]
  })
}

const initOJStatusChart = () => {
  if (!ojStatusChartRef.value) return
  const data = ojDistribution.value
  if (!data.length) return

  const colorMap: Record<string, string> = {
    'AC': '#67c23a', 'WA': '#f56c6c', 'TLE': '#e6a23c',
    'CE': '#909399', 'RE': '#c0c4cc', 'MLE': '#c0c4cc', 'OLE': '#c0c4cc'
  }
  const chartData = data.map((d: any) => ({
    value: d.value,
    name: d.name,
    itemStyle: { color: colorMap[d.name] || '#c0c4cc' }
  }))

  const chart = echarts.init(ojStatusChartRef.value)
  chart.setOption({
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
      data: chartData
    }]
  })
}

const initModuleChart = () => {
  if (!moduleChartRef.value) return
  const data = moduleVisits.value
  if (!data.length) return

  const chart = echarts.init(moduleChartRef.value)
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#909399']
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['访问量'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map((d: any) => d.name),
      axisLabel: { interval: 0 }
    },
    yAxis: { type: 'value' },
    series: [{
      name: '访问量',
      type: 'bar',
      data: data.map((d: any) => d.value),
      itemStyle: {
        color: (params: any) => colors[params.dataIndex % colors.length],
        borderRadius: [4, 4, 0, 0]
      },
      barWidth: '40%'
    }]
  })
}

const quickActions = [
  { icon: Document, title: 'OJ题目管理', desc: '新增/编辑/管理题目', path: '/content/problem' },
  { icon: User, title: '用户管理', desc: '查看用户数据', path: '/user/list' },
  { icon: Collection, title: '数据统计', desc: '查看运营数据', path: '/statistics/dashboard' }
]

const formatChange = (val: number) => {
  if (val > 0) return `+${val}%`
  if (val < 0) return `${val}%`
  return '0%'
}

const changeClass = (val: number) => val >= 0 ? 'up' : 'down'
const changeIcon = (val: number) => val >= 0 ? '↑' : '↓'
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <!-- 概览卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="今日活跃用户" :value="overview.todayActive || 0">
            <template #prefix>
              <el-icon class="stat-icon" color="#409eff"><User /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-trend" :class="changeClass(overview.todayActiveChange || 0)">
            <span>较昨日 {{ changeIcon(overview.todayActiveChange || 0) }} {{ formatChange(overview.todayActiveChange || 0) }}</span>
          </div>
          <div class="stat-sub">总用户数: {{ overview.totalUsers || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="今日OJ提交" :value="overview.todaySubmissions || 0">
            <template #prefix>
              <el-icon class="stat-icon" color="#67c23a"><Document /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-trend" :class="changeClass(overview.todaySubmissionsChange || 0)">
            <span>较昨日 {{ changeIcon(overview.todaySubmissionsChange || 0) }} {{ formatChange(overview.todaySubmissionsChange || 0) }}</span>
          </div>
          <div class="stat-sub">总提交数: {{ overview.totalSubmissions || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="AI对话次数" :value="overview.todayAIDialogues || 0">
            <template #prefix>
              <el-icon class="stat-icon" color="#e6a23c"><ChatDotRound /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-trend" :class="changeClass(overview.todayAIDialoguesChange || 0)">
            <span>较昨日 {{ changeIcon(overview.todayAIDialoguesChange || 0) }} {{ formatChange(overview.todayAIDialoguesChange || 0) }}</span>
          </div>
          <div class="stat-sub">累计对话: {{ overview.totalAIDialogues || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="OJ题目数" :value="overview.ojProblems || 0">
            <template #prefix>
              <el-icon class="stat-icon" color="#f56c6c"><Collection /></el-icon>
            </template>
          </el-statistic>
          <div class="stat-trend up">
            <span>面试题: {{ overview.interviewProblems || 0 }} | 算法: {{ overview.algorithms || 0 }}</span>
          </div>
          <div class="stat-sub">数据结构: {{ overview.dataStructures || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据趋势 + OJ判题分布 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>数据趋势（近7天）</span>
              <el-radio-group size="small" v-model="trendDays" @change="loadData">
                <el-radio-button value="7d">近7天</el-radio-button>
                <el-radio-button value="30d">近30天</el-radio-button>
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

    <!-- 模块访问量 + 热门题目 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <span>模块访问量</span>
          </template>
          <div ref="moduleChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <span>热门OJ题目 Top5</span>
          </template>
          <el-table :data="topOJProblems" stripe size="small" style="width: 100%">
            <el-table-column type="index" label="#" width="40" />
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="submissions" label="提交数" width="80" />
            <el-table-column prop="difficulty" label="难度" width="70">
              <template #default="{ row }">
                <el-tag v-if="row.difficulty === 'easy'" type="success" size="small">简单</el-tag>
                <el-tag v-else-if="row.difficulty === 'medium'" type="warning" size="small">中等</el-tag>
                <el-tag v-else type="danger" size="small">困难</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <span>热门面试题 Top5</span>
          </template>
          <el-table :data="topInterviewProblems" stripe size="small" style="width: 100%">
            <el-table-column type="index" label="#" width="40" />
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="views" label="浏览" width="70" />
            <el-table-column prop="category" label="分类" width="70" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>快捷入口</span>
          </template>
          <el-row :gutter="12">
            <el-col v-for="action in quickActions" :key="action.title" :span="8">
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

  &.up { color: #67c23a; }
  &.down { color: #f56c6c; }
}

.stat-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
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
