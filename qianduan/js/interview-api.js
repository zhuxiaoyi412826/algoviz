// 前台面试列表、详情、收藏、历史、点赞等接口
// 纯原生 JS（无 TS 无模块），使用 fetch 调用 /api/interview/user/* （对齐 API 文档 2.2）
// 所有接口返回结构：{success, code, message, data}
(function (global) {
  // 开发/生产多场景 BASE 自适应：
  //   - 页面与后端同源时走相对路径（Nginx/Spring 合并部署场景）
  //   - 否则指向本机 Spring Boot（端口 80），后端已开启全局 CORS 放行
  //   - file:// 本地直接打开 -> 也指向 localhost:80 绝对地址
  function resolveBase() {
    try {
      const u = new URL(location.href)
      if (u.protocol === 'file:') return 'http://localhost:80/api/interview/user'
      // 浏览器 URL.origin 默认端口会去掉：http://localhost:80 -> origin http://localhost
      const port = u.port || (u.protocol === 'https:' ? '443' : '80')
      if (port === '80' || port === '443') return '/api/interview/user'
      const host = u.hostname || 'localhost'
      const proto = u.protocol === 'https:' ? 'https' : 'http'
      return proto + '://' + host + ':80/api/interview/user'
    } catch (e) {
      return 'http://localhost:80/api/interview/user'
    }
  }
  const BASE = resolveBase()

  function toQuery(obj) {
    const p = new URLSearchParams()
    for (const k in obj) {
      if (obj[k] === undefined || obj[k] === null || obj[k] === '') continue
      p.set(k, String(obj[k]))
    }
    return p.toString()
  }

  async function req(path, options = {}) {
    const init = Object.assign({ credentials: 'include', headers: { 'Accept': 'application/json' } }, options)
    if (init.body && typeof init.body !== 'string' && !(init.body instanceof FormData)) {
      init.body = JSON.stringify(init.body)
      init.headers = Object.assign({}, init.headers, { 'Content-Type': 'application/json; charset=utf-8' })
    }
    const res = await fetch(BASE + path, init)
    const ct = res.headers.get('content-type') || ''
    if (ct.includes('application/json')) {
      const json = await res.json()
      if (!json.success) {
        throw new Error(json.message || ('请求失败 code=' + json.code))
      }
      return json.data
    }
    return await res.text()
  }

  const api = {
    // F1 列表
    listProblems(params) {
      const q = toQuery(params)
      return req('/problems' + (q ? '?' + q : ''))
    },
    // F2 id 详情（文档：GET /problems/{id}，访问自动累加阅读量，登录态下额外记录历史）
    getById(id) {
      return req('/problems/' + id)
    },
    // F3 problemNo 详情（文档：/problems/by-no/{problemNo}，访问也会累加阅读量）
    getByNo(problemNo) {
      return req('/problems/by-no/' + encodeURIComponent(problemNo))
    },
    // F4 标签（按热度排序）
    listTags() {
      return req('/tags')
    },
    // F5 分类
    listCategories() {
      return req('/categories')
    },
    // F6 收藏列表（文档：GET /favorites）
    listFavorites(page, pageSize) {
      return req('/favorites?page=' + (page || 1) + '&pageSize=' + (pageSize || 20))
    },
    // F7 浏览历史
    listHistory(page, pageSize) {
      return req('/history?page=' + (page || 1) + '&pageSize=' + (pageSize || 20))
    },
    // F8 删除单条历史（文档：DELETE /history/{problemId}）
    deleteHistory(problemId) {
      return req('/history/' + problemId, { method: 'DELETE' })
    },
    // F9 清空历史（文档：DELETE /history/clear）
    clearHistory() {
      return req('/history/clear', { method: 'DELETE' })
    },
    // F10 收藏（文档：POST /favorites，body {problemId}）
    addFavorite(problemId) {
      return req('/favorites', { method: 'POST', body: { problemId } })
    },
    // F11 取消收藏（文档：DELETE /favorites/{problemId}）
    removeFavorite(problemId) {
      return req('/favorites/' + problemId, { method: 'DELETE' })
    },
    // F12 清空收藏（文档：DELETE /favorites/clear）
    clearFavorites() {
      return req('/favorites/clear', { method: 'DELETE' })
    },
    // F13 点赞（文档：POST /problems/{id}/like）
    like(id) {
      return req('/problems/' + id + '/like', { method: 'POST' })
    },
    // F14 点踩（文档：POST /problems/{id}/dislike）
    dislike(id) {
      return req('/problems/' + id + '/dislike', { method: 'POST' })
    },
    // F15 是否收藏（文档：GET /favorites/{problemId}/check）
    isFavorite(problemId) {
      return req('/favorites/' + problemId + '/check')
    },
    // F16 全站统计（无需登录）
    stats() {
      return req('/stats')
    },
    // F17 搜索（题目/描述/标签等模糊匹配）
    search(keyword, page, pageSize) {
      const q = toQuery({ keyword: keyword || '', page: page || 1, pageSize: pageSize || 10 })
      return req('/search' + (q ? '?' + q : ''))
    },
    // 工具：后端 tags 字符串 -> 数组
    splitTags(tags) {
      if (!tags) return []
      if (Array.isArray(tags)) return tags
      return String(tags).split(/[,，;；|]/).map(s => s.trim()).filter(Boolean)
    },
    // 后端难度 -> 中文
    difficultyLabel(d) {
      return ({ easy: '简单', medium: '中等', hard: '困难' })[d] || (d || '')
    },
    // 后端难度 -> 颜色 class
    difficultyClass(d) {
      return ({ easy: 'diff-easy', medium: 'diff-medium', hard: 'diff-hard' })[d] || 'diff-medium'
    }
  }

  global.InterviewApi = api
})(window)
