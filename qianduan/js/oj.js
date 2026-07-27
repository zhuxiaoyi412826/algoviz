/**
 * Online Judge - 前端逻辑
 */

// 页面刷新防护机制（在页面加载时立即初始化）
let allowRefresh = true;
let refreshProtectionInitialized = false;

// 题目数据（从后端获取）
let OJ_PROBLEMS = [];

// API基础地址
const API_BASE = 'http://localhost:8080/api';

// 页面加载标志，用于检测页面是否重新加载
window.ALGOVIZ_PAGE_LOADED = Date.now();
console.log('页面加载时间戳:', window.ALGOVIZ_PAGE_LOADED);

// HTML转义函数，防止XSS攻击和HTML解析错误
function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// loadProblems调用计数器
let loadProblemsCount = 0;

// 从后端获取题目列表
async function loadProblems() {
    loadProblemsCount++;
    try {
        const response = await fetch(`${API_BASE}/problems`);
        const data = await response.json();
        
        if (data.success && data.problems) {
            console.log('原始数据:', data.problems[0]);
            
            OJ_PROBLEMS = data.problems.map((problem, index) => {
                const problemNoValue = problem.problem_no || problem.problemNo || problem['problem_no'] || problem['problemNo'];
                console.log(`题目${index}: problemNoValue =`, problemNoValue);
                
                let problemId = 0;
                if (problemNoValue) {
                    if (typeof problemNoValue === 'number') {
                        problemId = problemNoValue;
                    } else {
                        const parsed = parseInt(problemNoValue);
                        problemId = isNaN(parsed) ? (index + 1) * 1000 : parsed;
                    }
                } else {
                    problemId = (index + 1) * 1000;
                }
                
                return {
                    id: problemId,
                    title: problem.title || '未命名题目',
                    difficulty: problem.difficulty || 'easy',
                    tags: problem.tags ? problem.tags.split(',').map(tag => tag.trim()) : [],
                    description: problem.description || '',
                    inputFormat: problem.inputFormat || '',
                    outputFormat: problem.outputFormat || '',
                    sampleInput: problem.sampleInput || '',
                    sampleOutput: problem.sampleOutput || '',
                    templateCode: {
                        java: problem.template || '',
                        python: problem.pythonTemplate || '',
                        cpp: problem.cppTemplate || '',
                        javascript: problem.javascriptTemplate || ''
                    },
                    testCases: problem.testCases || []
                };
            });
            
            console.log(`成功加载 ${OJ_PROBLEMS.length} 道题目`);
            console.log('转换后题目数据:', OJ_PROBLEMS);
        } else {
            OJ_PROBLEMS = [];
            console.log('未获取到题目数据');
        }
    } catch (error) {
        console.error('加载题目失败:', error);
        OJ_PROBLEMS = [];
    }
}

// OJ 全局状态
const OJState = {
    currentProblem: null,
    currentLanguage: 'cpp',
    editor: null,
    fullscreenEditor: null,
    isRunning: false,
    isSubmitting: false,
    isFullscreen: false
};

