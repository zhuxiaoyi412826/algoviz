<script setup lang="ts">
/**
 * 算法题目设置 —— 独立编辑/新增页（复用 MdSplitEditor）
 * ------------------------------------------------------
 * 顶部：算法题目设置专属业务表单（题号 / 标题 / 难度 / 标签 / 状态 / 代码模板）
 * 中部：MdSplitEditor 通用 Markdown 实时分屏编辑器 → 编辑 form.description
 * 保存：写回 problem 表 (POST /api/problems 或 PUT /api/problems/{id})
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import MdSplitEditor from '@/components/common/MdSplitEditor.vue'

const route = useRoute()
const idParam = route.params.id as string | undefined
const isEdit = route.path.includes('/edit/') && !!idParam
const standalone = !!route.meta?.standalone

const API_BASE = 'http://localhost'
const title = computed(() => (isEdit ? '编辑算法题目' : '新增算法题目'))
const loading = ref(false)
const saving = ref(false)

const difficultyOptions = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' },
]
const statusOptions = [
  { label: '上线', value: 'online' },
  { label: '下线', value: 'offline' },
]
const tagOptions = [
  '数组', '链表', '树', '图', '动态规划', '回溯', '贪心', '分治', '排序', '查找', '字符串', '数学',
]
const languageOptions = [
  { label: 'Python', value: 'python' },
  { label: 'Java', value: 'java' },
  { label: 'C++', value: 'cpp' },
  { label: 'JavaScript', value: 'javascript' },
]

const form = reactive({
  id: '' as string | number,
  problemNo: '',
  title: '',
  difficulty: 'medium' as 'easy' | 'medium' | 'hard',
  tags: [] as string[],
  description: '',
  template: { java: '' } as Record<string, string>,
  templateLang: 'java' as string,
  status: 'offline' as 'online' | 'offline',
})

const descEditorRef = ref<InstanceType<typeof MdSplitEditor> | null>(null)

/* ---------- 工具函数 ---------- */
function notifyOpener(type: 'saved' | 'canceled', message?: string) {
  try {
    if (window.opener && !window.opener.closed) {
      window.opener.postMessage(
        { __from: 'problem-editor', type, message },
        window.location.origin,
      )
    }
  } catch { /* ignore */ }
}
function goBackOrClose() {
  try {
    if (window.opener && !window.opener.closed) {
      setTimeout(() => window.close(), 120)
      return
    }
  } catch { /* ignore */ }
  window.location.href = (import.meta as any).env?.BASE_URL
    ? `${(import.meta as any).env.BASE_URL.replace(/\/$/, '')}/content/problem`
    : '/content/problem'
}

/* ---------- 初始化 ---------- */
onMounted(async () => {
  document.title = `${title.value} - 算法可视化平台管理后台`
  if (isEdit) {
    loading.value = true
    try {
      const res = await fetch(`${API_BASE}/api/problems/${idParam}`)
      const data = await res.json()
      if (!data.success && !data.problem) {
        throw new Error(data.message || '加载题目失败')
      }
      const item = data.problem ?? data
      form.id = item.id
      form.problemNo = item.problemNo || ''
      form.title = item.title || ''
      form.difficulty = item.difficulty || 'medium'
      form.tags = item.tags ? (typeof item.tags === 'string' ? item.tags.split(',') : item.tags) : []
      form.description = item.description || ''
      form.status = item.status === 'ACTIVE' || item.status === 'online' ? 'online' : 'offline'
      // 解析 template
      try {
        const raw = item.template
        if (!raw || typeof raw !== 'object') {
          try {
            const parsed = JSON.parse(raw || '{}')
            if (typeof parsed === 'object' && parsed !== null) form.template = parsed
            else form.template = { java: raw || '' }
          } catch {
            form.template = { java: raw || '' }
          }
        } else {
          form.template = raw as Record<string, string>
        }
      } catch {
        form.template = { java: '' }
      }
      // 选中当前有内容的语言
      const tpl = form.template as Record<string, string>
      const langWithContent = Object.keys(tpl).find((k) => tpl[k])
      form.templateLang = langWithContent || 'java'
      if (!tpl[form.templateLang]) tpl[form.templateLang] = ''
    } catch (e: any) {
      ElMessage.error(e?.message || '加载题目失败')
    } finally {
      loading.value = false
    }
  }
})

