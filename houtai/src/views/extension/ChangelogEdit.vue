<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, onBeforeUnmount, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Close, Warning } from '@element-plus/icons-vue'
// marked + Vditor index.css：VDitor CSS 提供 .vditor-reset 的 GitHub 风格样式
// marked 负责把 md 转 html，VDitor CSS 让预览区显示得和 Vditor 官方预览一模一样
import 'vditor/dist/index.css'
import { marked } from 'marked'
import { changelogApi } from '@/api/system'
import dayjs from 'dayjs'
import type { Changelog } from '@/types'

// 配置 marked（支持 GFM、代码块保留、toc 标记等）
marked.setOptions({
  gfm: true,
  breaks: true,
  pedantic: false,
  headerIds: true,
  mangle: false,
})

const route = useRoute()
const idParam = route.params.id as string | undefined
const isEdit = route.path.includes('/edit/') && !!idParam
// 独立编辑页：顶级路由，不包 Layout
const standalone = !!route.meta?.standalone

const title = ref(isEdit ? '编辑更新日志' : '新增更新日志')
const loading = ref(false)
const saving = ref(false)

const form = reactive<{
  version: string
  type: Changelog['type']
  summary: string
  releaseDate: string
  modulesText: string
  knownIssues: string
  issuesTitle: string
  status: 0 | 1
  expandIssues: boolean | number[]
}>({
  version: '',
  type: 'new',
  summary: '',
  releaseDate: dayjs().format('YYYY-MM-DD'),
  modulesText: '',
  knownIssues: '',
  issuesTitle: '已知问题',
  status: 1,
  expandIssues: [],
})

/* ============================================================
   自定义左右分屏（左源码 / 右已渲染 Markdown，0 延迟）
   —— 完全手写，确保：
     1) 左 textarea 输入每一个字，右侧立即 re-render（无防抖，不依赖 Vditor sv）
     2) 中间分隔条可左右拖拽，修改 left / right 宽度百分比
     3) 左右滚动按比例双向同步
     4) 对外暴露 detailsVditor.setValue / getValue / destroy，100% 兼容原来调用处
   ============================================================ */

const leftRef = ref<HTMLTextAreaElement | null>(null)
const rightRef = ref<HTMLDivElement | null>(null)
const leftPct = ref(50)          // 左栏宽度百分比（右栏自动 100 - leftPct）
const detailsMd = ref('')        // 原始 Markdown 字符串（直接从 textarea v-model 绑定）
let isSyncingScroll = false      // 滚动同步锁，避免互相触发死循环

// 0 延迟渲染：每次 md 变动立即重算为 HTML（marked.parse 默认同步，用户打字体验几乎无延迟）
const previewHtml = computed(() => {
  try {
    const raw = detailsMd.value || ''
    if (!raw.trim()) {
      return '<div style="color:#c0c4cc;padding:24px;text-align:center;">在左侧输入 Markdown，这里会实时渲染预览</div>'
    }
    // marked 默认同步返回 string（marked >= 12 都保持同步）
    const html = marked.parse(raw) as string
    return html ?? ''
  } catch (e) {
    return '<pre style="color:#f56c6c;white-space:pre-wrap;">Markdown 解析失败：' + String(e) + '</pre>'
  }
})

/**
 * 对外兼容对象：原来的代码用 detailsVditor?.setValue / getValue / destroy
 * 现在用一个"假 Vditor"替换，API 完全相同，不破坏原有保存/加载逻辑。
 */
let detailsVditor: {
  getValue: () => string
  setValue: (v: string) => void
  destroy: () => void
} | null = null

function createCompatibleEditor() {
  detailsVditor = {
    getValue(): string {
      return detailsMd.value || ''
    },
    setValue(v: string) {
      detailsMd.value = typeof v === 'string' ? v : ''
      // 把光标挪到最末尾，并滚到顶部
      nextTick(() => {
        const el = leftRef.value
        if (el) {
          el.scrollTop = 0
          // 不抢焦点
        }
      })
    },
    destroy() {
      // 原生 DOM 的事件（input / scroll / resize 监听）会在组件 unmount 自动清理
    },
  }
}

