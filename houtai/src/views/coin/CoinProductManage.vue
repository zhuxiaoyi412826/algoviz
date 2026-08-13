<template>
  <div class="coin-product-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>🪙 硬币商品管理</span>
          <el-button type="primary" size="small" @click="openDialog()">
            <el-icon><Plus /></el-icon> 新增商品
          </el-button>
        </div>
      </template>

      <el-table :data="products" border stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="icon" label="图标" width="60" align="center">
          <template #default="{ row }">
            <span style="font-size: 20px;">{{ row.icon }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="productId" label="商品编号" width="180" show-overflow-tooltip />
        <el-table-column prop="productName" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="coinPrice" label="硬币价格" width="100" align="center">
          <template #default="{ row }">
            <span style="color: #f59e0b; font-weight: 600;">{{ row.coinPrice }} 🪙</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="550px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品编号" v-if="!isEdit">
          <el-input v-model="form.productId" placeholder="如: coin-new-product" />
        </el-form-item>
        <el-form-item label="商品编号" v-else>
          <el-input v-model="form.productId" disabled />
        </el-form-item>
        <el-form-item label="商品名称">
          <el-input v-model="form.productName" placeholder="如: 算法高级指南" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="商品详细描述" />
        </el-form-item>
        <el-form-item label="硬币价格">
          <el-input-number v-model="form.coinPrice" :min="1" :max="99999" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="emoji 如: 📊" style="width: 120px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 120px;">
            <el-option label="上架" value="ACTIVE" />
            <el-option label="下架" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getCoinProducts, createCoinProduct, updateCoinProduct, deleteCoinProduct } from '@/api/coin'

const loading = ref(false)
const products = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  productId: '',
  productName: '',
  description: '',
  coinPrice: 100,
  icon: '🪙',
  status: 'ACTIVE'
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await getCoinProducts()
    products.value = res?.products || res?.data?.products || []
  } catch (e) {
    console.error('加载商品失败', e)
  } finally {
    loading.value = false
  }
}

function openDialog(row?: any) {
  if (row) {
    isEdit.value = true
    form.value = { ...row }
  } else {
    isEdit.value = false
    form.value = { productId: '', productName: '', description: '', coinPrice: 100, icon: '🪙', status: 'ACTIVE' }
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.productId || !form.value.productName) {
    ElMessage.warning('请填写商品编号和名称')
    return
  }
  try {
    if (isEdit.value) {
      await updateCoinProduct(form.value.productId, form.value)
      ElMessage.success('编辑成功')
    } else {
      await createCoinProduct(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('保存失败', e)
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除商品「${row.productName}」？`, '提示', { type: 'warning' })
    await deleteCoinProduct(row.productId)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    // 取消
  }
}

onMounted(loadData)
</script>

<style scoped>
.coin-product-manage { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