// 初始化 OJ
async function initOJ() {
    console.log('===== initOJ 开始 =====');
    console.log('当前URL:', window.location.href);
    
    try {
        console.log('步骤1: 加载题目');
        await loadProblems();
        console.log('步骤1完成: 题目加载完毕，数量:', OJ_PROBLEMS.length);
        
        console.log('步骤2: 初始化编辑器');
        initEditor();
        console.log('步骤2完成');
        
        console.log('步骤3: 初始化OJ控件');
        initOJControls();
        console.log('步骤3完成');
        
        console.log('步骤4: 初始化可拖动分栏');
        initResizableLayout();
        console.log('步骤4完成');
        
        console.log('步骤5: 初始化垂直拖动');
        initVerticalResize();
        console.log('步骤5完成');
        
        console.log('步骤6: 初始化全屏编辑器');
        initFullscreenEditor();
        console.log('步骤6完成');
        
        // 如果有题目，选择第一个题目显示详情
        if (OJ_PROBLEMS.length > 0) {
            console.log('步骤7: 选择第一个题目');
            selectProblem(OJ_PROBLEMS[0]);
            console.log('步骤7完成');
        } else {
            // 没有题目时显示提示
            document.getElementById('ojProblemContent').innerHTML = `
                <div class="oj-empty-state">
                    <div class="oj-empty-icon">📋</div>
                    <p>暂无题目数据</p>
                </div>
            `;
        }
        
        console.log('===== initOJ 完成 =====');
    } catch (error) {
        console.error('OJ初始化失败:', error);
        console.error('错误堆栈:', error.stack);
        document.getElementById('ojProblemContent').innerHTML = `
            <div class="oj-empty-state">
                <div class="oj-empty-icon">⚠️</div>
                <p>加载题目失败，请检查网络连接</p>
            </div>
        `;
        initEditor();
        initOJControls();
        initResizableLayout();
        initFullscreenEditor();
    }
}

// 初始化代码编辑器
function initEditor() {
    const textarea = document.getElementById('ojCodeEditor');
    
    OJState.editor = CodeMirror.fromTextArea(textarea, {
        mode: 'text/x-c++src',
        theme: 'monokai',
        lineNumbers: true,
        matchBrackets: true,
        autoCloseBrackets: true,
        styleActiveLine: true,
        indentUnit: 4,
        tabSize: 4,
        indentWithTabs: false,
        electricChars: true,
        extraKeys: {
            'Ctrl-Z': 'undo',
            'Cmd-Z': 'undo',
            'Ctrl-Y': 'redo',
            'Cmd-Y': 'redo',
            'Ctrl-/': 'toggleComment',
            'Tab': 'indentMore',
            'Shift-Tab': 'indentLess'
        }
    });

    OJState.editor.setSize(null, '100%');
    OJState.editor.setValue('// 请选择题目或等待题目加载...');

    // 语言切换
    document.getElementById('ojLangSelect').addEventListener('change', (e) => {
        const lang = e.target.value;
        OJState.currentLanguage = lang;
        
        const modeMap = {
            java: 'text/x-java',
            cpp: 'text/x-c++src',
            python: 'text/x-python',
            javascript: 'text/javascript'
        };
        OJState.editor.setOption('mode', modeMap[lang] || 'text/x-c++src');
        
        // 更新模板代码
        if (OJState.currentProblem && OJState.currentProblem.templateCode[lang]) {
            OJState.editor.setValue(OJState.currentProblem.templateCode[lang]);
        }
    });
}

// 初始化全屏编辑器
function initFullscreenEditor() {
    const textarea = document.getElementById('ojFullscreenCodeEditor');
    
    OJState.fullscreenEditor = CodeMirror.fromTextArea(textarea, {
        mode: 'text/x-c++src',
        theme: 'monokai',
        lineNumbers: true,
        matchBrackets: true,
        autoCloseBrackets: true,
        styleActiveLine: true,
        indentUnit: 4,
        tabSize: 4,
        indentWithTabs: false,
        electricChars: true,
        extraKeys: {
            'Ctrl-Z': 'undo',
            'Cmd-Z': 'undo',
            'Ctrl-Y': 'redo',
            'Cmd-Y': 'redo',
            'Ctrl-/': 'toggleComment',
            'Tab': 'indentMore',
            'Shift-Tab': 'indentLess',
            'Esc': exitFullscreen
        }
    });

    OJState.fullscreenEditor.setSize(null, '100%');
}

