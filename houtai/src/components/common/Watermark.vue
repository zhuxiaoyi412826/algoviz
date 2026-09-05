<script setup lang="ts">
/**
 * ============================================================================
 * 后台管理系统全局水印组件（Vue3 setup 语法糖 + TypeScript）
 * ============================================================================
 *
 * 实现要点：
 *   1. 数据源    : 从 useUserStore 取当前登录账号(nickname/username)，可附加时间/租户ID
 *   2. 渲染方案  : Canvas 绘制单个倾斜水印 tile -> dataURL -> div background repeat 全屏平铺
 *   3. 安全防破解: MutationObserver 监听 DOM，删除/隐藏(display:none/visibility/opacity 等)
 *                 自动恢复（防 F12 删节点/改样式）
 *   4. 开关控制  : stores/watermark.ts 提供全局 enabled + hiddenRoles 角色维度；
 *                 组件 watch 配置动态开关/重绘
 *   5. 样式可配  : 透明度/字号/旋转角/颜色/间距/zIndex 全部来自 config（可 props 覆盖）
 *   6. 生命周期  : 挂在登录后的 layout 内 —— 登录自动出现；退出登录 layout 销毁 -> 本组件
 *                 onBeforeUnmount 清理 DOM + observer；路由切换 layout 常驻 -> 水印保持
 *   7. 边界      : z-index 默认 999 < Element Plus 弹层(2000+)，
 *                 因此 Dialog/Drawer/Message 浮层不会被水印覆盖；
 *                 组件本身 pointer-events:none，不拦截任何鼠标操作
 *   8. 备注      : 仅防截图/肉眼泄密的前端体验防护，不能作为安全唯一手段，
 *                 敏感操作需后端配套审计日志（本系统已有 OperationLogAspect）
 *
 * 【全局注册示例】(main.ts)
 *   import Watermark from '@/components/common/Watermark.vue'
 *   app.component('Watermark', Watermark)
 *   之后任意模板可用 <Watermark />（layout/index.vue 按局部 import 使用，利于类型推导）
 *
 * 【store 获取用户信息示例】
 *   const userStore = useUserStore()          // 登录账号: userInfo.nickname/username
 *   const watermarkStore = useWatermarkStore()// 开关与样式配置 config / shouldShow(roles)
 * ============================================================================
 */
import { computed, inject, onBeforeUnmount, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useWatermarkStore } from '@/stores/watermark'

/* ---------- Props（可在任意页面直接覆盖，便于局部定制） ---------- */
interface Props {
  /** 水印文本（默认由 store 账号信息自动拼接，设置后覆盖默认） */
  text?: string
  /** 附加当前登录时间（默认取 store.config.showTime） */
  showTime?: boolean
  /** 附加租户 ID（默认取 store.config.tenantId） */
  tenantId?: string
}

const props = withDefaults(defineProps<Props>(), {
  text: '',
  showTime: true,
  tenantId: undefined
})

const userStore = useUserStore()
const watermarkStore = useWatermarkStore()
const { config } = watermarkStore

/* 跳过最左侧菜单栏：展开 220px / 折叠 64px（与 layout/sider/index.vue 一致） */
const SIDEBAR_W = 220
const SIDEBAR_W_COLLAPSED = 64
const layoutState = inject<{ sidebarCollapsed: { value: boolean } } | null>('layoutState', null)
const sidebarLeft = computed(() => {
  // 未注入 layoutState（组件用在无菜单栏页面）-> 全屏水印
  if (!layoutState) return 0
  return layoutState.sidebarCollapsed?.value ? SIDEBAR_W_COLLAPSED : SIDEBAR_W
})

/* ========== 1. 数据源：由当前登录用户信息生成水印文本 ========== */
const defaultLines = computed<string[]>(() => {
  const info = userStore.userInfo
  const nickname = info?.nickname || ''
  const username = info?.username || ''

  const lines: string[] = []
  if (nickname && username) lines.push(`${nickname}(${username})`)
  else lines.push(username || nickname || '当前用户')

  const extra: string[] = []
  const useTime = props.showTime !== undefined ? props.showTime : config.showTime
  if (useTime) extra.push(new Date().toLocaleString())
  const tenantId = props.tenantId !== undefined ? props.tenantId : config.tenantId
  if (tenantId) extra.push(`租户:${tenantId}`)
  if (extra.length) lines.push(extra.join('  '))

  const role = userStore.roles?.[0]
  if (role) lines.push(role)
  return lines
})

const resolvedText = computed(() => (props.text ? [props.text] : defaultLines.value))

/** 是否应显示：已登录 && 全局开关 && 角色不在隐藏名单 */
const visible = computed(() => {
  if (!userStore.token) return false
  return watermarkStore.shouldShow(userStore.roles)
})

/* ========== 3. 防篡改相关 ========== */
const ROOT_ID = '__algoviz_watermark_root__'
let rootEl: HTMLDivElement | null = null
let rootObserver: MutationObserver | null = null
let styleObserver: MutationObserver | null = null
let recoverTimer: number | null = null

function scheduleRecover() {
  if (recoverTimer) window.clearTimeout(recoverTimer)
  recoverTimer = window.setTimeout(() => {
    recoverTimer = null
    renderWatermark()
  }, 200)
}

/** 检查元素是否被隐藏（display/visibility/opacity 0 / 尺寸为 0） */
function styleHidden(el: HTMLElement): boolean {
  const style = window.getComputedStyle(el)
  if (style.display === 'none' || style.visibility === 'hidden') return true
  if (Number(style.opacity) === 0) return true
  const rect = el.getBoundingClientRect()
  if (rect.width === 0 && rect.height === 0) return true
  return false
}

