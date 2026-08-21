<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import {
  ElTable, ElTableColumn, ElButton, ElTag, ElDialog, ElForm, ElFormItem,
  ElInput, ElSelect, ElOption, ElMessage, ElMessageBox, ElPagination,
  ElInputNumber, ElRadioGroup, ElRadio, ElDivider, ElCheckbox, ElSwitch,
  ElDescriptions, ElDescriptionsItem, ElTabs, ElTabPane, ElEmpty, ElUpload,
  ElProgress, ElIcon, ElCard
} from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, Upload, Download, View, MagicStick, Promotion, FullScreen, Aim, Loading, Warning } from '@element-plus/icons-vue'
import {
  listProblems, createProblem, updateProblem, deleteProblem,
  updateProblemStatus, batchImportProblems, exportProblemsUrl,
  aiGenerateProblems, aiBatchSave,
  convertToFront, convertToBackend, toBackendStatus
} from '@/api/interview'
import type { BackendProblem, ProblemSavePayload } from '@/api/interview'
import { vectorApi } from '@/api/vector'
import { renderMarkdown } from '@/utils/markdown'

interface InterviewProblem {
  id: number
  problemNo: string
  title: string
  difficulty: 'easy' | 'medium' | 'hard'
  tags: string[]
  category: string
  description: string
  inputFormat: string
  outputFormat: string
  solution: string
  isFrequent: boolean
  status: 'online' | 'offline'
  viewCount: number
  createTime: string
}

const allProblems = ref<InterviewProblem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogFullscreen = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('新增面试题')

const importVisible = ref(false)
const importLoading = ref(false)
const importOverwrite = ref(false)

// ==================== 导入进度（前端秒表 + 模拟进度 + 后端进度优先覆盖） ====================
// 关键背景：batchImportProblems 是同步 HTTP 请求（等待后端导入+同步全部完成才返回），
// 响应回来前浏览器无法拿到服务器内部处理条数。因此做了三层机制：
//   1) 前端秒表：实时显示「已用时间」，用户能感知任务还在跑
//   2) 前端总题数：文件解析后已知 payloads.length，显示「总数」避免显示 0
//   3) 模拟进度 + 后端轮询：后端进度一旦有有效值立即覆盖模拟值，无则缓慢爬升

const importProgress = reactive<{
  // —— 后端真实进度（来自 syncProgress，若可用） ——
  inProgress: boolean
  phase: 'idle' | 'preparing' | 'running' | 'completed' | 'failed' | string
  processed: number      // 已导入/同步成功条数（后端值，若 0 则用前端模拟 processed）
  failed: number         // 失败条数
  total: number          // 总条数（后端值，若 0 则用前端 clientTotal）
  remaining: number      // 剩余条数
  percent: number        // 百分比（后端 percent，若 0 且后端未更新则用 mockPercent）
  message: string
  lastError: string | null
  // —— 后端时间（若后端返回 elapsedSeconds 则优先使用） ——
  elapsedSeconds: number | null
  estimatedRemainingSeconds: number | null
  // —— 前端自有字段 ——
  clientTotal: number    // 前端解析 JSON 得到的总题数（payloads.length）
  mockProcessed: number  // 前端模拟的已处理数（后端 processed 为 0 时用）
  mockPercent: number    // 前端模拟百分比（后端 percent 为 0 时用）
  clientElapsed: number  // 前端秒表：已用秒数（一直累加）
  clientEstimated: number | null  // 前端推算的预计剩余秒数
  clientResult: { successNum: number; failNum: number } | null  // HTTP 返回后的最终结果
}>({
  inProgress: false,
  phase: 'idle',
  processed: 0,
  failed: 0,
  total: 0,
  remaining: 0,
  percent: 0,
  message: '',
  lastError: null,
  elapsedSeconds: null,
  estimatedRemainingSeconds: null,
  clientTotal: 0,
  mockProcessed: 0,
  mockPercent: 0,
  clientElapsed: 0,
  clientEstimated: null,
  clientResult: null
})

let progressPollTimer: number | null = null   // 后端 syncProgress 轮询 1s
let clientTickTimer: number | null = null     // 前端秒表 + 模拟进度 1s
let mockStartTs = 0                           // 模拟进度起点时间戳
let mockEstimatedMs = 10 * 1000               // 模拟进度总预估时长（ms，默认 10s，按 clientTotal 动态重算）
/** 批量导入 HTTP 请求中止控制器：用户取消导入/关闭页面/路由离开时 abrot() 中止浏览器端等待 */
let importAbortController: AbortController | null = null

const router = useRouter()

/** 是否有导入正在运行（HTTP 未返回 or 向量/ES 同步还在后端跑），用于关闭/刷新拦截判断 */
const isImportingNow = () => importLoading.value || importProgress.inProgress

/** 缓动函数 easeOutCubic：前快后慢，让进度条在 0~60% 区间快速爬升，用户感觉立刻在动 */
const easeOutCubic = (x: number) => 1 - Math.pow(1 - x, 3)
/** 缓动函数 easeInOutQuad：整体更平滑 */
const easeInOutQuad = (x: number) => (x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2)

// 格式化秒 -> "X小时Y分Z秒"
const formatSeconds = (sec: number | null | undefined): string => {
  if (sec == null || sec < 0) return '计算中...'
  if (sec === 0) return '0秒'
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  const s = sec % 60
  if (h > 0) return `${h}小时${m}分${s}秒`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}

// —— 后端进度轮询：一旦后端返回有效值（total>0 或 percent>0 或 processed>0），立即覆盖 ——
const fetchBackendProgress = async () => {
  try {
    const snap: any = await vectorApi.syncProgress()
    if (!snap) return
    if (snap.inProgress !== undefined) importProgress.inProgress = !!snap.inProgress
    if (snap.phase) importProgress.phase = snap.phase
    if (snap.message !== undefined) importProgress.message = snap.message || ''
    if (snap.lastError !== undefined) importProgress.lastError = snap.lastError || null
    if (typeof snap.elapsedSeconds === 'number') importProgress.elapsedSeconds = snap.elapsedSeconds
    if (typeof snap.estimatedRemainingSeconds === 'number') importProgress.estimatedRemainingSeconds = snap.estimatedRemainingSeconds

    const backTotal = Number(snap.total) || 0
    const backProcessed = Number(snap.processed) || 0
    const backFailed = Number(snap.failed) || 0
    const backPercent = Math.min(100, Number(snap.percent) || 0)
    const backRemaining = Number(snap.remaining) || 0

    // 只有后端返回有效值时才覆盖。避免后端暂时返回 0/空 覆盖了前端总题数
    if (backTotal > 0) importProgress.total = backTotal
    if (backProcessed > 0) importProgress.processed = backProcessed
    if (backFailed >= 0) importProgress.failed = backFailed
    if (backPercent > 0.001) importProgress.percent = backPercent
    if (backRemaining >= 0 && backTotal > 0) importProgress.remaining = backRemaining
  } catch (e) {
    // 静默，下一轮继续
  }
}

