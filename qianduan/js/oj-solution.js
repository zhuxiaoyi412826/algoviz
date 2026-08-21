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
        if (s.maskIdea) sections.push(renderSection('思路', s.maskIdea));
        if (s.maskProcess) sections.push(renderSection('解题过程', s.maskProcess));
        if (s.complexity) sections.push(renderSection('复杂度', esc(s.complexity)));
        if (s.maskCode) sections.push(renderSection('代码', `<pre style="margin:0;background:#1e1e1e;padding:12px;border-radius:4px;overflow-x:auto;font-size:12px;"><code>${esc(s.maskCode)}</code></pre>`));
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

    function renderSection(title, content) {
        return `<div style="margin-bottom:16px;"><h4 style="font-size:14px;font-weight:600;color:#8585ff;margin:0 0 8px;">${title}</h4><div style="font-size:13px;color:#d4d4d4;line-height:1.7;">${content}</div></div>`;
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

    // ==================== 评论 ====================

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

            // 评论列表
            if (list.length === 0) {
                html += '<div style="color:#6a6a6a;font-size:13px;text-align:center;padding:20px;">暂无评论，快来抢沙发！</div>';
            } else {
                html += list.map(c => renderComment(c, solutionId)).join('');
            }

            area.innerHTML = html;

            // 绑定发布评论
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
                        // 更新详情页评论数
                        const cmtCountEl = document.querySelector('.oj-sol-detail-comment-count');
                        if (cmtCountEl) cmtCountEl.textContent = (parseInt(cmtCountEl.textContent) || 0) + 1;
                    } else {
                        alert(r.message || '发布失败');
                    }
                });
            }

            // 绑定子评论展开和回复
            list.forEach(c => {
                const el = area.querySelector(`[data-comment-id="${c.id}"]`);
                if (!el) return;
                const expandBtn = el.querySelector('.oj-cmt-expand');
                if (expandBtn) {
                    expandBtn.addEventListener('click', () => loadReplies(c.id, solutionId));
                }
                const replyBtn = el.querySelector('.oj-cmt-reply');
                if (replyBtn) {
                    replyBtn.addEventListener('click', () => showReplyForm(c.id, solutionId, problemId, c.username));
                }
                const likeBtn = el.querySelector('.oj-cmt-like');
                if (likeBtn) {
                    likeBtn.addEventListener('click', async () => {
                        const r2 = await apiPost(`${API_BASE}/solutions/comments/${c.id}/like`);
                        if (r2.success) {
                            likeBtn.textContent = `👍 ${r2.liked ? (c.likeCount || 0) + 1 : Math.max(0, (c.likeCount || 0) - 1)}`;
                        }
                    });
                }
            });

        } catch (e) {
            area.innerHTML = `<div style="color:#f56c6c;">评论加载失败: ${e.message}</div>`;
        }
    }

    function renderComment(c, solutionId) {
        const replyCount = c.replyCount || 0;
        const expandBtn = replyCount > 0
            ? `<span class="oj-cmt-expand" style="cursor:pointer;color:#667eea;font-size:12px;">展开 ${replyCount} 条回复 ▼</span>`
            : '';
        return `
            <div data-comment-id="${c.id}" style="padding:10px 0;border-bottom:1px solid #2d2d2d;">
                <div style="display:flex;align-items:flex-start;gap:8px;">
                    <div style="flex:1;">
                        <div style="font-size:13px;font-weight:600;color:#d4d4d4;">${esc(c.username || '匿名')}</div>
                        <div style="font-size:13px;color:#a0a0a0;margin:4px 0;">${esc(c.maskContent || c.content || '')}</div>
                        <div style="display:flex;gap:12px;align-items:center;">
                            <span style="font-size:12px;color:#858585;">${fmtTime(c.createdAt)}</span>
                            <span class="oj-cmt-like" style="cursor:pointer;font-size:12px;color:#858585;">👍 ${c.likeCount || 0}</span>
                            <span class="oj-cmt-reply" style="cursor:pointer;font-size:12px;color:#858585;">回复</span>
                            ${expandBtn}
                        </div>
                    </div>
                </div>
                <div id="ojReplies_${c.id}" style="margin-left:24px;"></div>
                <div id="ojReplyForm_${c.id}"></div>
            </div>`;
    }

    async function loadReplies(rootId, solutionId) {
        const box = document.getElementById(`ojReplies_${rootId}`);
        if (!box) return;
        if (box.innerHTML) { box.innerHTML = ''; return; }
        try {
            const r = await apiGet(`${API_BASE}/solutions/comments/${rootId}/replies?page=1&pageSize=100`);
            const data = r.data || r;
            const list = data.list || [];
            box.innerHTML = list.map(c => `
                <div style="padding:8px 0;border-bottom:1px solid #1a1a1a;">
                    <div style="font-size:13px;font-weight:600;color:#d4d4d4;">${esc(c.username)} <span style="color:#858585;font-weight:normal;">回复 ${esc(c.replyToUsername || '')}</span></div>
                    <div style="font-size:13px;color:#a0a0a0;margin:4px 0;">${esc(c.maskContent || c.content)}</div>
                    <div style="display:flex;gap:12px;">
                        <span style="font-size:12px;color:#858585;">${fmtTime(c.createdAt)}</span>
                    </div>
                </div>`).join('');
        } catch (e) {
            box.innerHTML = '<div style="color:#f56c6c;font-size:12px;">加载回复失败</div>';
        }
    }

    function showReplyForm(commentId, solutionId, problemId, replyToUsername) {
        const form = document.getElementById(`ojReplyForm_${commentId}`);
        if (!form) return;
        if (form.innerHTML) { form.innerHTML = ''; return; }
        form.innerHTML = `
            <div style="display:flex;gap:8px;margin-top:8px;">
                <input type="text" placeholder="回复 ${esc(replyToUsername)}..." style="flex:1;padding:6px 10px;background:#1e1e1e;border:1px solid #3c3c3c;border-radius:4px;color:#d4d4d4;font-size:12px;outline:none;" />
                <button style="padding:6px 12px;background:#667eea;color:#fff;border:none;border-radius:4px;cursor:pointer;font-size:12px;">回复</button>
                <button style="padding:6px 12px;background:#3c3c3c;color:#d4d4d4;border:none;border-radius:4px;cursor:pointer;font-size:12px;">取消</button>
            </div>`;
        const input = form.querySelector('input');
        const submitBtn = form.querySelectorAll('button')[0];
        const cancelBtn = form.querySelectorAll('button')[1];
        submitBtn.addEventListener('click', async () => {
            const text = input.value.trim();
            if (!text) return;
            const u = getCurrentUser();
            const r = await apiPost(`${API_BASE}/solutions/${solutionId}/comments`, {
                content: text,
                problemId: problemId,
                username: u.username,
                avatar: u.avatar,
                parentId: commentId,
                replyToUsername: replyToUsername
            });
            if (r.success) {
                await loadReplies(commentId, solutionId);
                await loadReplies(commentId, solutionId);
                form.innerHTML = '';
                // 更新详情页评论数
                const cmtCountEl = document.querySelector('.oj-sol-detail-comment-count');
                if (cmtCountEl) cmtCountEl.textContent = (parseInt(cmtCountEl.textContent) || 0) + 1;
            } else {
                alert(r.message || '回复失败');
            }
        });
        cancelBtn.addEventListener('click', () => form.innerHTML = '');
    }

    // ==================== 发布/编辑题解 ====================

    async function openPublishForm(problemId) {
        const c = container();
        if (!c) return;
        const u = getCurrentUser();
        c.style.position = 'relative';
        c.style.overflow = 'hidden';
        c.style.padding = '0';
        c.innerHTML = renderPublishForm(u);
        const form = c.querySelector('#ojSolPublishForm');
        // 直接绑定发布按钮的 click，比 form submit 更可靠
        c.querySelector('.oj-sol-submit-btn').addEventListener('click', async () => {
            await submitPublish(problemId);
        });
        c.querySelector('.oj-sol-cancel-btn').addEventListener('click', () => loadList(problemId));
        // 先查是否已有题解
        try {
            const r = await apiGet(`${API_BASE}/solutions/my?problemId=${problemId}`);
            if (r.data) {
                const s = r.data;
                c.querySelector('[name="idea"]').value = s.idea || '';
                c.querySelector('[name="process"]').value = s.process || '';
                c.querySelector('[name="complexity"]').value = s.complexity || '';
                c.querySelector('[name="codeLang"]').value = s.codeLang || 'java';
                c.querySelector('[name="code"]').value = s.code || '';
            }
        } catch (e) {}
    }

    function renderPublishForm(u) {
        const problemTitle = (OJState.currentProblem && OJState.currentProblem.title) || '';
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
                            <textarea name="idea" rows="4" placeholder="描述你的解题思路..."
                                style="width:100%;padding:8px 12px;background:#1e1e1e;border:1px solid #3c3c3c;border-radius:4px;color:#d4d4d4;font-size:13px;resize:vertical;outline:none;box-sizing:border-box;"></textarea>
                        </div>
                        <div>
                            <label style="font-size:13px;color:#858585;display:block;margin-bottom:4px;">解题过程</label>
                            <textarea name="process" rows="5" placeholder="详细描述解题步骤..."
                                style="width:100%;padding:8px 12px;background:#1e1e1e;border:1px solid #3c3c3c;border-radius:4px;color:#d4d4d4;font-size:13px;resize:vertical;outline:none;box-sizing:border-box;"></textarea>
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
                            <textarea name="code" rows="10" placeholder="粘贴你的代码..."
                                style="width:100%;padding:8px 12px;background:#1e1e1e;border:1px solid #3c3c3c;border-radius:4px;color:#d4d4d4;font-family:'Fira Code',monospace;font-size:12px;resize:vertical;outline:none;box-sizing:border-box;"></textarea>
                        </div>
                    </form>
                </div>
            </div>`;
    }

    async function submitPublish(problemId) {
        const form = document.getElementById('ojSolPublishForm');
        if (!form) return;
        const u = getCurrentUser();
        const body = {
            problemId: problemId,
            problemTitle: OJState.currentProblem?.title || '',
            username: u.username,
            avatar: u.avatar,
            title: form.title.value,
            format: form.format.value,
            idea: form.idea.value,
            process: form.process.value,
            complexity: form.complexity.value,
            codeLang: form.codeLang.value,
            code: form.code.value
        };
        try {
            const r = await apiPost(`${API_BASE}/solutions`, body);
            if (r.success) {
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
