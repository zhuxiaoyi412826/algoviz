/**
 * AI 助手交互逻辑 (支持多会话)
 */

document.addEventListener('DOMContentLoaded', () => {
    const chatInput = document.getElementById('chatInput');
    const sendBtn = document.getElementById('sendBtn');
    const stopBtn = document.getElementById('stopBtn');
    const chatHistory = document.getElementById('chatHistory');
    const currentChatTitle = document.getElementById('currentChatTitle');
    const newChatBtn = document.getElementById('newChatBtn');
    const sessionListEl = document.getElementById('sessionList');

    // 状态管理
    let isGenerating = false;
    let abortController = null;
    
    // 多会话管理
    let sessions = []; // [{ id: string, title: string, messages: [] }]
    let currentSessionId = null;

    // 初始化 Marked
    marked.setOptions({
        highlight: function(code, lang) {
            const language = hljs.getLanguage(lang) ? lang : 'plaintext';
            return hljs.highlight(code, { language }).value;
        },
        langPrefix: 'hljs language-'
    });

    // 加载所有会话
    loadSessions();

    // 自动调整输入框高度
    chatInput.addEventListener('input', function() {
        this.style.height = '44px';
        this.style.height = (this.scrollHeight) + 'px';
        
        if (this.value.trim() === '') {
            sendBtn.disabled = true;
        } else {
            sendBtn.disabled = false;
        }
    });

    // 回车发送
    chatInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            if (!isGenerating && this.value.trim() !== '') {
                sendMessage();
            }
        }
    });

    // 发送按钮点击
    sendBtn.addEventListener('click', () => {
        if (!isGenerating && chatInput.value.trim() !== '') {
            sendMessage();
        }
    });

    // 停止生成
    stopBtn.addEventListener('click', () => {
        if (isGenerating && abortController) {
            abortController.abort();
            finishGeneration();
        }
    });

    // 新对话
    newChatBtn.addEventListener('click', () => {
        createNewSession();
    });

    function createNewSession() {
        const sessionId = Date.now().toString();
        const newSession = {
            id: sessionId,
            title: '新对话',
            messages: []
        };
        sessions.unshift(newSession);
        switchSession(sessionId);
        saveSessions();
        renderSessionList();
    }

    function switchSession(sessionId) {
        currentSessionId = sessionId;
        const session = sessions.find(s => s.id === sessionId);
        
        // 渲染聊天历史
        chatHistory.innerHTML = '';
        if (session && session.messages.length > 0) {
            currentChatTitle.textContent = session.title;
            session.messages.forEach(msg => {
                if (msg.role === 'user') {
                    addUserMessageHistory(msg.content);
                } else if (msg.role === 'assistant') {
                    addAIMessage(msg.content, false);
                }
            });
        } else {
            currentChatTitle.textContent = '新对话';
            addWelcomeMessage();
        }
        
        renderSessionList();
        scrollToBottom();
    }

    function deleteSession(sessionId, event) {
        event.stopPropagation();
        sessions = sessions.filter(s => s.id !== sessionId);
        saveSessions();
        
        if (sessions.length === 0) {
            createNewSession();
        } else if (currentSessionId === sessionId) {
            switchSession(sessions[0].id);
        } else {
            renderSessionList();
        }
    }

    function renderSessionList() {
        sessionListEl.innerHTML = '';
        sessions.forEach(session => {
            const item = document.createElement('div');
            item.className = `history-item ${session.id === currentSessionId ? 'active' : ''}`;
            item.innerHTML = `
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                <div class="history-item-text">${escapeHTML(session.title)}</div>
                <button class="history-item-delete" title="删除对话">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                </button>
            `;
            item.addEventListener('click', () => switchSession(session.id));
            item.querySelector('.history-item-delete').addEventListener('click', (e) => deleteSession(session.id, e));
            sessionListEl.appendChild(item);
        });
    }

    function saveSessions() {
        localStorage.setItem('algoviz-chat-sessions', JSON.stringify(sessions));
    }

    function loadSessions() {
        const historyStr = localStorage.getItem('algoviz-chat-sessions');
        if (historyStr) {
            try {
                sessions = JSON.parse(historyStr);
                if (sessions.length > 0) {
                    switchSession(sessions[0].id);
                    return;
                }
            } catch (e) {
                console.error("Failed to load chat history", e);
            }
        }
        createNewSession();
    }

    function getCurrentSession() {
        return sessions.find(s => s.id === currentSessionId);
    }

    function addWelcomeMessage() {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'message-ai chat-message fade-in';
        msgDiv.innerHTML = `
            <div class="message-avatar">✨</div>
            <div class="message-content">
                <p>你好！我是 AlgoViz 的 AI 助手。我可以解答关于数据结构、算法、代码实现等方面的问题。</p>
                <p>请问有什么我可以帮你的吗？</p>
            </div>
        `;
        chatHistory.appendChild(msgDiv);
    }

    function sendMessage() {
        const text = chatInput.value.trim();
        if (!text) return;

        const session = getCurrentSession();
        
        // 自动更新标题 (如果是第一条消息)
        if (session.messages.length === 0) {
            session.title = text.length > 15 ? text.substring(0, 15) + '...' : text;
            currentChatTitle.textContent = session.title;
            renderSessionList();
        }

        // 添加用户消息
        addUserMessage(text);
        
        // 保存到历史
        session.messages.push({ role: 'user', content: text });
        saveSessions();

        // 清空输入框
        chatInput.value = '';
        chatInput.style.height = '44px';
        sendBtn.disabled = true;

        // 触发 AI 回复
        generateAIResponse(text, session);
    }

    function addUserMessage(text) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'message-user chat-message fade-in';
        msgDiv.innerHTML = `
            <div class="message-avatar">
                ${localStorage.getItem('userAvatar') || '👤'}
            </div>
            <div class="message-content">
                <button class="copy-btn" title="复制内容" onclick="copyText(this)">复制</button>
                <div class="markdown-body">${escapeHTML(text)}</div>
            </div>
        `;
        chatHistory.appendChild(msgDiv);
        scrollToBottom();
    }

    function addAIMessage(text, isHTML = false) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'message-ai chat-message fade-in';
        
        const contentHtml = isHTML ? text : marked.parse(text);
        
        msgDiv.innerHTML = `
            <div class="message-avatar">✨</div>
            <div class="message-content">
                <button class="copy-btn" title="复制内容" onclick="copyText(this)">复制</button>
                <div class="markdown-body">${contentHtml}</div>
            </div>
        `;
        chatHistory.appendChild(msgDiv);
        addCodeCopyButtons(msgDiv);
        scrollToBottom();
        return msgDiv;
    }

    function showLoading() {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'message-ai chat-message fade-in loading-msg';
        msgDiv.innerHTML = `
            <div class="message-avatar">✨</div>
            <div class="message-content">
                <div class="typing-indicator">
                    <span>AI 思考中</span>
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                </div>
            </div>
        `;
        chatHistory.appendChild(msgDiv);
        scrollToBottom();
        return msgDiv;
    }

    async function generateAIResponse(userText, session) {
        isGenerating = true;
        sendBtn.style.display = 'none';
        stopBtn.classList.add('visible');
        
        abortController = new AbortController();
        const signal = abortController.signal;

        const loadingMsg = showLoading();

        try {
            // 改为调用后端API
            const API_URL = 'http://localhost:8080/api/ai/chat';
            console.log('[AI Debug] 请求URL:', API_URL);
            
            // 准备对话历史上下文
            const messages = [
                { role: 'system', content: '你是 AlgoViz 平台的 AI 助手，专门解答数据结构、算法、代码实现相关的问题。请使用 Markdown 格式回答，并在代码块中指明编程语言。如果可能，将用户的注意力引导到网站的"算法演示"或"数据结构"页面上进行学习。' }
            ];
            
            // 添加最近的历史记录
            const recentHistory = session.messages.slice(-10);
            messages.push(...recentHistory);

            console.log('[AI Debug] 发送请求...', { messages });
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    messages: messages
                }),
                signal: signal
            });

            console.log('[AI Debug] 响应状态:', response.status);
            
            const result = await response.json();
            console.log('[AI Debug] 响应数据:', result);
            
            if (!result.success) {
                throw new Error(result.error || 'API 请求失败');
            }

            loadingMsg.remove();
            
            const msgDiv = document.createElement('div');
            msgDiv.className = 'message-ai chat-message fade-in';
            msgDiv.innerHTML = `
                <div class="message-avatar">✨</div>
                <div class="message-content">
                    <button class="copy-btn" title="复制内容" onclick="copyText(this)">复制</button>
                    <div class="markdown-body"></div>
                </div>
            `;
            chatHistory.appendChild(msgDiv);
            const contentDiv = msgDiv.querySelector('.markdown-body');
            
            let aiResponse = '';
            try {
                const deepseekData = JSON.parse(result.data);
                console.log('[AI Debug] DeepSeek数据:', deepseekData);
                if (deepseekData.choices && deepseekData.choices[0] && deepseekData.choices[0].message) {
                    aiResponse = deepseekData.choices[0].message.content;
                }
            } catch (e) {
                console.error('[AI Debug] 解析DeepSeek响应错误:', e);
            }
            
            if (!aiResponse) {
                aiResponse = '抱歉，未能获取到响应。';
            }
            
            console.log('[AI Debug] AI响应:', aiResponse);
            
            if (typeof marked !== 'undefined') {
                contentDiv.innerHTML = marked.parse(aiResponse);
            } else {
                contentDiv.innerHTML = aiResponse.replace(/\n/g, '<br>');
            }
            
            scrollToBottom();
            addCodeCopyButtons(msgDiv);
            session.messages.push({ role: 'assistant', content: aiResponse });
            saveSessions();

        } catch (error) {
            console.error('[AI Debug] 错误:', error);
            if (error.name === 'AbortError' || error.message === 'aborted') {
                console.log('Generation aborted by user');
            } else {
                console.error('[AI Debug] API Error:', error);
                loadingMsg.remove();
                addAIMessage(`抱歉，请求出错：${error.message}。请检查：
1. 后端是否已在8080端口启动
2. 网络连接是否正常
3. API Key是否正确配置`);
            }
        } finally {
            finishGeneration();
        }
    }

    function finishGeneration() {
        isGenerating = false;
        sendBtn.style.display = 'flex';
        stopBtn.classList.remove('visible');
        abortController = null;
        chatInput.focus();
    }

    function scrollToBottom() {
        chatHistory.scrollTop = chatHistory.scrollHeight;
    }

    function addUserMessageHistory(text) {
        const msgDiv = document.createElement('div');
        msgDiv.className = 'message-user chat-message';
        msgDiv.innerHTML = `
            <div class="message-avatar">
                ${localStorage.getItem('userAvatar') || '👤'}
            </div>
            <div class="message-content">
                <button class="copy-btn" title="复制内容" onclick="copyText(this)">复制</button>
                <div class="markdown-body">${escapeHTML(text)}</div>
            </div>
        `;
        chatHistory.appendChild(msgDiv);
    }

    function addCodeCopyButtons(msgDiv) {
        const preElements = msgDiv.querySelectorAll('pre');
        preElements.forEach(pre => {
            if (pre.querySelector('.code-header')) return; 

            const langClass = pre.querySelector('code').className;
            const langMatch = langClass.match(/language-(\w+)/);
            const lang = langMatch ? langMatch[1] : 'code';

            const header = document.createElement('div');
            header.className = 'code-header';
            header.innerHTML = `
                <span>${lang}</span>
                <button class="code-copy-btn" onclick="copyCode(this)">复制</button>
            `;
            pre.insertBefore(header, pre.firstChild);
        });
    }

    function escapeHTML(str) {
        return str.replace(/[&<>'"]/g, 
            tag => ({
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                "'": '&#39;',
                '"': '&quot;'
            }[tag] || tag)
        );
    }
});

