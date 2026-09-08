<template>
  <div class="product-manage">
    <el-card shadow="never">
      <!-- 筛选区 -->
      <div class="filter-bar">
        <el-input
          v-model="filter.keyword"
          placeholder="商品名称关键词"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="filter.category" placeholder="全部分类" clearable style="width: 160px">
          <el-option v-for="c in categories" :key="c.name" :label="c.name" :value="c.name" />
        </el-select>
        <el-date-picker
          v-model="filter.range"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="创建开始"
          end-placeholder="创建结束"
          style="width: 280px"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增商品</el-button>
        <router-link to="/product/category">
          <el-button plain>分类维护</el-button>
        </router-link>
        <el-button text type="primary" @click="openGraphiql">GraphiQL 调试</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="productId" label="商品编号" min-width="130" />
        <el-table-column prop="productName" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="分类" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.category" type="warning" effect="plain">{{ row.category }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="100">
          <template #default="{ row }">
            <span class="price">¥{{ fen2yuan(row.price) }}</span>
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
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="资料下载链接" width="220">
          <template #default="{ row }">
            <a v-if="row.materialUrl" :href="row.materialUrl" target="_blank" rel="noopener" class="material-link">{{ row.materialUrl }}</a>
            <span v-else class="material-empty">未配置</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165" />
        <el-table-column prop="updatedAt" label="更新时间" width="165" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadList"
          @size-change="loadList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑商品' : '新增商品'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品编号" required>
          <el-input v-model="form.productId" placeholder="唯一编号，如 algo-notes-v2" />
        </el-form-item>
        <el-form-item label="商品名称" required>
          <el-input v-model="form.productName" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="价格（元）" required>
          <el-input-number v-model="form.priceYuan" :min="0" :precision="2" :step="1" style="width: 200px" />
          <span class="tip">支付金额单位（内部按分存储）</span>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" clearable filterable allow-create placeholder="选择或输入新分类" style="width: 220px">
            <el-option v-for="c in categories" :key="c.name" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="emoji 或图片 URL（http/https）" style="width: 300px" />
          <el-image
            v-if="isImg(form.icon)"
            :src="form.icon"
            :preview-src-list="[form.icon]"
            preview-teleported
            fit="contain"
            style="width: 36px; height: 36px; margin-left: 10px; border: 1px solid #eee; border-radius: 6px"
          />
          <div class="tip">支持 emoji（如 🛒）或图片链接（列表/前台会自动预览）</div>
        </el-form-item>
        <el-form-item label="资料下载链接">
          <el-input v-model="form.materialUrl" placeholder="购买成功后随邮件发放的资料链接（http/https），留空则不发送链接" />
          <div class="tip">仅在用户支付成功后的订单邮件中出现，公开商品页不会泄露该链接</div>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'

interface Category {
  id: string
  name: string
  sort: number
  status: number
}
interface ProductRow {
  id: string
  productId: string
  productName: string
  description: string
  price: number
  category: string
  icon: string
  materialUrl: string
  createdAt: string
  updatedAt: string
}

const loading = ref(false)
const saving = ref(false)
const list = ref<ProductRow[]>([])
const categories = ref<Category[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const filter = reactive<{ keyword: string; category: string; range: string[] }>({
  keyword: '',
  category: '',
  range: []
})

const dialogVisible = ref(false)
const editingId = ref('')
const form = reactive({
  productId: '',
  productName: '',
  description: '',
  priceYuan: 0,
  category: '',
  icon: '',
  materialUrl: ''
})

/** GraphQL 执行器：统一带 Sa-Token 头 */
async function gql(query: string, variables?: Record<string, unknown>) {
  const token = localStorage.getItem('token') || ''
  const res = await fetch('/graphql', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      satoken: token,
      Authorization: token ? 'Bearer ' + token : ''
    },
    body: JSON.stringify({ query, variables: variables || {} })
  })
  if (!res.ok) {
    let msg = 'GraphQL 请求失败(' + res.status + ')'
    try { const j = await res.json(); if (j && j.message) msg = j.message } catch { /* ignore */ }
    throw new Error(msg)
  }
  const body = await res.json()
  if (body.errors && body.errors.length) {
    throw new Error(body.errors[0].message || 'GraphQL 执行失败')
  }
  return body.data
}

