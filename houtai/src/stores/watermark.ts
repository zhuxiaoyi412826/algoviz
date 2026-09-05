import { defineStore } from 'pinia'
import { reactive, watch } from 'vue'

/**
 * 后台管理系统 - 全局水印配置 Store
 *
 * 作用：
 *   1. 提供全局开关（enabled），登录后由 layout 内的 Watermark 组件读取；
 *   2. 样式可配置项集中管理（透明度/字号/旋转角/间距/颜色/zIndex）；
 *   3. 支持按角色隐藏水印（hiddenRoles）；
 *   4. 配置自动持久化到 localStorage（刷新不丢失）。
 *
 * 【对接配置中心】若后续要由后端"配置中心"动态下发，只需：
 *   const data = await request.get('/api/admin/config/watermark')  // 后端配置接口
 *   setConfig(data)   // 同步到本地即可，Watermark 组件 watch 后自动重绘
 *
 * 【角色维度】hiddenRoles 示例：['DATA_ANALYST'] 表示数据分析管理员不显示水印。
 *   SUPER_ADMIN（超级管理员）默认不在 hiddenRoles —— 超管也要有水印（防截屏泄密）。
 */

const STORAGE_KEY = 'algoviz-watermark-config'

export interface WatermarkConfig {
  /** 本地配置版本号：改动默认样式后自增，使旧缓存失效（避免用户存旧的疏密参数） */
  schemaVersion?: number
  /** 全局开关：true=开启 false=关闭 */
  enabled: boolean
  /** 水印文字透明度（0~1） */
  opacity: number
  /** 字体大小 px */
  fontSize: number
  /** 字体粗细（400=常规 600=加粗；加粗提升辨识度，不影响观感） */
  fontWeight: number
  /** 文字旋转角度（度，负值向左倾斜） */
  rotate: number
  /** 横向平铺间距 px（越大水印越少） */
  gapX: number
  /** 纵向平铺间距 px（越大水印越少） */
  gapY: number
  /** 文字颜色（支持 #rrggbb / #rgb） */
  color: string
  /** 是否绘制白色细描边：让文字在深浅不同底色下都可辨识（无需整体加浓） */
  textStroke: boolean
  /** 层叠顺序：默认 999，低于 Element Plus 弹层(2000+)，保证弹窗不被水印覆盖 */
  zIndex: number
  /** 附加当前时间（每次创建/参数变化时写入） */
  showTime: boolean
  /** 可选：附加租户 ID */
  tenantId: string
  /** 不显示水印的角色编码列表（空数组 = 所有角色都显示） */
  hiddenRoles: string[]
}

const SCHEMA_VERSION = 4

/**
 * 间距/可见度设计说明：
 *   1) 平铺密度与 gapX×gapY 成反比。分两轮稀疏化：
 *      - 190×130 (24700px²) -> 235×158 (37130px²)：数量约 -1/3（保留≈66.5%）
 *      - 235×158 -> 285×195 (55575px²)：再 -1/3（保留≈66.8%，相对最初≈44%）
 *   2) "更明显但不影响观感"的策略是提升对比度而非浓度：
 *      - fontWeight 600 加粗（辨识度提升最大、视觉负担最小）
 *      - textStroke 白色细描边，使文字在深浅不同底色下都能被识别
 *      - opacity 0.15 仅轻微提升
 */
const DEFAULT_CONFIG: WatermarkConfig = {
  schemaVersion: SCHEMA_VERSION,
  enabled: true,
  opacity: 0.15,
  fontSize: 15,
  fontWeight: 600,
  rotate: -22,
  gapX: 285,
  gapY: 195,
  color: '#7a7a7a',
  textStroke: true,
  zIndex: 999,
  showTime: true,
  tenantId: '',
  hiddenRoles: []
}

function loadConfig(): WatermarkConfig {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { ...DEFAULT_CONFIG }
    const parsed = JSON.parse(raw)
    // 版本不匹配 -> 使用新默认（旧缓存中的疏密等参数作废）
    if (parsed.schemaVersion !== SCHEMA_VERSION) return { ...DEFAULT_CONFIG }
    return { ...DEFAULT_CONFIG, ...parsed }
  } catch {
    return { ...DEFAULT_CONFIG }
  }
}

export const useWatermarkStore = defineStore('watermark', () => {
  const config = reactive<WatermarkConfig>(loadConfig())

  // 深度 watch：配置变化自动持久化 + Watermark 组件 watch config 自动重绘
  watch(
    config,
    (val) => {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(val))
    },
    { deep: true }
  )

  /** 覆盖/合并配置（对接配置中心或开关按钮时调用） */
  const setConfig = (patch: Partial<WatermarkConfig>) => {
    Object.assign(config, patch)
  }

  /** 全局开关快捷方法 */
  const enableWatermark = () => setConfig({ enabled: true })
  const disableWatermark = () => setConfig({ enabled: false })

  /** 按角色判断当前是否应显示水印 */
  const shouldShow = (roles: string[]): boolean => {
    if (!config.enabled) return false
    if (!roles || roles.length === 0) return true
    return !roles.some((r) => config.hiddenRoles.includes(r))
  }

  return {
    config,
    setConfig,
    enableWatermark,
    disableWatermark,
    shouldShow
  }
})
