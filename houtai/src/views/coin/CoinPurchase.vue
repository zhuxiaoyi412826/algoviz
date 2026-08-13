<template>
  <div class="coin-purchase">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>🪙 硬币购买记录</span>
          <el-button type="primary" size="small" @click="loadData">刷新</el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="20" style="margin-bottom: 20px;">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">总消耗硬币</div>
            <div class="stat-value" style="color: #f59e0b;">{{ stats.totalSpent || 0 }} 🪙</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">总购买次数</div>
            <div class="stat-value">{{ stats.totalPurchases || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">商品总数</div>
            <div class="stat-value">{{ stats.productCount || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">客单价</div>
            <div class="stat-value">{{ avgPrice }} 🪙</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 搜索表单（结构同面试题目管理） -->
      <div class="filter-bar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="用户名/商品名称"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <!-- 购买记录表格 -->
      <el-table :data="purchases" border stripe style="width: 100%; margin-top: 16px" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="username" label="用户名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="productName" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="coinPrice" label="消耗硬币" width="110" align="center">
          <template #default="{ row }">
            <span style="color: #f59e0b; font-weight: 600;">{{ row.coinPrice }} 🪙</span>
          </template>
        </el-table-column>
        <el-table-column prop="coinBefore" label="购买前余额" width="110" align="center" />
        <el-table-column prop="coinAfter" label="购买后余额" width="110" align="center" />
        <el-table-column prop="createdAt" label="购买时间" min-width="160" />
      </el-table>

      <!-- 分页（结构同面试题目管理） -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[100, 200, 500]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="(p: number) => { page = p }"
          @size-change="(s: number) => { pageSize = s; page = 1 }"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getCoinPurchases, getCoinStats } from '@/api/coin'

const loading = ref(false)
const purchases = ref<any[]>([])
const stats = ref<any>({})

const page = ref(1)
const pageSize = ref(100)
const total = ref(0)

const searchForm = reactive({
  keyword: ''
})

const avgPrice = computed(() => {
  const totalSpent = stats.value.totalSpent || 0
  const count = stats.value.totalPurchases || 0
  return count > 0 ? Math.round(totalSpent / count) : 0
})

// 分页切换 -> 重新请求（同面试题目管理）
watch([page, pageSize], () => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const [purchRes, statRes] = await Promise.all([
      getCoinPurchases({
        keyword: searchForm.keyword || undefined,
        page: page.value,
        pageSize: pageSize.value
      }),
      getCoinStats()
    ])
    purchases.value = purchRes.purchases || []
    total.value = purchRes.total || 0
    stats.value = statRes || {}
  } catch (e: any) {
    console.error('加载硬币数据失败', e)
    ElMessage.error(e?.message || '加载硬币数据失败')
    purchases.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  page.value = 1
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.coin-purchase {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.stat-card {
  text-align: center;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}
</style>