// 初始化可拖动分栏
function initResizableLayout() {
    const leftPanel = document.getElementById('ojResizableLeft');
    const rightPanel = document.getElementById('ojResizableRight');
    const handle = document.getElementById('ojResizeHandle');
    
    if (!leftPanel || !rightPanel || !handle) {
        console.log('未找到可拖动布局元素');
        return;
    }
    
    let isResizing = false;
    let startX = 0;
    let startWidth = 0;
    
    handle.addEventListener('mousedown', (e) => {
        isResizing = true;
        startX = e.clientX;
        startWidth = leftPanel.offsetWidth;

        document.body.classList.add('is-resizing');
        document.addEventListener('mousemove', onResize);
        document.addEventListener('mouseup', stopResize);
        document.addEventListener('mouseleave', stopResize);

        e.preventDefault();
        e.stopPropagation();
    });

    function onResize(e) {
        if (!isResizing) return;
        
        const deltaX = e.clientX - startX;
        const containerWidth = document.querySelector('.oj-resizable-layout').offsetWidth;
        const newLeftWidth = startWidth + deltaX;
        
        // 移除限制，可以拖动到最左侧和最右侧
        let newLeftPercent = (newLeftWidth / containerWidth) * 100;
        
        // 确保百分比在合理范围内 (0.1% - 99.9%)
        newLeftPercent = Math.max(0.1, Math.min(99.9, newLeftPercent));
        
        leftPanel.style.flex = `0 0 ${newLeftPercent}%`;
        rightPanel.style.flex = `0 0 ${100 - newLeftPercent}%`;
    }
    
    function stopResize() {
        isResizing = false;
        document.body.classList.remove('is-resizing');
        document.removeEventListener('mousemove', onResize);
        document.removeEventListener('mouseup', stopResize);
        document.removeEventListener('mouseleave', stopResize);
    }
}

// 初始化垂直拖动功能
function initVerticalResize() {
    const verticalHandle = document.getElementById('ojVerticalResizeHandle');
    const topPanel = document.getElementById('ojVerticalTop');
    const bottomPanel = document.getElementById('ojVerticalBottom');
    const verticalLayout = document.getElementById('ojVerticalLayout');
    
    let isVerticalResizing = false;
    let startY = 0;
    let startHeight = 0;
    
    verticalHandle.addEventListener('mousedown', (e) => {
        isVerticalResizing = true;
        startY = e.clientY;
        startHeight = topPanel.offsetHeight;

        document.body.classList.add('is-resizing');
        document.addEventListener('mousemove', onVerticalResize);
        document.addEventListener('mouseup', stopVerticalResize);
        document.addEventListener('mouseleave', stopVerticalResize);

        e.preventDefault();
        e.stopPropagation();
    });
    
    function onVerticalResize(e) {
        if (!isVerticalResizing) return;
        
        const deltaY = e.clientY - startY;
        const containerHeight = verticalLayout.offsetHeight;
        const newTopHeight = startHeight + deltaY;
        
        // 移除限制，可以拖动到顶部和底部
        let newTopPercent = (newTopHeight / containerHeight) * 100;
        
        // 确保百分比在合理范围内 (0.1% - 99.9%)
        newTopPercent = Math.max(0.1, Math.min(99.9, newTopPercent));
        
        topPanel.style.flex = `0 0 ${newTopPercent}%`;
        bottomPanel.style.flex = `0 0 ${100 - newTopPercent}%`;
    }
    
    function stopVerticalResize() {
        isVerticalResizing = false;
        document.body.classList.remove('is-resizing');
        document.removeEventListener('mousemove', onVerticalResize);
        document.removeEventListener('mouseup', stopVerticalResize);
        document.removeEventListener('mouseleave', stopVerticalResize);
    }
}

