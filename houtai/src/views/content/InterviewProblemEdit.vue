<script setup lang="ts">
/**
 * 面试题目设置 —— 独立编辑/新增页（复用 MdSplitEditor）
 * ----------------------------------------------------------------
 * 顶部：面试题目设置专属业务表单（题号 / 标题 / 难度 / 分类 / 标签 / 输入·输出格式 / 高频 / 状态）
 * 中部：
 *   MdSplitEditor 1 → 编辑 form.description（题目描述，Markdown）
 *   MdSplitEditor 2 → 编辑 form.solution（参考答案，Markdown）
 * 保存：写回 interview_problem 表（调 api/interview 的 createProblem / updateProblem）
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import MdSplitEditor from '@/components/common/MdSplitEditor.vue'
import {
  getProblem as interviewGet,
  createProblem,
  updateProblem,
  convertToFront,
  convertToBackend,
} from '@/api/interview'

const route = useRoute()
const idParam = route.params.id as string | undefined
const isEdit = route.path.includes('/edit/') && !!idParam
const standalone = !!route.meta?.standalone

const title = computed(() => (isEdit ? '编辑面试题目' : '新增面试题目'))
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
  '数组', '链表', '栈', '队列', '哈希表', '树', '二叉树', '图', '堆', '并查集',
  '动态规划', '回溯', '贪心', '分治', '排序', '查找', '二分查找', '字符串', '数学',
  '位运算', '模拟', '递归', '广度优先搜索', '深度优先搜索', '滑动窗口', '前缀和', '双指针', '设计',
]
const categoryOptions = [
  { label: '数组', value: '数组' },
  { label: '链表', value: '链表' },
  { label: '栈', value: '栈' },
  { label: '队列', value: '队列' },
  { label: '树', value: '树' },
  { label: '图', value: '图' },
  { label: '动态规划', value: '动态规划' },
  { label: '回溯', value: '回溯' },
  { label: '贪心', value: '贪心' },
  { label: '分治', value: '分治' },
  { label: '排序', value: '排序' },
  { label: '查找', value: '查找' },
  { label: '字符串', value: '字符串' },
  { label: '数学', value: '数学' },
  { label: '设计', value: '设计' },
]

const form = reactive({
  id: undefined as number | undefined,
  problemNo: '',
  title: '',
  difficulty: 'medium' as 'easy' | 'medium' | 'hard',
  tags: [] as string[],
  category: '',
  description: '',
  inputFormat: '',
  outputFormat: '',
  solution: '',
  isFrequent: false,
  status: 'offline' as 'online' | 'offline',
})

const descEditorRef = ref<InstanceType<typeof MdSplitEditor> | null>(null)
const solEditorRef = ref<InstanceType<typeof MdSplitEditor> | null>(null)

/* ---------- 工具函数 ---------- */
function notifyOpener(type: 'saved' | 'canceled', message?: string) {
  try {
    if (window.opener && !window.opener.closed) {
      window.opener.postMessage(
        { __from: 'interview-editor', type, message },
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
    ? `${(import.meta as any).env.BASE_URL.replace(/\/$/, '')}/content/interview-problem`
    : '/content/interview-problem'
}

/* ---------- 初始化 ---------- */
onMounted(async () => {
  document.title = `${title.value} - 算法可视化平台管理后台`
  if (isEdit) {
    loading.value = true
    try {
      const raw: any = await interviewGet(Number(idParam))
      const item = convertToFront(raw) as any
      form.id = item.id
      form.problemNo = item.problemNo || ''
      form.title = item.title || ''
      form.difficulty = item.difficulty || 'medium'
      form.tags = Array.isArray(item.tags) ? item.tags : []
      form.category = item.category || ''
      form.description = item.description || ''
      form.inputFormat = item.inputFormat || ''
      form.outputFormat = item.outputFormat || ''
      form.solution = item.solution || ''
      form.isFrequent = !!item.isFrequent
      form.status = item.status || 'offline'
    } catch (e: any) {
      ElMessage.error(e?.message || '加载面试题失败')
    } finally {
      loading.value = false
    }
  }
})

/* ---------- 保存 ---------- */
async function handleSave() {
  if (!form.title.trim()) { ElMessage.warning('请输入标题'); return }
  if (!form.category)    { ElMessage.warning('请选择分类'); return }
  if (!form.description.trim()) { ElMessage.warning('题目描述（Markdown）不能为空'); return }

  saving.value = true
  try {
    const payload = convertToBackend(form as any)
    if (form.id) {
      await updateProblem(form.id, payload as any)
    } else {
      await createProblem(payload as any)
    }
    const msg = form.id ? '保存成功，面试题已更新' : '新增成功'
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
        <span>面试题目设置 · {{ title }}</span>
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
      <!-- 面试题目设置专属业务表单 -->
      <div class="form-meta">
        <el-form label-width="100px" size="default">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="4">
              <el-form-item label="题号">
                <el-input v-model="form.problemNo" :disabled="!!form.id" placeholder="如 MS001，留空自动分配" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="4">
              <el-form-item label="难度" required>
                <el-radio-group v-model="form.difficulty">
                  <el-radio v-for="o in difficultyOptions" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="4">
              <el-form-item label="分类" required>
                <el-select v-model="form.category" placeholder="请选择分类" style="width:100%">
                  <el-option v-for="o in categoryOptions" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="4">
              <el-form-item label="高频标记">
                <el-switch v-model="form.isFrequent" active-text="高频" inactive-text="普通" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="4">
              <el-form-item label="状态">
                <el-radio-group v-model="form.status">
                  <el-radio value="online">上线</el-radio>
                  <el-radio value="offline">下线</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="4">
              <el-form-item label="标签">
                <el-select v-model="form.tags" multiple filterable allow-create placeholder="选择/输入标签" style="width:100%">
                  <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="标题" required>
                <el-input v-model="form.title" placeholder="请输入面试题标题" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="12">
              <el-form-item label="输入格式">
                <el-input
                  v-model="form.inputFormat"
                  type="textarea"
                  :rows="2"
                  placeholder="描述输入参数格式，纯文本或 Markdown 均可"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="12">
              <el-form-item label="输出格式">
                <el-input
                  v-model="form.outputFormat"
                  type="textarea"
                  :rows="2"
                  placeholder="描述输出结果格式，纯文本或 Markdown 均可"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- 题目描述：Markdown 分屏编辑器 1 -->
      <div class="editor-block">
        <div class="editor-block__title">
          题目描述（Markdown · 左右分屏）
          <span class="hint">左栏输入源码，右栏即时渲染；中间分隔条支持左右拖拽</span>
        </div>
        <MdSplitEditor
          ref="descEditorRef"
          v-model="form.description"
          :height="560"
          placeholder="## 题目描述\n\n给定一个数组 nums 和目标值 target……\n\n### 示例\n\n输入：nums = [2,7,11,15], target = 9\n输出：[0, 1]"
        />
      </div>

      <!-- 参考答案：Markdown 分屏编辑器 2 -->
      <div class="editor-block">
        <div class="editor-block__title">
          参考答案（Markdown · 左右分屏）
          <span class="hint">包含解题思路 + 复杂度分析 + 可运行代码，前台会用 marked 渲染成 HTML</span>
        </div>
        <MdSplitEditor
          ref="solEditorRef"
          v-model="form.solution"
          :height="640"
          placeholder="## 解题思路\n\n1. 第一步：……\n2. 第二步：……\n\n## 复杂度分析\n\n- 时间复杂度：O(n)\n- 空间复杂度：O(1)\n\n## 代码实现\n\n```java\nclass Solution {\n    // ...\n}\n```"
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
.editor-topbar__title .dot { width: 10px; height: 10px; border-radius: 50%; background: linear-gradient(135deg, #ea580c, #f97316); }
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
