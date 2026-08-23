/**
 * 前台全站 footer 动态渲染脚本
 * ---------------------------------------------------
 * 用法：在所有前台 HTML 的 </footer> 之后引入：
 *   <script src="相对路径/js/site-footer.js"></script>
 *
 * 功能：
 *   1) 请求 GET {API_BASE}/api/public/site-config 读取后台配置的站点信息
 *   2) 动态替换当前页面 <footer class="footer"> 的内容，包括：
 *        - 站点 Logo（有就显示，没有则用文字站点名替代）
 *        - 站点标语（siteSlogan）
 *        - 版权信息（copyright）
 *        - ICP 备案号（自动跳转 beian.miit.gov.cn）
 *        - GitHub 链接（有链接才显示 GitHub 图标入口）
 *        - 更新日志 / 建议我 两个入口（始终保留）
 *   3) API 失败时保留原 footer 硬编码内容，不会空白
 */
(function () {
  // ---- 探测 API_BASE ----
  // 规则（和 changelog.html 保持一致，避免同一张页两个请求打到不同端口）：
  //   1) window.__API_BASE__ 优先（页面手动注入，最顶层兜底）
  //   2) file:// / origin 空 → 回落到 http://localhost
  //   3) 当前端口属于"纯静态开发服务器端口"（5500/5173/3000/8000/8080 …）→ 去掉端口，用同 hostname 的 80/443 当后端
  //   4) 其他情况直接用 origin
  function detectApiBase() {
    try {
      if (window.__API_BASE__) return String(window.__API_BASE__).replace(/\/+$/, '')
      var origin = window.location.origin || ''
      if (!origin || origin === 'null' || origin.indexOf('file:') === 0) return 'http://localhost'
      try {
        var u = new URL(origin)
        var staticPorts = {
          '5500': 1, '5501': 1,
          '5173': 1, '5174': 1, '4173': 1, '5175': 1, '5176': 1,
          '3000': 1, '3001': 1, '3002': 1,
          '8000': 1, '8001': 1, '8080': 1, '8081': 1, '8100': 1,
          '9000': 1, '9001': 1
        }
        if (u.port && staticPorts[u.port]) {
          return (u.protocol + '//' + u.hostname).replace(/\/+$/, '')
        }
      } catch (ue) { /* 非标准 URL，继续走 origin */ }
      return origin.replace(/\/+$/, '')
    } catch (e) {
      return 'http://localhost'
    }
  }

  var API_BASE = detectApiBase()
  var FOOTER_SELECTOR = 'footer.footer, footer#site-footer'

  function escapeHtml(s) {
    if (s == null) return ''
    return String(s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;')
  }

  function safeHref(s) {
    if (s == null) return ''
    var v = String(s).trim()
    if (!v) return ''
    if (/^(https?:|\/|mailto:|tel:)/i.test(v)) return v
    // 只允许 http(s) 开头的外链
    return 'https://' + v.replace(/^[^a-zA-Z0-9]+/, '')
  }

  function tryGetRelativeRoot() {
    // 推断当前 HTML 相对 qianduan 根目录的层级
    // 形如: pages/*.html → "../"    html/jiami/*.html → "../../"    index.html → ""
    var path = window.location.pathname
    // 去除协议域名后，按文件名之前的 / 计数
    var parts = path.split('/').filter(Boolean)
    // 去掉最后一项（文件名）
    parts.pop()
    // 把所有目录替换为 "../"
    if (parts.length === 0) return ''
    var up = ''
    for (var i = 0; i < parts.length; i++) up += '../'
    return up
  }

  function buildFooterHtml(cfg) {
    var root = tryGetRelativeRoot()
    var siteName = cfg.siteName ? escapeHtml(cfg.siteName) : 'AlgoViz'
    var slogan = cfg.siteSlogan ? escapeHtml(cfg.siteSlogan) : 'AlgoViz - 数据结构与算法可视化学习平台 · 让学习变得更有趣'
    var copyright = cfg.copyright ? escapeHtml(cfg.copyright) : '© 2026 AlgoViz'
    var icp = cfg.icpNumber ? escapeHtml(cfg.icpNumber) : ''
    var github = cfg.githubLink ? safeHref(cfg.githubLink) : ''
    var logo = cfg.siteLogo ? safeHref(cfg.siteLogo) : ''

    // logo 行：有 logo 显示 logo 图 + 站点名；无 logo 仅显示站点名
    var logoHtml = ''
    if (logo) {
      logoHtml +=
        '<a href="' + root + 'index.html" class="footer-logo-link" aria-label="' + siteName + '">' +
        '  <img src="' + logo + '" alt="' + siteName + '" class="footer-logo-img" ' +
        '   onerror="this.style.display=\'none\';this.nextElementSibling.style.display=\'inline-flex\';">' +
        '  <span class="footer-logo-text" style="display:none;">' + siteName + '</span>' +
        '</a>'
    } else {
      logoHtml =
        '<a href="' + root + 'index.html" class="footer-logo-link" aria-label="' + siteName + '">' +
        '  <span class="footer-logo-text" style="display:inline-flex;">' + siteName + '</span>' +
        '</a>'
    }

    // slogan 行（用户可控制显示）
    var sloganHtml = slogan
      ? '<div class="footer-slogan">' + slogan + '</div>'
      : ''

    // ICP 行
    var icpHtml = icp
      ? '<span class="footer-icp">ICP备案：<a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">' + icp + '</a></span>'
      : ''

    // GitHub 入口（只有填了链接才显示）
    var githubHtml = github
      ? '<a href="' + github + '" target="_blank" rel="noopener noreferrer" class="footer-link footer-github-link" aria-label="GitHub">' +
        '  <span class="footer-github-icon" style="display:inline-flex;align-items:center;gap:4px;">' +
        '    <svg viewBox="0 0 16 16" width="14" height="14" fill="currentColor" aria-hidden="true">' +
        '      <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z"/>' +
        '    </svg>' +
        '    GitHub' +
        '  </span>' +
        '</a>'
      : ''

    var changelogHtml = '<a href="' + root + 'pages/changelog.html" class="footer-link">更新日志</a>'
    var suggestionHtml = '<a href="' + root + 'pages/suggestion.html" class="footer-link">建议我</a>'

    // 组合：copyright 之后按 ICP / GitHub / 更新日志 / 建议我 的顺序，中间竖线分隔
    var items = [
      '<span>' + copyright + '</span>',
      icpHtml,
      githubHtml,
      changelogHtml,
      suggestionHtml
    ].filter(function (x) { return x && x.length > 0 })

    var bottomHtml = items.join('\n                    <span class="footer-divider">|</span>\n                    ')

    return (
      '        <div class="container">\n' +
      '            <div class="footer-content">\n' +
      '                <div class="footer-top-row" style="display:flex;align-items:center;gap:12px;margin-bottom:10px;">\n' +
      '                    ' + logoHtml + '\n' +
      '                </div>\n' +
      (sloganHtml ? '                ' + sloganHtml + '\n' : '') +
      '                <div class="footer-bottom-row">\n' +
      '                    ' + bottomHtml + '\n' +
      '                </div>\n' +
      '            </div>\n' +
      '        </div>'
    )
  }

  function render() {
    var footer = document.querySelector(FOOTER_SELECTOR)
    if (!footer) return
    var url = API_BASE.replace(/\/$/, '') + '/api/public/site-config'
    // 加个小随机数防止浏览器强缓存
    if (url.indexOf('?') === -1) url += '?_=' + Date.now()
    try {
      var xhr = new XMLHttpRequest()
      xhr.open('GET', url, true)
      xhr.timeout = 5000
      xhr.onreadystatechange = function () {
        if (xhr.readyState !== 4) return
        try {
          if (xhr.status === 200) {
            var json = JSON.parse(xhr.responseText)
            var data = (json && (json.data || json.result || json.body)) || {}
            // 兼容：如果 data 还是被再包一层（某些 axios拦截器已经解包）
            if (typeof data === 'object' && data.data && !('siteName' in data) && ('siteName' in data.data)) {
              data = data.data
            }
            footer.innerHTML = buildFooterHtml({
              siteName: data.siteName,
              siteLogo: data.siteLogo,
              icpNumber: data.icpNumber,
              copyright: data.copyright,
              githubLink: data.githubLink,
              siteSlogan: data.siteSlogan
            })
          }
        } catch (ignore) {
          // 解析失败时保留旧 footer，不给用户报错
        }
      }
      xhr.send()
    } catch (ignore) {
      // 极端失败也不影响页面
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', render)
  } else {
    render()
  }
})()
