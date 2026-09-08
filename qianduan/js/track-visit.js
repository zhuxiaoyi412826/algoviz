/**
 * 前台页面埋点（模块访问上报）
 * 用法：<script src="../js/track-visit.js" data-module="ds|algo|oj|ai"></script>
 *  - 页面加载时自动上报 1 次（进入页面）
 *  - AI 页每次发送对话再调 window.__algovizReportVisit('ai')（供 ai-chat.js 使用）
 * 设计：
 *  1. 失败静默：任何异常/网络失败都不影响页面渲染，不带 try/catch 之外的副作用
 *  2. 需登录：POST /api/visit/{module} 由后端 AuthInterceptor 保护，未登录 401 静默忽略
 *  3. 跨域凭证：sendBeacon 自动带 Cookie；fallback fetch with credentials:'include' + keepalive
 *  4. API 基址探测与项目既有 resolveApiBase 一致：
 *       file:// -> http://localhost:80 ；页面端口为 80/443 时走相对路径；否则 host:80
 */
(function () {
  var allowed = { ds: 1, algo: 1, oj: 1, ai: 1 };

  function apiBase() {
    try {
      var u = new URL(location.href);
      if (u.protocol === 'file:') return 'http://localhost:80';
      var port = u.port || (u.protocol === 'https:' ? '443' : '80');
      if (port === '80' || port === '443') return '';
      var host = u.hostname || 'localhost';
      return (u.protocol === 'https:' ? 'https' : 'http') + '://' + host + ':80';
    } catch (e) {
      return 'http://localhost:80';
    }
  }

  function report(module) {
    module = String(module || '').toLowerCase();
    if (!allowed[module]) return;
    try {
      var url = apiBase() + '/api/visit/' + module;
      // 优先 sendBeacon（页面卸载前也能送达且不阻塞）；返回 false 时降级 fetch
      if (navigator.sendBeacon) {
        var sent = false;
        try { sent = navigator.sendBeacon(url); } catch (e) { sent = false; }
        if (sent) return;
      }
      fetch(url, { method: 'POST', credentials: 'include', keepalive: true }).catch(function () {});
    } catch (e) {
      /* 埋点失败必须静默 */
    }
  }

  // 暴露全局：页面级埋点 + AI 对话埋点共用
  window.__algovizReportVisit = report;

  // 页面级模块自动上报一次
  var el = document.currentScript;
  if (el && el.getAttribute('data-module')) {
    report(el.getAttribute('data-module'));
  }
})();
