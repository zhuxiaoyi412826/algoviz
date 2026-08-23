<script setup lang="ts">
/**
 * 题解 —— 独立编辑页（管理员编辑用户题解，复用 MdSplitEditor）
 * --------------------------------------------------------------
 * 顶部：题解专属业务表单（题解标题 / 解题格式 / 复杂度 / 代码语言 / 思路 / 代码）
 * 中部：MdSplitEditor 通用 Markdown 实时分屏编辑器 → 编辑 form.process（解题过程）
 * 保存：写回 oj_solution 表 (PUT /api/admin/solutions/{id})
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import MdSplitEditor from '@/components/common/MdSplitEditor.vue'
import request from '@/api/request'

const route = useRoute()
const idParam = route.params.id as string | undefined
const standalone = !!route.meta?.standalone

const title = computed(() => '编辑题解')
const loading = ref(false)
const saving = ref(false)

const formatOptions = [
  { label: '暴力解法', value: '暴力' },
  { label: '双指针', value: '双指针' },
  { label: '哈希表', value: '哈希' },
  { label: '动态规划', value: 'DP' },
  { label: '回溯', value: '回溯' },
  { label: '贪心', value: '贪心' },
  { label: '分治', value: '分治' },
  { label: '其他', value: '其他' },
]
const codeLangOptions = [
  { label: 'Java', value: 'Java' },
  { label: 'Python', value: 'Python' },
  { label: 'C++', value: 'C++' },
  { label: 'JavaScript', value: 'JavaScript' },
  { label: 'Go', value: 'Go' },
  { label: 'C#', value: 'C#' },
]

const form = reactive({
  id: '' as string | number | undefined,
  problemId: '' as string | number | undefined,
  problemTitle: '',
  username: '',
  title: '',
  format: '',
  idea: '',
  process: '',
  complexity: '',
  codeLang: 'Java',
  code: '',
})

const processEditorRef = ref<InstanceType<typeof MdSplitEditor> | null>(null)

/* ---------- 工具函数 ---------- */
function notifyOpener(type: 'saved' | 'canceled', message?: string) {
  try {
    if (window.opener && !window.opener.closed) {
      window.opener.postMessage(
        { __from: 'solution-editor', type, message },
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
    ? `${(import.meta as any).env.BASE_URL.replace(/\/$/, '')}/audit/solution-review`
    : '/audit/solution-review'
}

/* ---------- 初始化 ---------- */
onMounted(async () => {
  document.title = `${title.value} - 算法可视化平台管理后台`
  if (!idParam) {
    ElMessage.error('缺少题解 ID 参数')
    return
  }
  loading.value = true
  try {
    const r: any = await request.get(`/admin/solutions/${idParam}`)
    const s: any = r ?? {}
    form.id = s.id
    form.problemId = s.problemId
    form.problemTitle = s.problemTitle || ''
    form.username = s.username || ''
    form.title = s.title || ''
    form.format = s.format || ''
    form.idea = s.idea || ''
    form.process = s.process || ''
    form.complexity = s.complexity || ''
    form.codeLang = s.codeLang || 'Java'
    form.code = s.code || ''
  } catch (e: any) {
    ElMessage.error(e?.message || '加载题解失败')
  } finally {
    loading.value = false
  }
})

/* ---------- 保存 ---------- */
async function handleSave() {
  if (!form.title.trim()) {
    ElMessage.warning('题解标题不能为空')
    return
  }
  if (!form.process.trim()) {
    ElMessage.warning('解题过程（Markdown）不能为空')
    return
  }
  saving.value = true
  try {
    // 与后端实体 OJSolution 字段对齐
    const payload = {
      title: form.title,
      format: form.format,
      idea: form.idea,
      process: form.process,
      complexity: form.complexity,
      codeLang: form.codeLang,
      code: form.code,
    }
    await request.put(`/admin/solutions/${form.id}`, payload)
    const msg = '保存成功，题解已更新（已重新审核）'
    notifyOpener('saved', msg)
    ElMessage.success(msg)
    goBackOrClose()
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
        <span>题解 · {{ title }}</span>
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
      <!-- 题解专属业务表单 -->
      <div class="form-meta">
        <el-form label-width="100px" size="default">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="所属题目">
                <el-input :model-value="form.problemTitle" disabled placeholder="（自动带入，不允许修改）" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="作者">
                <el-input :model-value="form.username" disabled placeholder="（自动带入）" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="解题格式">
                <el-select v-model="form.format" placeholder="如：双指针/DP/暴力" style="width:100%" clearable>
                  <el-option v-for="o in formatOptions" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="复杂度">
                <el-input v-model="form.complexity" placeholder="如：时间 O(n) / 空间 O(1)" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="题解标题" required>
                <el-input v-model="form.title" placeholder="一句话概括解法，例如：哈希表一次遍历" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="思路提示">
                <el-input
                  v-model="form.idea"
                  type="textarea"
                  :rows="2"
                  placeholder="简述核心思路，纯文本或 Markdown 均可，前台展示在「思路」一栏"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- 解题过程：Markdown 分屏编辑器（核心大字段） -->
      <div class="editor-block">
        <div class="editor-block__title">
          解题过程（Markdown · 左右分屏）
          <span class="hint">左栏输入 Markdown 源码，右栏即时渲染；中间分隔条支持左右拖拽；滚动自动同步</span>
        </div>
        <MdSplitEditor
          ref="processEditorRef"
          v-model="form.process"
          :height="680"
          placeholder="左侧输入 Markdown 源码，右侧即时渲染：\n\n## 算法思路\n\n使用哈希表记录已遍历过的数字及其下标……\n\n## 代码解释\n\n- 第 1 步：初始化 Map\n- 第 2 步：遍历数组\n\n```java\nclass Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // ...\n    }\n}\n```"
        />
      </div>

      <!-- 代码块：普通 textarea（等宽字体） -->
      <div class="editor-block">
        <div class="editor-block__title">
          题解代码
          <span class="hint">原始代码文本，非 Markdown</span>
        </div>
        <el-select v-model="form.codeLang" style="width:160px; margin-bottom:10px">
          <el-option v-for="o in codeLangOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-input
          v-model="form.code"
          type="textarea"
          :rows="12"
          placeholder="粘贴可运行的完整解法代码"
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
.editor-topbar__title .dot { width: 10px; height: 10px; border-radius: 50%; background: linear-gradient(135deg, #059669, #34d399); }
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