// 初始化 OJ 控制按钮
function initOJControls() {
    // 返回按钮
    document.getElementById('ojBackBtn').addEventListener('click', () => {
        window.location.href = '../index.html';
    });

    // 题库按钮
    document.getElementById('ojProblemsBtn').addEventListener('click', () => {
        showProblemList();
    });

    // 上一题按钮
    document.getElementById('ojPrevBtn').addEventListener('click', () => {
        switchToPrevProblem();
    });

    // 下一题按钮
    document.getElementById('ojNextBtn').addEventListener('click', () => {
        switchToNextProblem();
    });

    // 格式化按钮
    document.getElementById('ojFormatBtn').addEventListener('click', () => {
        formatCode();
    });

    // 清空按钮
    document.getElementById('ojClearBtn').addEventListener('click', () => {
        OJState.editor.setValue('');
    });

    // 复制按钮
    document.getElementById('ojCopyBtn').addEventListener('click', () => {
        const code = OJState.editor.getValue();
        navigator.clipboard.writeText(code).then(() => {
            showToast('代码已复制到剪贴板');
        }).catch(() => {
            showToast('复制失败');
        });
    });

    // 全屏按钮
    document.getElementById('ojFullscreenBtn').addEventListener('click', () => {
        enterFullscreen();
    });

    // 运行按钮
    document.getElementById('ojRunBtn').addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        runCode();
    });

    // 提交按钮
    document.getElementById('ojSubmitBtn').addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        submitCode();
    });

    // 结果面板切换
    document.querySelectorAll('.oj-result-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.oj-result-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
        });
    });

    // 题目标签切换
    document.querySelectorAll('.oj-problem-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.oj-problem-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
        });
    });

    // 全屏相关按钮
    document.getElementById('ojExitFullscreenBtn').addEventListener('click', exitFullscreen);
    document.getElementById('ojFullscreenFormatBtn').addEventListener('click', () => {
        formatCode(true);
    });
    document.getElementById('ojFullscreenClearBtn').addEventListener('click', () => {
        OJState.fullscreenEditor.setValue('');
    });
    document.getElementById('ojFullscreenCopyBtn').addEventListener('click', () => {
        const code = OJState.fullscreenEditor.getValue();
        navigator.clipboard.writeText(code).then(() => {
            showToast('代码已复制到剪贴板');
        }).catch(() => {
            showToast('复制失败');
        });
    });
    document.getElementById('ojFullscreenSubmitBtn').addEventListener('click', () => {
        OJState.editor.setValue(OJState.fullscreenEditor.getValue());
        exitFullscreen();
        submitCode();
    });
}

// 进入全屏模式
function enterFullscreen() {
    const overlay = document.getElementById('ojFullscreenOverlay');
    const currentCode = OJState.editor.getValue();
    
    OJState.fullscreenEditor.setValue(currentCode);
    OJState.fullscreenEditor.focus();
    
    overlay.classList.add('active');
    OJState.isFullscreen = true;
    
    document.body.style.overflow = 'hidden';
}

// 退出全屏模式
function exitFullscreen() {
    const overlay = document.getElementById('ojFullscreenOverlay');
    const fullscreenCode = OJState.fullscreenEditor.getValue();
    
    OJState.editor.setValue(fullscreenCode);
    
    overlay.classList.remove('active');
    OJState.isFullscreen = false;
    
    document.body.style.overflow = '';
}