/* ---------- 保存 ---------- */
async function handleSave() {
  if (!form.problemNo.trim() && !isEdit) {
    ElMessage.warning('请填写题号')
    return
  }
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!form.description.trim()) {
    ElMessage.warning('题目描述不能为空')
    return
  }
  const templateObj = form.template || {}
  const templateStr = Object.keys(templateObj).length > 0 ? JSON.stringify(templateObj) : ''
  const payload = {
    problemNo: form.problemNo,
    title: form.title,
    difficulty: form.difficulty,
    tags: (form.tags || []).join(','),
    description: form.description,
    template: templateStr,
    status: form.status === 'online' ? 'ACTIVE' : 'INACTIVE',
  }
  saving.value = true
  try {
    let res: Response
    if (isEdit) {
      res = await fetch(`${API_BASE}/api/problems/${form.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
    } else {
      res = await fetch(`${API_BASE}/api/problems`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
    }
    const data = await res.json()
    if (data.success) {
      const msg = isEdit ? '保存成功，题目已更新' : `新增成功，记录 ID=${data.id ?? ''}`
      notifyOpener('saved', msg)
      ElMessage.success(msg)
      goBackOrClose()
    } else {
      ElMessage.error(data.message || '保存失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  notifyOpener('canceled')
  goBackOrClose()
}
</script>

<template>
  <div class="editor-page" :class="{ standalone }">
    <div class="editor-topbar">
      <div class="editor-topbar__title">
        <span class="dot" />
        <span>算法题目设置 · {{ title }}</span>
        <span class="sub">
          · 左 Markdown 源码 / 右实时渲染 &nbsp;|&nbsp; 保存后自动关闭当前页面
        </span>
      </div>
      <div class="editor-topbar__actions">
        <el-button :icon="Close" @click="handleCancel">取消返回</el-button>
        <el-button type="primary" :icon="Check" :loading="saving || loading" @click="handleSave">
          保存并关闭
        </el-button>
      </div>
    </div>

    <div class="editor-body" v-loading="loading">
      <!-- 业务表单区：算法题目设置 -->
      <div class="form-meta">
        <el-form label-width="100px" size="default">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="题号" required>
                <el-input v-model="form.problemNo" :disabled="isEdit" placeholder="如 1500" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="难度" required>
                <el-radio-group v-model="form.difficulty">
                  <el-radio v-for="o in difficultyOptions" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="状态">
                <el-radio-group v-model="form.status">
                  <el-radio value="online">上线</el-radio>
                  <el-radio value="offline">下线</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="标签">
                <el-select v-model="form.tags" multiple placeholder="选择标签" style="width:100%">
                  <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="标题" required>
                <el-input v-model="form.title" placeholder="请输入题目标题，例如：两数之和" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- 题目描述：Markdown 分屏编辑器（核心字段） -->
      <div class="editor-block">
        <div class="editor-block__title">
          题目描述（Markdown · 左右分屏）
          <span class="hint">左栏输入源码，右栏即时渲染；中间分隔条支持左右拖拽；滚动自动同步</span>
        </div>
        <MdSplitEditor
          ref="descEditorRef"
          v-model="form.description"
          :height="640"
          placeholder="左侧输入 Markdown 源码，右侧即时渲染：\n\n## 题目描述\n\n给定一个整数数组 nums 和一个整数目标值 target……\n\n## 示例\n\n```\n输入：nums = [2,7,11,15], target = 9\n输出：[0,1]\n```"
        />
      </div>

      <!-- 代码模板：普通 textarea（代码不是 Markdown，使用等宽字体） -->
      <div class="editor-block">
        <div class="editor-block__title">
          代码模板（按语言切换）
          <span class="hint">原始代码文本，无需 Markdown 语法</span>
        </div>
        <el-select v-model="form.templateLang" style="width:160px; margin-bottom:10px">
          <el-option v-for="lang in languageOptions" :key="lang.value" :label="lang.label" :value="lang.value" />
        </el-select>
        <el-input
          v-model="(form.template as any)[form.templateLang]"
          type="textarea"
          :rows="10"
          placeholder="请粘贴该题目的代码模板骨架"
          style="font-family: 'Fira Code', Consolas, monospace; font-size: 13px"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.editor-page { min-height: 100vh; background: #f5f7fa; display: flex; flex-direction: column; }
.editor-topbar {
  position: sticky; top: 0; z-index: 50;
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 24px; background: #fff; border-bottom: 1px solid #e4e7ed;
}
.editor-topbar__title { display: flex; align-items: center; gap: 10px; font-size: 16px; font-weight: 600; color: #303133; flex-wrap: wrap; }
.editor-topbar__title .dot { width: 10px; height: 10px; border-radius: 50%; background: linear-gradient(135deg, #2563eb, #60a5fa); }
.editor-topbar__title .sub { font-size: 12px; color: #909399; font-weight: 400; }
.editor-body { flex: 1; padding: 18px 24px 40px; max-width: 1500px; width: 100%; margin: 0 auto; box-sizing: border-box; }
.form-meta {
  background: #fff; border-radius: 8px; padding: 16px 20px 4px;
  border: 1px solid #ebeef5; margin-bottom: 14px;
}
.editor-block {
  background: #fff; border-radius: 8px; padding: 14px 16px 18px;
  border: 1px solid #ebeef5; margin-bottom: 14px;
}
.editor-block__title {
  font-size: 13px; color: #606266; margin-bottom: 10px;
  padding-left: 2px; line-height: 1.6;
}
.editor-block__title .hint {
  display: inline-block; margin-left: 10px;
  color: #909399; font-size: 12px; font-weight: 400;
}
</style>
