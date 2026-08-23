<script setup lang="ts">
/**
 * MdSplitEditor — 通用 Markdown 左右分屏编辑器组件
 * -------------------------------------------------------
 * 定位：纯编辑器 UI，不做任何业务保存，只负责：
 *   1) 左边 Markdown 源码 textarea
 *   2) 右边 marked.parse() 之后即时渲染的 HTML（0 延迟，computed 同步）
 *   3) 中间分隔条鼠标左右拖拽（15% ~ 85%）
 *   4) 左右滚动按比例双向同步
 *   5) 顶部 Markdown 工具栏（H1/H3 / B/I/S / 代码 / 列表 / 引用 / 代码块 / 分割线 / 表格）
 *
 * 复用场景（组件的 props/emit 都是通用的，不绑定任何数据表）：
 *   · 更新日志编辑  → details 字段
 *   · OJ 用户题解编辑  → oj_solution.content TEXT 字段（存原始 md，前台再 marked 渲染）
 *   · 算法题目设置   → problem.description / problem.analysis
 *   · 面试题目设置   → interview.description / interview.answer
 *
 * 用法：
 *   <MdSplitEditor v-model="md" :height="620" placeholder="# 左侧源码，右侧即时渲染" />
 */
import { computed, onBeforeUnmount, ref, watch, nextTick } from 'vue'
import 'vditor/dist/index.css'
import { marked } from 'marked'

marked.setOptions({ gfm: true, breaks: true, pedantic: false, headerIds: true, mangle: false })

interface Props {
  modelValue?: string
  height?: number | string
  placeholder?: string
  showToolbar?: boolean
  initialLeftPct?: number
}
const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  height: 640,
  placeholder: '左侧输入 Markdown 源码，右侧即时渲染预览…',
  showToolbar: true,
  initialLeftPct: 50,
})
const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'ready'): void
}>()

/* -------- 内部响应式数据 -------- */
const localMd = ref<string>(props.modelValue || '')
const leftPct = ref<number>(
  Math.min(85, Math.max(15, props.initialLeftPct || 50))
)
let isSyncingScroll = false
let dragAbort: (() => void) | null = null

// 对外暴露：v-model 同步时不覆盖用户正在打的字（只有外部 setValue 时才覆盖本地）
let fromOuter = false
watch(
  () => props.modelValue,
  (v) => {
    if (fromOuter) { fromOuter = false; return }
    const nv = typeof v === 'string' ? v : ''
    if (nv !== localMd.value) localMd.value = nv
  }
)
// 本地变更 → emit 给 v-model
watch(localMd, (v) => {
  fromOuter = true
  emit('update:modelValue', v)
})

/* -------- DOM 引用 -------- */
const leftRef = ref<HTMLTextAreaElement | null>(null)
const rightRef = ref<HTMLDivElement | null>(null)
const splitWrapRef = ref<HTMLDivElement | null>(null)

/* -------- 0 延迟渲染 -------- */
const previewHtml = computed(() => {
  try {
    const raw = localMd.value || ''
    if (!raw.trim()) {
      return '<div style="color:#c0c4cc;padding:24px;text-align:center;">在左侧输入 Markdown，这里会实时渲染预览</div>'
    }
    const html = marked.parse(raw) as string
    return html ?? ''
  } catch (e) {
    return '<pre style="color:#f56c6c;white-space:pre-wrap;">Markdown 解析失败：' + String(e) + '</pre>'
  }
})

/* -------- 中间 resizer 拖拽 -------- */
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
    if (newPct < 15) newPct = 15
    if (newPct > 85) newPct = 85
    leftPct.value = newPct
  }
  function noSelect(ev: Event) { ev.preventDefault() }
  function onUp() {
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
    window.removeEventListener('selectstart', noSelect)
    document.body.style.userSelect = ''
    document.body.style.cursor = ''
    dragAbort = null
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
  window.addEventListener('selectstart', noSelect)
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'col-resize'
  dragAbort = onUp
}

/* -------- 滚动双向同步 -------- */
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

/* -------- 工具栏按钮：插入 Markdown 语法 -------- */
type ToolAction =
  | 'bold' | 'italic' | 'strike' | 'code' | 'link'
  | 'heading1' | 'heading2' | 'heading3'
  | 'ul' | 'ol' | 'quote' | 'codeblock' | 'hr' | 'table'

function wrapSelection(before: string, after: string, placeholder: string) {
  const ta = leftRef.value
  if (!ta) return
  const start = ta.selectionStart ?? 0
  const end = ta.selectionEnd ?? 0
  const src = localMd.value
  const sel = src.substring(start, end) || placeholder
  localMd.value = src.substring(0, start) + before + sel + after + src.substring(end)
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
  const src = localMd.value
  let lineStart = start
  while (lineStart > 0 && src.charAt(lineStart - 1) !== '\n') lineStart--
  localMd.value = src.substring(0, lineStart) + prefix + src.substring(lineStart)
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
  const src = localMd.value
  const nlS = start > 0 && src.charAt(start - 1) !== '\n' ? '\n' : ''
  const nlE = end < src.length && src.charAt(end) !== '\n' ? '\n' : ''
  localMd.value = src.substring(0, start) + nlS + block + nlE + src.substring(end)
  nextTick(() => {
    ta.focus()
    const pos = start + nlS.length + block.length
    ta.setSelectionRange(pos, pos)
  })
}
function onTool(action: ToolAction) {
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
      return insertBlock('| 列 1 | 列 2 | 列 3 |\n| --- | --- | --- |\n| A | B | C |\n')
  }
}