// —— 前端秒表 tick + 模拟进度推进（每秒 1 次） ——
const clientTickOnce = () => {
  // 1) 秒表累加
  importProgress.clientElapsed += 1

  // 2) 模拟进度：基于 clientTotal×0.3s/道 预估总时长，缓动爬升到 95%，用户每秒能看到数字在涨
  //    后端进度一旦有有效值立刻停止模拟，切换真实值
  const backUpdated =
    (importProgress.total > 0 && importProgress.total !== importProgress.clientTotal)
    || importProgress.processed > Math.max(importProgress.mockProcessed, 1)
    || importProgress.percent > Math.max(importProgress.mockPercent, 0.5)

  if (!backUpdated && importProgress.clientTotal > 0) {
    const elapsed = Date.now() - mockStartTs  // ms
    const ratio = Math.min(1, elapsed / mockEstimatedMs)
    // 前 60% 时间快速爬 0%~85%（easeOutCubic：用户感知迅速开始工作）
    // 后 40% 时间缓慢爬 85%~95%（等待后端向量/ES 同步阶段）
    let target: number
    if (ratio < 0.6) {
      const sub = ratio / 0.6   // 0 ~ 1
      target = easeOutCubic(sub) * 85
    } else {
      const sub = (ratio - 0.6) / 0.4  // 0 ~ 1
      target = 85 + easeInOutQuad(sub) * 10
    }
    const mockPct = Math.min(95, target)
    importProgress.mockPercent = mockPct
    importProgress.mockProcessed = Math.min(
      importProgress.clientTotal,
      Math.floor(importProgress.clientTotal * (mockPct / 100))
    )
  }

  // 3) 前端估算预计剩余时间（基于 percent 和 clientElapsed）
  const pct = displayPercent.value
  if (pct > 0.5 && pct < 99.5 && importProgress.clientElapsed > 2) {
    const elapsed = importProgress.clientElapsed
    // 剩余 = elapsed * (100 - pct) / pct
    const remain = Math.max(0, Math.round(elapsed * (100 - pct) / pct))
    importProgress.clientEstimated = remain
  } else if (pct >= 99.5) {
    importProgress.clientEstimated = 0
  }
}

// —— 对外：显示用 computed（统一前端+后端源） ——
// 关键规则：clientResult 仅是入库最终数，但后端向量/ES 同步可能还在跑（percent<100），
// 因此不能一有 clientResult 就强制 100%/最终 processed/failed，必须等进度条真正到 99.9%+ 才贴最终数字。

const displayPercent = computed(() => {
  // 1) 后端进度 >= 99.9 直接 100（后端真的干完了）
  if (importProgress.percent >= 99.9) return 100
  // 2) 后端明确 phase=completed 且 clientResult 已拿到（入库+同步的最终结果都齐了）→ 100
  if (importProgress.phase === 'completed' && importProgress.clientResult) return 100
  // 3) clientResult + 当前 percent>=95 → 直接补到 100（避免用户看着 95%~99% 卡死不动等很久）
  if (importProgress.clientResult && importProgress.percent >= 95) return 100
  // 4) 后端 percent 有值（向量/ES 同步阶段真实进度）→ 优先显示（保留 1 位小数）
  if (importProgress.percent > 0.001) return Number(importProgress.percent.toFixed(1))
  // 5) 否则用前端模拟进度（0% ~ 95% 缓动爬升，保证一直在动）
  return Number(importProgress.mockPercent.toFixed(1))
})

const displayTotal = computed(() => {
  // 总数永远用前端 clientTotal（JSON 解析后已知，最准、立刻有值）
  // 除非 clientResult 已返回且后端 percent=100，这时用合计校验一致性
  if (importProgress.clientResult && displayPercent.value >= 99.9) {
    return importProgress.clientResult.successNum + importProgress.clientResult.failNum
  }
  if (importProgress.clientTotal > 0) return importProgress.clientTotal
  if (importProgress.total > 0) return importProgress.total
  if (importProgress.clientResult) return importProgress.clientResult.successNum + importProgress.clientResult.failNum
  return 0
})

const displayProcessed = computed(() => {
  // 只有进度条到 >=99.9 才显示最终入库成功数（clientResult.successNum）
  // 否则优先后端 processed（向量/ES 同步阶段真实值），其次前端模拟估算值
  if (displayPercent.value >= 99.9 && importProgress.clientResult) {
    return importProgress.clientResult.successNum
  }
  if (importProgress.processed > 0) return importProgress.processed
  // 前端估算：clientTotal * (displayPercent / 100)，保证百分比和道数口径一致
  if (importProgress.clientTotal > 0 && displayPercent.value > 0) {
    return Math.min(
      importProgress.clientTotal,
      Math.floor(importProgress.clientTotal * (displayPercent.value / 100))
    )
  }
  return importProgress.mockProcessed
})

const displayFailed = computed(() => {
  // 进度条 >=99.9 才显示最终失败数，否则显示后端 failed（通常为 0）
  if (displayPercent.value >= 99.9 && importProgress.clientResult) {
    return importProgress.clientResult.failNum
  }
  return importProgress.failed
})

const displayRemaining = computed(() => {
  const total = displayTotal.value
  const done = displayProcessed.value + displayFailed.value
  return Math.max(0, total - done)
})

const displayElapsed = computed(() => {
  // 优先后端 elapsedSeconds，否则前端秒表 clientElapsed（一直都有）
  if (typeof importProgress.elapsedSeconds === 'number' && importProgress.elapsedSeconds >= importProgress.clientElapsed) {
    return importProgress.elapsedSeconds
  }
  return importProgress.clientElapsed
})

const displayEstimated = computed(() => {
  // 最终完成 → 0
  if (displayPercent.value >= 99.9) return 0
  // 后端 estimatedRemaining 存在且合理就用
  if (typeof importProgress.estimatedRemainingSeconds === 'number'
    && importProgress.estimatedRemainingSeconds >= 0
    && importProgress.estimatedRemainingSeconds <= 3 * 3600) {
    return importProgress.estimatedRemainingSeconds
  }
  return importProgress.clientEstimated
})

/** 是否真正"完成"（进度条 100 且最终数据齐了），用于 UI 切换 4 列/6 列统计面板 */
const isImportFinished = computed(() => {
  return displayPercent.value >= 99.9
    && (!!importProgress.clientResult || importProgress.phase === 'completed')
})

const progressStatusType = computed(() => {
  // 失败：红色 x
  if (importProgress.phase === 'failed'
    || (importProgress.clientResult && importProgress.clientResult.failNum > 0 && importProgress.clientResult.successNum === 0)) {
    return 'exception' as const
  }
  // 严格：只有进度条真正 >=99.9 才绿色 ✓（避免 clientResult 一回来就提前绿勾）
  if (displayPercent.value >= 99.9) return 'success' as const
  if (importProgress.phase === 'preparing') return 'warning' as const
  // 默认空字符串：蓝色运行态（进度条正常蓝色条纹）
  return '' as const
})

const progressHeaderText = computed(() => {
  if (isImportFinished.value) {
    const s = importProgress.clientResult
    if (s) {
      if (s.successNum > 0 && s.failNum > 0) return `导入完成：成功 ${s.successNum} 道，失败 ${s.failNum} 道`
      if (s.successNum > 0) return `导入完成：成功 ${s.successNum} 道`
      if (s.failNum > 0) return `导入完成：失败 ${s.failNum} 道`
    }
    return '导入完成'
  }
  if (importProgress.inProgress || importLoading.value) {
    // 后端 message 优先（向量/ES 同步阶段会写清晰的分阶段提示）
    if (importProgress.message) return importProgress.message
    // 入库阶段：明确告诉用户正在导入多少道
    if (importProgress.clientTotal > 0) {
      if (displayPercent.value < 85) return `正在导入 ${importProgress.clientTotal} 道题目（写入数据库中）`
      return `正在同步向量库与 ES 索引（${importProgress.clientTotal} 道）`
    }
    return '正在导入面试题...'
  }
  if (importProgress.phase === 'failed') return '导入失败'
  return '导入进度'
})

const importProgressVisible = computed(() => {
  // 任何时候只要：正在导入 / 后端有同步任务 / 有 clientTotal（已选文件）/ 最终结果存在 / 失败 → 显示进度卡
  // 但如果弹窗被关闭且完成了，watch(importVisible) 会 reset，不会残留
  return importLoading.value
    || importProgress.inProgress
    || importProgress.clientTotal > 0
    || importProgress.clientResult
    || importProgress.phase === 'completed'
    || importProgress.phase === 'failed'
})