/* ---------- 拖拽：中间分隔条左右拖动 ---------- */
const splitWrapRef = ref<HTMLDivElement | null>(null)
let dragAbort: (() => void) | null = null

function onResizerMouseDown(e: MouseEvent) {
  e.preventDefault()
  const wrap = splitWrapRef.value
  if (!wrap) return
  const startX = e.clientX
  const total = wrap.getBoundingClientRect().width
  const startLeftPct = leftPct.value

  function onMove(ev: MouseEvent) {
    if (!total) return
    const dx = ev.clientX - startX
    let newPct = startLeftPct + (dx / total) * 100
    // 限制范围 15% ~ 85%，防止某一栏被拖没
    if (newPct < 15) newPct = 15
    if (newPct > 85) newPct = 85
    leftPct.value = newPct
  }
  function onUp() {
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
    window.removeEventListener('selectstart', noSelect)
    document.body.style.userSelect = ''
    document.body.style.cursor = ''
    dragAbort = null
  }
  function noSelect(ev: Event) { ev.preventDefault() }

  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
  // 防止拖动时选中了文字
  window.addEventListener('selectstart', noSelect)
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'col-resize'

  dragAbort = onUp
}

/* ---------- 滚动同步：按 scrollHeight 比例双向同步 ---------- */
function syncScrollFromLeft() {
  if (isSyncingScroll) return
  const l = leftRef.value, r = rightRef.value
  if (!l || !r) return
  isSyncingScroll = true
  const max = l.scrollHeight - l.clientHeight
  const ratio = max > 0 ? l.scrollTop / max : 0
  r.scrollTop = ratio * (r.scrollHeight - r.clientHeight)
  requestAnimationFrame(() => { isSyncingScroll = false })
}
function syncScrollFromRight() {
  if (isSyncingScroll) return
  const l = leftRef.value, r = rightRef.value
  if (!l || !r) return
  isSyncingScroll = true
  const max = r.scrollHeight - r.clientHeight
  const ratio = max > 0 ? r.scrollTop / max : 0
  l.scrollTop = ratio * (l.scrollHeight - l.clientHeight)
  requestAnimationFrame(() => { isSyncingScroll = false })
}

/* ---------- 把工具栏常用语法按钮的插入实现 ---------- */
type ToolButtonAction =
  | 'bold' | 'italic' | 'strike' | 'code' | 'link'
  | 'heading1' | 'heading2' | 'heading3'
  | 'ul' | 'ol' | 'quote' | 'codeblock' | 'hr' | 'table'

function wrapSelection(before: string, after: string = before, placeholder: string = '文本') {
  const ta = leftRef.value
  if (!ta) return
  const start = ta.selectionStart ?? 0
  const end = ta.selectionEnd ?? 0
  const src = detailsMd.value
  const sel = src.substring(start, end) || placeholder
  const next = src.substring(0, start) + before + sel + after + src.substring(end)
  detailsMd.value = next
  nextTick(() => {
    ta.focus()
    const pos = start + before.length + sel.length
    ta.setSelectionRange(pos, pos)
  })
}
function prependLine(prefix: string) {
  const ta = leftRef.value
  if (!ta) return
  const start = ta.selectionStart ?? 0
  const src = detailsMd.value
  // 找到光标所在行的行首
  let lineStart = start
  while (lineStart > 0 && src.charAt(lineStart - 1) !== '\n') lineStart--
  const next = src.substring(0, lineStart) + prefix + src.substring(lineStart)
  detailsMd.value = next
  nextTick(() => {
    ta.focus()
    const pos = start + prefix.length
    ta.setSelectionRange(pos, pos)
  })
}
function insertBlock(block: string) {
  const ta = leftRef.value
  if (!ta) return
  const start = ta.selectionStart ?? 0
  const end = ta.selectionEnd ?? 0
  const src = detailsMd.value
  // 如果光标不在行首，先换行
  const needNlStart = start > 0 && src.charAt(start - 1) !== '\n' ? '\n' : ''
  const needNlEnd = end < src.length && src.charAt(end) !== '\n' ? '\n' : ''
  const next = src.substring(0, start) + needNlStart + block + needNlEnd + src.substring(end)
  detailsMd.value = next
  nextTick(() => {
    ta.focus()
    const pos = start + needNlStart.length + block.length
    ta.setSelectionRange(pos, pos)
  })
}
function onToolClick(action: ToolButtonAction) {
  switch (action) {
    case 'bold':      return wrapSelection('**', '**', '粗体文本')
    case 'italic':    return wrapSelection('*', '*', '斜体文本')
    case 'strike':    return wrapSelection('~~', '~~', '删除线文本')
    case 'code':      return wrapSelection('`', '`', 'code')
    case 'link':      return wrapSelection('[', '](https://)', '链接文字')
    case 'heading1':  return prependLine('# ')
    case 'heading2':  return prependLine('## ')
    case 'heading3':  return prependLine('### ')
    case 'ul':        return prependLine('- ')
    case 'ol':        return prependLine('1. ')
    case 'quote':     return prependLine('> ')
    case 'codeblock': return insertBlock('```\n// code here\n```')
    case 'hr':        return insertBlock('\n---\n')
    case 'table':
      return insertBlock(
        '| 列 1 | 列 2 | 列 3 |\n| --- | --- | --- |\n| A | B | C |\n'
      )
  }
}