// 快捷填充 Prompt 函数
window.fillPrompt = function(el) {
    const input = document.getElementById('chatInput');
    const sendBtn = document.getElementById('sendBtn');
    
    // 移除 emoji 图标只取文字
    let text = el.innerText.trim();
    if (/^[\uD800-\uDBFF][\uDC00-\uDFFF]/.test(text) || text.length > 0 && text.charCodeAt(0) > 255) {
        text = text.substring(text.indexOf(' ') + 1).trim();
    }
    
    input.value = text;
    input.focus();
    
    // 触发 input 事件以调整高度和按钮状态
    input.dispatchEvent(new Event('input'));
};

// 全局复制函数
window.copyText = function(btn) {
    const content = btn.nextElementSibling.innerText;
    navigator.clipboard.writeText(content).then(() => {
        const originalText = btn.innerText;
        btn.innerText = '已复制';
        setTimeout(() => {
            btn.innerText = originalText;
        }, 2000);
    });
};

window.copyCode = function(btn) {
    const codeElement = btn.parentElement.nextElementSibling;
    const content = codeElement.innerText;
    navigator.clipboard.writeText(content).then(() => {
        const originalText = btn.innerText;
        btn.innerText = '成功';
        setTimeout(() => {
            btn.innerText = originalText;
        }, 2000);
    });
};
