<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const canvasRef = ref<HTMLCanvasElement | null>(null)

const formData = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// ==================== 左侧条款内容（保密条例 3 条 + 员工守则 2 条，共 5 条） ====================
interface RegEntry {
  no: number
  group: string
  firstOfGroup: boolean
  text: string
}

const regulations: RegEntry[] = [
  {
    no: 1,
    group: '保密条例',
    firstOfGroup: true,
    text: '未经授权，不得外传、拷贝平台业务数据、用户信息及后台页面截图。'
  },
  {
    no: 2,
    group: '保密条例',
    firstOfGroup: false,
    text: '平台账号仅限本人使用，严禁转借、共享或代为操作。'
  },
  {
    no: 3,
    group: '保密条例',
    firstOfGroup: false,
    text: '发现疑似数据泄露，应立即上报安全管理员并配合审计排查。'
  },
  {
    no: 4,
    group: '员工守则',
    firstOfGroup: true,
    text: '发布内容须经审核，不得传播涉密、违法违规或违规信息。'
  },
  {
    no: 5,
    group: '员工守则',
    firstOfGroup: false,
    text: '操作须按规范留痕，接受后台操作日志审计。'
  }
]

// ==================== 水波动画 ====================
let animationId: number | null = null
let ctx: CanvasRenderingContext2D | null = null
let width = 0
let height = 0
const ripples: Array<{
  x: number
  y: number
  radius: number
  maxRadius: number
  alpha: number
  velocity: number
}> = []

const mousePos = { x: 0, y: 0, active: false }

const initCanvas = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  ctx = canvas.getContext('2d')
  if (!ctx) return

  const resize = () => {
    width = canvas.width = window.innerWidth
    height = canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  window.addEventListener('mousemove', (e) => {
    mousePos.x = e.clientX
    mousePos.y = e.clientY
    mousePos.active = true
    // 鼠标移动时产生小涟漪
    if (Math.random() < 0.15) {
      createRipple(e.clientX, e.clientY, 20 + Math.random() * 30)
    }
  })

  window.addEventListener('touchmove', (e) => {
    if (e.touches.length > 0) {
      const t = e.touches[0]
      mousePos.x = t.clientX
      mousePos.y = t.clientY
      mousePos.active = true
      if (Math.random() < 0.2) {
        createRipple(t.clientX, t.clientY, 20 + Math.random() * 30)
      }
    }
  })

  window.addEventListener('click', (e) => {
    createRipple(e.clientX, e.clientY, 80)
  })

  animate()
}

const createRipple = (x: number, y: number, maxRadius: number) => {
  ripples.push({
    x,
    y,
    radius: 0,
    maxRadius,
    alpha: 0.6,
    velocity: 1.5 + Math.random() * 2
  })
  // 限制涟漪数量
  if (ripples.length > 60) ripples.shift()
}

