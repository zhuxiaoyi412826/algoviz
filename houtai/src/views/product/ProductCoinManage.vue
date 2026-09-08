<template>
  <div class="coin-product-manage">
    <el-card shadow="never">
      <div class="filter-bar">
        <el-input v-model="filter.keyword" placeholder="商品名称关键词" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-select v-model="filter.category" placeholder="全部分类" clearable style="width: 150px">
          <el-option v-for="c in catOptions" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filter.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="上架" value="ACTIVE" />
          <el-option label="下架" value="INACTIVE" />
        </el-select>
        <el-date-picker v-model="filter.range" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
          start-placeholder="创建开始" end-placeholder="创建结束" style="width: 260px" />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增金币商品</el-button>
        <el-button text type="primary" @click="openGraphiql">GraphiQL 调试</el-button>
        <span class="type-tag">商品类型：金币</span>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="productId" label="商品编号" min-width="150" />
        <el-table-column prop="productName" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.category" type="warning" effect="plain">{{ row.category }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="硬币价格" width="110">
          <template #default="{ row }">
            <span class="price">{{ row.coinPrice }} 🪙</span>
          </template>
        </el-table-column>
        <el-table-column label="图标" width="90" align="center">
          <template #default="{ row }">
            <el-image
              v-if="isImg(row.icon)"
              :src="row.icon"
              :preview-src-list="[row.icon]"
              preview-teleported
              fit="contain"
              style="width: 36px; height: 36px"
            />
            <span v-else class="icon-emoji">{{ row.icon || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="165" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadList" @size-change="loadList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingProductId ? '编辑金币商品' : '新增金币商品'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="商品编号" required>
          <el-input v-model="form.productId" :disabled="!!editingProductId" placeholder="唯一编号，如 coin-dp-master" />
        </el-form-item>
        <el-form-item label="商品名称" required>
          <el-input v-model="form.productName" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="硬币价格" required>
          <el-input-number v-model="form.coinPrice" :min="0" :step="10" style="width: 200px" />
          <span class="tip">消耗的硬币数</span>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" clearable filterable allow-create placeholder="默认 coin" style="width: 200px">
            <el-option v-for="c in catOptions" :key="c" :label="c" :value="c" />
          </el-select>
          <div class="tip">可选分类字典中已启用的分类（默认 coin）</div>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="emoji（🪙）或图片 URL" style="width: 260px" />
          <el-image
            v-if="isImg(form.icon)"
            :src="form.icon"
            :preview-src-list="[form.icon]"
            preview-teleported
            fit="contain"
            style="width: 32px; height: 32px; margin-left: 8px; border: 1px solid #eee; border-radius: 6px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">上架</el-radio>
            <el-radio value="INACTIVE">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="商品描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'

interface CoinRow {
  id: string
  productId: string
  productName: string
  description: string
  coinPrice: number
  category: string
  icon: string
  status: string
  createdAt: string
  updatedAt: string
}

const loading = ref(false)
const saving = ref(false)
const list = ref<CoinRow[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

/** 已启用分类（分类字典 categoriesEnabled + 历史默认 coin） */
const cats = ref<{ name: string }[]>([])
const catOptions = computed<string[]>(() => {
  const set = new Set<string>(['coin'])
  cats.value.forEach((c) => c.name && set.add(c.name))
  return Array.from(set)
})

/** 是否为图片地址（http/data 开头） */
function isImg(icon: string): boolean {
  const v = icon || ''
  return v.indexOf('http') === 0 || v.indexOf('data:') === 0
}

const filter = reactive<{ keyword: string; category: string; status: string; range: string[] }>({
  keyword: '', category: '', status: '', range: []
})
const dialogVisible = ref(false)
const editingProductId = ref('')
const form = reactive({
  productId: '', productName: '', description: '', coinPrice: 0, category: 'coin', icon: '', status: 'ACTIVE'
})

async function gql(query: string, variables?: Record<string, unknown>) {
  const token = localStorage.getItem('token') || ''
  const res = await fetch('/graphql', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', satoken: token, Authorization: token ? 'Bearer ' + token : '' },
    body: JSON.stringify({ query, variables: variables || {} })
  })
  if (!res.ok) {
    let msg = 'GraphQL 请求失败(' + res.status + ')'
    try { const j = await res.json(); if (j && j.message) msg = j.message } catch { /* ignore */ }
    throw new Error(msg)
  }
  const body = await res.json()
  if (body.errors && body.errors.length) throw new Error(body.errors[0].message || 'GraphQL 执行失败')
  return body.data
}

const CATEGORY_Q = '{ categoriesEnabled { name } }'
const LIST_Q = `query($keyword:String,$category:String,$status:String,$startDate:String,$endDate:String,$page:Int!,$size:Int!){
  coinProducts(keyword:$keyword,category:$category,status:$status,startDate:$startDate,endDate:$endDate,page:$page,size:$size){
    total list { id productId productName description coinPrice category icon status createdAt updatedAt }
  }
}`

async function loadCategories() {
  try {
    const d = await gql(CATEGORY_Q)
    cats.value = d.categoriesEnabled || []
  } catch {
    cats.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const range = filter.range || []
    const d = await gql(LIST_Q, {
      keyword: filter.keyword || null,
      category: filter.category || null,
      status: filter.status || null,
      startDate: range[0] || null,
      endDate: range[1] || null,
      page: page.value,
      size: size.value
    })
    const p = d.coinProducts
    list.value = p.list || []
    total.value = p.total || 0
  } catch (e) {
    ElMessage.error('加载失败：' + (e as Error).message)
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.value = 1; loadList() }
function handleReset() {
  filter.keyword = ''; filter.category = ''; filter.status = ''; filter.range = []
  page.value = 1; loadList()
}
function resetForm() {
  form.productId = ''; form.productName = ''; form.description = ''
  form.coinPrice = 0; form.category = 'coin'; form.icon = ''; form.status = 'ACTIVE'
}
function openCreate() { editingProductId.value = ''; resetForm(); dialogVisible.value = true }
function openEdit(row: CoinRow) {
  editingProductId.value = row.productId
  form.productId = row.productId
  form.productName = row.productName
  form.description = row.description || ''
  form.coinPrice = row.coinPrice
  form.category = row.category || 'coin'
  form.icon = row.icon || ''
  form.status = row.status
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.productName.trim()) return ElMessage.error('请填写商品名称')
  saving.value = true
  try {
    const input = {
      productId: form.productId.trim(),
      productName: form.productName.trim(),
      description: form.description || '',
      coinPrice: form.coinPrice,
      category: form.category || 'coin',
      icon: form.icon || '',
      status: form.status
    }
    if (editingProductId.value) {
      await gql('mutation($productId:String!,$input:CoinProductInput!){ updateCoinProduct(productId:$productId,input:$input){ id } }', {
        productId: editingProductId.value, input
      })
      ElMessage.success('修改成功')
    } else {
      await gql('mutation($input:CoinProductInput!){ createCoinProduct(input:$input){ id } }', { input })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error('保存失败：' + (e as Error).message)
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: CoinRow) {
  try {
    await ElMessageBox.confirm(
      `确认删除金币商品「${row.productName}」？已有购买记录的商品会被拦截，请改为「下架」。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch { return }
  try {
    await gql('mutation($productId:String!){ deleteCoinProduct(productId:$productId) }', { productId: row.productId })
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    ElMessage.error('删除失败：' + (e as Error).message)
  }
}

function openGraphiql() { window.open('/graphiql', '_blank') }

onMounted(() => {
  loadList()
  loadCategories()
})
</script>

<style scoped lang="scss">
.filter-bar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; align-items: center; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.price { color: #e6a23c; font-weight: 600; }
.icon-emoji { font-size: 20px; }
.tip { margin-left: 0; width: 100%; color: var(--color-text-secondary); font-size: 12px; }
.type-tag { color: #e6a23c; font-weight: 600; margin-left: auto; }
</style>