// 选择题目并显示详情
function selectProblem(problem) {
    if (!problem) return;
    
    OJState.currentProblem = problem;
    
    // 更新题目标题
    document.getElementById('ojProblemTitle').textContent = `${problem.id}. ${problem.title}`;
    
    // 更新难度标签
    const difficultyMap = {
        'easy': { label: '简单', class: 'oj-difficulty-easy' },
        'medium': { label: '中等', class: 'oj-difficulty-medium' },
        'hard': { label: '困难', class: 'oj-difficulty-hard' }
    };
    const difficulty = difficultyMap[problem.difficulty] || difficultyMap['easy'];
    const difficultySpan = document.getElementById('ojProblemDifficulty');
    difficultySpan.textContent = difficulty.label;
    difficultySpan.className = `oj-problem-difficulty ${difficulty.class}`;
    
    // 更新标签
    const tagsDiv = document.getElementById('ojProblemTags');
    tagsDiv.innerHTML = problem.tags.map(tag => {
        const tagClass = difficultyMap[tag.toLowerCase()] ? `oj-tag oj-tag-${tag.toLowerCase()}` : 'oj-tag';
        return `<span class="${tagClass}">${tag}</span>`;
    }).join('');
    
    // 更新题目内容
    const contentDiv = document.getElementById('ojProblemContent');
    contentDiv.innerHTML = `
        <h2 id="ojProblemTitle" class="oj-problem-title">${problem.id}. ${problem.title}</h2>
        <div class="oj-problem-tags" id="ojProblemTags">
            ${problem.tags.map(tag => {
                const tagClass = difficultyMap[tag.toLowerCase()] ? `oj-tag oj-tag-${tag.toLowerCase()}` : 'oj-tag';
                return `<span class="${tagClass}">${tag}</span>`;
            }).join('')}
        </div>
        <div class="oj-problem-body">
            ${problem.description}
            ${problem.inputFormat ? `<div class="oj-sample-section"><h4>输入格式：</h4><pre>${problem.inputFormat}</pre></div>` : ''}
            ${problem.outputFormat ? `<div class="oj-sample-section"><h4>输出格式：</h4><pre>${problem.outputFormat}</pre></div>` : ''}
            ${problem.sampleInput ? `
                <div class="oj-sample-section">
                    <h4>示例：</h4>
                    <div class="oj-sample-block">
                        <div class="oj-sample-label">输入：</div>
                        <div>${problem.sampleInput}</div>
                        <div class="oj-sample-label">输出：</div>
                        <div>${problem.sampleOutput || ''}</div>
                    </div>
                </div>
            ` : ''}
        </div>
    `;
    
    // 更新编辑器模板代码
    if (problem.templateCode && problem.templateCode[OJState.currentLanguage]) {
        OJState.editor.setValue(problem.templateCode[OJState.currentLanguage]);
    }
    
    // 更新导航按钮状态
    onProblemSelected(problem.id);
    
    console.log('题目详情已更新:', problem.title);
}

// 格式化代码
function formatCode(isFullscreen = false) {
    const editor = isFullscreen ? OJState.fullscreenEditor : OJState.editor;
    let code = editor.getValue();
    
    if (OJState.currentLanguage === 'java' || OJState.currentLanguage === 'cpp') {
        code = code.replace(/\s+/g, ' ');
        code = code.replace(/\s*{\s*/g, ' {\n    ');
        code = code.replace(/\s*}\s*/g, '\n}\n');
        code = code.replace(/;\s*/g, ';\n');
    }
    
    editor.setValue(code);
    showToast('代码已格式化');
}

// 运行代码
function runCode() {
    const code = OJState.editor.getValue();
    const language = OJState.currentLanguage;
    const input = document.getElementById('ojCustomInput').value;
    
    console.log('语言:', language);
    console.log('代码长度:', code.length);
    console.log('输入内容:', input ? '有输入' : '无输入');
    
    OJState.isRunning = true;
    const runBtn = document.getElementById('ojRunBtn');
    runBtn.disabled = true;
    runBtn.innerHTML = '<span class="oj-spinner" style="width:16px;height:16px;"></span> 运行中...';

    document.getElementById('ojResultBody').innerHTML = `
        <div class="oj-loading">
            <div class="oj-spinner"></div>
            <span>正在编译运行...</span>
        </div>
    `;

    // 模拟运行结果
    simulateRunCode(code, language, input)
        .then(result => {
            if (result.success) {
                displayRunResult({
                    status: result.status,
                    output: result.output,
                    message: result.message,
                    time: result.time,
                    memory: result.memory
                });
            } else {
                displayRunResult({ status: 'error', message: result.message });
            }
        })
        .catch(error => {
            console.error('运行代码失败:', error);
            displayRunResult({ status: 'error', message: '网络错误，无法连接后端服务' });
        })
        .finally(() => {
            OJState.isRunning = false;
            runBtn.disabled = false;
            runBtn.innerHTML = '<span>▶</span> 运行';
        });
}