const animate = () => {
  if (!ctx) return
  ctx.clearRect(0, 0, width, height)

  // 绘制背景渐变
  const gradient = ctx.createRadialGradient(
    width / 2, height / 2, 0,
    width / 2, height / 2, Math.max(width, height) / 1.2
  )
  gradient.addColorStop(0, 'rgba(102, 126, 234, 0.05)')
  gradient.addColorStop(0.5, 'rgba(118, 75, 162, 0.03)')
  gradient.addColorStop(1, 'rgba(25, 30, 80, 0)')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, width, height)

  // 鼠标位置的光晕
  if (mousePos.active) {
    const glow = ctx.createRadialGradient(
      mousePos.x, mousePos.y, 0,
      mousePos.x, mousePos.y, 150
    )
    glow.addColorStop(0, 'rgba(100, 180, 255, 0.12)')
    glow.addColorStop(0.5, 'rgba(100, 180, 255, 0.05)')
    glow.addColorStop(1, 'rgba(100, 180, 255, 0)')
    ctx.fillStyle = glow
    ctx.beginPath()
    ctx.arc(mousePos.x, mousePos.y, 150, 0, Math.PI * 2)
    ctx.fill()
  }

  // 更新和绘制涟漪
  for (let i = ripples.length - 1; i >= 0; i--) {
    const r = ripples[i]
    r.radius += r.velocity
    r.alpha *= 0.96

    if (r.radius >= r.maxRadius || r.alpha < 0.01) {
      ripples.splice(i, 1)
      continue
    }

    // 外层涟漪
    ctx.beginPath()
    ctx.arc(r.x, r.y, r.radius, 0, Math.PI * 2)
    ctx.strokeStyle = `rgba(150, 200, 255, ${r.alpha})`
    ctx.lineWidth = 2
    ctx.stroke()

    // 内层涟漪（小一圈）
    if (r.radius > 10) {
      ctx.beginPath()
      ctx.arc(r.x, r.y, r.radius * 0.7, 0, Math.PI * 2)
      ctx.strokeStyle = `rgba(200, 230, 255, ${r.alpha * 0.5})`
      ctx.lineWidth = 1
      ctx.stroke()
    }

    // 中心点
    if (r.radius < 15) {
      ctx.beginPath()
      ctx.arc(r.x, r.y, 3, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(255, 255, 255, ${r.alpha})`
      ctx.fill()
    }
  }

  // 绘制背景粒子点（网格点效果）
  drawGridDots()

  animationId = requestAnimationFrame(animate)
}

const drawGridDots = () => {
  if (!ctx) return
  const spacing = 60
  const time = Date.now() * 0.001

  for (let x = spacing; x < width; x += spacing) {
    for (let y = spacing; y < height; y += spacing) {
      // 计算点到鼠标的距离
      const dx = x - mousePos.x
      const dy = y - mousePos.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      const maxDist = 200

      if (dist < maxDist) {
        // 鼠标附近的点会发亮并产生涟漪效果
        const influence = 1 - dist / maxDist
        const wave = Math.sin(time * 3 - dist * 0.05) * 0.5 + 0.5
        const alpha = influence * wave * 0.6
        const size = 1 + influence * 2 + wave

        ctx.beginPath()
        ctx.arc(x, y, size, 0, Math.PI * 2)
        ctx.fillStyle = `rgba(180, 220, 255, ${alpha})`
        ctx.fill()
      } else {
        // 远处的点保持微弱
        ctx.beginPath()
        ctx.arc(x, y, 1, 0, Math.PI * 2)
        ctx.fillStyle = 'rgba(150, 180, 220, 0.08)'
        ctx.fill()
      }
    }
  }
}

onMounted(() => {
  initCanvas()
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})

// ==================== 登录逻辑 ====================
const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    loading.value = true
    try {
      const data: any = await request.post('/admin/login', {
        username: formData.username,
        password: formData.password
      })

      const payload = data?.data || data
      const token = payload?.token
      const userInfo = payload?.userInfo
      const userRoles = payload?.roles || []
      const userPermissions = payload?.permissions || []

      if (!token) {
        ElMessage.error('登录失败，未获取到 Token')
        return
      }

      userStore.setToken(token)
      if (userInfo) {
        const rawStatus = userInfo.status
        const statusValue: 'active' | 'disabled' =
          rawStatus === 1 || rawStatus === 'active' || rawStatus === 'enabled'
            ? 'active'
            : 'disabled'

        const adaptedUserInfo = {
          id: userInfo.id?.toString() || '',
          username: userInfo.username || '',
          nickname: userInfo.realName || userInfo.username || '',
          email: userInfo.email || '',
          phone: userInfo.phone || '',
          role: userRoles[0] || '',
          status: statusValue,
          avatar: userInfo.avatar || '',
          createTime: userInfo.createdAt || '',
          updateTime: userInfo.updatedAt || '',
          lastLoginTime: userInfo.lastLoginTime || ''
        }
        userStore.setUserInfo(adaptedUserInfo, userRoles, userPermissions)
        localStorage.setItem('userInfo', JSON.stringify(adaptedUserInfo))
        localStorage.setItem('roles', JSON.stringify(userRoles))
        localStorage.setItem('permissions', JSON.stringify(userPermissions))
      }

      ElMessage.success('登录成功')
      router.push('/')
    } catch (error: any) {
      const msg = error?.response?.data?.message || error?.message || '登录失败，请检查用户名和密码'
      ElMessage.error(msg)
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="login-container">
    <canvas ref="canvasRef" class="wave-canvas" />
    <div class="login-body">
      <!-- 左侧：保密条例 + 员工守则（共 5 条） -->
      <section class="login-left">
        <div class="glass-card left-card">
          <h2 class="left-brand">算法可视化平台</h2>
          <p class="left-tip">
            登录即表示您已知晓并承诺遵守以下
            <b>保密条例</b> 与 <b>员工守则</b>：
          </p>
          <div class="reg-list">
            <template v-for="r in regulations" :key="r.no">
              <h3 v-if="r.firstOfGroup" class="reg-title">
                {{ r.group }}
              </h3>
              <div class="reg-row">
                <span class="reg-no">{{ String(r.no).padStart(2, '0') }}</span>
                <span class="reg-text">{{ r.text }}</span>
              </div>
            </template>
          </div>
        </div>
      </section>

      <!-- 右侧：登录表单 -->
      <div class="login-box">
        <div class="login-header">
          <h1>算法可视化平台</h1>
          <p>管理后台</p>
        </div>
        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="formData.username"
              placeholder="用户名"
              size="large"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-btn"
              native-type="submit"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
        <div class="login-footer">
          <span>默认账号: algovize / algovize123</span>
        </div>
      </div>
    </div>
    <div class="bg-orbs">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-container {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1f4e 0%, #2d1f5e 40%, #4a1952 70%, #1a1f4e 100%);
  overflow: hidden;
}

.wave-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.bg-orbs {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.5;
  animation: float 8s ease-in-out infinite;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(100, 150, 255, 0.6), transparent 70%);
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.orb-2 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(200, 100, 255, 0.5), transparent 70%);
  bottom: -80px;
  right: -80px;
  animation-delay: -3s;
}

.orb-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(100, 200, 200, 0.4), transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -5s;
  animation-duration: 12s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -40px) scale(1.05);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.95);
  }
}

/* 两栏布局：左侧条款 + 右侧登录 */
.login-body {
  position: absolute;
  inset: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7vw;
  padding: 0 6vw;
}

.login-left {
  flex: 0 1 min(520px, 38vw);
  animation: fadeInUp 0.8s ease-out;
}

.glass-card {
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.left-card {
  padding: 40px 38px;
  text-align: left;
}

.left-brand {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2px;
  margin: 0 0 8px;
  text-shadow: 0 2px 16px rgba(100, 150, 255, 0.4);
}

.left-tip {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.62);
  line-height: 1.7;
  margin: 0 0 18px;

  b {
    color: #9ec2ff;
    font-weight: 600;
  }
}

.reg-title {
  font-size: 14px;
  font-weight: 600;
  color: #cdd7ff;
  letter-spacing: 1px;
  margin: 16px 0 10px;
  padding-left: 10px;
  border-left: 3px solid #667eea;
}

.reg-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 7px 0;
}

.reg-no {
  flex: none;
  min-width: 26px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #aebcff;
  border: 1px solid rgba(160, 180, 255, 0.5);
  border-radius: 6px;
  margin-top: 2px;
  box-sizing: border-box;
}

.reg-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.7;
}

.login-box {
  flex: 0 0 420px;
  z-index: 10;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  animation: fadeInUp 0.8s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 窄屏：隐藏左侧条款，保持登录框可用 */
@media (max-width: 1080px) {
  .login-left {
    display: none;
  }

  .login-body {
    gap: 0;
    padding: 0 4vw;
  }

  .login-box {
    flex: 0 0 420px;
  }
}

.login-header {
  text-align: center;
  margin-bottom: 36px;

  h1 {
    font-size: 30px;
    font-weight: 700;
    color: #fff;
    margin-bottom: 8px;
    text-shadow: 0 2px 20px rgba(100, 150, 255, 0.4);
    letter-spacing: 2px;
  }

  p {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.6);
    letter-spacing: 8px;
    margin: 0;
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 24px;
  }

  :deep(.el-input__wrapper) {
    padding: 4px 16px;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 10px;
    box-shadow: none;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.15);
      border-color: rgba(255, 255, 255, 0.3);
    }

    &.is-focus {
      background: rgba(255, 255, 255, 0.15);
      border-color: rgba(100, 180, 255, 0.6);
      box-shadow: 0 0 20px rgba(100, 180, 255, 0.2);
    }
  }

  :deep(.el-input__inner) {
    color: #fff;

    &::placeholder {
      color: rgba(255, 255, 255, 0.4);
    }
  }

  :deep(.el-input__prefix .el-icon) {
    color: rgba(255, 255, 255, 0.5);
  }
}

.login-btn {
  width: 100%;
  font-size: 16px;
  letter-spacing: 6px;
  height: 48px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 25px rgba(102, 126, 234, 0.6);
  }

  &:active {
    transform: translateY(0);
  }
}

.login-footer {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.35);
  margin-top: 20px;
  letter-spacing: 1px;
}
</style>