// —— 生命周期方法 ——
const startAllProgressTimers = () => {
  stopAllProgressTimers()
  mockStartTs = Date.now()
  // 根据 clientTotal 预估总时长：每道 0.3 秒（入库+向量+ES 平均），最小 15 秒保证动画不会瞬间结束
  mockEstimatedMs = Math.max(15 * 1000, importProgress.clientTotal * 300)
  progressPollTimer = window.setInterval(fetchBackendProgress, 1000)
  clientTickTimer = window.setInterval(clientTickOnce, 1000)
  fetchBackendProgress()
}

const stopAllProgressTimers = () => {
  if (progressPollTimer != null) { clearInterval(progressPollTimer); progressPollTimer = null }
  if (clientTickTimer != null)  { clearInterval(clientTickTimer);  clientTickTimer = null }
}

const resetImportProgress = () => {
  stopAllProgressTimers()
  Object.assign(importProgress, {
    inProgress: false,
    phase: 'idle',
    processed: 0,
    failed: 0,
    total: 0,
    remaining: 0,
    percent: 0,
    message: '',
    lastError: null,
    elapsedSeconds: null,
    estimatedRemainingSeconds: null,
    clientTotal: 0,
    mockProcessed: 0,
    mockPercent: 0,
    clientElapsed: 0,
    clientEstimated: null,
    clientResult: null
  })
}

/**
 * 用户点击"取消导入"时的统一回滚方法（前端语义上的取消）：
 * 1. 中止仍在 pending 中的 HTTP 批量导入请求（AbortController.abort）
 * 2. 停止前端进度轮询 / 秒表
 * 3. 重置进度状态 & 清文件选择
 * 4. 弹窗告知用户：纯前端中止 ≠ 数据库事务回滚，已入库题目可能需要手动清理
 */
const cancelImportAndRollbackUI = () => {
  // 1) 中止 HTTP（若仍在 pending）
  if (importAbortController) {
    try { importAbortController.abort() } catch { /* noop */ }
    importAbortController = null
  }
  // 2) 停所有计时器
  stopAllProgressTimers()
  // 3) 重置所有进度状态 & 清上传文件
  resetImportProgress()
  if (uploadRef.value?.clearFiles) uploadRef.value.clearFiles()
  uploadFile.value = null
  importLoading.value = false
  // 4) 提示用户语义边界（前端无法回滚已提交 DB 的 chunk）
  ElMessage.warning({
    message:
      '已取消导入（浏览器端已停止等待）。\n注意：如果后端已处理部分数据写入数据库，前端取消不会自动回滚已入库的题目，请在"面试题管理"列表中按题目编号或创建时间筛选后手动删除不需要的数据。',
    duration: 6000,
    showClose: true,
    type: 'warning'
  })
}

/**
 * 弹窗关闭前置守卫（before-close）：导入正在进行中 → 弹确认
 * @param action 'close'（点右上角 ✕）| 'cancel'（点遮罩/ESC）
 */
const beforeImportDialogClose = async (action: 'confirm' | 'cancel' | 'close') => {
  // 如果此时没有在导入，直接关闭
  if (!isImportingNow()) return true
  // 导入运行中：二次确认
  try {
    await ElMessageBox.confirm(
      `正在导入 ${importProgress.clientTotal > 0 ? importProgress.clientTotal : ''} 道题目，确认关闭并取消导入吗？
前端取消不会自动回滚已写入数据库的题目，可在列表中按创建时间清理。`,
      '取消导入确认',
      {
        confirmButtonText: '确认取消导入',
        cancelButtonText: '继续导入',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
        distinguishCancelAndClose: true,
        dangerouslyUseHTMLString: false
      }
    )
    // 用户点了"确认取消导入"
    cancelImportAndRollbackUI()
    importVisible.value = false
    return true
  } catch {
    // 用户点"继续导入"或点 ESC/遮罩关闭 → 不关弹窗
    return false
  }
}

const searchForm = reactive({
  keyword: '',
  difficulty: '',
  status: '',
  tag: '',
  category: ''
})

const formData = reactive<Partial<InterviewProblem>>({
  id: undefined,
  problemNo: '',
  title: '',
  difficulty: 'medium',
  tags: [],
  category: '',
  description: '',
  inputFormat: '',
  outputFormat: '',
  solution: '',
  isFrequent: false,
  status: 'offline'
})

const formRef = ref()
const currentProblem = ref<InterviewProblem | null>(null)