/* ========== 2. 渲染方案：Canvas 绘制水印 + dataURL 平铺 ========== */
function hexToRgb(hex: string): { r: number; g: number; b: number } {
  let h = hex.replace('#', '')
  if (h.length === 3) h = h.split('').map((c) => c + c).join('')
  const num = parseInt(h, 16)
  return { r: (num >> 16) & 255, g: (num >> 8) & 255, b: num & 255 }
}

function renderWatermark() {
  if (!visible.value) return
  const lines = resolvedText.value
  if (!lines.length) return

  const fontSize = config.fontSize
  const gapX = config.gapX
  const gapY = config.gapY
  const rgb = hexToRgb(config.color)

  // --- 单 tile Canvas ---
  const canvas = document.createElement('canvas')
  canvas.width = gapX
  canvas.height = gapY
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.clearRect(0, 0, gapX, gapY)
  ctx.translate(gapX / 2, gapY / 2)
  ctx.rotate((config.rotate * Math.PI) / 180)
  ctx.fillStyle = `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${config.opacity})`
  ctx.font = `${config.fontWeight} ${fontSize}px "Microsoft YaHei", sans-serif`
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.lineJoin = 'round'
  // 白色细描边：让文字在深浅不同底色下均可辨识（对比度增强，而非加浓）
  if (config.textStroke) {
    ctx.strokeStyle = `rgba(255, 255, 255, ${Math.min(0.45, config.opacity * 1.5)})`
    ctx.lineWidth = Math.max(1, fontSize / 12)
  }

  const lineHeight = fontSize * 1.6
  lines.forEach((line, i) => {
    const y = (i - (lines.length - 1) / 2) * lineHeight
    if (config.textStroke) ctx.strokeText(line, 0, y)
    ctx.fillText(line, 0, y)
  })
  const dataUrl = canvas.toDataURL('image/png')

  // --- 根节点 + 平铺 ---
  if (!rootEl || !rootEl.isConnected) {
    rootEl = document.createElement('div')
    rootEl.id = ROOT_ID
    document.body.appendChild(rootEl)
  }
  rootEl.setAttribute('data-wm', lines.join('|'))
  // left 跳过最左侧菜单栏（展开 220px / 折叠 64px），折叠切换时由 watch 触发重绘
  rootEl.style.cssText = [
    'position:fixed',
    'top:0',
    `left:${sidebarLeft.value}px`,
    'right:0',
    'bottom:0',
    `z-index:${config.zIndex}`,
    'pointer-events:none',
    'overflow:hidden'
  ].join(';')
  rootEl.style.backgroundImage = `url(${dataUrl})`
  rootEl.style.backgroundRepeat = 'repeat'
  rootEl.style.backgroundSize = `${gapX}px ${gapY}px`
  rootEl.setAttribute('aria-hidden', 'true')
}

/* ========== 3. 防篡改：MutationObserver ========== */
/** 观察 body 子节点：水印根节点被删除则恢复 */
function startRootObserver() {
  if (rootObserver) rootObserver.disconnect()
  rootObserver = new MutationObserver(() => {
    if (!rootEl || !document.body.contains(rootEl)) {
      scheduleRecover()
    }
  })
  rootObserver.observe(document.body, { childList: true })
}

/** 观察水印根节点属性：关键样式被删/被隐藏则恢复 */
function startStyleObserver() {
  if (styleObserver) styleObserver.disconnect()
  if (!rootEl) return
  styleObserver = new MutationObserver(() => {
    if (!rootEl || !document.body.contains(rootEl)) {
      scheduleRecover()
      return
    }
    const css = rootEl.style.cssText || ''
    const needBase =
      css.indexOf('position:fixed') !== -1 &&
      css.indexOf('pointer-events:none') !== -1
    if (!needBase || styleHidden(rootEl)) {
      scheduleRecover()
    }
  })
  styleObserver.observe(rootEl, { attributes: true, attributeFilter: ['style', 'class'] })
}

/* ========== 可见性同步 ========== */
function syncByVisible() {
  if (visible.value) {
    renderWatermark()
    startRootObserver()
    startStyleObserver()
  } else {
    destroyWatermark()
  }
}

/* ========== 销毁（页面卸载/登出/开关关闭） ========== */
function destroyWatermark() {
  if (recoverTimer) {
    window.clearTimeout(recoverTimer)
    recoverTimer = null
  }
  if (rootObserver) {
    rootObserver.disconnect()
    rootObserver = null
  }
  if (styleObserver) {
    styleObserver.disconnect()
    styleObserver = null
  }
  if (rootEl && rootEl.parentNode) {
    rootEl.parentNode.removeChild(rootEl)
  }
  rootEl = null
}

/* ========== 6. 生命周期 ========== */
onMounted(() => {
  syncByVisible()
  // 登录/登出、文本、样式配置、开关变化 -> 动态更新
  watch(
    [
      visible,
      resolvedText,
      sidebarLeft,
      () => config.opacity,
      () => config.fontSize,
      () => config.fontWeight,
      () => config.textStroke,
      () => config.rotate,
      () => config.gapX,
      () => config.gapY,
      () => config.color,
      () => config.zIndex
    ],
    () => syncByVisible()
  )
})

onBeforeUnmount(() => {
  destroyWatermark()
})
</script>

<!-- 组件自身无业务可见 DOM，水印节点由 JS 挂载到 body（body 级 fixed 保证路由切换不丢失） -->
<template></template>