// 模拟运行代码
function simulateRunCode(code, language, input) {
    return new Promise((resolve) => {
        setTimeout(() => {
            if (!code.trim()) {
                resolve({ success: true, status: 'error', message: '代码不能为空' });
                return;
            }

            if (language === 'java' && !code.includes('public static void main')) {
                resolve({ success: true, status: 'ce', message: '编译错误: 找不到 main 方法\n请确保代码包含 public static void main(String[] args)' });
                return;
            }

            resolve({
                success: true,
                status: 'success',
                output: input || '2.00000',
                time: Math.random() * 100 + 10,
                memory: Math.floor(Math.random() * 10000 + 5000)
            });
        }, 1000 + Math.random() * 1000);
    });
}

// 提交代码
function submitCode() {
    if (OJState.isSubmitting) {
        return;
    }
    
    const code = OJState.editor.getValue();
    const language = OJState.currentLanguage;
    
    OJState.isSubmitting = true;
    const submitBtn = document.getElementById('ojSubmitBtn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="oj-spinner" style="width:16px;height:16px;"></span> 判题中...';

    document.querySelectorAll('.oj-result-tab').forEach(t => t.classList.remove('active'));
    document.querySelector('.oj-result-tab[data-tab="submit"]').classList.add('active');

    document.getElementById('ojResultBody').innerHTML = `
        <div class="oj-loading">
            <div class="oj-spinner"></div>
            <span>正在判题...</span>
        </div>
    `;

    simulateJudge(code, language)
        .then(result => {
            displayJudgeResult(result);
        })
        .catch(error => {
            console.error('提交代码失败:', error);
            displayJudgeResult({ status: 'error', message: '网络错误，无法连接后端服务' });
        })
        .finally(() => {
            OJState.isSubmitting = false;
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<span>📤</span> 提交';
        });
}

// 模拟判题
function simulateJudge(code, language) {
    return new Promise((resolve) => {
        setTimeout(() => {
            if (!code.trim()) {
                resolve({ status: 'ce', message: '代码不能为空', results: [], passedCount: 0, totalCount: 1 });
                return;
            }

            if (language === 'java' && !code.includes('public static void main')) {
                resolve({ 
                    status: 'ce', 
                    message: '编译错误: 找不到 main 方法\n请确保代码包含 public static void main(String[] args)',
                    results: [],
                    passedCount: 0,
                    totalCount: 1
                });
                return;
            }

            const random = Math.random();
            const passed = random > 0.3;
            
            const results = [{
                index: 1,
                status: passed ? 'passed' : 'failed',
                time: Math.floor(Math.random() * 100 + 10),
                memory: Math.floor(Math.random() * 10000 + 5000)
            }];

            resolve({
                status: passed ? 'accepted' : 'wa',
                results: results,
                passedCount: passed ? 1 : 0,
                totalCount: 1,
                time: Math.floor(Math.random() * 200 + 50),
                memory: Math.floor(Math.random() * 30000 + 10000)
            });
        }, 2000 + Math.random() * 2000);
    });
}

// 显示运行结果
function displayRunResult(result) {
    const body = document.getElementById('ojResultBody');
    
    if (result.status === 'success') {
        body.innerHTML = `
            <div class="oj-result-status oj-status-accepted">
                <span class="oj-status-icon">✓</span>
                <div class="oj-status-info">
                    <div class="oj-status-label">运行成功</div>
                    <div class="oj-status-detail">耗时: ${result.time.toFixed(2)}ms | 内存: ${result.memory}KB</div>
                </div>
            </div>
            <div class="oj-sample-block" style="margin-top: 1rem;">
                <div class="oj-sample-label">运行输出</div>
${result.output}
            </div>
        `;
    } else if (result.status === 'ce') {
        const escapedMessage = escapeHtml(result.message || '');
        body.innerHTML = `
            <div class="oj-result-status oj-status-error">
                <span class="oj-status-icon">⚠</span>
                <div class="oj-status-info">
                    <div class="oj-status-label">编译错误</div>
                </div>
            </div>
            <div class="oj-error-output oj-compile-error" style="margin-top: 1rem;">
${escapedMessage}
            </div>
        `;
    } else if (result.status === 're') {
        body.innerHTML = `
            <div class="oj-result-status oj-status-error">
                <span class="oj-status-icon">⚠</span>
                <div class="oj-status-info">
                    <div class="oj-status-label">运行错误</div>
                </div>
            </div>
            <div class="oj-error-output" style="margin-top: 1rem;">
${result.message || 'Runtime Error: ArrayIndexOutOfBoundsException'}
            </div>
        `;
    } else {
        body.innerHTML = `
            <div class="oj-result-status oj-status-error">
                <span class="oj-status-icon">✗</span>
                <div class="oj-status-info">
                    <div class="oj-status-label">发生错误</div>
                    <div class="oj-status-detail">${result.message}</div>
                </div>
            </div>
        `;
    }
}

// 显示判题结果
function displayJudgeResult(result) {
    const body = document.getElementById('ojResultBody');
    
    const statusConfig = {
        'accepted': { icon: '✓', label: '通过', class: 'oj-status-accepted' },
        'wa': { icon: '✗', label: '答案错误', class: 'oj-status-wrong' },
        'ce': { icon: '⚠', label: '编译失败', class: 'oj-status-error' },
        're': { icon: '⚠', label: '运行崩溃', class: 'oj-status-error' },
        'tle': { icon: '⏱', label: '超时', class: 'oj-status-timeout' },
        'error': { icon: '✗', label: '错误', class: 'oj-status-error' }
    };

    const config = statusConfig[result.status] || statusConfig['error'];

    let html = `
        <div class="oj-result-status ${config.class}">
            <span class="oj-status-icon">${config.icon}</span>
            <div class="oj-status-info">
                <div class="oj-status-label">${config.label}</div>
                <div class="oj-status-detail">
                    ${result.passedCount !== undefined ? 
                        `通过 ${result.passedCount}/${result.totalCount} 个测试用例 | ` : ''}
                    耗时: ${result.time}ms | 内存: ${result.memory}KB
                </div>
            </div>
        </div>
    `;

    if (result.results && result.results.length > 0) {
        html += '<div class="oj-test-results">';
        
        const progress = result.totalCount > 0 ? (result.passedCount / result.totalCount) * 100 : 0;
        html += `
            <div class="oj-progress">
                <div class="oj-progress-bar">
                    <div class="oj-progress-fill" style="width: ${progress}%"></div>
                </div>
                <span class="oj-progress-text">${result.passedCount}/${result.totalCount}</span>
            </div>
        `;

        result.results.forEach(tc => {
            const isPassed = tc.status === 'passed';
            html += `
                <div class="oj-test-item">
                    <span class="oj-status-icon ${isPassed ? 'oj-test-passed' : 'oj-test-failed'}">
                        ${isPassed ? '✓' : '✗'}
                    </span>
                    <span>测试用例 #${tc.index}</span>
                    <span style="color: var(--text-muted); margin-left: auto;">
                        ${tc.time}ms | ${tc.memory}KB
                    </span>
                </div>
            `;
        });

        html += '</div>';
    }

    if (result.message) {
        html += `
            <div class="oj-error-output ${result.status === 'ce' ? 'oj-compile-error' : ''}" style="margin-top: 1rem;">
${result.message}
            </div>
        `;
    }

    body.innerHTML = html;
}

// 显示提示
function showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'oj-toast';
    toast.style.cssText = `
        position: fixed;
        bottom: 20px;
        left: 50%;
        transform: translateX(-50%);
        background: var(--bg-card);
        border: 1px solid var(--primary);
        color: var(--text-primary);
        padding: 12px 24px;
        border-radius: 8px;
        z-index: 10000;
        animation: fadeIn 0.3s ease;
    `;
    toast.textContent = message;
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'fadeOut 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 2000);
}

