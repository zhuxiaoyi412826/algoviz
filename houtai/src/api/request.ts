import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      // Sa-Token 默认使用 satoken 作为 header 名称
      config.headers['satoken'] = token
      // 同时兼容旧的 Authorization header
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 兼容两种响应格式：{code:200, data:...} 和 {success:true, ...}
    if (res.code !== undefined && res.code !== 200) {
      // 后端返回业务错误（如 token 无效）
      const msg = res.message || '请求失败'
      if (msg.includes('token') || msg.includes('Token') || msg.includes('登录') || res.code === 401) {
        clearSessionAndRedirect(msg)
        return Promise.reject(new Error(msg))
      }
      ElMessage.error(msg)
      return Promise.reject(new Error(msg))
    }
    // 如果有 data 字段返回 data，否则返回整个响应体
    return res.data !== undefined ? res.data : res
  },
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message || '网络错误'

    // 401 / 403 → session 过期或 token 无效
    if (status === 401 || status === 403) {
      clearSessionAndRedirect(msg)
    } else if (msg.includes('token') || msg.includes('Token') || msg.includes('未能读取')) {
      clearSessionAndRedirect(msg)
    } else if (error.code === 'ECONNABORTED' || /timeout/i.test(error.message || '')) {
      ElMessage.error('请求超时：后端处理面试题导入/向量同步耗时较长，请稍后重试或减少单次导入数量')
    } else if (status === 500 && /token|Token|未能读取/.test(msg)) {
      // 500 但消息是 token 相关 → 也按 session 过期处理
      clearSessionAndRedirect(msg)
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

// 统一清除登录态并跳转登录页
let isRedirecting = false
function clearSessionAndRedirect(msg: string) {
  if (isRedirecting) return  // 防止重复跳转
  isRedirecting = true
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('roles')
  localStorage.removeItem('permissions')
  ElMessage.warning(msg || '登录已过期，请重新登录')
  setTimeout(() => {
    isRedirecting = false
    window.location.href = '/login'
  }, 500)
}

export default request
