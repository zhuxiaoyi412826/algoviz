<template>
  <div class="coin-dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-revenue">
          <div class="stat-icon">💰</div>
          <div class="stat-info">
            <div class="stat-label">总硬币收入</div>
            <div class="stat-value">{{ stats.totalSpent || 0 }} 🪙</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-orders">
          <div class="stat-icon">📦</div>
          <div class="stat-info">
            <div class="stat-label">总购买次数</div>
            <div class="stat-value">{{ stats.totalPurchases || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-products">
          <div class="stat-icon">🏷️</div>
          <div class="stat-info">
            <div class="stat-label">商品总数</div>
            <div class="stat-value">{{ stats.productCount || 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-avg">
          <div class="stat-icon">📊</div>
          <div class="stat-info">
            <div class="stat-label">客单价</div>
            <div class="stat-value">{{ avgPrice }} 🪙</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 商品收入排行（柱状图） -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>🏆 商品收入排行</span>
              <el-button size="small" @click="loadData">刷新</el-button>
            </div>
          </template>
          <div class="bar-chart" v-loading="loading">
            <div v-for="item in productRevenue" :key="item.productId" class="bar-item">
              <div class="bar-label">
                <span class="bar-icon">{{ item.icon }}</span>
                <span class="bar-name">{{ item.productName }}</span>
              </div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: item.percent + '%' }">
                  <span class="bar-value">{{ item.totalRevenue }} 🪙</span>
                </div>
              </div>
              <div class="bar-count">{{ item.count }} 次</div>
            </div>
            <el-empty v-if="productRevenue.length === 0" description="暂无数据" />
          </div>
        </el-card>
      </el-col>

      <!-- 最近购买记录 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>🕐 最近购买记录</span>
            </div>
          </template>
          <el-table :data="recentPurchases" size="small" style="width: 100%" v-loading="loading">
            <el-table-column prop="username" label="用户" width="100" show-overflow-tooltip />
            <el-table-column prop="productName" label="商品" min-width="100" show-overflow-tooltip />
            <el-table-column prop="coinPrice" label="消耗" width="70" align="center">
              <template #default="{ row }">
                <span style="color: #f59e0b;">{{ row.coinPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="140" show-overflow-tooltip>
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getCoinStats, getCoinPurchases, getCoinProducts } from '@/api/coin'

const loading = ref(false)
const stats = ref<any>({})
const purchases = ref<any[]>([])
const products = ref<any[]>([])

const avgPrice = computed(() => {
  const total = stats.value.totalSpent || 0
  const count = stats.value.totalPurchases || 0
  return count > 0 ? Math.round(total / count) : 0
})

const recentPurchases = computed(() => purchases.value.slice(0, 10))

const productRevenue = computed(() => {
  const map = new Map<string, { productId: string; productName: string; icon: string; totalRevenue: number; count: number }>()
  purchases.value.forEach(p => {
    const existing = map.get(p.productId)
    if (existing) {
      existing.totalRevenue += p.coinPrice
      existing.count++
    } else {
      const product = products.value.find(pr => pr.productId === p.productId)
      map.set(p.productId, {
        productId: p.productId,
        productName: p.productName,
        icon: product?.icon || '🪙',
        totalRevenue: p.coinPrice,
        count: 1
      })
    }
  })
  const arr = Array.from(map.values()).sort((a, b) => b.totalRevenue - a.totalRevenue)
  const max = arr.length > 0 ? arr[0].totalRevenue : 1
  arr.forEach(item => { item.percent = Math.round((item.totalRevenue / max) * 100) })
  return arr
})

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

async function loadData() {
  loading.value = true
  try {
    const [statRes, purchRes, prodRes] = await Promise.all([
      getCoinStats(),
      getCoinPurchases(),
      getCoinProducts()
    ])
    stats.value = statRes?.data || statRes || {}
    purchases.value = purchRes?.purchases || purchRes?.data?.purchases || []
    products.value = prodRes?.products || prodRes?.data?.products || []
  } catch (e) {
    console.error('加载硬币数据失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.coin-dashboard { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }

.stat-card {
  display: flex;
  align-items: center;
  padding: 10px;
}
.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}
.stat-icon { font-size: 40px; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: 700; color: #303133; }

.stat-revenue .stat-icon { filter: hue-rotate(0deg); }
.stat-orders .stat-icon { filter: hue-rotate(120deg); }

/* 柱状图 */
.bar-chart { display: flex; flex-direction: column; gap: 14px; padding: 10px 0; }
.bar-item { display: flex; align-items: center; gap: 12px; }
.bar-label { width: 160px; display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.bar-icon { font-size: 18px; }
.bar-name { font-size: 13px; color: #606266; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bar-track { flex: 1; height: 28px; background: #f0f0f0; border-radius: 14px; overflow: hidden; }
.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #f59e0b, #f97316);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 10px;
  transition: width 0.5s ease;
  min-width: 60px;
}
.bar-value { font-size: 12px; color: white; font-weight: 600; }
.bar-count { width: 60px; font-size: 12px; color: #909399; text-align: right; flex-shrink: 0; }
</style>