// 全局事件处理
window.addEventListener('unhandledrejection', function(event) {
    console.error('未处理的Promise拒绝:', event.reason);
    event.preventDefault();
});

window.addEventListener('error', function(event) {
    console.error('全局错误:', event.error);
});

// 显示题目列表
function showProblemList() {
    if (OJ_PROBLEMS.length === 0) {
        showToast('暂无题目数据');
        return;
    }
    
    let html = `
        <div style="position: fixed; top: 50px; left: 20px; right: 20px; max-height: 60vh; background: #1a1a2e; border: 1px solid rgba(102, 126, 234, 0.2); border-radius: 8px; padding: 16px; z-index: 1000; overflow-y: auto;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                <h3 style="color: #f0f1f5; margin: 0;">题目列表</h3>
                <button onclick="this.parentElement.parentElement.remove()" style="padding: 4px 8px; background: rgba(239, 68, 68, 0.2); border: none; border-radius: 4px; color: #ef4444; cursor: pointer;">关闭</button>
            </div>
            <div style="display: grid; gap: 6px;">
    `;
    
    OJ_PROBLEMS.forEach((problem, index) => {
        const isCurrent = OJState.currentProblemId === problem.id;
        html += `
            <div onclick="selectProblem(${problem.id}); this.parentElement.parentElement.parentElement.remove()" 
                 style="display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; background: ${isCurrent ? 'rgba(102, 126, 234, 0.2)' : 'rgba(0, 0, 0, 0.2)'}; border-radius: 6px; cursor: pointer; transition: background 0.2s;">
                <div>
                    <span style="color: #667eea; margin-right: 8px;">${problem.problemNo || index + 1}</span>
                    <span style="color: ${isCurrent ? '#a5b4fc' : '#f0f1f5'};">${problem.title}</span>
                </div>
                <span style="padding: 2px 8px; border-radius: 10px; font-size: 11px; ${getDifficultyStyle(problem.difficulty)}">${problem.difficulty}</span>
            </div>
        `;
    });
    
    html += `</div></div>`;
    
    document.body.insertAdjacentHTML('beforeend', html);
}

