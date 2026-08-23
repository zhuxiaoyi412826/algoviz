/**
 * OJ 用户题解模块（方案A）
 * 功能：题解列表 / 详情 / 发布 / 评论 / 点赞
 * 依赖：oj.js 的 API_BASE 和 OJState.currentProblem
 */
(function () {
    'use strict';

    const API_BASE = 'http://localhost:80/api';
    const container = () => document.getElementById('ojUserSolutionsContent');

    /** 获取当前登录用户 */
    function getCurrentUser() {
        try {
            const raw = localStorage.getItem('userInfo');
            if (raw) {
                const u = JSON.parse(raw);
                return {
                    id: u.id || u.userId || 1,
                    username: u.username || u.nickname || '前端用户',
                    avatar: u.avatar || localStorage.getItem('userAvatar') || '👤'
                };
            }
        } catch (e) {}
        return { id: 1, username: '前端用户', avatar: '👤' };
    }

    /** API 请求带 X-User-Id header + credentials */
    async function apiGet(url) {
        const u = getCurrentUser();
        const r = await fetch(url, { headers: { 'X-User-Id': String(u.id) }, credentials: 'include' });
        return r.json();
    }
    async function apiPost(url, body) {
        const u = getCurrentUser();
        const r = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-User-Id': String(u.id) },
            credentials: 'include',
            body: JSON.stringify(body)
        });
        return r.json();
    }
    async function apiPut(url) {
        const r = await fetch(url, { method: 'PUT', credentials: 'include' });
        return r.json();
    }

    // ==================== 题解列表 ====================

    let listPage = 1;
    const listPageSize = 10;

    async function loadList(problemId) {
        destroyProcessVditor(); // 切回列表时先销毁 Vditor，释放 DOM/监听
        const c = container();
        if (!c) return;
        c.style.position = 'relative';
        c.style.overflow = 'hidden';
        c.style.padding = '0';
        c.innerHTML = `
            <div style="display:flex;flex-direction:column;position:absolute;top:0;left:0;right:0;bottom:0;">
            <div class="oj-solution-toolbar" style="display:flex;justify-content:space-between;align-items:center;padding:10px 12px;border-bottom:1px solid #3c3c3c;flex-shrink:0;">
                <span style="font-size:14px;font-weight:600;color:#d4d4d4;">用户题解</span>
                <button class="oj-solution-publish-btn" style="padding:6px 16px;background:#667eea;color:#fff;border:none;border-radius:4px;cursor:pointer;font-size:13px;">✏️ 发布题解</button>
            </div>
            <div id="ojSolutionListBody" style="flex:1;overflow-y:auto;padding:8px 0;min-height:0;"></div>
            <div id="ojSolutionListPager" style="padding:8px 12px;border-top:1px solid #3c3c3c;display:flex;justify-content:center;gap:8px;flex-shrink:0;"></div>
            </div>
        `;
        c.querySelector('.oj-solution-publish-btn').addEventListener('click', () => openPublishForm(problemId));
        await fetchList(problemId, 1);
    }

    async function fetchList(problemId, page) {
        listPage = page;
        const body = document.getElementById('ojSolutionListBody');
        if (!body) return;
        body.innerHTML = '<div style="text-align:center;padding:40px;color:#6a6a6a;">加载中...</div>';
        try {
            const r = await apiGet(`${API_BASE}/solutions?problemId=${problemId}&page=${page}&pageSize=${listPageSize}`);
            const data = r.data || r;
            const list = data.list || [];
            const total = data.total || 0;
            if (list.length === 0) {
                body.innerHTML = `
                    <div style="text-align:center;padding:40px;color:#6a6a6a;">
                        <div style="font-size:32px;margin-bottom:8px;">💬</div>
                        <p>暂无用户题解，快来发布第一篇吧！</p>
                    </div>`;
            } else {
                body.innerHTML = list.map(s => renderSolutionCard(s)).join('');
                list.forEach(s => {
                    const el = body.querySelector(`[data-solution-id="${s.id}"]`);
                    if (el) {
                        el.querySelector('.oj-sol-card-title').addEventListener('click', (e) => { e.stopPropagation(); loadDetail(s.id, problemId); });
                        el.querySelector('.oj-sol-like-btn').addEventListener('click', (e) => { e.stopPropagation(); toggleSolutionLike(s.id, el); });
                        el.querySelector('.oj-sol-comment-btn').addEventListener('click', (e) => { e.stopPropagation(); loadDetail(s.id, problemId).then(() => {
                            const area = document.getElementById('ojCommentArea');
                            if (area) area.scrollIntoView({ behavior: 'smooth', block: 'start' });
                        }); });
                    }
                });
            }
            renderPager('ojSolutionListPager', page, Math.ceil(total / listPageSize), (p) => fetchList(problemId, p));
        } catch (e) {
            body.innerHTML = `<div style="text-align:center;padding:40px;color:#f56c6c;">加载失败: ${e.message}</div>`;
        }
    }

    function renderSolutionCard(s) {
        const passed = s.isPassed === 1 ? '<span style="color:#52c41a;">✓ AC</span>' : '';
        return `
            <div data-solution-id="${s.id}" style="padding:12px 16px;border-bottom:1px solid #2d2d2d;cursor:pointer;">
                <div style="display:flex;align-items:center;gap:8px;">
                    <span class="oj-sol-card-title" style="font-size:15px;font-weight:600;color:#d4d4d4;flex:1;">${esc(s.title)}</span>
                    ${passed}
                    <span style="font-size:12px;color:#858585;">${esc(s.format || '')}</span>
                </div>
                <div style="display:flex;align-items:center;gap:12px;margin-top:6px;font-size:12px;color:#858585;">
                    <span>${esc(s.username || '匿名')}</span>
                    <span>👁 <span class="oj-sol-view-count">${s.viewCount || 0}</span></span>
                    <span class="oj-sol-like-btn" style="cursor:pointer;">👍 <span class="oj-sol-like-count">${s.likeCount || 0}</span></span>
                    <span class="oj-sol-comment-btn" style="cursor:pointer;">💬 <span class="oj-sol-comment-count">${s.commentCount || 0}</span></span>
                    <span>${fmtTime(s.createdAt)}</span>
                </div>
            </div>`;
    }

    // ==================== 题解详情 ====================

    async function loadDetail(id, problemId) {
        destroyProcessVditor(); // 切到详情时也先销毁编辑器
        const c = container();
        if (!c) return;
        c.style.position = 'relative';
        c.style.overflow = 'hidden';
        c.style.padding = '0';
        c.innerHTML = '<div style="text-align:center;padding:40px;color:#6a6a6a;">加载题解中...</div>';
        try {
            const r = await apiGet(`${API_BASE}/solutions/${id}`);
            const s = r.data;
            if (!s) { c.innerHTML = '<div style="padding:40px;color:#f56c6c;">题解不存在</div>'; return; }
            c.innerHTML = renderDetail(s, problemId);
            // 上一题/下一题
            c.querySelector('.oj-sol-prev').addEventListener('click', () => loadPrevNext(problemId, -1));
            c.querySelector('.oj-sol-next').addEventListener('click', () => loadPrevNext(problemId, 1));
            c.querySelector('.oj-sol-back').addEventListener('click', () => loadList(problemId));
            // 点赞
            c.querySelector('.oj-sol-detail-like').addEventListener('click', async () => {
                const r2 = await apiPost(`${API_BASE}/solutions/${id}/like`);
                if (r2.success) {
                    const countEl = c.querySelector('.oj-sol-detail-like-count');
                    const current = parseInt(countEl.textContent) || 0;
                    countEl.textContent = r2.liked ? current + 1 : Math.max(0, current - 1);
                }
            });
            // 评论按钮
            const cmtBtn = c.querySelector('.oj-sol-detail-comment-btn');
            if (cmtBtn) {
                cmtBtn.addEventListener('click', () => {
                    const area = document.getElementById('ojCommentArea');
                    if (area) {
                        area.scrollIntoView({ behavior: 'smooth', block: 'start' });
                        const input = document.getElementById('ojCommentInput');
                        if (input) input.focus();
                    }
                });
            }
            // 加载评论
            await loadComments(id, problemId);
        } catch (e) {
            c.innerHTML = `<div style="padding:40px;color:#f56c6c;">加载失败: ${e.message}</div>`;
        }
    }

    function renderDetail(s, problemId) {
        const prevNext = `
            <div style="display:flex;justify-content:space-between;padding:8px 12px;border-top:1px solid #3c3c3c;border-bottom:1px solid #3c3c3c;flex-shrink:0;">
                <button class="oj-sol-prev" style="background:none;border:none;color:#667eea;cursor:pointer;font-size:13px;">◀ 上一题</button>
                <button class="oj-sol-back" style="background:none;border:none;color:#858585;cursor:pointer;font-size:13px;">返回题解列表</button>
                <button class="oj-sol-next" style="background:none;border:none;color:#667eea;cursor:pointer;font-size:13px;">下一题 ▶</button>
            </div>`;
        const titleBar = `<div style="padding:12px 16px;border-bottom:1px solid #3c3c3c;flex-shrink:0;"><h3 style="margin:0;font-size:18px;color:#d4d4d4;">${esc(s.title)}</h3></div>`;
        const metaBar = `
            <div style="display:flex;align-items:center;gap:12px;padding:8px 16px;font-size:12px;color:#858585;border-bottom:1px solid #2d2d2d;flex-shrink:0;">
                <span>${esc(s.username || '匿名')}</span>
                <span>${fmtTime(s.createdAt)}</span>
                <span>👁 <span class="oj-sol-detail-view-count">${s.viewCount || 0}</span></span>
                <span class="oj-sol-detail-like" style="cursor:pointer;">👍 <span class="oj-sol-detail-like-count">${s.likeCount || 0}</span></span>
                <span class="oj-sol-detail-comment-btn" style="cursor:pointer;color:#667eea;">💬 <span class="oj-sol-detail-comment-count">${s.commentCount || 0}</span> 评论</span>
            </div>`;
        const sections = [];
        if (s.maskIdea) sections.push(renderSection('思路', s.maskIdea, { markdown: true }));
        if (s.maskProcess) sections.push(renderSection('解题过程', s.maskProcess, { markdown: true }));
        if (s.complexity) sections.push(renderSection('复杂度', esc(s.complexity), { markdown: false }));
        if (s.maskCode) sections.push(renderSection('代码', `<pre style="margin:0;background:#1e1e1e;padding:12px;border-radius:4px;overflow-x:auto;font-size:12px;"><code>${esc(s.maskCode)}</code></pre>`, { markdown: false, raw: true }));
        const contentArea = `<div style="padding:12px 16px;">${sections.join('')}</div>`;
        const commentArea = `<div id="ojCommentArea" style="padding:12px 16px;border-top:1px solid #3c3c3c;"></div>`;
        return `
            <div style="display:flex;flex-direction:column;position:absolute;top:0;left:0;right:0;bottom:0;">
                ${prevNext}
                ${titleBar}
                ${metaBar}
                <div style="flex:1;overflow-y:auto;min-height:0;">
                    ${contentArea}
                    ${commentArea}
                </div>
            </div>`;
    }

    /**
     * 渲染题解详情的分节块
     * @param {string} title    分节标题
     * @param {string} content  内容：Markdown 字符串 或 已构造好的 HTML（代码段的 pre）
     * @param {{markdown?:boolean,raw?:boolean}} options
     *   - markdown: true → 用 marked 解析 md，再经 DOMPurify 安全过滤，外层挂 .vditor-reset 获得排版
     *   - raw: true      → 内容已是安全 HTML，直接插入（如代码段 pre 包裹的内容）
     *   - 默认           → 纯文本直接显示，保留 esc() 兜底
     */
    function renderSection(title, content, options) {
        options = options || {};
        let html = '';
        if (options.markdown) {
            const mdText = String(content || '');
            let rendered = '';
            try {
                if (window.marked && typeof marked.parse === 'function') {
                    rendered = marked.parse(mdText, { breaks: true, gfm: true });
                } else {
                    // marked 未加载兜底：换行转 <br/>，保留基本可读性
                    rendered = esc(mdText).replace(/\n/g, '<br/>');
                }
                if (window.DOMPurify && typeof DOMPurify.sanitize === 'function') {
                    rendered = DOMPurify.sanitize(rendered);
                }
            } catch (e) {
                rendered = esc(mdText);
            }
            // 包 vditor-reset 获得 Markdown 排版样式（深色主题会继承 OJ 背景色）
            html = `<div class="vditor-reset" style="background:transparent;padding:0;border:0;color:#d4d4d4;line-height:1.75;">${rendered}</div>`;
        } else if (options.raw) {
            // 代码段：调用方已手工拼好 pre+esc，直接用
            html = String(content || '');
        } else {
            // 纯文本字段（复杂度标题等）
            html = `<div style="font-size:13px;color:#d4d4d4;line-height:1.7;">${esc(content)}</div>`;
        }
        return `<div style="margin-bottom:16px;"><h4 style="font-size:14px;font-weight:600;color:#8585ff;margin:0 0 8px;">${title}</h4>${html}</div>`;
    }

    async function loadPrevNext(problemId, dir) {
        if (!window.OJState || !OJState.problems) return;
        const idx = OJState.problems.findIndex(p => p.id == problemId);
        if (idx < 0) return;
        const ni = idx + dir;
        if (ni < 0 || ni >= OJState.problems.length) return;
        const np = OJState.problems[ni];
        // 切换题目（复用 oj.js 的逻辑）
        if (window.selectProblem) selectProblem(np);
        else if (window.loadProblem) loadProblem(np.id);
        // 加载新题目的题解列表
        setTimeout(() => loadList(np.id), 200);
    }

    // ==================== 评论（支持二级/三级/N级嵌套回复） ====================

    let commentPage = 1;
    const commentPageSize = 20;

    async function loadComments(solutionId, problemId) {
        commentPage = 1;
        await fetchComments(solutionId, problemId, 1);
    }

    async function fetchComments(solutionId, problemId, page) {
        commentPage = page;
        const area = document.getElementById('ojCommentArea');
        if (!area) return;
        area.innerHTML = '<div style="color:#6a6a6a;font-size:13px;">加载评论...</div>';
        try {
            const r = await apiGet(`${API_BASE}/solutions/${solutionId}/comments?page=${page}&pageSize=${commentPageSize}`);
            const data = r.data || r;
            const list = data.list || [];
            const total = data.total || 0;

            let html = `<div style="font-size:14px;font-weight:600;color:#d4d4d4;margin-bottom:12px;">评论 (${total})</div>`;

            // 评论输入框
            const u = getCurrentUser();
            html += `
                <div style="display:flex;gap:8px;margin-bottom:16px;">
                    <input type="text" id="ojCommentInput" placeholder="写下你的评论..." style="flex:1;padding:8px 12px;background:#1e1e1e;border:1px solid #3c3c3c;border-radius:4px;color:#d4d4d4;font-size:13px;outline:none;" />
                    <button id="ojCommentSubmitBtn" style="padding:8px 16px;background:#667eea;color:#fff;border:none;border-radius:4px;cursor:pointer;font-size:13px;">发布</button>
                </div>`;

            // 顶层评论列表（每个评论有占位容器 ojReplies_ 用来放子评论树）
            if (list.length === 0) {
                html += '<div style="color:#6a6a6a;font-size:13px;text-align:center;padding:20px;">暂无评论，快来抢沙发！</div>';
            } else {
                html += list.map(c => renderComment(c, solutionId)).join('');
            }

            area.innerHTML = html;

            // 绑定发布顶层评论
            const submitBtn = document.getElementById('ojCommentSubmitBtn');
            if (submitBtn) {
                submitBtn.addEventListener('click', async () => {
                    const input = document.getElementById('ojCommentInput');
                    const text = input.value.trim();
                    if (!text) return;
                    const r = await apiPost(`${API_BASE}/solutions/${solutionId}/comments`, {
                        content: text,
                        problemId: problemId,
                        username: u.username,
                        avatar: u.avatar,
                        parentId: 0
                    });
                    if (r.success) {
                        input.value = '';
                        await fetchComments(solutionId, problemId, commentPage);
                        const cmtCountEl = document.querySelector('.oj-sol-detail-comment-count');
                        if (cmtCountEl) cmtCountEl.textContent = (parseInt(cmtCountEl.textContent) || 0) + 1;
                    } else {
                        alert(r.message || '发布失败');
                    }
                });
            }

            // 事件委托：点赞 / 展开子评论 / 回复（统一在容器上，跨层级生效）
            bindCommentAreaEvents(area, solutionId, problemId, list);

        } catch (e) {
            area.innerHTML = `<div style="color:#f56c6c;">评论加载失败: ${e.message}</div>`;
        }
    }

    /** 顶层评论卡片（子评论树占位符 + 每层卡片样式保持一致） */
    function renderComment(c, solutionId) {
        const replyCount = c.replyCount || 0;
        const expandBtn = replyCount > 0
            ? `<span class="oj-cmt-expand" data-root-id="${c.id}" style="cursor:pointer;color:#667eea;font-size:12px;">展开 ${replyCount} 条回复 ▼</span>`
            : '';
        return `
            <div data-comment-id="${c.id}" data-username="${esc(c.username || '匿名')}"
                 style="padding:10px 0;border-bottom:1px solid #2d2d2d;">
                <div style="display:flex;align-items:flex-start;gap:8px;">
                    <div style="flex:1;">
                        <div style="font-size:13px;font-weight:600;color:#d4d4d4;">${esc(c.username || '匿名')}</div>
                        <div style="font-size:13px;color:#a0a0a0;margin:4px 0;">${esc(c.maskContent || c.content || '')}</div>
                        <div style="display:flex;gap:12px;align-items:center;">
                            <span style="font-size:12px;color:#858585;">${fmtTime(c.createdAt)}</span>
                            <span class="oj-cmt-like" data-like-id="${c.id}" data-like-count="${c.likeCount || 0}"
                                  style="cursor:pointer;font-size:12px;color:#858585;">👍 ${c.likeCount || 0}</span>
                            <span class="oj-cmt-reply" data-reply-id="${c.id}" data-reply-user="${esc(c.username || '匿名')}" data-root-id="${c.id}"
                                  style="cursor:pointer;font-size:12px;color:#858585;">回复</span>
                            ${expandBtn}
                        </div>
                    </div>
                </div>
                <div id="ojReplies_${c.id}" style="margin-left:24px;"></div>
                <div id="ojReplyForm_${c.id}"></div>
            </div>`;
    }

    /**
     * 递归渲染嵌套子评论树（children 数组）。
     * 生成的 HTML：
     *   - 每个节点卡片都带 data-comment-id / data-username
     *   - 每级评论带「回复 / 点赞」按钮（事件委托到顶层容器）
     *   - 显示「回复 @xxx :」前缀（当 replyToUsername 非空）
     */
    function renderCommentTree(list) {
        if (!list || list.length === 0) return '';
        return list.map(c => {
            const childrenHtml = (c.children && c.children.length > 0)
                ? `<div style="margin-left:20px;margin-top:6px;">${renderCommentTree(c.children)}</div>`
                : '';
            const replyPrefix = c.replyToUsername
                ? `<span style="color:#858585;font-weight:normal;">回复 <span style="color:#667eea;">@${esc(c.replyToUsername)}</span> :</span>`
                : '';
            // 计算这一层节点属于哪个顶层 root：优先用 c.rootId；root 其实一直不变，但展开回复框传 rootId 是为了再展开时不会错（因为 submit 后端用 parent 查 root）
            return `
                <div data-comment-id="${c.id}" data-username="${esc(c.username || '匿名')}"
                     style="padding:8px 0;border-bottom:1px dashed #2a2a2a;">
                    <div style="font-size:13px;font-weight:600;color:#d4d4d4;">${esc(c.username || '匿名')} ${replyPrefix}</div>
                    <div style="font-size:13px;color:#a0a0a0;margin:4px 0;">${esc(c.maskContent || c.content || '')}</div>
                    <div style="display:flex;gap:12px;align-items:center;">
                        <span style="font-size:12px;color:#858585;">${fmtTime(c.createdAt)}</span>
                        <span class="oj-cmt-like" data-like-id="${c.id}" data-like-count="${c.likeCount || 0}"
                              style="cursor:pointer;font-size:12px;color:#858585;">👍 ${c.likeCount || 0}</span>
                        <span class="oj-cmt-reply" data-reply-id="${c.id}" data-reply-user="${esc(c.username || '匿名')}"
                              style="cursor:pointer;font-size:12px;color:#858585;">回复</span>
                    </div>
                    ${childrenHtml}
                    <div id="ojReplyForm_${c.id}"></div>
                </div>`;
        }).join('');
    }

    /** 事件委托：点赞 / 展开回复 / 打开回复框（递归子节点也会命中） */
    function bindCommentAreaEvents(area, solutionId, problemId, topLevelList) {
        // 点赞
        area.addEventListener('click', async (e) => {
            const likeBtn = e.target.closest('.oj-cmt-like');
            if (likeBtn) {
                const id = likeBtn.getAttribute('data-like-id');
                const cur = parseInt(likeBtn.getAttribute('data-like-count') || '0');
                if (!id) return;
                try {
                    const r2 = await apiPost(`${API_BASE}/solutions/comments/${id}/like`);
                    if (r2.success) {
                        const next = r2.liked ? cur + 1 : Math.max(0, cur - 1);
                        likeBtn.textContent = `👍 ${next}`;
                        likeBtn.setAttribute('data-like-count', next);
                    }
                } catch (err) { console.warn(err); }
                return;
            }

            // 展开某顶层评论下的完整子树
            const expandBtn = e.target.closest('.oj-cmt-expand');
            if (expandBtn) {
                const rootId = expandBtn.getAttribute('data-root-id');
                if (!rootId) return;
                const box = document.getElementById(`ojReplies_${rootId}`);
                if (!box) return;
                if (box.innerHTML) {
                    // 已加载：折叠
                    box.innerHTML = '';
                    expandBtn.textContent = expandBtn.textContent.replace(/▲$/, '▼').replace('收起', '展开');
                } else {
                    await loadRepliesTree(rootId);
                    expandBtn.textContent = expandBtn.textContent.replace('展开', '收起').replace('▼', '▲');
                }
                return;
            }

            // 回复（顶层评论或嵌套里的子评论都可命中）
            const replyBtn = e.target.closest('.oj-cmt-reply');
            if (replyBtn) {
                const replyId = replyBtn.getAttribute('data-reply-id');
                const replyUser = replyBtn.getAttribute('data-reply-user');
                if (!replyId) return;
                showReplyForm(replyId, solutionId, problemId, replyUser);
            }
        });
    }

    /** 加载某顶层评论下的完整嵌套回复树（二级、三级...N级）并渲染到 ojReplies_rootId */
    async function loadRepliesTree(rootId) {
        const box = document.getElementById(`ojReplies_${rootId}`);
        if (!box) return;
        try {
            const r = await apiGet(`${API_BASE}/solutions/comments/${rootId}/replies/tree`);
            const tree = (r && (r.data || r)) || [];
            if (!tree || tree.length === 0) {
                box.innerHTML = '<div style="color:#6a6a6a;font-size:12px;">暂无回复</div>';
                return;
            }
            box.innerHTML = renderCommentTree(tree);
        } catch (e) {
            box.innerHTML = '<div style="color:#f56c6c;font-size:12px;">加载回复失败</div>';
        }
    }

    /**
     * 显示/隐藏 回复输入框（对任意层级评论）。
     * 提交时只需传 parentId（= 被回复评论 id），后端会根据父评论自动填 rootId / replyToUserId。
     */
    function showReplyForm(commentId, solutionId, problemId, replyToUsername) {
        const form = document.getElementById(`ojReplyForm_${commentId}`);
        if (!form) return;
        if (form.innerHTML) { form.innerHTML = ''; return; }
        form.innerHTML = `
            <div style="display:flex;gap:8px;margin-top:8px;">
                <input type="text" placeholder="回复 ${esc(replyToUsername)}..." style="flex:1;padding:6px 10px;background:#1e1e1e;border:1px solid #3c3c3c;border-radius:4px;color:#d4d4d4;font-size:12px;outline:none;" />
                <button class="oj-reply-submit" style="padding:6px 12px;background:#667eea;color:#fff;border:none;border-radius:4px;cursor:pointer;font-size:12px;">回复</button>
                <button class="oj-reply-cancel" style="padding:6px 12px;background:#3c3c3c;color:#d4d4d4;border:none;border-radius:4px;cursor:pointer;font-size:12px;">取消</button>
            </div>`;
        const input = form.querySelector('input');
        const submitBtn = form.querySelector('.oj-reply-submit');
        const cancelBtn = form.querySelector('.oj-reply-cancel');
        cancelBtn.addEventListener('click', () => form.innerHTML = '');
        submitBtn.addEventListener('click', async () => {
            const text = input.value.trim();
            if (!text) return;
            const u = getCurrentUser();
            const r = await apiPost(`${API_BASE}/solutions/${solutionId}/comments`, {
                content: text,
                problemId: problemId,
                username: u.username,
                avatar: u.avatar,
                parentId: parseInt(commentId, 10)
            });
            if (r.success) {
                form.innerHTML = '';
                // 回复成功：
                //   若回复的是顶层评论（ojReplies_xxx 存在），刷新树；
                //   若回复的是嵌套子评论，找到最近的顶层 root 重新加载那棵树；
                //   找不到顶层就整体刷新评论列表兜底。
                let ok = false;
                if (document.getElementById(`ojReplies_${commentId}`)) {
                    await loadRepliesTree(commentId);
                    ok = true;
                } else {
                    // 找到嵌套所在的最近顶层：上溯 DOM 中第一个带 [data-root-id] 或第一个 #ojReplies_xxx 的容器
                    const area = document.getElementById('ojCommentArea');
                    const thisNode = area ? area.querySelector(`[data-comment-id="${commentId}"]`) : null;
                    if (area && thisNode) {
                        const expanded = area.querySelectorAll('[id^="ojReplies_"]');
                        for (const box of expanded) {
                            if (box.innerHTML && box.querySelector(`[data-comment-id="${commentId}"]`)) {
                                const rootId = box.id.replace('ojReplies_', '');
                                await loadRepliesTree(rootId);
                                ok = true;
                                break;
                            }
                        }
                    }
                }
                if (!ok) await fetchComments(solutionId, problemId, commentPage);
                // 更新详情页评论数
                const cmtCountEl = document.querySelector('.oj-sol-detail-comment-count');
                if (cmtCountEl) cmtCountEl.textContent = (parseInt(cmtCountEl.textContent) || 0) + 1;
            } else {
                alert(r.message || '回复失败');
            }
        });
    }

    // ==================== 发布/编辑题解 ====================
    // 自建 Markdown 分屏编辑器：左原生 textarea（源码）+ 右预览 div + 原生 oninput 每键必触发 → 真正 0 延迟
    // （不再依赖 Vditor sv 模式：其源码编辑区是 contenteditable，input 回调不稳定，导致右侧不更新）
    let processVditor = null; // 对外接口不变：{ getValue, setValue, focus }
    let _splitMoveHandler = null;
    let _splitUpHandler = null;
    let _syncLeftHandler = null;
    let _syncRightHandler = null;
    let _isSyncing = false;
    let _toolbarBtnHandlers = [];

    function destroyProcessVditor() {
        if (_splitMoveHandler) { document.removeEventListener('mousemove', _splitMoveHandler); _splitMoveHandler = null; }
        if (_splitUpHandler)   { document.removeEventListener('mouseup',   _splitUpHandler);   _splitUpHandler = null; }
        if (processVditor && processVditor.textarea && _syncLeftHandler) {
            try { processVditor.textarea.removeEventListener('scroll', _syncLeftHandler); } catch (e) {}
        }
        if (processVditor && processVditor.preview && _syncRightHandler) {
            try { processVditor.preview.removeEventListener('scroll', _syncRightHandler); } catch (e) {}
        }
        _syncLeftHandler = _syncRightHandler = null;
        for (const h of _toolbarBtnHandlers) { try { h.el.removeEventListener('click', h.fn); } catch (e) {} }
        _toolbarBtnHandlers = [];
        if (processVditor) {
            try { if (processVditor.textarea) processVditor.textarea.oninput = null; } catch (e) {}
            processVditor = null;
        }
    }

    /** Markdown → 安全 HTML：marked.parse + DOMPurify */
    function mdToHtml(md) {
        md = String(md || '');
        let html = '';
        try {
            if (window.marked && typeof marked.parse === 'function') {
                html = marked.parse(md, { breaks: true, gfm: true, headerIds: false, mangle: false });
            } else {
                html = esc(md).replace(/\n/g, '<br/>');
            }
            if (window.DOMPurify && typeof DOMPurify.sanitize === 'function') {
                html = DOMPurify.sanitize(html);
            }
        } catch (e) {
            html = esc(md);
        }
        return html;
    }

    /* ========== Markdown 工具栏操作：操作选区/行首，完成后立刻触发 oninput 同步右侧 ========== */
    function wrapSelection(ta, before, after) {
        after = after || '';
        const s = ta.selectionStart, e = ta.selectionEnd;
        const v = ta.value;
        const sel = v.slice(s, e);
        ta.value = v.slice(0, s) + before + sel + after + v.slice(e);
        ta.focus();
        ta.setSelectionRange(s + before.length, e + before.length);
        ta.dispatchEvent(new Event('input', { bubbles: true }));
    }
    function prependLine(ta, prefix) {
        const s = ta.selectionStart, e = ta.selectionEnd;
        const v = ta.value;
        const lineStart = v.lastIndexOf('\n', s - 1) + 1;
        const lineEnd = v.indexOf('\n', e);
        const sel = v.slice(lineStart, lineEnd === -1 ? v.length : lineEnd);
        ta.value = v.slice(0, lineStart) + prefix + sel + (lineEnd === -1 ? '' : v.slice(lineEnd));
        ta.focus();
        ta.setSelectionRange(s + prefix.length, e + prefix.length);
        ta.dispatchEvent(new Event('input', { bubbles: true }));
    }
    function insertAtCursor(ta, snippet) {
        const s = ta.selectionStart, e = ta.selectionEnd;
        const v = ta.value;
        ta.value = v.slice(0, s) + snippet + v.slice(e);
        ta.focus();
        const pos = s + snippet.length;
        ta.setSelectionRange(pos, pos);
        ta.dispatchEvent(new Event('input', { bubbles: true }));
    }
    function buildMdToolbarDef() {
        return [
            { label: 'H1',  title: '一级标题',   act: (ta) => prependLine(ta, '# ') },
            { label: 'H2',  title: '二级标题',   act: (ta) => prependLine(ta, '## ') },
            { label: 'H3',  title: '三级标题',   act: (ta) => prependLine(ta, '### ') },
            { sep: true },
            { label: 'B',   title: '粗体',       act: (ta) => wrapSelection(ta, '**', '**') },
            { label: 'I',   title: '斜体',       act: (ta) => wrapSelection(ta, '_', '_') },
            { label: 'S',   title: '删除线',     act: (ta) => wrapSelection(ta, '~~', '~~') },
            { sep: true },
            { label: '•',   title: '无序列表',   act: (ta) => prependLine(ta, '- ') },
            { label: '1.',  title: '有序列表',   act: (ta) => prependLine(ta, '1. ') },
            { label: '❝',   title: '引用',       act: (ta) => prependLine(ta, '> ') },
            { sep: true },
            { label: '🔗',  title: '链接',       act: (ta) => wrapSelection(ta, '[', '](https://)') },
            { label: '<>',  title: '行内代码',   act: (ta) => wrapSelection(ta, '`', '`') },
            { label: '{ }', title: '代码块',     act: (ta) => { const sel = ta.value.slice(ta.selectionStart, ta.selectionEnd) || 'java';
                const block = sel.indexOf('\n') >= 0
                    ? { b: '```\n', a: '\n```', mid: sel }
                    : { b: '```' + sel + '\n', a: '\n```', mid: '' };
                wrapSelection(ta, block.b, block.a);
            } },
            { sep: true },
            { label: '⊞',   title: '表格',       act: (ta) => insertAtCursor(ta, '\n| 列1 | 列2 | 列3 |\n| --- | --- | --- |\n| 内容 | 内容 | 内容 |\n') },
            { label: '—',   title: '分割线',     act: (ta) => insertAtCursor(ta, '\n\n---\n\n') },
            { sep: true },
            { label: '↶',   title: '撤销',       act: (ta) => { try { document.execCommand('undo'); ta.dispatchEvent(new Event('input', { bubbles: true })); } catch (err) {} } },
            { label: '↷',   title: '重做',       act: (ta) => { try { document.execCommand('redo'); ta.dispatchEvent(new Event('input', { bubbles: true })); } catch (err) {} } }
        ];
    }
    function toolbarHtml(def) {
        let html = '';
        for (const b of def) {
            if (b.sep) html += `<span class="md-tb-sep" style="display:inline-block;width:1px;height:16px;background:#3c3c3c;margin:0 4px;vertical-align:middle;"></span>`;
            else html += `<button type="button" class="md-tb-btn" data-act="${esc(b.label)}" title="${esc(b.title)}" style="display:inline-flex;align-items:center;justify-content:center;min-width:28px;height:26px;padding:0 6px;margin:0 1px;background:transparent;color:#d4d4d4;border:1px solid transparent;border-radius:3px;cursor:pointer;font-size:12px;font-weight:600;font-family:inherit;box-sizing:border-box;vertical-align:middle;">${esc(b.label)}</button>`;
        }
        return html;
    }

    async function openPublishForm(problemId) {
        const c = container();
        if (!c) return;
        destroyProcessVditor();
        const u = getCurrentUser();
        c.style.position = 'relative';
        c.style.overflow = 'hidden';
        c.style.padding = '0';
        const tbDef = buildMdToolbarDef();
        c.innerHTML = renderPublishForm(u, toolbarHtml(tbDef));

        const wrap = document.getElementById('ojSolSplitWrap');
        const leftPane = wrap ? wrap.querySelector('.split-left') : null;
        const rightPane = wrap ? wrap.querySelector('.split-right') : null;
        const resizer = wrap ? wrap.querySelector('.split-resizer') : null;
        const previewBox = rightPane ? rightPane.querySelector('.oj-sol-preview') : null;
        const textarea = leftPane ? leftPane.querySelector('textarea[name="process"]') : null;
        const toolbar = leftPane ? leftPane.querySelector('.md-toolbar') : null;
        if (!wrap || !textarea || !previewBox) return;

        /* ===== 右侧实时渲染 + 滚动位置比例保留 ===== */
        function renderNow(md) {
            previewBox.innerHTML = mdToHtml(md);
            if (!_isSyncing) {
                const r = textarea.scrollHeight - textarea.clientHeight;
                const ratio = r > 0 ? textarea.scrollTop / r : 0;
                const rr = previewBox.scrollHeight - previewBox.clientHeight;
                previewBox.scrollTop = rr * ratio;
            }
        }

        /* ===== 核心：原生 textarea.oninput，保证每次击键/工具栏操作后立刻同步右侧，0 延迟 ===== */
        textarea.oninput = function () { renderNow(textarea.value); };

        /* ===== 工具栏按钮绑定：每操作一次都 dispatch input 事件 ===== */
        const actMap = {};
        for (const b of tbDef) { if (!b.sep) actMap[b.label] = b; }
        if (toolbar) {
            const btns = toolbar.querySelectorAll('.md-tb-btn');
            btns.forEach(btn => {
                const lab = btn.getAttribute('data-act') || btn.textContent;
                const b = actMap[lab];
                if (!b || !b.act) return;
                const fn = (ev) => {
                    ev.preventDefault();
                    textarea.focus();
                    b.act(textarea);
                };
                btn.addEventListener('click', fn);
                btn.addEventListener('mouseenter', () => { btn.style.background = '#3c3c3c'; btn.style.color = '#ffffff'; });
                btn.addEventListener('mouseleave', () => { btn.style.background = 'transparent'; btn.style.color = '#d4d4d4'; });
                _toolbarBtnHandlers.push({ el: btn, fn });
            });
        }

        /* ===== 对外统一接口（保持 processVditor 旧名，提交流程零改动）===== */
        processVditor = {
            textarea,
            toolbar,
            preview: previewBox,
            getValue() { return textarea.value; },
            setValue(v) { textarea.value = String(v || ''); renderNow(textarea.value); },
            focus() { textarea.focus(); }
        };

        /* ===== 左右滚动双向按比例同步 ===== */
        _syncLeftHandler = function () {
            if (_isSyncing) return;
            _isSyncing = true;
            const lmax = textarea.scrollHeight - textarea.clientHeight;
            const rmax = previewBox.scrollHeight - previewBox.clientHeight;
            if (lmax > 0 && rmax > 0) previewBox.scrollTop = (textarea.scrollTop / lmax) * rmax;
            requestAnimationFrame(() => { _isSyncing = false; });
        };
        _syncRightHandler = function () {
            if (_isSyncing) return;
            _isSyncing = true;
            const lmax = textarea.scrollHeight - textarea.clientHeight;
            const rmax = previewBox.scrollHeight - previewBox.clientHeight;
            if (lmax > 0 && rmax > 0) textarea.scrollTop = (previewBox.scrollTop / rmax) * lmax;
            requestAnimationFrame(() => { _isSyncing = false; });
        };
        textarea.addEventListener('scroll', _syncLeftHandler, { passive: true });
        previewBox.addEventListener('scroll', _syncRightHandler, { passive: true });

        /* ===== 拖拽分隔条（15%~85%）===== */
        if (resizer) {
            let startX = 0, startL = 0, totalW = 0;
            resizer.addEventListener('mousedown', function (e) {
                e.preventDefault();
                startX = e.clientX;
                totalW = wrap.clientWidth - resizer.clientWidth;
                startL = leftPane.getBoundingClientRect().width;
                _splitMoveHandler = function (ev) {
                    const delta = ev.clientX - startX;
                    let percent = ((startL + delta) / totalW) * 100;
                    if (percent < 15) percent = 15;
                    if (percent > 85) percent = 85;
                    leftPane.style.flex = `0 0 ${percent}%`;
                    leftPane.style.width = percent + '%';
                    rightPane.style.flex = '1 1 auto';
                };
                _splitUpHandler = function () {
                    document.removeEventListener('mousemove', _splitMoveHandler);
                    document.removeEventListener('mouseup', _splitUpHandler);
                    _splitMoveHandler = _splitUpHandler = null;
                };
                document.addEventListener('mousemove', _splitMoveHandler);
                document.addEventListener('mouseup', _splitUpHandler);
            });
        }

        /* ===== 取消/发布按钮 ===== */
        const submitBtn = c.querySelector('.oj-sol-submit-btn');
        const cancelBtn = c.querySelector('.oj-sol-cancel-btn');
        if (submitBtn) submitBtn.addEventListener('click', async () => { await submitPublish(problemId); });
        if (cancelBtn) cancelBtn.addEventListener('click', () => { destroyProcessVditor(); loadList(problemId); });

        /* ===== 回显已有题解 / 默认 placeholder 预览 ===== */
        try {
            const r = await apiGet(`${API_BASE}/solutions/my?problemId=${problemId}`);
            if (r.data) {
                const s = r.data;
                const ideaEl = c.querySelector('[name="idea"]');
                const compEl = c.querySelector('[name="complexity"]');
                const langEl = c.querySelector('[name="codeLang"]');
                const codeEl = c.querySelector('[name="code"]');
                if (ideaEl) ideaEl.value = s.idea || '';
                if (compEl) compEl.value = s.complexity || '';
                if (langEl) langEl.value = s.codeLang || 'java';
                if (codeEl) codeEl.value = s.code || '';
                processVditor.setValue(s.process || '');
            } else {
                const placeholder = `> 左侧开始输入 Markdown 源码，这里**即时**渲染预览。

## 算法思路
…描述核心算法…

## 解题步骤
1. 第一步
2. 第二步

\`\`\`java
class Solution {
    public void solve() {
        // your code
    }
}
\`\`\`
`;
                textarea.placeholder = '## 算法思路\n……\n\n## 解题步骤\n1. ……\n\n```java\nclass Solution { }\n```';
                renderNow(placeholder);
            }
        } catch (e) {
            renderNow('> 左侧开始输入 Markdown 源码，这里 **即时** 渲染预览。');
        }
    }

    function renderPublishForm(u, toolbarHtmlStr) {
        const problemTitle = (OJState.currentProblem && OJState.currentProblem.title) || '';
        toolbarHtmlStr = toolbarHtmlStr || '';
        return `
            <div style="display:flex;flex-direction:column;position:absolute;top:0;left:0;right:0;bottom:0;">
                <div style="padding:10px 16px;border-bottom:1px solid #3c3c3c;display:flex;justify-content:space-between;align-items:center;flex-shrink:0;">
                    <div>
                        <h3 style="margin:0 0 4px;font-size:18px;color:#d4d4d4;">发布题解</h3>
                        <p style="margin:0;font-size:13px;color:#858585;">题目：${esc(problemTitle)}</p>
                    </div>
                    <div style="display:flex;gap:8px;">
                        <button type="button" class="oj-sol-cancel-btn" style="padding:6px 14px;background:#3c3c3c;color:#d4d4d4;border:none;border-radius:4px;cursor:pointer;font-size:13px;">取消</button>
                        <button type="button" class="oj-sol-submit-btn" style="padding:6px 14px;background:#667eea;color:#fff;border:none;border-radius:4px;cursor:pointer;font-size:13px;">发布</button>
                    </div>
                </div>
                <div style="padding:12px 16px;overflow-y:auto;flex:1;min-height:0;">
                    <form id="ojSolPublishForm" style="display:flex;flex-direction:column;gap:12px;">
                        <input type="hidden" name="title" value="${esc(problemTitle)}" />
                        <input type="hidden" name="format" value="" />
                        <div>
                            <label style="font-size:13px;color:#858585;display:block;margin-bottom:4px;">思路</label>
                            <textarea name="idea" rows="3" placeholder="一句话或简要点明核心思路，纯文本/Markdown均可"
                                style="width:100%;padding:8px 12px;background:#1e1e1e;border:1px solid #3c3c3c;border-radius:4px;color:#d4d4d4;font-size:13px;resize:vertical;outline:none;box-sizing:border-box;"></textarea>
                        </div>
                        <!-- 解题过程：自建分屏（左 textarea + 工具栏 + 右预览），确保输入后右侧立即渲染 Markdown -->
                        <div>
                            <label style="font-size:13px;color:#858585;display:block;margin-bottom:6px;">
                                解题过程 <span style="color:#667eea;font-size:12px;">· 左侧 Markdown 源码 / 右侧每击一键即时渲染 · 存入原始 md 字符串</span>
                            </label>
                            <div id="ojSolSplitWrap" style="height:520px;display:flex;border:1px solid #3c3c3c;border-radius:6px;overflow:hidden;background:#1e1e1e;color:#d4d4d4;">
                                <div class="split-left" style="flex:0 0 50%;width:50%;height:100%;display:flex;flex-direction:column;background:#1e1e1e;border-right:1px solid #3c3c3c;min-width:0;">
                                    <div class="md-toolbar" style="flex-shrink:0;background:#252526;border-bottom:1px solid #3c3c3c;padding:4px 6px;white-space:nowrap;overflow-x:auto;">${toolbarHtmlStr}</div>
                                    <textarea name="process" spellcheck="false" autocorrect="off" autocomplete="off"
                                        style="flex:1 1 auto;min-height:0;width:100%;resize:none;padding:10px 12px;background:#1e1e1e;border:0;color:#d4d4d4;caret-color:#d4d4d4;font-family:'Fira Code',Consolas,monospace;font-size:13px;line-height:1.6;outline:none;box-sizing:border-box;"
                                        placeholder="## 算法思路&#10;……&#10;&#10;## 解题步骤&#10;1. ……&#10;&#10;&#96;&#96;&#96;java&#10;class Solution { }&#10;&#96;&#96;&#96;"></textarea>
                                </div>
                                <div class="split-resizer" style="width:6px;background:#3c3c3c;cursor:col-resize;flex-shrink:0;user-select:none;transition:background .15s;"
                                     onmouseenter="this.style.background='#667eea';" onmouseleave="this.style.background='#3c3c3c';"></div>
                                <div class="split-right" style="flex:1 1 auto;height:100%;overflow:auto;background:#1e1e1e;padding:12px 16px;box-sizing:border-box;min-width:0;">
                                    <div class="oj-sol-preview vditor-reset" style="background:transparent;border:0;padding:0;color:#d4d4d4;line-height:1.75;font-size:13px;"></div>
                                </div>
                            </div>
                            <style>
                                /* 预览排版（深色 #1e1e1e / #3c3c3c / #d4d4d4，与下方代码 textarea 完全一致）*/
                                #ojSolSplitWrap .oj-sol-preview pre,
                                #ojSolSplitWrap .oj-sol-preview code {
                                    background:#1e1e1e !important; color:#d4d4d4 !important;
                                    border:1px solid #3c3c3c !important; border-radius:4px !important;
                                }
                                #ojSolSplitWrap .oj-sol-preview pre { padding:12px !important; overflow-x:auto; font-family:'Fira Code',Consolas,monospace; font-size:12px;}
                                #ojSolSplitWrap .oj-sol-preview :not(pre) > code { padding:1px 5px; font-family:'Fira Code',Consolas,monospace; }
                                #ojSolSplitWrap .oj-sol-preview h1,
                                #ojSolSplitWrap .oj-sol-preview h2,
                                #ojSolSplitWrap .oj-sol-preview h3,
                                #ojSolSplitWrap .oj-sol-preview h4,
                                #ojSolSplitWrap .oj-sol-preview h5,
                                #ojSolSplitWrap .oj-sol-preview h6 { color:#ffffff; margin:0.8em 0 0.4em; line-height:1.35; }
                                #ojSolSplitWrap .oj-sol-preview h1 { font-size:20px; border-bottom:1px solid #3c3c3c; padding-bottom:6px;}
                                #ojSolSplitWrap .oj-sol-preview h2 { font-size:17px; border-bottom:1px solid #3c3c3c; padding-bottom:4px;}
                                #ojSolSplitWrap .oj-sol-preview h3 { font-size:15px; }
                                #ojSolSplitWrap .oj-sol-preview p  { margin:0.4em 0; }
                                #ojSolSplitWrap .oj-sol-preview ul,
                                #ojSolSplitWrap .oj-sol-preview ol { margin:0.4em 0 0.4em 1.6em; padding:0; }
                                #ojSolSplitWrap .oj-sol-preview li { margin:2px 0; }
                                #ojSolSplitWrap .oj-sol-preview a  { color:#667eea; text-decoration:none; }
                                #ojSolSplitWrap .oj-sol-preview a:hover { text-decoration:underline; }
                                #ojSolSplitWrap .oj-sol-preview hr { border:0; border-top:1px solid #3c3c3c; margin:12px 0; }
                                #ojSolSplitWrap .oj-sol-preview img { max-width:100%; border-radius:4px; }
                                #ojSolSplitWrap .oj-sol-preview table { border-collapse:collapse; margin:0.4em 0; }
                                #ojSolSplitWrap .oj-sol-preview th,
                                #ojSolSplitWrap .oj-sol-preview td {
                                    border:1px solid #3c3c3c !important; padding:6px 10px;
                                    background:#1e1e1e !important; color:#d4d4d4 !important;
                                }
                                #ojSolSplitWrap .oj-sol-preview th { background:#252526 !important; font-weight:600; }
                                #ojSolSplitWrap .oj-sol-preview blockquote {
                                    border-left:4px solid #667eea !important; background:#252526 !important;
                                    color:#d4d4d4 !important; padding:8px 12px; margin:8px 0;
                                    border-radius:0 4px 4px 0 !important;
                                }
                            </style>
                        </div>
                        <div>
                            <label style="font-size:13px;color:#858585;display:block;margin-bottom:4px;">复杂度</label>
                            <input name="complexity" type="text" placeholder="如：时间 O(n) 空间 O(1)"
                                style="width:100%;padding:8px 12px;background:#1e1e1e;border:1px solid #3c3c3c;border-radius:4px;color:#d4d4d4;font-size:14px;outline:none;box-sizing:border-box;" />
                        </div>
                        <div style="display:flex;gap:12px;">
                            <div style="flex:1;">
                                <label style="font-size:13px;color:#858585;display:block;margin-bottom:4px;">代码语言</label>
                                <select name="codeLang" style="width:100%;padding:8px 12px;background:#1e1e1e;border:1px solid #3c3c3c;border-radius:4px;color:#d4d4d4;font-size:14px;">
                                    <option value="java">Java</option>
                                    <option value="c">C</option>
                                    <option value="python">Python</option>
                                    <option value="go">Go</option>
                                </select>
                            </div>
                        </div>
                        <div>
                            <label style="font-size:13px;color:#858585;display:block;margin-bottom:4px;">代码</label>
                            <textarea name="code" rows="10" placeholder="粘贴可运行的完整代码（非 Markdown，纯代码文本）"
                                style="width:100%;padding:8px 12px;background:#1e1e1e;border:1px solid #3c3c3c;border-radius:4px;color:#d4d4d4;font-family:'Fira Code',Consolas,monospace;font-size:12px;resize:vertical;outline:none;box-sizing:border-box;"></textarea>
                        </div>
                    </form>
                </div>
            </div>`;
    }

    async function submitPublish(problemId) {
        const form = document.getElementById('ojSolPublishForm');
        if (!form) return;
        // 优先走 processVditor.getValue（现在是自建 textarea 封装，100% 可靠）
        let processMd = '';
        if (processVditor && typeof processVditor.getValue === 'function') {
            try { processMd = processVditor.getValue() || ''; } catch (e) { processMd = ''; }
        }
        if (!processMd) {
            const fallback = form.querySelector && form.querySelector('[name="process"]');
            if (fallback) processMd = fallback.value || '';
        }
        if (!processMd.trim()) {
            alert('请填写解题过程（Markdown）');
            if (processVditor && processVditor.focus) { try { processVditor.focus(); } catch (e) {} }
            return;
        }
        const u = getCurrentUser();
        const body = {
            problemId: problemId,
            problemTitle: OJState.currentProblem?.title || '',
            username: u.username,
            avatar: u.avatar,
            title: form.title.value,
            format: form.format.value,
            idea: form.idea.value,
            process: processMd,          // 原始 Markdown 字符串，后端存入 TEXT 字段
            complexity: form.complexity.value,
            codeLang: form.codeLang.value,
            code: form.code.value
        };
        try {
            const r = await apiPost(`${API_BASE}/solutions`, body);
            if (r.success) {
                destroyProcessVditor();
                alert(r.message || '发布成功');
                await loadList(problemId);
            } else {
                alert(r.message || '发布失败');
            }
        } catch (e) {
            alert('发布失败: ' + e.message);
        }
    }

    // ==================== 点赞题解 ====================

    async function toggleSolutionLike(id, el) {
        const r = await apiPost(`${API_BASE}/solutions/${id}/like`);
        if (r.success) {
            const countEl = el.querySelector('.oj-sol-like-count');
            const currentCount = parseInt(countEl.textContent) || 0;
            countEl.textContent = r.liked ? currentCount + 1 : Math.max(0, currentCount - 1);
        }
    }

    // ==================== 工具 ====================

    function esc(s) { return String(s || '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }

    function fmtTime(t) {
        if (!t) return '';
        const d = new Date(t);
        if (isNaN(d)) return t;
        return `${d.getMonth()+1}月${d.getDate()}日 ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
    }

    function renderPager(elId, page, totalPages, onNav) {
        const el = document.getElementById(elId);
        if (!el) return;
        if (totalPages <= 1) { el.innerHTML = ''; return; }
        let html = '';
        if (page > 1) html += `<button class="oj-pager-btn" data-p="${page-1}" style="padding:4px 10px;background:#2d2d2d;color:#d4d4d4;border:1px solid #3c3c3c;border-radius:3px;cursor:pointer;">上一页</button>`;
        for (let i = Math.max(1, page-2); i <= Math.min(totalPages, page+2); i++) {
            html += `<button class="oj-pager-btn" data-p="${i}" style="padding:4px 10px;${i===page?'background:#667eea;color:#fff;':'background:#2d2d2d;color:#d4d4d4;'}border:1px solid #3c3c3c;border-radius:3px;cursor:pointer;">${i}</button>`;
        }
        if (page < totalPages) html += `<button class="oj-pager-btn" data-p="${page+1}" style="padding:4px 10px;background:#2d2d2d;color:#d4d4d4;border:1px solid #3c3c3c;border-radius:3px;cursor:pointer;">下一页</button>`;
        el.innerHTML = html;
        el.querySelectorAll('.oj-pager-btn').forEach(b => b.addEventListener('click', () => onNav(parseInt(b.dataset.p))));
    }

    // ==================== 导出 ====================

    window.OJSolution = {
        loadList,
        loadDetail
    };
})();