const CATEGORY_Q = '{ categoriesEnabled { name } }'
const LIST_Q = `query($keyword:String,$category:String,$startDate:String,$endDate:String,$page:Int!,$size:Int!){
  products(keyword:$keyword,category:$category,startDate:$startDate,endDate:$endDate,page:$page,size:$size){ total list { id productId productName description price category icon materialUrl createdAt updatedAt } }
}`

async function loadCategories() {
  try {
    const d = await gql(CATEGORY_Q)
    categories.value = d.categoriesEnabled || []
  } catch (e) {
    categories.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const range = filter.range || []
    const d = await gql(LIST_Q, {
      keyword: filter.keyword || null,
      category: filter.category || null,
      startDate: range[0] || null,
      endDate: range[1] || null,
      page: page.value,
      size: size.value
    })
    const p = d.products
    list.value = p.list || []
    total.value = p.total || 0
  } catch (e) {
    ElMessage.error('加载失败：' + (e as Error).message)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadList()
}
function handleReset() {
  filter.keyword = ''
  filter.category = ''
  filter.range = []
  page.value = 1
  loadList()
}

function resetForm() {
  form.productId = ''
  form.productName = ''
  form.description = ''
  form.priceYuan = 0
  form.category = ''
  form.icon = ''
  form.materialUrl = ''
}
function openCreate() {
  editingId.value = ''
  resetForm()
  dialogVisible.value = true
}
function openEdit(row: ProductRow) {
  editingId.value = row.id
  form.productId = row.productId
  form.productName = row.productName
  form.description = row.description || ''
  form.priceYuan = Number((row.price / 100).toFixed(2))
  form.category = row.category || ''
  form.icon = row.icon || ''
  form.materialUrl = row.materialUrl || ''
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.productId.trim()) return ElMessage.error('请填写商品编号')
  if (!form.productName.trim()) return ElMessage.error('请填写商品名称')
  saving.value = true
  try {
    const input = {
      productId: form.productId.trim(),
      productName: form.productName.trim(),
      description: form.description || '',
      price: Math.round(form.priceYuan * 100),
      category: form.category || '',
      icon: form.icon || '',
      materialUrl: form.materialUrl.trim() || null
    }
    if (editingId.value) {
      await gql(`mutation($id:ID!,$input:ProductInput!){ updateProduct(id:$id,input:$input){ id } }`, { id: editingId.value, input })
      ElMessage.success('修改成功')
    } else {
      await gql(`mutation($input:ProductInput!){ createProduct(input:$input){ id } }`, { input })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadList()
    loadCategories()
  } catch (e) {
    ElMessage.error('保存失败：' + (e as Error).message)
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: ProductRow) {
  try {
    await ElMessageBox.confirm(
      `确认删除商品「${row.productName}」？若已被订单引用会被拦截，请改用分类维护/停售。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await gql(`mutation($id:ID!){ deleteProduct(id:$id) }`, { id: row.id })
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    ElMessage.error('删除失败：' + (e as Error).message)
  }
}

function fen2yuan(fen: number): string {
  return ((fen || 0) / 100).toFixed(2)
}

/** 是否为图片地址（http/data 开头） */
function isImg(icon: string): boolean {
  const v = icon || ''
  return v.indexOf('http') === 0 || v.indexOf('data:') === 0
}

function openGraphiql() {
  window.open('/graphiql', '_blank')
}

onMounted(() => {
  loadList()
  loadCategories()
})
</script>

<style scoped lang="scss">
.filter-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
  align-items: center;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
.price {
  color: #f56c6c;
  font-weight: 600;
}
.icon-emoji {
  font-size: 20px;
}
.material-link {
  color: #409eff;
  text-decoration: none;
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
.material-link:hover {
  text-decoration: underline;
}
.material-empty {
  color: var(--color-text-secondary);
  font-size: 12px;
}
.tip {
  width: 100%;
  margin-left: 0;
  color: var(--color-text-secondary);
  font-size: 12px;
}
</style>