const rules = {
  problemNo: [{ required: false, message: '留空时自动分配题号', trigger: 'blur' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  description: [{ required: true, message: '请输入题目描述', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const difficultyOptions = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' }
]

const statusOptions = [
  { label: '上线', value: 'online' },
  { label: '下线', value: 'offline' }
]

const tagOptions = [
  '数组', '链表', '栈', '队列', '哈希表', '树', '二叉树', '图', '堆', '并查集',
  '动态规划', '回溯', '贪心', '分治', '排序', '查找', '二分查找', '字符串', '数学',
  '位运算', '模拟', '递归', '广度优先搜索', '深度优先搜索', '滑动窗口', '前缀和', '双指针', '设计'
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
  { label: '设计', value: '设计' }
]

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 后端分页，不再用 computed 切分；当前页数据就是 allProblems.value
// pagedProblems 作为兼容返回
const pagedProblems = allProblems

/** 刷新/关闭标签页时的系统级确认拦截（浏览器原生，返回非空即弹确认） */
const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  if (!isImportingNow()) return
  const msg = '当前正在批量导入面试题，离开后前端取消导入，部分已入库的题目不会自动回滚，确定离开？'
  e.preventDefault()
  e.returnValue = msg
  return msg
}

onMounted(() => {
  loadData()
  // 全局绑定刷新 / 关闭标签页拦截
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  // 离开页面时清理进度轮询定时器，避免内存泄漏
  stopAllProgressTimers()
  window.removeEventListener('beforeunload', handleBeforeUnload)
  // 页面卸载前仍在跑的导入 → 直接 abort（不弹 warning，避免提示风暴）
  if (importAbortController) {
    try { importAbortController.abort() } catch { /* noop */ }
    importAbortController = null
  }
})

// SPA 内路由离开（点击侧边栏切其他页面）：导入中 → 弹 Element 确认框
onBeforeRouteLeave(async (to, from, next) => {
  if (!isImportingNow()) { next(); return }
  try {
    await ElMessageBox.confirm(
      `当前正在导入 ${importProgress.clientTotal > 0 ? importProgress.clientTotal : ''} 道题目，离开当前页面会取消导入。
前端取消不会自动回滚已写入数据库的题目，确认离开？`,
      '页面离开确认',
      {
        confirmButtonText: '确认离开并取消导入',
        cancelButtonText: '留在此页面',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
        distinguishCancelAndClose: true
      }
    )
    // 用户确认离开
    cancelImportAndRollbackUI()
    next()
  } catch {
    // 留在此页面
    next(false)
  }
})

// 分页切换 -> 重新请求
watch([page, pageSize], () => {
  loadData()
})

// 导入弹窗开闭控制进度条残留：
//   - 打开时：没有进行中的任务就 reset（干净的开始）
//   - 关闭时：没有进行中的任务，延迟 3 秒（等 finally 的收尾计时器停）后 reset；下次打开不显示上次的结果
watch(importVisible, (val: boolean) => {
  if (val) {
    // 打开弹窗：当前没在导入中就清干净
    if (!importLoading.value && !importProgress.inProgress) {
      resetImportProgress()
    }
  } else {
    // 关闭弹窗：当前没在干活则延迟几秒后 reset（避免下次再打开看到上次的完成态进度）
    if (!importLoading.value && !importProgress.inProgress) {
      // 如果进度已经完成，立即 reset（下一次打开就看不到）
      if (isImportFinished.value) {
        resetImportProgress()
      } else {
        window.setTimeout(() => {
          if (!importLoading.value && !importProgress.inProgress && !importVisible.value) {
            resetImportProgress()
          }
        }, 4000)
      }
    }
  }
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await listProblems({
      keyword: searchForm.keyword || undefined,
      difficulty: searchForm.difficulty || undefined,
      category: searchForm.category || undefined,
      tag: searchForm.tag || undefined,
      status: searchForm.status ? toBackendStatus(searchForm.status) : undefined,
      page: page.value,
      pageSize: pageSize.value,
      sortBy: 'id',
      order: 'desc'
    }) as any

    const backendList = (res.list || []) as BackendProblem[]
    allProblems.value = backendList.map(convertToFront) as InterviewProblem[]
    total.value = res.total || 0
  } catch (error: any) {
    console.error('加载面试题失败:', error)
    ElMessage.error(error?.message || '加载面试题失败')
    allProblems.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.difficulty = ''
  searchForm.status = ''
  searchForm.tag = ''
  searchForm.category = ''
  page.value = 1
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增面试题'
  dialogFullscreen.value = false
  Object.assign(formData, {
    id: undefined,
    problemNo: '',
    title: '',
    difficulty: 'medium',
    tags: [],
    category: '',
    description: '',
    inputFormat: '',
    outputFormat: '',
    solution: '',
    isFrequent: false,
    status: 'offline'
  })
  dialogVisible.value = true
}

const handleEdit = (row: InterviewProblem) => {
  dialogTitle.value = '编辑面试题'
  dialogFullscreen.value = false
  Object.assign(formData, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

const handleView = (row: InterviewProblem) => {
  currentProblem.value = row
  detailVisible.value = true
}

const handleDelete = async (row: InterviewProblem) => {
  try {
    await ElMessageBox.confirm(`确定要删除题目「${row.title}」吗？`, '提示', { type: 'warning' })
    await deleteProblem(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    if (e && e !== 'cancel' && !(e instanceof Error && e.message?.includes('cancel'))) {
      // 用户取消忽略；其他错误提示
    }
  }
}

const handleToggleStatus = async (row: InterviewProblem) => {
  const targetStatus = row.status
  const statusLabel = targetStatus === 'online' ? '上线' : '下线'
  try {
    await ElMessageBox.confirm(`确定要${statusLabel}该题目吗？`, '提示', { type: 'warning' })
    await updateProblemStatus(row.id, toBackendStatus(targetStatus))
    ElMessage.success(`${statusLabel}成功`)
  } catch {
    row.status = targetStatus === 'online' ? 'offline' : 'online'
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    try {
      const payload = convertToBackend(formData) as ProblemSavePayload
      if (formData.id) {
        await updateProblem(formData.id as number, payload)
        ElMessage.success('编辑成功')
      } else {
        await createProblem(payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (error: any) {
      console.error('保存面试题失败:', error)
      ElMessage.error(error?.message || '保存失败')
    }
  })
}

const onDialogClosed = () => {
  formRef.value?.resetFields()
  dialogFullscreen.value = false
}

const handleBatchImport = () => {
  importOverwrite.value = false
  importVisible.value = true
}

const handleFileChange = async (uploadFile: any) => {
  if (!uploadFile || !uploadFile.raw) return
  const raw: File = uploadFile.raw

  if (!/\.json$/i.test(raw.name)) {
    ElMessage.error('仅支持 .json 文件')
    return
  }
  if (raw.size > 10 * 1024 * 1024) {
    ElMessage.error('文件超过 10MB')
    return
  }

  resetImportProgress()
  importLoading.value = true
  try {
    const text = await raw.text()
    const data = JSON.parse(text)
    const problemsRaw: any[] = Array.isArray(data) ? data : (data.problems || [])

    if (problemsRaw.length === 0) {
      ElMessage.warning('文件中没有题目数据')
      return
    }

    // 自动识别 JSON 格式并统一映射为后端 payload
    // 格式A（系统标准）: { problemNo, title, difficulty, tags[], category, description, solution, ... }
    // 格式B（0-100.json 等外部导入）: { id, tag("JavaSE,多线程"), difficulty("Medium"), question, answer }
    const isExternalFormat = problemsRaw.some(p => p.question && p.answer && !p.description && !p.title)

    const payloads: ProblemSavePayload[] = problemsRaw.map(p => {
      if (isExternalFormat) {
        // 格式B -> 格式A 字段映射
        const rawDifficulty = String(p.difficulty ?? 'Medium').toLowerCase()
        const difficulty = ['easy', 'medium', 'hard'].includes(rawDifficulty) ? rawDifficulty : 'medium'
        const tagRaw = p.tag ?? ''
        const tags: string[] = Array.isArray(tagRaw)
          ? tagRaw.map((s: any) => String(s || '').trim()).filter(Boolean)
          : String(tagRaw).split(/[,，;；|]/).map(s => s.trim()).filter(Boolean)
        const category = tags.length > 0 ? tags[0] : '未分类'
        // question 太长时截取前30字作为标题
        const questionStr: string = String(p.question ?? '')
        const title = questionStr.length > 30 ? questionStr.substring(0, 30) + '...' : questionStr
        const idNum = p.id || 0
        const problemNo = p.problemNo || ('MS' + String(idNum).padStart(3, '0'))
        return {
          problemNo,
          title,
          difficulty,
          category,
          tags,
          description: questionStr,
          inputFormat: '',
          outputFormat: '',
          solution: String(p.answer ?? ''),
          status: 'ACTIVE',
          isFrequent: 0
        }
      }
      // 格式A 原样映射
      return {
        problemNo: p.problemNo,
        title: p.title,
        difficulty: p.difficulty || 'medium',
        category: p.category || '',
        tags: p.tags || [],
        description: p.description || '',
        inputFormat: p.inputFormat || '',
        outputFormat: p.outputFormat || '',
        solution: p.solution || '',
        status: toBackendStatus(p.status || 'offline'),
        isFrequent: typeof p.isFrequent === 'boolean' ? (p.isFrequent ? 1 : 0) : (p.isFrequent === 1 ? 1 : 0)
      }
    })

    // 【关键】前端已知总题数，立刻写入 clientTotal，让进度条从一开始就显示"总数 N 道"
    importProgress.clientTotal = payloads.length

    // 【关键】每次发起批量导入 HTTP 前新建 AbortController，取消时可中断浏览器端等待
    importAbortController?.abort()  // 防御性：防止上一个异常遗留的 controller
    importAbortController = new AbortController()

    // 【关键】提交 HTTP 之前启动所有计时器：
    //   - clientTickTimer 每秒 +1 秒表 + 推进模拟进度（后端未更新前）
    //   - progressPollTimer 每秒拉取后端 syncProgress，一旦后端有进度立刻覆盖模拟值
    startAllProgressTimers()

    const res = await batchImportProblems(payloads, importOverwrite.value, importAbortController.signal)
    const failNum = res?.failNum ?? 0
    const succNum = res?.successNum ?? 0

    // 写入最终结果 -> display computed 会立刻让进度条到 100% 并显示结果数字
    importProgress.clientResult = { successNum: succNum, failNum: failNum }
    importProgress.phase = failNum > 0 && succNum === 0 ? 'failed' : 'completed'
    importProgress.percent = 100
    importProgress.mockPercent = 100

    if (failNum > 0) {
      ElMessage.warning(`导入完成：成功 ${succNum} 道，失败 ${failNum} 道`)
      if (res?.failList?.length) ElMessageBox.alert(res.failList.join('<br/>'), '失败明细', { dangerouslyUseHTMLString: true, type: 'warning' })
    } else {
      ElMessage.success(`导入完成：成功 ${succNum} 道`)
    }
    importVisible.value = false
    loadData()
  } catch (e: any) {
    // 【关键】区分"用户主动取消"和"真实失败"：CanceledError（Axios 0.22+）/ code==ERR_CANCELED
    const isCanceled =
      e?.code === 'ERR_CANCELED'
      || e?.name === 'CanceledError'
      || e?.name === 'AbortError'
      || /cancel/i.test(e?.message || '')

    if (isCanceled) {
      // 前端已经在 cancelImportAndRollbackUI 中做了 reset + warning，此处只兜底
      importProgress.phase = 'failed'
      importProgress.lastError = '用户取消导入'
      return
    }
    importProgress.phase = 'failed'
    importProgress.lastError = e?.message || String(e || '未知错误')
    ElMessage.error('导入异常：' + (e.message || e))
  } finally {
    // 请求结束后再跑 3 秒：等后端最后一次进度刷新写入 syncProgress，同时让用户看到完整过渡到 100%
    setTimeout(() => {
      stopAllProgressTimers()
      // 收尾：如果弹窗仍然打开、进度还没补到 100（有 clientResult 但 percent<100），
      // 手动补最后 5% → 100，保证最终数字和绿勾都正常显示
      if (importProgress.clientResult && displayPercent.value < 99.9) {
        importProgress.percent = 100
        importProgress.mockPercent = 100
      }
      if (importAbortController && !importAbortController.signal.aborted) {
        importAbortController = null
      }
    }, 3000)
    importLoading.value = false
  }
}

const downloadTemplate = () => {
  const template = [
    {
      problemNo: 'MS001',
      title: '示例题目',
      difficulty: 'easy',
      tags: ['数组', '哈希表'],
      category: '数组',
      description: '题目描述（支持 Markdown 格式）',
      inputFormat: '输入格式说明',
      outputFormat: '输出格式说明',
      solution: '参考答案',
      isFrequent: true,
      status: 'online'
    }
  ]
  const blob = new Blob([JSON.stringify(template, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '面试题目模板.json'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('模板已开始下载')
}

const handleExport = () => {
  // 直接调用后端导出接口（避免前端遍历导致分页缺失）
  const url = exportProblemsUrl(searchForm.difficulty || undefined, searchForm.category || undefined)
  const a = document.createElement('a')
  a.href = url
  a.download = `interview_problems_${Date.now()}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  ElMessage.success('已提交导出，文件下载中...')
}

const getDifficultyType = (difficulty: string) => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = { easy: 'success', medium: 'warning', hard: 'danger' }
  return map[difficulty] || 'info'
}

const getDifficultyLabel = (difficulty: string) => {
  const map: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难' }
  return map[difficulty] || difficulty
}

const getCategoryLabel = (category: string) => {
  return categoryOptions.find(c => c.value === category)?.label || category
}

/* ================== AI 生成 ================== */

const aiDialogVisible = ref(false)
const aiPreviewVisible = ref(false)
const aiGenerating = ref(false)
const aiProgressText = ref('')
const aiGeneratedProblems = ref<any[]>([])

const aiConfigForm = reactive({
  knowledgePoints: [] as string[],
  difficulty: 'medium',
  count: 3,
  category: '',
  additionalRequirements: ''
})

const aiEditVisible = ref(false)
const aiEditForm = ref({
  draftId: '',
  problemNo: '',
  title: '',
  difficulty: 'medium',
  tags: '',
  category: '',
  description: '',
  inputFormat: '',
  outputFormat: '',
  solution: '',
  isFrequent: false
})

const aiTagOptions = tagOptions
const aiDifficultyOptions = difficultyOptions

const handleAIGenerate = () => {
  aiConfigForm.knowledgePoints = []
  aiConfigForm.difficulty = 'medium'
  aiConfigForm.count = 3
  aiConfigForm.category = ''
  aiConfigForm.additionalRequirements = ''
  aiGeneratedProblems.value = []
  aiDialogVisible.value = true
}

const handleAIGenerateSubmit = async () => {
  if (aiConfigForm.count < 1 || aiConfigForm.count > 10) {
    ElMessage.warning('题目数量需在 1~10 之间')
    return
  }
  if (aiConfigForm.knowledgePoints.length === 0) {
    ElMessage.warning('请至少选择 1 个知识点')
    return
  }
  aiGenerating.value = true
  aiProgressText.value = '正在调用后端 AI 生成题目...'
  aiGeneratedProblems.value = []
  try {
    const category = aiConfigForm.category || aiConfigForm.knowledgePoints[0] || ''
    const res = await aiGenerateProblems({
      category: category,
      difficulty: aiConfigForm.difficulty,
      num: aiConfigForm.count
    }) as any[] || []
    aiGeneratedProblems.value = res.map((r, i) => ({
      draftId: `draft_${Date.now()}_${i}`,
      problemNo: r.problemNo || '',
      title: r.title || '',
      difficulty: r.difficulty || 'medium',
      tags: Array.isArray(r.tags) ? (r.tags as string[]).join(',') : (r.tags || aiConfigForm.knowledgePoints.join(',')),
      category: r.category || category,
      description: r.description || '',
      inputFormat: r.inputFormat || '',
      outputFormat: r.outputFormat || '',
      solution: r.solution || '',
      isFrequent: r.isFrequent ? true : false,
      selected: true
    }))
    aiProgressText.value = `成功生成 ${aiGeneratedProblems.value.length} 道面试题`
    aiDialogVisible.value = false
    aiPreviewVisible.value = true
    ElMessage.success('生成完成')
  } catch (e: any) {
    ElMessage.error('AI 生成失败：' + (e?.message || e))
  } finally {
    aiGenerating.value = false
  }
}

const handleRemoveGenerated = (index: number) => {
  aiGeneratedProblems.value.splice(index, 1)
}

const handleBatchAddToLibrary = async () => {
  const selected = aiGeneratedProblems.value.filter(p => p.selected)
  if (selected.length === 0) {
    ElMessage.warning('请至少勾选 1 道题目')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定将选中的 ${selected.length} 道面试题添加到题库？`,
      '批量入库',
      { type: 'info' }
    )
  } catch { return }

  const payloads: ProblemSavePayload[] = selected.map(p => ({
    problemNo: p.problemNo || undefined,
    title: p.title,
    difficulty: p.difficulty || 'medium',
    category: p.category || '',
    tags: (p.tags || '').split(',').map((s: string) => s.trim()).filter(Boolean),
    description: p.description || '',
    inputFormat: p.inputFormat || '',
    outputFormat: p.outputFormat || '',
    solution: p.solution || '',
    status: 'INACTIVE',
    isFrequent: p.isFrequent ? 1 : 0
  }))

  try {
    const res = await aiBatchSave(payloads)
    ElMessageBox.alert(
      `成功入库 ${res?.successNum ?? 0} 道，失败 ${res?.failNum ?? 0} 道`,
      '批量入库完成',
      { type: (res?.successNum ?? 0) > 0 ? 'success' : 'error' }
    )
    if ((res?.successNum ?? 0) > 0) {
      aiGeneratedProblems.value = aiGeneratedProblems.value.filter(p => !p.selected)
      loadData()
      if (aiGeneratedProblems.value.length === 0) aiPreviewVisible.value = false
    }
  } catch (e: any) {
    ElMessage.error('批量入库失败：' + (e?.message || e))
  }
}

const handleEditGenerated = (p: any) => {
  aiEditForm.value = {
    draftId: p.draftId,
    problemNo: p.problemNo || '',
    title: p.title || '',
    difficulty: p.difficulty || 'medium',
    tags: p.tags || '',
    category: p.category || '',
    description: p.description || '',
    inputFormat: p.inputFormat || '',
    outputFormat: p.outputFormat || '',
    solution: p.solution || '',
    isFrequent: p.isFrequent || false
  }
  aiEditVisible.value = true
}

const saveAIEdit = () => {
  if (!aiEditForm.value.title) {
    ElMessage.warning('请输入题目标题')
    return
  }
  const target = aiGeneratedProblems.value.find(p => p.draftId === aiEditForm.value.draftId)
  if (target) {
    target.problemNo = aiEditForm.value.problemNo
    target.title = aiEditForm.value.title
    target.difficulty = aiEditForm.value.difficulty
    target.tags = aiEditForm.value.tags
    target.category = aiEditForm.value.category
    target.description = aiEditForm.value.description
    target.inputFormat = aiEditForm.value.inputFormat
    target.outputFormat = aiEditForm.value.outputFormat
    target.solution = aiEditForm.value.solution
    target.isFrequent = aiEditForm.value.isFrequent
  }
  ElMessage.success('已保存到草稿')
  aiEditVisible.value = false
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>面试题目管理</h2>
      <div>
        <el-button :icon="Upload" @click="handleBatchImport">批量导入</el-button>
        <el-button :icon="Download" @click="handleExport">导出</el-button>
        <el-button type="success" :icon="MagicStick" @click="handleAIGenerate">AI 生成题目</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增面试题</el-button>
      </div>
    </div>

    <div class="card-container">
      <div class="filter-bar">
        <el-input v-model="searchForm.keyword" placeholder="题号/标题/标签" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <el-select v-model="searchForm.tag" placeholder="标签" clearable filterable style="width: 140px">
          <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
        </el-select>
        <el-select v-model="searchForm.difficulty" placeholder="难度" clearable style="width: 100px">
          <el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="searchForm.category" placeholder="分类" clearable style="width: 120px">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 100px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="pagedProblems" v-loading="loading" stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="problemNo" label="题号" width="80" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="difficulty" label="难度" width="80">
          <template #default="{ row }">
            <el-tag :type="getDifficultyType(row.difficulty)" size="small">
              {{ getDifficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="tag in row.tags" :key="tag" size="small" style="margin-right: 4px; margin-bottom: 2px">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ getCategoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isFrequent" label="高频" width="60" align="center">
          <template #default="{ row }">
            <span v-if="row.isFrequent" style="color: #e6a23c; font-weight: bold;">✓</span>
            <span v-else style="color: #c0c4cc;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              inline-prompt
              active-value="online"
              inactive-value="offline"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览数" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="140" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="(p: number) => { page = p }"
          @size-change="(s: number) => { pageSize = s; page = 1 }"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="720px"
      :fullscreen="dialogFullscreen"
      :top="'calc(10vh - 30px)'"
      @closed="onDialogClosed"
    >
      <template #header>
        <div class="dialog-header">
          <span>{{ dialogTitle }}</span>
          <el-button
            :icon="dialogFullscreen ? Aim : FullScreen"
            circle
            size="small"
            @click="dialogFullscreen = !dialogFullscreen"
          />
        </div>
      </template>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="题号" prop="problemNo">
          <el-input v-model="formData.problemNo" placeholder="如 MS001" :disabled="!!formData.id" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入题目标题" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-radio-group v-model="formData.difficulty">
            <el-radio v-for="item in difficultyOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="formData.tags" multiple filterable allow-create placeholder="选择或输入标签" style="width: 100%">
            <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="6" placeholder="支持 Markdown 格式" />
        </el-form-item>
        <el-form-item label="输入格式">
          <el-input v-model="formData.inputFormat" type="textarea" :rows="2" placeholder="描述输入参数的格式" />
        </el-form-item>
        <el-form-item label="输出格式">
          <el-input v-model="formData.outputFormat" type="textarea" :rows="2" placeholder="描述输出结果的格式" />
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input v-model="formData.solution" type="textarea" :rows="6" placeholder="支持 Markdown 格式，包含解题思路和代码实现" style="font-family: 'Fira Code', Consolas, monospace; font-size: 13px" />
        </el-form-item>
        <el-form-item label="高频标记">
          <el-switch v-model="formData.isFrequent" active-text="高频" inactive-text="普通" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio value="online">上线</el-radio>
            <el-radio value="offline">下线</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="面试题详情" width="800px">
      <template v-if="currentProblem">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="题号">{{ currentProblem.problemNo }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ currentProblem.title }}</el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-tag :type="getDifficultyType(currentProblem.difficulty)" size="small">
              {{ getDifficultyLabel(currentProblem.difficulty) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="分类">
            <el-tag size="small" effect="plain">{{ getCategoryLabel(currentProblem.category) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="高频">
            <span v-if="currentProblem.isFrequent" style="color: #e6a23c; font-weight: bold;">✓ 高频</span>
            <span v-else>普通</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentProblem.status === 'online' ? '上线' : '下线' }}</el-descriptions-item>
          <el-descriptions-item label="浏览数">{{ currentProblem.viewCount }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentProblem.createTime }}</el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <el-tag v-for="tag in currentProblem.tags" :key="tag" size="small" style="margin-right: 4px">{{ tag }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">题目描述</el-divider>
        <div class="markdown-body" v-html="renderMarkdown(currentProblem.description || '')"></div>

        <template v-if="currentProblem.inputFormat">
          <el-divider content-position="left">输入格式</el-divider>
          <div class="problem-description">{{ currentProblem.inputFormat }}</div>
        </template>

        <template v-if="currentProblem.outputFormat">
          <el-divider content-position="left">输出格式</el-divider>
          <div class="problem-description">{{ currentProblem.outputFormat }}</div>
        </template>

        <el-divider content-position="left">参考答案</el-divider>
        <div class="markdown-body answer-markdown" v-html="renderMarkdown(currentProblem.solution || '暂无参考答案')"></div>
      </template>
    </el-dialog>

    <!-- AI 生成配置弹窗 -->
    <el-dialog
      v-model="aiDialogVisible"
      title="AI 生成面试题 - 配置参数"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px" :disabled="aiGenerating">
        <el-form-item label="知识点 / 标签" required>
          <el-select
            v-model="aiConfigForm.knowledgePoints"
            multiple
            filterable
            allow-create
            placeholder="选择或输入知识点（至少 1 个）"
            style="width: 100%"
          >
            <el-option v-for="tag in aiTagOptions" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="aiConfigForm.difficulty">
            <el-radio v-for="item in aiDifficultyOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题目数量">
          <el-input-number v-model="aiConfigForm.count" :min="1" :max="10" />
          <span style="margin-left: 12px; color: #909399; font-size: 12px">建议 1~5 道，避免超时</span>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="aiConfigForm.category" placeholder="选择题目分类（可选）" clearable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="额外要求">
          <el-input
            v-model="aiConfigForm.additionalRequirements"
            type="textarea"
            :rows="3"
            placeholder="可选：例如特定考点、难度要求等"
          />
        </el-form-item>
        <el-form-item v-if="aiGenerating" label="进度">
          <el-progress :percentage="100" :indeterminate="true" :duration="1" />
          <div style="margin-top: 8px; color: #409eff; font-size: 13px">{{ aiProgressText }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiDialogVisible = false" :disabled="aiGenerating">取消</el-button>
        <el-button type="primary" :loading="aiGenerating" :icon="MagicStick" @click="handleAIGenerateSubmit">
          {{ aiGenerating ? '生成中...' : '开始生成' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- AI 生成结果预览弹窗 -->
    <el-dialog
      v-model="aiPreviewVisible"
      title="AI 生成结果预览"
      width="1100px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div class="ai-preview-toolbar">
        <div>
          <span style="color: #909399; font-size: 12px">
            已勾选 {{ aiGeneratedProblems.filter(p => p.selected).length }} / {{ aiGeneratedProblems.length }} 道
          </span>
        </div>
        <div>
          <el-button @click="aiDialogVisible = true; aiPreviewVisible = false">重新生成</el-button>
          <el-button type="primary" :icon="Promotion" @click="handleBatchAddToLibrary">
            批量添加到题库
          </el-button>
        </div>
      </div>

      <div v-if="aiGeneratedProblems.length === 0" style="padding: 60px 0">
        <el-empty description="暂无生成结果" />
      </div>

      <div v-else class="ai-problem-list">
        <div v-for="(p, idx) in aiGeneratedProblems" :key="p.draftId" class="ai-problem-card">
          <div class="card-header">
            <el-checkbox v-model="p.selected" />
            <span class="problem-no">{{ p.problemNo || `待分配（#${idx+1}）` }}</span>
            <el-input v-model="p.title" class="title-input" placeholder="题目标题" />
            <el-select v-model="p.difficulty" style="width: 100px">
              <el-option v-for="item in aiDifficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button link type="danger" @click="handleRemoveGenerated(idx)">删除</el-button>
            <el-button link type="primary" @click="handleEditGenerated(p)">单题编辑</el-button>
          </div>

          <div class="card-body">
            <el-form label-width="80px" size="small">
              <el-form-item label="标签">
                <el-input v-model="p.tags" placeholder="多个标签用英文逗号分隔" />
              </el-form-item>
              <el-form-item label="分类">
                <el-select v-model="p.category" placeholder="选择分类" style="width: 100%">
                  <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="题目描述">
                <el-input
                  v-model="p.description"
                  type="textarea"
                  :rows="4"
                  placeholder="支持 Markdown 格式"
                />
              </el-form-item>
              <div class="form-row">
                <el-form-item label="输入格式" style="flex: 1">
                  <el-input v-model="p.inputFormat" type="textarea" :rows="2" />
                </el-form-item>
                <el-form-item label="输出格式" style="flex: 1; margin-left: 12px">
                  <el-input v-model="p.outputFormat" type="textarea" :rows="2" />
                </el-form-item>
              </div>
              <el-form-item label="参考答案">
                <el-input
                  v-model="p.solution"
                  type="textarea"
                  :rows="5"
                  placeholder="解题思路和代码实现"
                  style="font-family: 'Fira Code', Consolas, monospace; font-size: 12px"
                />
              </el-form-item>
              <el-form-item label="高频标记">
                <el-switch v-model="p.isFrequent" size="small" />
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="aiPreviewVisible = false">关闭</el-button>
        <el-button type="primary" :icon="Promotion" @click="handleBatchAddToLibrary">
          批量添加 {{ aiGeneratedProblems.filter(p => p.selected).length }} 道到题库
        </el-button>
      </template>
    </el-dialog>

    <!-- AI 单题编辑弹窗 -->
    <el-dialog
      v-model="aiEditVisible"
      title="单题编辑（AI 草稿）"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px" size="default">
        <el-form-item label="题号">
          <el-input v-model="aiEditForm.problemNo" placeholder="留空则入库时由系统自动分配" />
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="aiEditForm.title" placeholder="题目标题" />
        </el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="aiEditForm.difficulty">
            <el-radio-button value="easy">简单</el-radio-button>
            <el-radio-button value="medium">中等</el-radio-button>
            <el-radio-button value="hard">困难</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="aiEditForm.tags" placeholder="多个标签用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="aiEditForm.category" placeholder="选择分类" style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目描述">
          <el-input v-model="aiEditForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="输入格式">
          <el-input v-model="aiEditForm.inputFormat" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="输出格式">
          <el-input v-model="aiEditForm.outputFormat" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input v-model="aiEditForm.solution" type="textarea" :rows="6" style="font-family: 'Fira Code', Consolas, monospace; font-size: 13px" />
        </el-form-item>
        <el-form-item label="高频标记">
          <el-switch v-model="aiEditForm.isFrequent" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAIEdit">保存到草稿</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog
      v-model="importVisible"
      title="批量导入面试题"
      width="640px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="beforeImportDialogClose"
    >
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        <template #title>
          仅支持 JSON 格式导入。请确保文件格式符合模板要求。
        </template>
      </el-alert>

      <el-form label-width="120px">
        <el-form-item label="题号冲突处理">
          <el-switch
            v-model="importOverwrite"
            active-text="覆盖（题号冲突时更新）"
            inactive-text="跳过（题号冲突时跳过）"
          />
          <div style="color:#909399;font-size:12px;margin-top:4px">
            推荐保持 <b>关闭</b>：避免误覆盖已有题目
          </div>
        </el-form-item>

        <el-form-item label="选择 JSON 文件">
          <el-upload
            action=""
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".json"
            :on-change="handleFileChange"
            :on-exceed="() => ElMessage.warning('只支持 1 个文件')"
            :disabled="importLoading"
          >
            <el-button type="primary" :icon="Upload" :loading="importLoading">
              {{ importLoading ? '导入中...' : '点击选择 JSON 文件' }}
            </el-button>
            <template #tip>
              <div style="color:#909399;font-size:12px;margin-top:8px">
                支持 .json 格式（数组或 { problems: [...] } 包装对象），最大 10MB。
                <el-button type="primary" link size="small" @click="downloadTemplate">下载模板</el-button>
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <!-- 导入进度条：导入中或后端有同步任务时显示，实时展示已导入/剩余/时间/成功失败数 -->
      <el-card v-if="importProgressVisible" shadow="never" class="import-progress-card">
        <div class="import-progress-header">
          <el-icon v-if="!isImportFinished && (importProgress.inProgress || importLoading)" class="spin-icon" :size="16" color="#409eff">
            <Loading />
          </el-icon>
          <el-icon v-else-if="importProgress.phase === 'failed'" :size="16" color="#f56c6c">
            <Warning />
          </el-icon>
          <el-icon v-else-if="isImportFinished" :size="16" color="#67c23a">
            <Refresh />
          </el-icon>
          <span class="import-progress-title">
            {{ progressHeaderText }}
          </span>
          <!-- 右上角实时百分比（1 位小数，导入中保持动画感） -->
          <span class="import-progress-percent">
            {{ displayPercent.toFixed(displayPercent % 1 === 0 ? 0 : 1) }}%
          </span>
        </div>

        <el-progress
          :percentage="Number(displayPercent.toFixed(1))"
          :status="progressStatusType"
          :stroke-width="22"
          :format="(p: number) => `${p}%`"
          striped
          striped-flow
        />

        <!-- 统计面板：导入中 6 列（含剩余/预计），完成态 4 列（已导入/失败/总数/用时） -->
        <div v-if="!isImportFinished" class="import-progress-stats stats-6col">
          <div class="stat-item">
            <span class="stat-label">已导入</span>
            <span class="stat-value stat-ok">{{ displayProcessed }} 道</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-label">失败</span>
            <span class="stat-value stat-err">{{ displayFailed }} 道</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-label">总数</span>
            <span class="stat-value">{{ displayTotal }} 道</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-label">未导入</span>
            <span class="stat-value stat-warn">{{ displayRemaining }} 道</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-label">已用时间</span>
            <span class="stat-value time-value">{{ formatSeconds(displayElapsed) }}</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-label">预计剩余</span>
            <span class="stat-value time-value stat-warn">
              {{ formatSeconds(displayEstimated) }}
            </span>
          </div>
        </div>

        <div v-else class="import-progress-stats stats-4col">
          <div class="stat-item">
            <span class="stat-label">已导入</span>
            <span class="stat-value stat-ok">{{ displayProcessed }} 道</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-label">失败</span>
            <span class="stat-value stat-err">{{ displayFailed }} 道</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-label">总数</span>
            <span class="stat-value">{{ displayTotal }} 道</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-label">用时</span>
            <span class="stat-value time-value">{{ formatSeconds(displayElapsed) }}</span>
          </div>
        </div>

        <div v-if="importProgress.message && !isImportFinished" class="import-progress-msg">
          <span class="msg-label">状态：</span>
          <span class="msg-text">{{ importProgress.message }}</span>
        </div>

        <el-alert
          v-if="importProgress.phase === 'failed' && importProgress.lastError"
          type="error"
          :title="'导入失败: ' + importProgress.lastError"
          show-icon
          :closable="false"
          style="margin-top: 10px"
        />
      </el-card>

      <template #footer>
        <el-button @click="importVisible = false" :disabled="importLoading">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  font-size: 16px;
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.problem-description {
  white-space: pre-wrap;
  line-height: 1.6;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
  min-height: 60px;
}

.markdown-body {
  line-height: 1.8;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  min-height: 60px;
  word-break: break-word;
  color: #303133;
}
.markdown-body h1, .markdown-body h2, .markdown-body h3, .markdown-body h4 {
  margin: 16px 0 8px;
  line-height: 1.4;
  font-weight: 700;
  color: #303133;
}
.markdown-body h1 { font-size: 1.5em; }
.markdown-body h2 { font-size: 1.3em; }
.markdown-body h3 { font-size: 1.15em; }
.markdown-body h4 { font-size: 1.05em; }
.markdown-body p { margin: 8px 0; }
.markdown-body ul, .markdown-body ol {
  padding-left: 24px;
  margin: 8px 0;
}
.markdown-body li { margin: 4px 0; }
.markdown-body ul li { list-style: disc; }
.markdown-body ol li { list-style: decimal; }
.markdown-body code {
  background: #f0f2f5;
  color: #d63384;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', Consolas, monospace;
  font-size: 0.9em;
}
.markdown-body pre {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 14px 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
}
.markdown-body pre code {
  background: none;
  padding: 0;
  color: #d4d4d4;
  font-size: 0.9em;
  line-height: 1.6;
}
.markdown-body blockquote {
  border-left: 4px solid #409eff;
  padding: 8px 16px;
  margin: 12px 0;
  background: rgba(64, 158, 255, 0.08);
  border-radius: 0 6px 6px 0;
  color: #606266;
}
.markdown-body blockquote p { margin: 4px 0; }
.markdown-body table {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}
.markdown-body th, .markdown-body td {
  border: 1px solid #ebeef5;
  padding: 8px 12px;
  text-align: left;
}
.markdown-body th {
  background: #f5f7fa;
  font-weight: 600;
  color: #303133;
}
.markdown-body a {
  color: #409eff;
  text-decoration: none;
}
.markdown-body a:hover { text-decoration: underline; }
.markdown-body strong { font-weight: 700; color: #303133; }
.markdown-body hr {
  border: none;
  border-top: 1px solid #ebeef5;
  margin: 20px 0;
}
.markdown-body img { max-width: 100%; border-radius: 6px; }

.answer-markdown {
  background: #fafbfc;
  border-color: #e4e7ed;
}

.ai-preview-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 16px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 16px;
}

.ai-problem-list {
  max-height: 65vh;
  overflow-y: auto;
  padding-right: 8px;
}

.ai-problem-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 16px;
  transition: all 0.2s;
  background: #fff;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: #f5f7fa;
    border-bottom: 1px solid #ebeef5;
    border-radius: 6px 6px 0 0;

    .problem-no {
      font-family: 'Fira Code', Consolas, monospace;
      font-weight: bold;
      color: #409eff;
      min-width: 50px;
    }

    .title-input {
      flex: 1;
    }
  }

  .card-body {
    padding: 16px 20px;
  }
}

.form-row {
  display: flex;
  gap: 0;
  width: 100%;
}

/* ============ 导入进度条样式 ============ */
.import-progress-card {
  margin-top: 16px;
  border: 1px solid #d9ecff;
  background: #f5fbff;
}

.import-progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  position: relative;
}

.import-progress-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

/* 右上角实时百分比 */
.import-progress-percent {
  font-size: 22px;
  font-weight: 700;
  color: #409eff;
  font-variant-numeric: tabular-nums;
  font-family: 'Fira Code', Consolas, monospace;
  line-height: 1;
}

.spin-icon {
  animation: import-spin 1s linear infinite;
}
@keyframes import-spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

.import-progress-stats {
  display: grid;
  gap: 0;
  margin-top: 14px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}

/* 6 列：已导入 / 失败 / 总数 / 未导入 / 已用时间 / 预计剩余 */
.import-progress-stats.stats-6col {
  grid-template-columns: 1fr 0.05fr 1fr 0.05fr 1fr 0.05fr 1fr 0.05fr 1fr 0.05fr 1fr;
}

/* 4 列（完成态）：已导入 / 失败 / 总数 / 用时，字体更大更突出 */
.import-progress-stats.stats-4col {
  grid-template-columns: 1fr 0.05fr 1fr 0.05fr 1fr 0.05fr 1fr;
}

.import-progress-stats .stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 14px 4px;
}
.import-progress-stats.stats-4col .stat-item {
  padding: 20px 4px;
}
.import-progress-stats .stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.import-progress-stats .stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  font-variant-numeric: tabular-nums;
}
.import-progress-stats.stats-4col .stat-value {
  font-size: 26px;
}
/* 时间字段字号略小，避免溢出 6 列布局 */
.import-progress-stats.stats-6col .stat-value.time-value {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: -0.2px;
}
.import-progress-stats.stats-4col .stat-value.time-value {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.1px;
}
.import-progress-stats .stat-ok   { color: #67c23a; }
.import-progress-stats .stat-err { color: #f56c6c; }
.import-progress-stats .stat-warn{ color: #e6a23c; }

.import-progress-stats .stat-divider {
  width: 1px;
  height: 40px;
  align-self: center;
  justify-self: center;
  background: #ebeef5;
}
.import-progress-stats.stats-4col .stat-divider {
  height: 48px;
}

.import-progress-msg {
  margin-top: 10px;
  padding: 8px 12px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 4px;
  font-size: 13px;
}
.import-progress-msg .msg-label {
  flex-shrink: 0;
  font-weight: 600;
  color: #409eff;
}
.import-progress-msg .msg-text {
  color: #303133;
  word-break: break-all;
}

@media (max-width: 640px) {
  .import-progress-stats.stats-6col {
    /* 窄屏 3 列 + 2 分隔线 共 5 列，2 行堆叠 */
    grid-template-columns: 1fr 0.05fr 1fr 0.05fr 1fr;
  }
  .import-progress-stats.stats-6col .stat-divider:nth-child(10) { display: none; }
  .import-progress-stats.stats-4col {
    /* 2 列 + 1 分隔线 共 3 列，2 行堆叠 */
    grid-template-columns: 1fr 0.05fr 1fr;
  }
  .import-progress-stats .stat-value {
    font-size: 18px;
  }
  .import-progress-stats.stats-4col .stat-value {
    font-size: 20px;
  }
  .import-progress-stats.stats-6col .stat-value.time-value {
    font-size: 12px;
  }
}
</style>