// 获取难度样式
function getDifficultyStyle(difficulty) {
    const styles = {
        'easy': 'background: rgba(72, 199, 142, 0.15); color: #48c78a;',
        'medium': 'background: rgba(251, 191, 36, 0.15); color: #fbbf24;',
        'hard': 'background: rgba(239, 68, 68, 0.15); color: #ef4444;'
    };
    return styles[difficulty] || styles['medium'];
}

// 切换到上一题
function switchToPrevProblem() {
    if (OJ_PROBLEMS.length === 0) return;
    
    const currentIndex = OJ_PROBLEMS.findIndex(p => p.id === OJState.currentProblemId);
    if (currentIndex > 0) {
        selectProblem(OJ_PROBLEMS[currentIndex - 1].id);
        updateNavButtons(currentIndex - 1);
    }
}

// 切换到下一题
function switchToNextProblem() {
    if (OJ_PROBLEMS.length === 0) return;
    
    const currentIndex = OJ_PROBLEMS.findIndex(p => p.id === OJState.currentProblemId);
    if (currentIndex < OJ_PROBLEMS.length - 1) {
        selectProblem(OJ_PROBLEMS[currentIndex + 1].id);
        updateNavButtons(currentIndex + 1);
    }
}

// 更新导航按钮状态
function updateNavButtons(currentIndex) {
    const prevBtn = document.getElementById('ojPrevBtn');
    const nextBtn = document.getElementById('ojNextBtn');
    
    prevBtn.disabled = currentIndex === 0;
    nextBtn.disabled = currentIndex >= OJ_PROBLEMS.length - 1;
}

// 更新导航按钮状态（在选择题目后调用）
function onProblemSelected(problemId) {
    const currentIndex = OJ_PROBLEMS.findIndex(p => p.id === problemId);
    updateNavButtons(currentIndex);
}

document.addEventListener('submit', function(e) {
    e.preventDefault();
    e.stopPropagation();
    e.stopImmediatePropagation();
    return false;
}, true);

document.addEventListener('DOMContentLoaded', initOJ);