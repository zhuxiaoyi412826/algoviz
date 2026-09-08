<template>
  <div class="category-manage">
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增分类</el-button>
        <el-button text type="primary" @click="openGraphiql">GraphiQL 调试</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="180" />
        <el-table-column prop="sort" label="排序值" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165" />
        <el-table-column prop="updatedAt" label="更新时间" width="165" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="分类名称" required>
          <el-input v-model="form.name" placeholder="与 product.category 一致，如：笔记 / 教程 / 项目" />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sort" :min="0" />
          <span class="tip">越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
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
import { Plus } from '@element-plus/icons-vue'

interface CategoryRow {
  id: string
  name: string
  sort: number
  status: number
  createdAt: string
  updatedAt: string
}

const loading = ref(false)
const saving = ref(false)
const list = ref<CategoryRow[]>([])
const dialogVisible = ref(false)
const editingId = ref('')
const form = reactive<{ name: string; sort: number; status: number }>({
  name: '',
  sort: 0,
  status: 1
})

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

const LIST_Q = '{ categories { id name sort status createdAt updatedAt } }'

async function loadList() {
  loading.value = true
  try {
    const d = await gql(LIST_Q)
    list.value = d.categories || []
  } catch (e) {
    ElMessage.error('加载失败：' + (e as Error).message)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.name = ''
  form.sort = 0
  form.status = 1
}
function openCreate() {
  editingId.value = ''
  resetForm()
  dialogVisible.value = true
}
function openEdit(row: CategoryRow) {
  editingId.value = row.id
  form.name = row.name
  form.sort = row.sort
  form.status = row.status
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name.trim()) return ElMessage.error('请填写分类名称')
  saving.value = true
  try {
    if (editingId.value) {
      await gql('mutation($id:ID!,$name:String,$sort:Int,$status:Int){ updateCategory(id:$id,name:$name,sort:$sort,status:$status){ id } }', {
        id: editingId.value, name: form.name.trim(), sort: form.sort, status: form.status
      })
      ElMessage.success('修改成功')
    } else {
      await gql('mutation($name:String!,$sort:Int,$status:Int){ createCategory(name:$name,sort:$sort,status:$status){ id } }', {
        name: form.name.trim(), sort: form.sort, status: form.status
      })
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

async function handleDelete(row: CategoryRow) {
  try {
    await ElMessageBox.confirm(`确认删除分类「${row.name}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await gql('mutation($id:ID!){ deleteCategory(id:$id) }', { id: row.id })
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    ElMessage.error('删除失败：' + (e as Error).message)
  }
}

function openGraphiql() {
  window.open('/graphiql', '_blank')
}

onMounted(loadList)
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  align-items: center;
}
.tip {
  margin-left: 8px;
  color: var(--color-text-secondary);
  font-size: 12px;
}
</style>
