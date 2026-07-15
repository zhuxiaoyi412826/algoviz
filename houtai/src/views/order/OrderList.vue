<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { 
  ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElMessage, 
  ElPagination, ElInput, ElSelect, ElOption, ElMessageBox, ElForm,
  ElFormItem
} from 'element-plus'
import { Search, Refresh, Edit, Check, Delete } from '@element-plus/icons-vue'

interface Order {
  id: number
  orderId: string
  productId: string
  productName: string
  amount: number
  userId: number
  status: string
  refundStatus: string
  refundReason: string
  username: string
  email: string
  createTime: string
  payTime: string
}

const tableData = ref<Order[]>([])
const loading = ref(false)
const detailVisible = ref(false)
const refundDialogVisible = ref(false)
const currentOrder = ref<Order | null>(null)
const rejectReason = ref('')

const searchForm = reactive({ keyword: '', status: '', refundStatus: '' })
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const stats = ref({
  totalCount: 0,
  pendingCount: 0,
  successCount: 0,
  appliedRefundCount: 0,
  refundedCount: 0,
  totalAmount: 0,
  successAmount: 0
})

onMounted(() => {
  loadData()
  loadStats()
})

const loadData = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (searchForm.keyword) params.append('keyword', searchForm.keyword)
    if (searchForm.status) params.append('status', searchForm.status)
    if (searchForm.refundStatus) params.append('refundStatus', searchForm.refundStatus)
    
    const response = await fetch(`http://localhost/api/orders?${params.toString()}`)
    const data = await response.json()
    
    if (data.success) {
      tableData.value = data.orders.map((item: any) => ({
        id: item.id,
        orderId: item.orderId,
        productId: item.productId,
        productName: item.productName,
        amount: item.amount,
        userId: item.userId,
        status: item.status,
        refundStatus: item.refundStatus || 'NONE',
        refundReason: item.refundReason || '',
        username: item.username || '',
        email: item.email || '',
        createTime: item.createTime,
        payTime: item.payTime
      }))
      total.value = data.count
    } else {
      ElMessage.error('加载订单失败')
    }
  } catch (error) {
    console.error('加载订单失败:', error)
    ElMessage.error('加载订单失败')
  } finally { loading.value = false }
}

const loadStats = async () => {
  try {
    const response = await fetch('http://localhost/api/orders/stats')
    const data = await response.json()
    if (data.success) {
      stats.value = data
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  searchForm.refundStatus = ''
  loadData()
}

const handleView = (row: Order) => {
  currentOrder.value = row
  detailVisible.value = true
}

const handleApproveRefund = (row: Order) => {
  currentOrder.value = row
  ElMessageBox.confirm('确定要同意该退款申请吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      const response = await fetch(`http://localhost/api/orders/${row.orderId}/refund/approve`, {
        method: 'PUT'
      })
      const data = await response.json()
      if (data.success) {
        ElMessage.success('退款成功')
        loadData()
        loadStats()
      } else {
        ElMessage.error(data.message || '退款失败')
      }
    } catch (error) {
      ElMessage.error('退款失败')
    }
  }).catch(() => {})
}

const handleRejectRefund = (row: Order) => {
  currentOrder.value = row
  rejectReason.value = ''
  refundDialogVisible.value = true
}

const confirmReject = async () => {
  if (!currentOrder.value) return
  
  try {
    const response = await fetch(`http://localhost/api/orders/${currentOrder.value.orderId}/refund/reject`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reason: rejectReason.value })
    })
    const data = await response.json()
    if (data.success) {
      ElMessage.success('已拒绝退款申请')
      refundDialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    'PENDING': '待支付',
    'SUCCESS': '已支付',
    'REFUNDED': '已退款'
  }
  return map[status] || status
}

const getStatusType = (status: string): 'success' | 'warning' | 'info' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info'> = {
    'PENDING': 'warning',
    'SUCCESS': 'success',
    'REFUNDED': 'info'
  }
  return map[status] || 'info'
}

const getRefundStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    'NONE': '无',
    'APPLIED': '待审核',
    'REFUNDED': '已退款',
    'REJECTED': '已拒绝',
    'FAILED': '退款失败'
  }
  return map[status] || status
}

const getRefundStatusType = (status: string): 'success' | 'warning' | 'info' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    'NONE': 'info',
    'APPLIED': 'warning',
    'REFUNDED': 'success',
    'REJECTED': 'danger',
    'FAILED': 'danger'
  }
  return map[status] || 'info'
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>订单管理</h2>
    </div>

    <div class="stats-card">
      <div class="stat-item">
        <div class="stat-value">{{ stats.totalCount }}</div>
        <div class="stat-label">订单总数</div>
      </div>
      <div class="stat-item">
        <div class="stat-value warning">{{ stats.pendingCount }}</div>
        <div class="stat-label">待支付</div>
      </div>
      <div class="stat-item">
        <div class="stat-value success">{{ stats.successCount }}</div>
        <div class="stat-label">已支付</div>
      </div>
      <div class="stat-item">
        <div class="stat-value danger">{{ stats.appliedRefundCount }}</div>
        <div class="stat-label">待退款</div>
      </div>
      <div class="stat-item">
        <div class="stat-value info">¥{{ (stats.successAmount / 100).toFixed(2) }}</div>
        <div class="stat-label">总收入</div>
      </div>
    </div>

    <div class="card-container">
      <div class="filter-bar">
        <el-input 
          v-model="searchForm.keyword" 
          placeholder="订单号/商品名称/用户名" 
          clearable 
          style="width: 200px" 
          @keyup.enter="handleSearch" 
        />
        <el-select v-model="searchForm.status" placeholder="订单状态" clearable style="width: 120px">
          <el-option label="待支付" value="PENDING" />
          <el-option label="已支付" value="SUCCESS" />
          <el-option label="已退款" value="REFUNDED" />
        </el-select>
        <el-select v-model="searchForm.refundStatus" placeholder="退款状态" clearable style="width: 120px">
          <el-option label="待审核" value="APPLIED" />
          <el-option label="已退款" value="REFUNDED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe style="margin-top:16px">
        <el-table-column prop="orderId" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="productName" label="商品名称" min-width="150" />
        <el-table-column prop="amount" label="金额" width="100">
          <template #default="{row}">¥{{ (row.amount / 100).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="订单状态" width="100">
          <template #default="{row}">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="refundStatus" label="退款状态" width="100">
          <template #default="{row}">
            <el-tag :type="getRefundStatusType(row.refundStatus)" size="small">{{ getRefundStatusLabel(row.refundStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="150" />
        <el-table-column label="操作" width="200">
          <template #default="{row}">
            <el-button type="primary" link :icon="Edit" @click="handleView(row)">详情</el-button>
            <template v-if="row.refundStatus === 'APPLIED'">
              <el-button type="success" link :icon="Check" @click="handleApproveRefund(row)">同意退款</el-button>
              <el-button type="danger" link :icon="Delete" @click="handleRejectRefund(row)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination 
          v-model:current-page="page" 
          v-model:page-size="pageSize" 
          :total="total" 
          layout="total,sizes,prev,pager,next" 
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号">{{ currentOrder.orderId }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ currentOrder.productName }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ (currentOrder.amount / 100).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getStatusType(currentOrder.status)">{{ getStatusLabel(currentOrder.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="退款状态">
          <el-tag :type="getRefundStatusType(currentOrder.refundStatus)">{{ getRefundStatusLabel(currentOrder.refundStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="退款原因">{{ currentOrder.refundReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentOrder.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentOrder.email }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ currentOrder.payTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="refundDialogVisible" title="拒绝退款申请" width="400px">
      <el-form>
        <el-form-item label="拒绝原因" required>
          <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.stats-card {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-item {
  flex: 1;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-value.warning { color: #e6a23c; }
.stat-value.success { color: #67c23a; }
.stat-value.danger { color: #f56c6c; }
.stat-value.info { color: #409eff; }

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}
</style>