/* -------- 对外暴露方法：兼容旧的 detailsVditor.setValue/getValue 调用方 -------- */
defineExpose({
  setValue(v: string) {
    localMd.value = typeof v === 'string' ? v : ''
    nextTick(() => { if (leftRef.value) leftRef.value.scrollTop = 0 })
  },
  getValue(): string {
    return localMd.value || ''
  },
})

onBeforeUnmount(() => {
  try { dragAbort?.() } catch { /* ignore */ }
})

// 挂载完成通知父组件
nextTick(() => emit('ready'))
</script>

<template>
  <div class="md-split-editor">
    <!-- 顶部 Markdown 工具栏 -->
    <div v-if="showToolbar" class="md-toolbar">
      <div class="md-toolbar__group">
        <button type="button" class="md-tb-btn" title="标题 1"    @click="onTool('heading1')">H1</button>
        <button type="button" class="md-tb-btn" title="标题 2"    @click="onTool('heading2')">H2</button>
        <button type="button" class="md-tb-btn" title="标题 3"    @click="onTool('heading3')">H3</button>
      </div>
      <div class="md-toolbar__group">
        <button type="button" class="md-tb-btn md-tb-btn--bold"    title="粗体 **"   @click="onTool('bold')">B</button>
        <button type="button" class="md-tb-btn md-tb-btn--italic"  title="斜体 *"    @click="onTool('italic')">I</button>
        <button type="button" class="md-tb-btn md-tb-btn--strike"  title="删除线 ~~" @click="onTool('strike')">S</button>
        <button type="button" class="md-tb-btn" title="行内代码 `"  @click="onTool('code')">{`<>`}</button>
        <button type="button" class="md-tb-btn" title="链接 []()"  @click="onTool('link')">🔗</button>
      </div>
      <div class="md-toolbar__group">
        <button type="button" class="md-tb-btn" title="无序列表 -"   @click="onTool('ul')">• 列表</button>
        <button type="button" class="md-tb-btn" title="有序列表 1."  @click="onTool('ol')">1. 列表</button>
        <button type="button" class="md-tb-btn" title="引用 >"        @click="onTool('quote')">" 引用</button>
      </div>
      <div class="md-toolbar__group">
        <button type="button" class="md-tb-btn" title="代码块 ```" @click="onTool('codeblock')">``` 代码块</button>
        <button type="button" class="md-tb-btn" title="分割线 ---"  @click="onTool('hr')">— 分割</button>
        <button type="button" class="md-tb-btn" title="表格 | |"    @click="onTool('table')">⊞ 表格</button>
      </div>
    </div>

    <!-- 分屏本体 -->
    <div
      ref="splitWrapRef"
      class="md-split"
      :style="{ height: typeof height === 'number' ? height + 'px' : String(height) }"
      :class="{ 'md-split--no-toolbar': !showToolbar }"
    >
      <textarea
        ref="leftRef"
        class="md-split__left"
        v-model="localMd"
        spellcheck="false"
        :placeholder="placeholder"
        :style="{ width: leftPct + '%' }"
        @scroll="syncScrollFromLeft"
      ></textarea>

      <div
        class="md-split__resizer"
        @mousedown="onResizerMouseDown"
        title="拖拽调整左右宽度（15% ~ 85%）"
      >
        <span class="md-split__resizer-dots"></span>
      </div>

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
</template>

<style scoped>
/* 容器 */
.md-split-editor {
  display: flex;
  flex-direction: column;
  width: 100%;
  box-sizing: border-box;
}

/* 工具栏 */
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
.md-tb-btn:hover { background: #eef2ff; color: #7c3aed; border-color: #e9d5ff; }
.md-tb-btn:active { background: #ede9fe; transform: translateY(1px); }
.md-tb-btn--bold   { font-weight: 700; }
.md-tb-btn--italic { font-style: italic; }
.md-tb-btn--strike { text-decoration: line-through; }

/* 分屏本体 */
.md-split {
  position: relative;
  display: flex;
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 0 0 6px 6px;
  overflow: hidden;
  background: #fff;
  min-height: 300px;
}
.md-split--no-toolbar {
  border-radius: 6px;
}
.md-split__left,
.md-split__right {
  height: 100%;
  box-sizing: border-box;
  overflow: auto;
  outline: none;
  flex-shrink: 0;
}
.md-split__left {
  border: none;
  padding: 14px 16px;
  resize: none;
  font-family: 'Fira Code', ui-monospace, Consolas, 'Courier New', monospace;
  font-size: 13.5px;
  line-height: 1.65;
  background: #fcfcfd;
  color: #1f2937;
  white-space: pre;
  tab-size: 2;
  -moz-tab-size: 2;
}
.md-split__left::placeholder { color: #c0c4cc; }
.md-split__left:focus {
  background: #fff;
  box-shadow: inset 2px 0 0 #7c3aed;
}
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
.md-split__right {
  background: #fff;
  padding: 0;
}
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
</style>

<!-- 非 scoped：vditor-reset 要穿透到 v-html 内容上，且用前缀命名空间防止污染全局 -->
<style>
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
.md-split__preview-inner.vditor-reset blockquote p { margin: .35em 0; }
.md-split__preview-inner.vditor-reset a {
  color: #7c3aed;
  text-decoration: underline;
  text-underline-offset: 2px;
}
.md-split__preview-inner.vditor-reset a:hover { color: #5b21b6; }
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
