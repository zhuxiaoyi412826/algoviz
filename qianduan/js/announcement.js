/**
 * 公告系统 - 前端逻辑
 *
 * 功能：
 *  - 页面加载时拉取已发布公告，若有未读则在右下角弹出悬浮提示
 *  - 点击悬浮窗体打开公告详情面板（模态框），可查看历史公告
 *  - 点击悬浮窗 x 仅关闭悬浮窗，不标记已读
 *  - 关闭详情面板时，将当前查看的公告 ID 加入已读列表
 *
 * localStorage：key=readAnnouncementIds，值为 JSON 数组（公告 ID 字符串）
 * API 前缀：http://localhost
 */
(function () {
    'use strict';

    var API_PREFIX = 'http://localhost';
    var READ_IDS_KEY = 'readAnnouncementIds';

    // 公告类型 -> 标签文案与颜色
    var TYPE_CONFIG = {
        notice:      { label: '公告', color: '#667eea' },
        update:      { label: '更新', color: '#10b981' },
        maintenance: { label: '维护', color: '#f59e0b' },
        warning:     { label: '警告', color: '#ef4444' },
        event:       { label: '活动', color: '#8b5cf6' },
        info:        { label: '通知', color: '#3b82f6' }
    };

    function getTypeConfig(type) {
        if (type && TYPE_CONFIG[type]) return TYPE_CONFIG[type];
        return { label: type || '公告', color: '#6b7280' };
    }

    // ---- 状态 ----
    var publishedAnnouncements = [];
    var readIds = loadReadIds();
    var currentAnnouncementId = null;
    var floatingEl = null;
    var backdropEl = null;
    var panelEl = null;

    // ---- localStorage 读写 ----
    function loadReadIds() {
        try {
            var raw = localStorage.getItem(READ_IDS_KEY);
            var arr = raw ? JSON.parse(raw) : [];
            if (!Array.isArray(arr)) return [];
            return arr.map(function (id) { return String(id); });
        } catch (e) {
            return [];
        }
    }

    function saveReadIds(ids) {
        try {
            localStorage.setItem(READ_IDS_KEY, JSON.stringify(ids));
        } catch (e) { /* 忽略写入异常 */ }
    }

    function markAsRead(id) {
        if (id == null) return;
        var idStr = String(id);
        if (readIds.indexOf(idStr) === -1) {
            readIds.push(idStr);
            saveReadIds(readIds);
        }
    }

    function getUnreadAnnouncements() {
        return publishedAnnouncements.filter(function (a) {
            return readIds.indexOf(String(a.id)) === -1;
        });
    }

    // ---- 工具函数 ----
    function formatTime(timeStr) {
        if (!timeStr) return '';
        var d = new Date(timeStr);
        if (isNaN(d.getTime())) {
            return String(timeStr).replace('T', ' ').substring(0, 16);
        }
        function pad(n) { return String(n).padStart(2, '0'); }
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
            ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function escapeHtml(str) {
        if (str == null) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    // 内容渲染：含 HTML 标签则按 HTML 渲染，否则转义并把换行转为 <br>
    function renderContent(content) {
        if (!content) return '<p style="color:var(--text-muted);margin:0;">暂无内容</p>';
        var text = String(content);
        if (/<[a-z][\s\S]*>/i.test(text)) {
            return text;
        }
        return escapeHtml(text).replace(/\n/g, '<br>');
    }

    function plainTextSnippet(content, len) {
        if (!content) return '';
        var plain = String(content).replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();
        if (plain.length > len) return plain.slice(0, len) + '…';
        return plain;
    }

    // ---- 注入样式 ----
    function injectStyles() {
        if (document.getElementById('announcement-styles')) return;
        var style = document.createElement('style');
        style.id = 'announcement-styles';
        style.textContent = [
            '/* ===== 公告悬浮窗 ===== */',
            '.ann-floating {',
            '  position: fixed; right: 20px; bottom: 20px; width: 320px;',
            '  background: var(--bg-card, #ffffff); color: var(--text-primary, #1a202c);',
            '  border: 1px solid var(--border, #e2e8f0); border-radius: 12px;',
            '  box-shadow: 0 10px 40px rgba(0,0,0,0.18); z-index: 9999; overflow: hidden;',
            '  cursor: pointer; font-family: inherit;',
            '  opacity: 0; transform: translateY(24px) scale(0.98);',
            '  transition: opacity .3s ease, transform .3s ease, box-shadow .2s ease;',
            '}',
            '.ann-floating.ann-floating-show { opacity: 1; transform: translateY(0) scale(1); }',
            '.ann-floating:hover { box-shadow: 0 14px 48px rgba(0,0,0,0.24); }',
            '.ann-floating-header {',
            '  display: flex; align-items: center; gap: 8px;',
            '  padding: 12px 14px 6px;',
            '}',
            '.ann-floating-type {',
            '  font-size: .72rem; font-weight: 600; color: #fff;',
            '  padding: 2px 8px; border-radius: 4px; white-space: nowrap; flex-shrink: 0;',
            '}',
            '.ann-floating-title {',
            '  font-size: .95rem; font-weight: 600; flex: 1; min-width: 0;',
            '  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;',
            '}',
            '.ann-floating-close {',
            '  background: transparent; border: none; cursor: pointer;',
            '  color: var(--text-muted, #718096); font-size: 1.1rem; line-height: 1;',
            '  padding: 2px 6px; border-radius: 4px; flex-shrink: 0;',
            '  transition: background .2s, color .2s;',
            '}',
            '.ann-floating-close:hover { background: var(--bg-card-hover, #f0f2f5); color: var(--text-primary, #1a202c); }',
            '.ann-floating-body {',
            '  padding: 0 14px 8px; font-size: .85rem; line-height: 1.5;',
            '  color: var(--text-secondary, #4a5568);',
            '  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;',
            '}',
            '.ann-floating-footer {',
            '  padding: 6px 14px 12px; display: flex; align-items: center;',
            '  justify-content: space-between; font-size: .75rem;',
            '  color: var(--text-muted, #718096);',
            '}',
            '.ann-floating-hint { color: var(--primary, #667eea); font-weight: 500; }',
            '',
            '/* ===== 公告详情面板（模态框）===== */',
            '.ann-backdrop {',
            '  position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 10000;',
            '  display: flex; align-items: center; justify-content: center; padding: 20px;',
            '  opacity: 0; transition: opacity .25s ease;',
            '}',
            '.ann-backdrop.ann-backdrop-show { opacity: 1; }',
            '.ann-panel {',
            '  background: var(--bg-card, #ffffff); color: var(--text-primary, #1a202c);',
            '  border: 1px solid var(--border, #e2e8f0); border-radius: 14px;',
            '  width: 100%; max-width: 700px; max-height: 80vh;',
            '  display: flex; flex-direction: column; overflow: hidden;',
            '  box-shadow: 0 20px 60px rgba(0,0,0,0.3);',
            '  transform: scale(.96); opacity: 0;',
            '  transition: transform .25s ease, opacity .25s ease;',
            '}',
            '.ann-backdrop.ann-backdrop-show .ann-panel { transform: scale(1); opacity: 1; }',
            '.ann-panel-header {',
            '  display: flex; align-items: center; justify-content: space-between;',
            '  padding: 16px 20px; border-bottom: 1px solid var(--border, #e2e8f0); flex-shrink: 0;',
            '}',
            '.ann-panel-title-text { font-size: 1.1rem; font-weight: 700; }',
            '.ann-panel-close {',
            '  background: transparent; border: none; cursor: pointer;',
            '  color: var(--text-muted, #718096); font-size: 1.5rem; line-height: 1;',
            '  padding: 4px 8px; border-radius: 6px; transition: background .2s, color .2s;',
            '}',
            '.ann-panel-close:hover { background: var(--bg-card-hover, #f0f2f5); color: var(--text-primary, #1a202c); }',
            '.ann-panel-body { flex: 1; overflow: hidden; display: flex; min-height: 0; }',
            '.ann-history {',
            '  width: 220px; flex-shrink: 0; overflow-y: auto; padding: 8px;',
            '  border-right: 1px solid var(--border, #e2e8f0);',
            '}',
            '.ann-history-title {',
            '  font-size: .72rem; font-weight: 600; color: var(--text-muted, #718096);',
            '  text-transform: uppercase; letter-spacing: .5px; padding: 6px 10px 8px;',
            '}',
            '.ann-history-item {',
            '  padding: 8px 10px; border-radius: 8px; cursor: pointer;',
            '  transition: background .15s; margin-bottom: 2px;',
            '}',
            '.ann-history-item:hover { background: var(--bg-card-hover, #f0f2f5); }',
            '.ann-history-item.active { background: var(--primary, #667eea); }',
            '.ann-history-item.active .ann-history-item-title,',
            '.ann-history-item.active .ann-history-item-time { color: #fff; }',
            '.ann-history-item-title {',
            '  font-size: .85rem; font-weight: 500; color: var(--text-primary, #1a202c);',
            '  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;',
            '}',
            '.ann-history-item-time { font-size: .72rem; color: var(--text-muted, #718096); margin-top: 2px; }',
            '.ann-history-item-dot {',
            '  display: inline-block; width: 6px; height: 6px; border-radius: 50%;',
            '  background: var(--primary, #667eea); margin-right: 6px; vertical-align: middle;',
            '}',
            '.ann-history-item.active .ann-history-item-dot { background: #fff; }',
            '.ann-detail { flex: 1; overflow-y: auto; padding: 20px 24px; min-width: 0; }',
            '.ann-detail-meta {',
            '  display: flex; align-items: center; gap: 10px; margin-bottom: 12px; flex-wrap: wrap;',
            '}',
            '.ann-detail-type {',
            '  font-size: .75rem; font-weight: 600; color: #fff;',
            '  padding: 3px 10px; border-radius: 4px;',
            '}',
            '.ann-detail-time { font-size: .8rem; color: var(--text-muted, #718096); }',
            '.ann-detail-title { font-size: 1.3rem; font-weight: 700; margin: 0 0 14px; line-height: 1.4; }',
            '.ann-detail-content { font-size: .92rem; line-height: 1.7; color: var(--text-primary, #1a202c); }',
            '.ann-detail-content p { margin: 0 0 10px; }',
            '.ann-empty { padding: 40px 20px; text-align: center; color: var(--text-muted, #718096); }',
            '',
            '@media (max-width: 540px) {',
            '  .ann-panel-body { flex-direction: column; }',
            '  .ann-history { width: 100%; max-height: 130px; border-right: none;',
            '    border-bottom: 1px solid var(--border, #e2e8f0); }',
            '  .ann-floating { width: calc(100vw - 40px); max-width: 320px; }',
            '}'
        ].join('\n');
        document.head.appendChild(style);
    }

    // ---- 悬浮窗 ----
    function showFloatingWindow(announcement) {
        hideFloatingWindow();
        if (!announcement) return;

        var typeCfg = getTypeConfig(announcement.type);

        floatingEl = document.createElement('div');
        floatingEl.className = 'ann-floating';
        floatingEl.setAttribute('role', 'button');
        floatingEl.setAttribute('tabindex', '0');
        floatingEl.innerHTML =
            '<div class="ann-floating-header">' +
                '<span class="ann-floating-type" style="background:' + typeCfg.color + '">' + escapeHtml(typeCfg.label) + '</span>' +
                '<span class="ann-floating-title">' + escapeHtml(announcement.title || '新公告') + '</span>' +
                '<button class="ann-floating-close" type="button" aria-label="关闭" title="关闭">&times;</button>' +
            '</div>' +
            '<div class="ann-floating-body">' + escapeHtml(plainTextSnippet(announcement.content, 80)) + '</div>' +
            '<div class="ann-floating-footer">' +
                '<span>' + escapeHtml(formatTime(announcement.publishTime || announcement.createTime)) + '</span>' +
                '<span class="ann-floating-hint">点击查看详情 ›</span>' +
            '</div>';

        // 点击 x：仅关闭悬浮窗，不标记已读
        var closeBtn = floatingEl.querySelector('.ann-floating-close');
        closeBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            hideFloatingWindow();
        });

        // 点击窗体：打开详情面板
        var openHandler = function () {
            hideFloatingWindow();
            openPanel(announcement.id);
        };
        floatingEl.addEventListener('click', openHandler);
        floatingEl.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                openHandler();
            }
        });

        document.body.appendChild(floatingEl);
        // 触发进入动画
        requestAnimationFrame(function () {
            if (floatingEl) floatingEl.classList.add('ann-floating-show');
        });
    }

    function hideFloatingWindow() {
        if (!floatingEl) return;
        var el = floatingEl;
        floatingEl = null;
        el.classList.remove('ann-floating-show');
        setTimeout(function () {
            if (el.parentNode) el.parentNode.removeChild(el);
        }, 300);
    }

    // ---- 详情面板 ----
    function openPanel(announcementId) {
        if (publishedAnnouncements.length === 0) {
            renderEmptyPanel();
            return;
        }
        // 默认展示最新一条（已按 publish_time DESC 排序）
        var id = announcementId;
        if (id == null) {
            id = publishedAnnouncements[0].id;
        }
        // 若 id 不存在，则回退到第一条
        var exists = publishedAnnouncements.some(function (a) { return String(a.id) === String(id); });
        if (!exists) id = publishedAnnouncements[0].id;

        currentAnnouncementId = id;
        renderPanel();
    }

    function renderPanel() {
        // 移除已有面板
        removePanel(false);

        backdropEl = document.createElement('div');
        backdropEl.className = 'ann-backdrop';
        backdropEl.setAttribute('role', 'dialog');
        backdropEl.setAttribute('aria-modal', 'true');

        panelEl = document.createElement('div');
        panelEl.className = 'ann-panel';
        panelEl.innerHTML =
            '<div class="ann-panel-header">' +
                '<span class="ann-panel-title-text">📢 公告详情</span>' +
                '<button class="ann-panel-close" type="button" aria-label="关闭" title="关闭">&times;</button>' +
            '</div>' +
            '<div class="ann-panel-body">' +
                renderHistoryList() +
                renderDetail(currentAnnouncementId) +
            '</div>';

        backdropEl.appendChild(panelEl);
        document.body.appendChild(backdropEl);

        // 关闭按钮：标记当前公告为已读后关闭
        panelEl.querySelector('.ann-panel-close').addEventListener('click', function (e) {
            e.stopPropagation();
            closePanel();
        });
        // 点击背景遮罩：同样标记已读并关闭
        backdropEl.addEventListener('click', function (e) {
            if (e.target === backdropEl) closePanel();
        });
        // 点击面板内部阻止冒泡到 backdrop（避免误关闭）
        panelEl.addEventListener('click', function (e) {
            e.stopPropagation();
        });
        // 历史列表点击切换
        var historyEl = panelEl.querySelector('.ann-history');
        if (historyEl) {
            historyEl.addEventListener('click', function (e) {
                var item = e.target.closest('.ann-history-item');
                if (!item) return;
                var newId = item.getAttribute('data-id');
                if (newId != null && String(newId) !== String(currentAnnouncementId)) {
                    currentAnnouncementId = newId;
                    // 仅更新历史高亮与详情区
                    updateActiveHistoryItem();
                    var detailContainer = panelEl.querySelector('.ann-detail');
                    if (detailContainer) {
                        detailContainer.outerHTML = renderDetail(currentAnnouncementId);
                    }
                    // 滚动详情区到顶部
                    var newDetail = panelEl.querySelector('.ann-detail');
                    if (newDetail) newDetail.scrollTop = 0;
                }
            });
        }

        // 触发进入动画
        requestAnimationFrame(function () {
            if (backdropEl) backdropEl.classList.add('ann-backdrop-show');
        });

        // ESC 关闭
        document.addEventListener('keydown', onPanelKeydown);
    }

    function renderEmptyPanel() {
        removePanel(false);
        backdropEl = document.createElement('div');
        backdropEl.className = 'ann-backdrop';
        panelEl = document.createElement('div');
        panelEl.className = 'ann-panel';
        panelEl.innerHTML =
            '<div class="ann-panel-header">' +
                '<span class="ann-panel-title-text">📢 公告详情</span>' +
                '<button class="ann-panel-close" type="button" aria-label="关闭">&times;</button>' +
            '</div>' +
            '<div class="ann-empty">暂无公告</div>';
        backdropEl.appendChild(panelEl);
        document.body.appendChild(backdropEl);

        panelEl.querySelector('.ann-panel-close').addEventListener('click', function (e) {
            e.stopPropagation();
            closePanel();
        });
        backdropEl.addEventListener('click', function (e) {
            if (e.target === backdropEl) closePanel();
        });
        requestAnimationFrame(function () {
            if (backdropEl) backdropEl.classList.add('ann-backdrop-show');
        });
        document.addEventListener('keydown', onPanelKeydown);
    }

    function renderHistoryList() {
        if (publishedAnnouncements.length === 0) return '';
        var items = publishedAnnouncements.map(function (a) {
            var isActive = String(a.id) === String(currentAnnouncementId);
            return '<div class="ann-history-item' + (isActive ? ' active' : '') + '" data-id="' + escapeHtml(a.id) + '">' +
                '<div class="ann-history-item-title">' +
                    '<span class="ann-history-item-dot"></span>' +
                    escapeHtml(a.title || '（无标题）') +
                '</div>' +
                '<div class="ann-history-item-time">' + escapeHtml(formatTime(a.publishTime || a.createTime)) + '</div>' +
            '</div>';
        }).join('');
        return '<div class="ann-history">' +
            '<div class="ann-history-title">历史公告</div>' +
            items +
        '</div>';
    }

    function renderDetail(id) {
        var ann = publishedAnnouncements.filter(function (a) {
            return String(a.id) === String(id);
        })[0];
        if (!ann) {
            return '<div class="ann-detail"><div class="ann-empty">公告不存在</div></div>';
        }
        var typeCfg = getTypeConfig(ann.type);
        return '<div class="ann-detail">' +
            '<div class="ann-detail-meta">' +
                '<span class="ann-detail-type" style="background:' + typeCfg.color + '">' + escapeHtml(typeCfg.label) + '</span>' +
                '<span class="ann-detail-time">📅 ' + escapeHtml(formatTime(ann.publishTime || ann.createTime)) + '</span>' +
            '</div>' +
            '<h3 class="ann-detail-title">' + escapeHtml(ann.title || '（无标题）') + '</h3>' +
            '<div class="ann-detail-content">' + renderContent(ann.content) + '</div>' +
        '</div>';
    }

    function updateActiveHistoryItem() {
        if (!panelEl) return;
        var items = panelEl.querySelectorAll('.ann-history-item');
        items.forEach(function (item) {
            if (String(item.getAttribute('data-id')) === String(currentAnnouncementId)) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });
    }

    function onPanelKeydown(e) {
        if (e.key === 'Escape') {
            e.preventDefault();
            closePanel();
        }
    }

    function closePanel() {
        // 关闭面板时，将当前查看的公告 ID 加入已读列表
        if (currentAnnouncementId != null) {
            markAsRead(currentAnnouncementId);
        }
        removePanel(true);
    }

    function removePanel(animated) {
        document.removeEventListener('keydown', onPanelKeydown);
        if (!backdropEl) return;
        var bd = backdropEl;
        var pn = panelEl;
        backdropEl = null;
        panelEl = null;
        if (animated) {
            bd.classList.remove('ann-backdrop-show');
            setTimeout(function () {
                if (bd.parentNode) bd.parentNode.removeChild(bd);
            }, 250);
        } else {
            if (bd.parentNode) bd.parentNode.removeChild(bd);
        }
        // 避免引用残留
        void pn;
    }

    // ---- API ----
    function fetchAnnouncements() {
        var url = API_PREFIX + '/api/announcements/published';
        console.log('[announcement] 请求接口：' + url);
        return fetch(url)
            .then(function (resp) {
                console.log('[announcement] 响应状态：' + resp.status + ' ' + resp.statusText);
                if (!resp.ok) return [];
                return resp.json();
            })
            .then(function (data) {
                console.log('[announcement] 响应数据：', data);
                if (data && data.success && Array.isArray(data.announcements)) {
                    console.log('[announcement] 获取到 ' + data.announcements.length + ' 条公告');
                    return data.announcements;
                }
                // 兼容直接返回数组的情况
                if (Array.isArray(data)) {
                    console.log('[announcement] 直接返回数组，共 ' + data.length + ' 条');
                    return data;
                }
                console.warn('[announcement] 响应格式异常，data=', data);
                return [];
            })
            .catch(function (e) {
                console.warn('[announcement] 获取公告失败：', e);
                return [];
            });
    }

    // ---- 全局接口（供 footer 链接等调用）----
    window.openAnnouncementPanel = function (id) {
        if (publishedAnnouncements.length === 0) {
            fetchAnnouncements().then(function (list) {
                publishedAnnouncements = list;
                if (list.length === 0) {
                    alert('暂无公告');
                    return;
                }
                openPanel(id);
            }).catch(function () {
                alert('获取公告失败，请稍后重试');
            });
            return;
        }
        openPanel(id);
    };

    // ---- 初始化 ----
    function init() {
        fetchAnnouncements().then(function (list) {
            publishedAnnouncements = list;
            var unread = getUnreadAnnouncements();
            if (unread.length > 0) {
                // 列表已按 publish_time DESC 排序，最新未读即为 unread[0]
                showFloatingWindow(unread[0]);
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        injectStyles();
        init();
    });
})();
