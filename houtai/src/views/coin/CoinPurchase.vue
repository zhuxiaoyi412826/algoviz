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

      <!-- 购买记录表格 -->
      <el-table :data="purchases" border stripe style="width: 100%" v-loading="loading">
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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getCoinPurchases, getCoinStats } from '@/api/coin'

const loading = ref(false)
const purchases = ref<any[]>([])
const stats = ref<any>({})

const avgPrice = computed(() => {
  const total = stats.value.totalSpent || 0
  const count = stats.value.totalPurchases || 0
  return count > 0 ? Math.round(total / count) : 0
})

async function loadData() {
  loading.value = true
  try {
    const [purchRes, statRes] = await Promise.all([
      getCoinPurchases(),
      getCoinStats()
    ])
    purchases.value = purchRes.data?.purchases || purchRes.purchases || []
    stats.value = statRes.data || statRes
  } catch (e) {
    console.error('加载硬币数据失败', e)
  } finally {
    loading.value = false
  }
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