/* ---------- 其它：通知 opener / 路由返回、保存、取消 ---------- */
function notifyOpener(type: 'saved' | 'canceled', message?: string) {
  try {
    if (window.opener && !window.opener.closed) {
      window.opener.postMessage(
        { __from: 'changelog-editor', type, message },
        window.location.origin
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
    ? `${(import.meta as any).env.BASE_URL.replace(/\/$/, '')}/extension/changelog`
    : '/extension/changelog'
}

async function initEditors() {
  await nextTick()
  // 兼容性对象已在上面 createCompatibleEditor 创建（组件顶部就创建好了，不用等 DOM）
}

onMounted(async () => {
  document.title = `${title.value} - 算法可视化平台管理后台`
  createCompatibleEditor()
  await initEditors()
  if (isEdit) {
    loading.value = true
    try {
      const detail = (await changelogApi.getDetail(Number(idParam))) as unknown as Changelog
      form.version = detail.version
      form.type = detail.type
      form.summary = detail.summary
      form.releaseDate = detail.releaseDate
      form.issuesTitle = detail.issuesTitle || '已知问题'
      form.status = (detail.status === 0 ? 0 : 1)
      try {
        const arr: string[] = JSON.parse(detail.modules || '[]')
        form.modulesText = Array.isArray(arr) ? arr.join('\n') : ''
      } catch {
        form.modulesText = ''
      }
      form.knownIssues = detail.knownIssues || ''
      if ((form.knownIssues || '').trim()) {
        form.expandIssues = ['issues']
      }
      detailsVditor?.setValue(detail.details || '')
    } catch (e: any) {
      ElMessage.error(e?.message || '加载记录失败')
    } finally {
      loading.value = false
    }
  } else {
    detailsVditor?.setValue([
      '## 本次更新摘要',
      '',
      '- 新增 XXX 页面，包含 A/B/C 三个功能',
      '- 优化 XXX 交互体验',
      '- 修复 XXX BUG',
      '',
      '## 注意',
      '',
      '> 若需要用户清除浏览器缓存，请在此注明。',
    ].join('\n'))
  }
})

onBeforeUnmount(() => {
  try { dragAbort?.() } catch { /* ignore */ }
  try { detailsVditor?.destroy() } catch { /* ignore */ }
})

async function handleSave() {
  if (!form.version.trim()) {
    ElMessage.warning('请填写版本号，例如 v1.2.3')
    return
  }
  if (!form.releaseDate) {
    ElMessage.warning('请选择发布日期')
    return
  }
  const detailsMdValue = detailsVditor?.getValue() || ''
  if (!detailsMdValue.trim()) {
    ElMessage.warning('更新说明（Markdown）不能为空')
    return
  }
  const modulesArr = Array.from(new Set(
    form.modulesText.split('\n').map(s => s.trim()).filter(Boolean)
  ))
  const payload: Partial<Changelog> = {
    version: form.version.trim(),
    type: form.type,
    summary: form.summary.trim(),
    releaseDate: form.releaseDate,
    modules: JSON.stringify(modulesArr),
    details: detailsMdValue,
    knownIssues: form.knownIssues.trim(),
    issuesTitle: form.issuesTitle.trim() || '已知问题',
    status: form.status,
  }
  saving.value = true
  try {
    if (isEdit) {
      await changelogApi.update(Number(idParam), payload)
      const msg = '保存成功，更新日志已更新'
      notifyOpener('saved', msg)
      ElMessage.success(msg)
    } else {
      const r = (await changelogApi.create(payload)) as unknown as Changelog
      const msg = `新增成功，记录 ID=${r?.id}`
      notifyOpener('saved', msg)
      ElMessage.success(msg)
    }
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
  <div
    class="changelog-editor-page"
    :class="{ standalone }"
  >
    <div class="editor-topbar">
      <div class="editor-topbar__title">
        <span class="dot" />
        <span>{{ title }}</span>
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
      <!-- 元信息表单 -->
      <div class="form-meta">
        <el-form label-width="100px" size="default">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="版本号" required>
                <el-input v-model="form.version" placeholder="如 v1.2.3" maxlength="32" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="更新类型" required>
                <el-select v-model="form.type" style="width:100%">
                  <el-option label="新增" value="new" />
                  <el-option label="优化" value="optimize" />
                  <el-option label="修复" value="fix" />
                  <el-option label="紧急更新" value="urgent" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="发布日期" required>
                <el-date-picker
                  v-model="form.releaseDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择发布日期"
                  style="width:100%"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="6">
              <el-form-item label="发布状态">
                <el-radio-group v-model="form.status">
                  <el-radio :value="1">已发布</el-radio>
                  <el-radio :value="0">草稿</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="16">
              <el-form-item label="更新摘要">
                <el-input
                  v-model="form.summary"
                  placeholder="一句话说明本次更新（前台卡片顶部短摘要）"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="功能模块">
                <el-input
                  v-model="form.modulesText"
                  type="textarea"
                  :rows="1"
                  placeholder="每行一个模块名，例：个人中心 / 商品页面 / 头像系统"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- 更新说明：自定义左右分屏（左源码 + 右实时预览，0 延迟，resizer 可拖拽，滚动同步） -->
      <div class="editor-block">
        <div class="editor-block__title">
          更新说明（Markdown · 左右分屏）
          <span class="hint">左栏输入源码，右栏即时渲染；中间分隔条支持左右拖拽；滚动自动同步</span>
        </div>

        <!-- 顶部工具栏：常用 Markdown 语法按钮（点击直接插入对应语法） -->
        <div class="md-toolbar">
          <div class="md-toolbar__group">
            <button type="button" class="md-tb-btn" title="标题 1"    @click="onToolClick('heading1')">H1</button>
            <button type="button" class="md-tb-btn" title="标题 2"    @click="onToolClick('heading2')">H2</button>
            <button type="button" class="md-tb-btn" title="标题 3"    @click="onToolClick('heading3')">H3</button>
          </div>
          <div class="md-toolbar__group">
            <button type="button" class="md-tb-btn md-tb-btn--bold"    title="粗体 **"   @click="onToolClick('bold')">B</button>
            <button type="button" class="md-tb-btn md-tb-btn--italic"  title="斜体 *"    @click="onToolClick('italic')">I</button>
            <button type="button" class="md-tb-btn md-tb-btn--strike"  title="删除线 ~~" @click="onToolClick('strike')">S</button>
            <button type="button" class="md-tb-btn" title="行内代码 `"  @click="onToolClick('code')">{'<>'}</button>
            <button type="button" class="md-tb-btn" title="链接 []()"  @click="onToolClick('link')">🔗</button>
          </div>
          <div class="md-toolbar__group">
            <button type="button" class="md-tb-btn" title="无序列表 -"   @click="onToolClick('ul')">• 列表</button>
            <button type="button" class="md-tb-btn" title="有序列表 1."  @click="onToolClick('ol')">1. 列表</button>
            <button type="button" class="md-tb-btn" title="引用 >"        @click="onToolClick('quote')">" 引用</button>
          </div>
          <div class="md-toolbar__group">
            <button type="button" class="md-tb-btn" title="代码块 ```" @click="onToolClick('codeblock')">``` 代码块</button>
            <button type="button" class="md-tb-btn" title="分割线 ---"  @click="onToolClick('hr')">— 分割</button>
            <button type="button" class="md-tb-btn" title="表格 | |"    @click="onToolClick('table')">⊞ 表格</button>
          </div>
        </div>

        <!-- 分屏本体：左源码 / 中 resizer / 右预览 -->
        <div
          class="md-split"
          ref="splitWrapRef"
        >
          <!-- 左：Markdown 源码（textarea v-model → 0 延迟） -->
          <textarea
            ref="leftRef"
            class="md-split__left"
            v-model="detailsMd"
            spellcheck="false"
            placeholder="左侧输入 Markdown 源码，右侧会即时渲染成 HTML 预览…"
            :style="{ width: leftPct + '%' }"
            @scroll="syncScrollFromLeft"
          ></textarea>

          <!-- 中：可拖拽分隔条 -->
          <div
            class="md-split__resizer"
            @mousedown="onResizerMouseDown"
            title="拖拽调整左右宽度（15% ~ 85%）"
          >
            <span class="md-split__resizer-dots"></span>
          </div>

          <!-- 右：渲染结果（.vditor-reset 复用 Vditor GitHub 风格 CSS） -->
          <div
            class="md-split__right"
            :style="{ width: (100 - leftPct) + '%' }"
            @scroll="syncScrollFromRight"
          >
            <div
              ref="rightRef"
              class="vditor-reset md-split__preview-inner"
              v-html="previewHtml"
            ></div>
          </div>
        </div>
      </div>

      <!-- 已知问题：折叠收起（默认折叠，展开后普通 textarea 写 md，不再用分屏 Vditor） -->
      <div class="editor-block editor-block--folded">
        <el-collapse v-model="form.expandIssues">
          <el-collapse-item name="issues">
            <template #title>
              <div class="issues-title">
                <el-icon :size="16" color="#e6a23c"><Warning /></el-icon>
                <span>
                  已知问题 / {{ form.issuesTitle || '注意事项' }}
                  <span v-if="form.knownIssues.trim()" class="issues-dot">已填写</span>
                  <span v-else class="issues-dot issues-dot--muted">留空前台显示"暂无已知问题"</span>
                </span>
              </div>
            </template>
            <el-form label-width="90px" size="default" style="padding-top:6px">
              <el-form-item label="自定义标题">
                <el-input
                  v-model="form.issuesTitle"
                  placeholder="例如：注意事项 / 已知问题 / 温馨提示"
                  maxlength="16"
                  style="max-width: 420px"
                />
              </el-form-item>
              <el-form-item label="Markdown 内容">
                <el-input
                  v-model="form.knownIssues"
                  type="textarea"
                  :rows="6"
                  placeholder="Markdown 文本，例如：&#10;- 老版本用户需重新登录一次&#10;> 若出现 502 请 2 分钟后重试"
                />
              </el-form-item>
              <div class="issues-tips">
                <b>支持：</b>
                <code>## 标题</code>
                <code>- 列表</code>
                <code>**粗体**</code>
                <code>*斜体*</code>
                <code>`代码`</code>
                <code>``` ``` 代码块</code>
                <code>> 引用</code>
                <span class="tips-sep">|</span>
                前台展示时会再用 marked 渲染成 HTML
              </div>
            </el-form>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ---- 页面级布局 ---- */
.changelog-editor-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}
.changelog-editor-page.standalone {
  width: 100%;
}
.editor-topbar {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
}
.editor-topbar__title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex-wrap: wrap;
}
.editor-topbar__title .dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #7c3aed, #a855f7);
}
.editor-topbar__title .sub {
  font-size: 12px;
  color: #909399;
  font-weight: 400;
}
.editor-body {
  flex: 1;
  padding: 18px 24px 40px;
  max-width: 1500px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}
.form-meta {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px 4px;
  border: 1px solid #ebeef5;
  margin-bottom: 14px;
}
.editor-block {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px 18px;
  border: 1px solid #ebeef5;
  margin-bottom: 14px;
}
.editor-block__title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 10px;
  padding-left: 2px;
  line-height: 1.6;
}
.editor-block__title .hint {
  display: inline-block;
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
  font-weight: 400;
}
.editor-block--folded { padding: 2px 10px; }

/* ---- 顶部 Markdown 工具栏 ---- */
.md-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  padding: 8px 10px;
  background: #fafbfc;
  border: 1px solid #dcdfe6;
  border-bottom: none;
  border-radius: 6px 6px 0 0;
  user-select: none;
}
.md-toolbar__group {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding-right: 8px;
  border-right: 1px solid #e4e7ed;
}
.md-toolbar__group:last-child {
  border-right: none;
  padding-right: 0;
}
.md-tb-btn {
  height: 28px;
  min-width: 28px;
  padding: 0 8px;
  border: 1px solid transparent;
  background: transparent;
  color: #4b5563;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  line-height: 26px;
  font-family: inherit;
  transition: all .15s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.md-tb-btn:hover {
  background: #eef2ff;
  color: #7c3aed;
  border-color: #e9d5ff;
}
.md-tb-btn:active {
  background: #ede9fe;
  transform: translateY(1px);
}
.md-tb-btn--bold   { font-weight: 700; }
.md-tb-btn--italic { font-style: italic; }
.md-tb-btn--strike { text-decoration: line-through; }

/* ---- 左右分屏本体 ---- */
.md-split {
  position: relative;
  display: flex;
  width: 100%;
  height: 640px;
  border: 1px solid #dcdfe6;
  border-radius: 0 0 6px 6px;
  overflow: hidden;
  background: #fff;
}
.md-split__left,
.md-split__right {
  height: 100%;
  box-sizing: border-box;
  overflow: auto;
  outline: none;
  /* flex-shrink=0：避免因为 resizer 宽度的存在被挤压换行 */
  flex-shrink: 0;
}

/* 左栏：源码 textarea */
.md-split__left {
  border: none;
  border-right: none;
  padding: 14px 16px;
  resize: none;
  font-family: 'Fira Code', ui-monospace, Consolas, 'Courier New', monospace;
  font-size: 13.5px;
  line-height: 1.65;
  background: #fcfcfd;
  color: #1f2937;
  white-space: pre;             /* 保持和预览区一致的断行逻辑 */
  tab-size: 2;
  -moz-tab-size: 2;
}
.md-split__left::placeholder { color: #c0c4cc; }
.md-split__left:focus {
  background: #fff;
  box-shadow: inset 2px 0 0 #7c3aed;
}

/* 中栏：可拖拽分隔条 */
.md-split__resizer {
  flex-shrink: 0;
  width: 5px;
  cursor: col-resize;
  background: #e4e7ed;
  position: relative;
  transition: background .15s;
}
.md-split__resizer:hover,
.md-split__resizer:active {
  background: #a855f7;
}
.md-split__resizer-dots {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 3px;
  height: 42px;
  transform: translate(-50%, -50%);
  background:
    radial-gradient(circle, #fff 1.2px, transparent 1.5px) 0 0 / 3px 7px repeat-y,
    transparent;
  opacity: .85;
}

/* 右栏：预览区外层 */
.md-split__right {
  background: #fff;
  padding: 0;
}
/* 预览区内层：直接复用 Vditor .vditor-reset 提供的 GitHub 风格排版 */
:deep(.md-split__preview-inner) {
  padding: 16px 22px 80px;
  max-width: 100%;
  line-height: 1.75;
  font-size: 14.5px;
  color: #24292f;
}
:deep(.md-split__preview-inner > :first-child) {
  margin-top: 0;
}

/* ---- 已知问题折叠面板样式（保持原有） ---- */
.issues-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.issues-dot {
  margin-left: 10px;
  padding: 0 8px;
  height: 18px;
  line-height: 18px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 400;
  color: #fff;
  background: linear-gradient(135deg, #e6a23c, #f08a24);
}
.issues-dot--muted {
  background: #f2f6fc;
  color: #8a94a6;
}
.issues-tips {
  padding: 8px 0 0 90px;
  color: #909399;
  font-size: 12px;
  line-height: 1.9;
}
.issues-tips code {
  background: #f6f8fa;
  border: 1px solid #eaecef;
  padding: 1px 6px;
  border-radius: 4px;
  margin: 0 2px;
  color: #7928ca;
  font-family: 'Fira Code', Consolas, monospace;
  font-size: 11px;
}
.issues-tips .tips-sep {
  margin: 0 8px;
  color: #dcdfe6;
}
</style>

<!-- 非 scoped：vditor-reset 必须穿透到 v-html 内容上（scoped 会加 data-v-xxx 导致不生效） -->
<style>
/* 用 .md-split__preview-inner 命名空间隔离，不污染全局 .vditor-reset */
.md-split__preview-inner.vditor-reset h1,
.md-split__preview-inner.vditor-reset h2,
.md-split__preview-inner.vditor-reset h3,
.md-split__preview-inner.vditor-reset h4,
.md-split__preview-inner.vditor-reset h5,
.md-split__preview-inner.vditor-reset h6 {
  margin-top: 1.3em;
  margin-bottom: .5em;
  line-height: 1.3;
  font-weight: 700;
  padding-bottom: .25em;
  color: #111827;
}
.md-split__preview-inner.vditor-reset h1 { font-size: 1.9em;  border-bottom: 1px solid #e5e7eb; }
.md-split__preview-inner.vditor-reset h2 { font-size: 1.55em; border-bottom: 1px solid #eef2ff; }
.md-split__preview-inner.vditor-reset h3 { font-size: 1.25em; }

.md-split__preview-inner.vditor-reset p {
  margin: .75em 0;
  line-height: 1.75;
}

.md-split__preview-inner.vditor-reset ul,
.md-split__preview-inner.vditor-reset ol {
  padding-left: 1.6em;
  margin: .7em 0;
}
.md-split__preview-inner.vditor-reset li {
  margin: .2em 0;
  line-height: 1.75;
}

.md-split__preview-inner.vditor-reset code {
  background: #f6f8fa;
  border: 1px solid #eaecef;
  padding: 1px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', ui-monospace, Consolas, monospace;
  font-size: .9em;
  color: #d6336c;
}
.md-split__preview-inner.vditor-reset pre {
  background: #f6f8fa;
  border-radius: 6px;
  padding: 12px 14px;
  overflow: auto;
  margin: .9em 0;
}
.md-split__preview-inner.vditor-reset pre code {
  background: transparent;
  border: none;
  color: #24292f;
  padding: 0;
}
.md-split__preview-inner.vditor-reset blockquote {
  border-left: 4px solid #7c3aed;
  padding: 4px 14px;
  margin: 10px 0;
  background: #f5f3ff;
  border-radius: 0 6px 6px 0;
  color: #5b21b6;
}
.md-split__preview-inner.vditor-reset blockquote p {
  margin: .35em 0;
}
.md-split__preview-inner.vditor-reset a {
  color: #7c3aed;
  text-decoration: underline;
  text-underline-offset: 2px;
}
.md-split__preview-inner.vditor-reset a:hover {
  color: #5b21b6;
}
.md-split__preview-inner.vditor-reset hr {
  border: none;
  border-top: 1px dashed #c7d2fe;
  margin: 1.4em 0;
}
.md-split__preview-inner.vditor-reset table {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
  font-size: 13.5px;
}
.md-split__preview-inner.vditor-reset th,
.md-split__preview-inner.vditor-reset td {
  border: 1px solid #e4e7ed;
  padding: 7px 10px;
  text-align: left;
}
.md-split__preview-inner.vditor-reset th {
  background: #f5f3ff;
  color: #5b21b6;
  font-weight: 600;
}
.md-split__preview-inner.vditor-reset img {
  max-width: 100%;
  border-radius: 6px;
  margin: .5em 0;
}
.md-split__preview-inner.vditor-reset input[type="checkbox"] {
  margin-right: 6px;
  vertical-align: middle;
}
</style>
