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
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 如果有 data 字段返回 data，否则返回整个响应体
    return res.data !== undefined ? res.data : res
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    } else if (error.code === 'ECONNABORTED' || /timeout/i.test(error.message || '')) {
      // axios 超时：error.code = 'ECONNABORTED'，message 含 'timeout'
      ElMessage.error('请求超时：后端处理面试题导入/向量同步耗时较长，请稍后重试或减少单次导入数量')
    } else {
      ElMessage.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
