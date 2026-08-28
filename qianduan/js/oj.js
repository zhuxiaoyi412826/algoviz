/**
 * Online Judge - 前端逻辑
 */

// API基础地址
const API_BASE = 'http://localhost:80/api';

// 从localStorage获取当前登录用户信息
function getCurrentUser() {
    try {
        const raw = localStorage.getItem('userInfo');
        if (raw) {
            const u = JSON.parse(raw);
            return {
                id: u.id || u.userId || 1,
                username: u.username || u.nickname || '前端用户'
            };
        }
    } catch (e) {}
    return { id: 1, username: '前端用户' };
}

// 语言配置映射
const LANGUAGE_CONFIG = {
    java: { label: 'Java', mode: 'text/x-java', ext: 'java' },
    c: { label: 'C', mode: 'text/x-csrc', ext: 'c' },
    python: { label: 'Python', mode: 'text/x-python', ext: 'py' },
    go: { label: 'Go', mode: 'text/x-go', ext: 'go' }
};

// 空模板（编辑器默认显示）
const EMPTY_TEMPLATES = {
    java: 'public class Main {\n    public static void main(String[] args) {\n        // 在此处编写你的代码\n        int a = 12;\n        int b = 28;\n        int sum = a + b;\n        System.out.println(sum);\n    }\n}',
    c: '#include <stdio.h>\n\nint main() {\n    // 在此处编写你的代码\n    return 0;\n}',
    python: 'def main():\n    # 在此处编写你的代码\n    pass\n\nif __name__ == "__main__":\n    main()',
    go: 'package main\n\nimport "fmt"\n\nfunc main() {\n    // 在此处编写你的代码\n    fmt.Println("Hello")\n}'
};

// 离线题目数据
const OFFLINE_PROBLEMS = [
    {
        id: 1001, title: '两数之和', difficulty: 'easy', tags: ['数组', '哈希表'],
        description: '<p>给定一个整数数组 <code>nums</code> 和一个整数目标值 <code>target</code>，请你在该数组中找出和为目标值 <code>target</code> 的那两个整数，并返回它们的数组下标。</p><p>你可以假设每种输入只会对应一个答案，并且你不能使用两次相同的元素。</p>',
        inputFormat: 'nums = [2,7,11,15], target = 9',
        outputFormat: '[0,1]',
        sampleInput: 'nums = [2,7,11,15], target = 9',
        sampleOutput: '[0,1]',
        templateCode: {
            java: 'class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // 在此处编写你的代码\n        \n    }\n}',
            c: '#include <stdlib.h>\nint* twoSum(int* nums, int numsSize, int target, int* returnSize) {\n    // 在此处编写你的代码\n    \n}',
            python: 'class Solution:\n    def twoSum(self, nums: list[int], target: int) -> list[int]:\n        # 在此处编写你的代码\n        pass',
            go: 'func twoSum(nums []int, target int) []int {\n    // 在此处编写你的代码\n    \n}'
        }
    },
    {
        id: 1002, title: '反转链表', difficulty: 'easy', tags: ['链表', '递归'],
        description: '<p>给你单链表的头节点 <code>head</code> ，请你反转链表，并返回反转后的链表。</p>',
        inputFormat: 'head = [1,2,3,4,5]',
        outputFormat: '[5,4,3,2,1]',
        sampleInput: 'head = [1,2,3,4,5]',
        sampleOutput: '[5,4,3,2,1]',
        templateCode: {
            java: 'class Solution {\n    public ListNode reverseList(ListNode head) {\n        // 在此处编写你的代码\n        \n    }\n}',
            c: 'struct ListNode* reverseList(struct ListNode* head) {\n    // 在此处编写你的代码\n    \n}',
            python: 'class Solution:\n    def reverseList(self, head: ListNode) -> ListNode:\n        # 在此处编写你的代码\n        pass',
            go: 'func reverseList(head *ListNode) *ListNode {\n    // 在此处编写你的代码\n    \n}'
        }
    },
    {
        id: 1003, title: 'LRU缓存', difficulty: 'medium', tags: ['设计', '哈希表', '链表'],
        description: '<p>请你设计并实现一个满足 <b>LRU (最近最少使用) 缓存</b> 约束的数据结构。</p><p>实现 <code>LRUCache</code> 类：</p><ul><li><code>LRUCache(int capacity)</code> 以正整数作为容量 <code>capacity</code> 初始化 LRU 缓存</li><li><code>int get(int key)</code> 如果关键字 <code>key</code> 存在于缓存中，则返回关键字的值，否则返回 <code>-1</code></li><li><code>void put(int key, int value)</code> 如果关键字 <code>key</code> 已经存在，则变更其数据值 <code>value</code>；如果不存在，则向缓存中插入该组 <code>key-value</code>。如果插入操作导致关键字数量超过 <code>capacity</code>，则应该逐出最久未使用的关键字。</li></ul>',
        inputFormat: '["LRUCache", "put", "put", "get", "put", "get", "put", "get", "get", "get"]\n[[2], [1, 1], [2, 2], [1], [3, 3], [2], [4, 4], [1], [3], [4]]',
        outputFormat: '[null, null, null, 1, null, -1, null, -1, 3, 4]',
        sampleInput: 'capacity = 2, operations = ["put(1,1)","put(2,2)","get(1)","put(3,3)","get(2)","put(4,4)","get(1)","get(3)","get(4)"]',
        sampleOutput: '[null,null,1,null,-1,null,-1,3,4]',
        templateCode: {
            java: 'class LRUCache {\n    public LRUCache(int capacity) {\n        // 初始化缓存\n    }\n    public int get(int key) {\n        // 获取缓存\n        return -1;\n    }\n    public void put(int key, int value) {\n        // 设置缓存\n    }\n}',
            c: 'typedef struct {\n    int key;\n    int value;\n} LRUCache;\n\nLRUCache* lRUCacheCreate(int capacity) {\n    // 初始化\n    return NULL;\n}\nint lRUCacheGet(LRUCache* obj, int key) {\n    // 获取\n    return -1;\n}\nvoid lRUCachePut(LRUCache* obj, int key, int value) {\n    // 设置\n}\nvoid lRUCacheFree(LRUCache* obj) {\n    // 释放内存\n}',
            python: 'class LRUCache:\n    def __init__(self, capacity: int):\n        # 初始化\n        pass\n    def get(self, key: int) -> int:\n        # 获取\n        return -1\n    def put(self, key: int, value: int) -> None:\n        # 设置\n        pass',
            go: 'type LRUCache struct {\n    // 定义数据结构\n}\n\nfunc Constructor(capacity int) LRUCache {\n    // 初始化\n    return LRUCache{}\n}\n\nfunc (this *LRUCache) Get(key int) int {\n    // 获取\n    return -1\n}\n\nfunc (this *LRUCache) Put(key int, value int) {\n    // 设置\n}'
        }
    },
    {
        id: 1004, title: '无重复字符的最长子串', difficulty: 'medium', tags: ['哈希表', '字符串', '滑动窗口'],
        description: '<p>给定一个字符串 <code>s</code> ，请你找出其中不含有重复字符的 <b>最长子串</b> 的长度。</p>',
        inputFormat: 's = "abcabcbb"',
        outputFormat: '3',
        sampleInput: 's = "abcabcbb"',
        sampleOutput: '3',
        templateCode: {
            java: 'class Solution {\n    public int lengthOfLongestSubstring(String s) {\n        // 在此处编写你的代码\n        \n    }\n}',
            c: 'int lengthOfLongestSubstring(char* s) {\n    // 在此处编写你的代码\n    \n}',
            python: 'class Solution:\n    def lengthOfLongestSubstring(self, s: str) -> int:\n        # 在此处编写你的代码\n        pass',
            go: 'func lengthOfLongestSubstring(s string) int {\n    // 在此处编写你的代码\n    \n}'
        }
    },
    {
        id: 1005, title: '接雨水', difficulty: 'hard', tags: ['栈', '双指针', '动态规划'],
        description: '<p>给定 <code>n</code> 个非负整数表示每个宽度为 <code>1</code> 的柱子的高度图，计算按此排列的柱子，下雨之后能接多少雨水。</p>',
        inputFormat: 'height = [0,1,0,2,1,0,1,3,2,1,2,1]',
        outputFormat: '6',
        sampleInput: 'height = [0,1,0,2,1,0,1,3,2,1,2,1]',
        sampleOutput: '6',
        templateCode: {
            java: 'class Solution {\n    public int trap(int[] height) {\n        // 在此处编写你的代码\n        \n    }\n}',
            c: 'int trap(int* height, int heightSize) {\n    // 在此处编写你的代码\n    \n}',
            python: 'class Solution:\n    def trap(self, height: list[int]) -> int:\n        # 在此处编写你的代码\n        pass',
            go: 'func trap(height []int) int {\n    // 在此处编写你的代码\n    \n}'
        }
    },
    {
        id: 1006, title: '合并两个有序链表', difficulty: 'easy', tags: ['链表', '递归'],
        description: '<p>将两个升序链表合并为一个新的 <b>升序</b> 链表并返回。新链表是通过拼接给定的两个链表的所有节点组成的。</p>',
        inputFormat: 'l1 = [1,2,4], l2 = [1,3,4]',
        outputFormat: '[1,1,2,3,4,4]',
        sampleInput: 'l1 = [1,2,4], l2 = [1,3,4]',
        sampleOutput: '[1,1,2,3,4,4]',
        templateCode: {
            java: 'class Solution {\n    public ListNode mergeTwoLists(ListNode l1, ListNode l2) {\n        // 在此处编写你的代码\n        \n    }\n}',
            c: 'struct ListNode* mergeTwoLists(struct ListNode* l1, struct ListNode* l2) {\n    // 在此处编写你的代码\n    \n}',
            python: 'class Solution:\n    def mergeTwoLists(self, l1: ListNode, l2: ListNode) -> ListNode:\n        # 在此处编写你的代码\n        pass',
            go: 'func mergeTwoLists(l1 *ListNode, l2 *ListNode) *ListNode {\n    // 在此处编写你的代码\n    \n}'
        }
    }
];

// OJ 全局状态
const OJState = {
    problems: [],
    currentProblem: null,
    currentProblemId: null,
    currentLanguage: 'java',
    currentSolutionLang: 'java',
    editor: null,
    fullscreenEditor: null,
    isFullscreen: false,
    isRunning: false,
    isSubmitting: false
};

// HTML 转义函数
function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', initOJ);

// 初始化 OJ
async function initOJ() {
    // 从URL获取题目ID
    const urlParams = new URLSearchParams(window.location.search);
    const problemId = urlParams.get('id');

    try {
        // 加载题目列表
        await loadProblems();

        // 初始化编辑器
        initEditor();
        initFullscreenEditor();

        // 初始化控件
        initOJControls();
        initTabs();
        initLanguageSwitchers();

        // 初始化可拖动分栏
        initResizableLayout();
        initVerticalResize();

        // 根据URL参数选择题目
        if (problemId) {
            const id = parseInt(problemId);
            let problem = OJState.problems.find(p => p.id === id);
            if (!problem) {
                // 不在第一页（前20道）→ 直接按题号/ID 查详情，修复第 21 道之后打开显示错题目的 bug
                problem = await fetchProblemById(problemId);
            }
            selectProblem(problem || OJState.problems[0]);
        } else {
            // 没有ID参数，选择第一题
            if (OJState.problems.length > 0) {
                selectProblem(OJState.problems[0]);
            }
        }
    } catch (error) {
        console.error('OJ初始化失败:', error);
        // 使用离线数据
        OJState.problems = OFFLINE_PROBLEMS;
        initEditor();
        initFullscreenEditor();
        initOJControls();
        initTabs();
        initLanguageSwitchers();
        initResizableLayout();
        initVerticalResize();
        selectProblem(OJState.problems[0]);
    }
}

// 后端题目 → 前端题目对象（id 取题号 problem_no，与列表页跳转链接的 ?id= 保持一致）
function mapProblem(problem, index) {
    const problemNoValue = problem.problem_no || problem.problemNo;
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
        tags: problem.tags ? problem.tags.split(',').map(tag => tag.trim()).filter(t => t) : [],
        description: problem.description || '',
        inputFormat: problem.inputFormat || '',
        outputFormat: problem.outputFormat || '',
        sampleInput: problem.sampleInput || '',
        sampleOutput: problem.sampleOutput || '',
        referenceSolution: problem.template || ''
    };
}

// 从后端获取题目列表（第一页，用于列表导航）
async function loadProblems() {
    try {
        const curUser = getCurrentUser();
        const response = await fetch(`${API_BASE}/problems`, {
            credentials: 'include',
            headers: { 'X-User-Id': String(curUser.id) }
        });
        const data = await response.json();

        if (data.success && data.problems && data.problems.length > 0) {
            OJState.problems = data.problems.map(mapProblem);
        } else {
            OJState.problems = OFFLINE_PROBLEMS;
        }
    } catch (error) {
        console.log('使用离线题目数据');
        OJState.problems = OFFLINE_PROBLEMS;
    }
}

// 按题号/ID 直接拉取题目详情（URL ?id= 传的是题号 problem_no；兼容 DB 主键 id）
async function fetchProblemById(id) {
    const curUser = getCurrentUser();
    // 优先按题号查（前端链接 id=题号），再兜底按 DB 主键查
    for (const url of [`${API_BASE}/problems/by-no/${id}`, `${API_BASE}/problems/${id}`]) {
        try {
            const response = await fetch(url, {
                credentials: 'include',
                headers: { 'X-User-Id': String(curUser.id) }
            });
            const data = await response.json();
            if (data.success && data.problem) {
                return mapProblem(data.problem);
            }
        } catch (e) { /* 尝试下一个接口 */ }
    }
    return null;
}

// 初始化代码编辑器
function initEditor() {
    const textarea = document.getElementById('ojCodeEditor');
    OJState.editor = CodeMirror.fromTextArea(textarea, {
        mode: LANGUAGE_CONFIG[OJState.currentLanguage].mode,
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
            'Ctrl-Z': 'undo', 'Cmd-Z': 'undo',
            'Ctrl-Y': 'redo', 'Cmd-Y': 'redo',
            'Ctrl-/': 'toggleComment',
            'Tab': 'indentMore', 'Shift-Tab': 'indentLess'
        }
    });
    OJState.editor.setSize(null, '100%');
}

// 初始化全屏编辑器
function initFullscreenEditor() {
    const textarea = document.getElementById('ojFullscreenCodeEditor');
    OJState.fullscreenEditor = CodeMirror.fromTextArea(textarea, {
        mode: LANGUAGE_CONFIG[OJState.currentLanguage].mode,
        theme: 'monokai',
        lineNumbers: true,
        matchBrackets: true,
        autoCloseBrackets: true,
        styleActiveLine: true,
        indentUnit: 4,
        tabSize: 4,
        indentWithTabs: false,
        extraKeys: {
            'Ctrl-Z': 'undo', 'Cmd-Z': 'undo',
            'Ctrl-Y': 'redo', 'Cmd-Y': 'redo',
            'Ctrl-/': 'toggleComment',
            'Tab': 'indentMore', 'Shift-Tab': 'indentLess',
            'Esc': exitFullscreen
        }
    });
    OJState.fullscreenEditor.setSize(null, '100%');
}

// 初始化标签页切换
function initTabs() {
    document.querySelectorAll('.oj-problem-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            const tabName = tab.dataset.tab;
            console.log('标签页切换:', tabName);
            
            // 更新标签状态
            document.querySelectorAll('.oj-problem-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');

            // 切换内容区域
            document.getElementById('ojProblemContent').style.display = 'none';
            document.getElementById('ojSolutionContent').style.display = 'none';
            document.getElementById('ojSubmissionsContent').style.display = 'none';
            document.getElementById('ojUserSolutionsContent').style.display = 'none';

            // 切换语言选择栏（仅解法标签显示）
            const langBar = document.getElementById('ojSolutionLangBar');

            if (tabName === 'description') {
                document.getElementById('ojProblemContent').style.display = 'block';
                langBar.style.display = 'none';
            } else if (tabName === 'solution') {
                document.getElementById('ojSolutionContent').style.display = 'block';
                langBar.style.display = 'flex';
                console.log('调用 renderSolutionContent, currentProblem:', !!OJState.currentProblem);
                renderSolutionContent();
            } else if (tabName === 'submissions') {
                document.getElementById('ojSubmissionsContent').style.display = 'block';
                langBar.style.display = 'none';
            } else if (tabName === 'userSolutions') {
                document.getElementById('ojUserSolutionsContent').style.display = 'block';
                langBar.style.display = 'none';
                if (window.OJSolution && OJState.currentProblem) {
                    OJSolution.loadList(OJState.currentProblem.id);
                }
            }
        });
    });

    // 结果标签
    document.querySelectorAll('.oj-result-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelectorAll('.oj-result-tab').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
        });
    });
}

// 初始化语言切换器
function initLanguageSwitchers() {
    // 编辑器语言切换
    document.getElementById('ojLangSelect')?.addEventListener('change', (e) => {
        const lang = e.target.value;
        OJState.currentLanguage = lang;

        OJState.editor.setOption('mode', LANGUAGE_CONFIG[lang].mode);
        OJState.fullscreenEditor.setOption('mode', LANGUAGE_CONFIG[lang].mode);

        // 更新全屏语言选择
        const fsSelect = document.getElementById('ojFullscreenLangSelect');
        if (fsSelect && fsSelect.value !== lang) fsSelect.value = lang;

        // 加载空模板
        OJState.editor.setValue(EMPTY_TEMPLATES[lang] || '');
    });

    // 解法标签页语言切换
    document.getElementById('ojSolutionLangSelect')?.addEventListener('change', (e) => {
        OJState.currentSolutionLang = e.target.value;
        renderSolutionContent();
    });

    // 全屏语言切换
    document.getElementById('ojFullscreenLangSelect')?.addEventListener('change', (e) => {
        const lang = e.target.value;
        OJState.currentLanguage = lang;
        OJState.editor.setOption('mode', LANGUAGE_CONFIG[lang].mode);
        OJState.fullscreenEditor.setOption('mode', LANGUAGE_CONFIG[lang].mode);
        // 更新编辑器语言选择
        const mainSelect = document.getElementById('ojLangSelect');
        if (mainSelect && mainSelect.value !== lang) mainSelect.value = lang;

        // 加载空模板
        OJState.editor.setValue(EMPTY_TEMPLATES[lang] || '');
        OJState.fullscreenEditor.setValue(EMPTY_TEMPLATES[lang] || '');
    });
}

// 渲染解法内容（显示后端参考解法）
function renderSolutionContent() {
    try {
        const content = document.getElementById('ojSolutionContent');
        if (!content) {
            console.error('找不到 ojSolutionContent 元素');
            return;
        }
        
        if (!OJState.currentProblem) {
            content.innerHTML = '<div style="padding: 20px; color: #f14c4c;">错误：未选择题目</div>';
            return;
        }

        const lang = OJState.currentSolutionLang || 'java';
        const langLabel = LANGUAGE_CONFIG[lang]?.label || 'Java';
        const referenceSolution = OJState.currentProblem.referenceSolution || '';

        console.log('渲染解法内容:', {
            problemId: OJState.currentProblem.id,
            problemTitle: OJState.currentProblem.title,
            hasReferenceSolution: !!referenceSolution,
            solutionLength: referenceSolution.length
        });

        let solutionHtml = '';
        if (referenceSolution) {
            solutionHtml = referenceSolution;
        } else {
            solutionHtml = '暂无该题目的参考解法';
        }

        content.innerHTML = `
            <div style="margin-bottom: 16px;">
                <div style="font-size: 12px; color: #858585; margin-bottom: 8px;">
                    <span style="background: rgba(102,126,234,0.15); color: #a5b4fc; padding: 2px 8px; border-radius: 4px; margin-right: 8px;">${langLabel} 参考解法</span>
                    <span style="color: #666;">以下是该题的参考解法</span>
                </div>
                <pre style="background: #1e1e1e; border: 1px solid #3c3c3c; border-radius: 6px; padding: 12px; font-family: 'Fira Code', monospace; font-size: 13px; line-height: 1.6; color: #d4d4d4; overflow-x: auto; white-space: pre-wrap; word-break: break-all;">${escapeHtml(solutionHtml)}</pre>
                <div style="margin-top: 12px; display: flex; gap: 8px;">
                    <button onclick="copyReferenceSolution()" style="padding: 6px 12px; background: rgba(102,126,234,0.15); border: 1px solid rgba(102,126,234,0.3); border-radius: 4px; color: #a5b4fc; cursor: pointer; font-size: 12px;">📋 复制解法</button>
                    <button onclick="applyReferenceSolution()" style="padding: 6px 12px; background: rgba(78,201,176,0.15); border: 1px solid rgba(78,201,176,0.3); border-radius: 4px; color: #4ec9b0; cursor: pointer; font-size: 12px;">✅ 应用到编辑器</button>
                </div>
            </div>
        `;
    } catch (e) {
        console.error('渲染解法内容失败:', e);
        const content = document.getElementById('ojSolutionContent');
        if (content) {
            content.innerHTML = `<div style="padding: 20px; color: #f14c4c;">加载解法失败: ${escapeHtml(e.message)}</div>`;
        }
    }
}

// 复制参考解法
function copyReferenceSolution() {
    if (!OJState.currentProblem) return;
    const solution = OJState.currentProblem.referenceSolution || '';
    navigator.clipboard.writeText(solution).then(() => {
        showToast('参考解法已复制到剪贴板');
    }).catch(() => showToast('复制失败'));
}

// 应用参考解法到编辑器
function applyReferenceSolution() {
    if (!OJState.currentProblem) return;
    const solution = OJState.currentProblem.referenceSolution || '';
    if (!solution) {
        showToast('暂无参考解法');
        return;
    }
    OJState.editor.setValue(solution);
    OJState.fullscreenEditor.setValue(solution);
    showToast('参考解法已应用到编辑器');
}

// 选择题目并显示详情
function selectProblem(problem) {
    if (!problem) return;

    OJState.currentProblem = problem;
    OJState.currentProblemId = problem.id;

    // 更新难度标签
    const diffMap = {
        'easy': { label: '简单', class: 'oj-difficulty-easy' },
        'medium': { label: '中等', class: 'oj-difficulty-medium' },
        'hard': { label: '困难', class: 'oj-difficulty-hard' }
    };
    const diff = diffMap[problem.difficulty] || diffMap['easy'];
    const diffSpan = document.getElementById('ojProblemDifficulty');
    diffSpan.textContent = diff.label;
    diffSpan.className = `oj-problem-difficulty ${diff.class}`;

    // 更新题目内容
    const contentDiv = document.getElementById('ojProblemContent');
    const tagsHtml = problem.tags.map(t => {
        const cls = ['easy', 'medium', 'hard'].includes(t.toLowerCase())
            ? `oj-tag oj-tag-${t.toLowerCase()}`
            : 'oj-tag';
        return `<span class="${cls}">${t}</span>`;
    }).join('');

    // 使用 marked 解析 Markdown 描述
    let descriptionHtml = problem.description || '暂无题目描述';
    if (typeof marked !== 'undefined') {
        try {
            descriptionHtml = marked.parse(descriptionHtml);
        } catch (e) {
            console.warn('Markdown 解析失败，使用原始文本:', e);
            descriptionHtml = `<p>${escapeHtml(problem.description || '暂无题目描述')}</p>`;
        }
    }

    contentDiv.innerHTML = `
        <h2 style="font-size: 18px; font-weight: 700; margin: 0 0 12px 0; color: #fff;">${problem.id}. ${problem.title}</h2>
        <div class="oj-problem-tags" style="display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 14px;">${tagsHtml}</div>
        <div class="oj-problem-body markdown-body" style="color: #d4d4d4; line-height: 1.8; font-size: 13px;">
            ${descriptionHtml}
            ${problem.inputFormat ? `<div style="margin-top: 14px;"><h4 style="color: #9cdcfe; font-size: 12px; font-weight: 600; margin-bottom: 6px;">输入格式</h4><pre style="background: #2d2d30; padding: 10px; border-radius: 4px; font-family: 'Fira Code', monospace; font-size: 12px; color: #d4d4d4;">${escapeHtml(problem.inputFormat)}</pre></div>` : ''}
            ${problem.outputFormat ? `<div style="margin-top: 10px;"><h4 style="color: #9cdcfe; font-size: 12px; font-weight: 600; margin-bottom: 6px;">输出格式</h4><pre style="background: #2d2d30; padding: 10px; border-radius: 4px; font-family: 'Fira Code', monospace; font-size: 12px; color: #d4d4d4;">${escapeHtml(problem.outputFormat)}</pre></div>` : ''}
            ${problem.sampleInput ? `<div style="margin-top: 14px;"><h4 style="color: #9cdcfe; font-size: 12px; font-weight: 600; margin-bottom: 6px;">示例</h4><div style="background: #2d2d30; padding: 12px; border-radius: 4px; font-family: 'Fira Code', monospace; font-size: 12px;"><div><span style="color: #858585;">输入：</span>${escapeHtml(problem.sampleInput)}</div>${problem.sampleOutput ? `<div style="margin-top: 6px;"><span style="color: #858585;">输出：</span>${escapeHtml(problem.sampleOutput)}</div>` : ''}</div></div>` : ''}
        </div>
    `;

    // 加载空模板（编辑器默认显示）
    const lang = OJState.currentLanguage;
    const emptyTemplate = EMPTY_TEMPLATES[lang] || '';
    OJState.editor.setValue(emptyTemplate);
    OJState.fullscreenEditor.setValue(emptyTemplate);

    // 更新导航按钮
    updateNavButtons();

    // 重置结果
    resetResultPanel();

    // 如果当前显示的是解法标签，重新渲染
    const activeTab = document.querySelector('.oj-problem-tab.active');
    if (activeTab && activeTab.dataset.tab === 'solution') {
        renderSolutionContent();
    }

    console.log('题目已选择:', problem.title);
}

// 重置结果面板
function resetResultPanel() {
    document.getElementById('ojResultBody').innerHTML = `
        <div class="oj-empty-state" style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #6a6a6a;">
            <div style="font-size: 32px; margin-bottom: 8px;">▶</div>
            <span>点击运行查看结果</span>
        </div>
    `;
}

// 更新导航按钮状态
function updateNavButtons() {
    const currentIndex = OJState.problems.findIndex(p => p.id === OJState.currentProblemId);
    document.getElementById('ojPrevBtn').disabled = currentIndex <= 0;
    document.getElementById('ojNextBtn').disabled = currentIndex >= OJState.problems.length - 1;
}

// 初始化OJ控件
function initOJControls() {
    // 返回题目列表
    document.getElementById('ojBackToListBtn')?.addEventListener('click', () => {
        window.location.href = 'oj-list.html';
    });

    // 上一题
    document.getElementById('ojPrevBtn')?.addEventListener('click', () => {
        switchToProblem(-1);
    });

    // 下一题
    document.getElementById('ojNextBtn')?.addEventListener('click', () => {
        switchToProblem(1);
    });

    // 运行代码
    document.getElementById('ojRunBtn')?.addEventListener('click', runCode);
    document.getElementById('ojFullscreenRunBtn')?.addEventListener('click', () => {
        OJState.editor.setValue(OJState.fullscreenEditor.getValue());
        exitFullscreen();
        runCode();
    });

    // 提交代码
    document.getElementById('ojSubmitBtn')?.addEventListener('click', submitCode);
    document.getElementById('ojFullscreenSubmitBtn')?.addEventListener('click', () => {
        OJState.editor.setValue(OJState.fullscreenEditor.getValue());
        exitFullscreen();
        submitCode();
    });

    // 格式化代码
    document.getElementById('ojFormatBtn')?.addEventListener('click', formatCode);
    document.getElementById('ojFormatBtn2')?.addEventListener('click', formatCode);
    document.getElementById('ojFullscreenFormatBtn')?.addEventListener('click', () => formatCode(true));

    // 清空代码
    document.getElementById('ojClearBtn')?.addEventListener('click', () => OJState.editor.setValue(''));
    document.getElementById('ojClearBtn2')?.addEventListener('click', () => OJState.editor.setValue(''));
    document.getElementById('ojFullscreenClearBtn')?.addEventListener('click', () => OJState.fullscreenEditor.setValue(''));

    // 复制代码
    document.getElementById('ojCopyBtn')?.addEventListener('click', copyCode);
    document.getElementById('ojCopyBtn2')?.addEventListener('click', copyCode);
    document.getElementById('ojFullscreenCopyBtn')?.addEventListener('click', copyFullscreenCode);

    // 全屏
    document.getElementById('ojFullscreenBtn')?.addEventListener('click', enterFullscreen);
    document.getElementById('ojFullscreenBtn2')?.addEventListener('click', enterFullscreen);
    document.getElementById('ojExitFullscreenBtn')?.addEventListener('click', exitFullscreen);
}

// 切换题目
function switchToProblem(direction) {
    const currentIndex = OJState.problems.findIndex(p => p.id === OJState.currentProblemId);
    const newIndex = currentIndex + direction;
    if (newIndex >= 0 && newIndex < OJState.problems.length) {
        selectProblem(OJState.problems[newIndex]);
        window.scrollTo(0, 0);
    }
}

// 格式化代码
function formatCode(isFullscreen = false) {
    const editor = isFullscreen ? OJState.fullscreenEditor : OJState.editor;
    let code = editor.getValue();

    if (OJState.currentLanguage === 'java' || OJState.currentLanguage === 'c') {
        code = code.replace(/\s+/g, ' ');
        code = code.replace(/\s*{\s*/g, ' {\n    ');
        code = code.replace(/\s*}\s*/g, '\n}\n');
        code = code.replace(/;\s*/g, ';\n');
    } else if (OJState.currentLanguage === 'python') {
        // Python 格式化（简单缩进）
        code = code.replace(/\n\s*\n/g, '\n\n');
    }

    editor.setValue(code);
    showToast('代码已格式化');
}

// 复制代码
function copyCode() {
    const code = OJState.editor.getValue();
    navigator.clipboard.writeText(code).then(() => showToast('代码已复制')).catch(() => showToast('复制失败'));
}

// 复制全屏代码
function copyFullscreenCode() {
    const code = OJState.fullscreenEditor.getValue();
    navigator.clipboard.writeText(code).then(() => showToast('代码已复制')).catch(() => showToast('复制失败'));
}

// 进入全屏
function enterFullscreen() {
    const overlay = document.getElementById('ojFullscreenOverlay');
    const currentCode = OJState.editor.getValue();
    OJState.fullscreenEditor.setValue(currentCode);
    overlay.classList.add('active');
    OJState.isFullscreen = true;
    document.body.style.overflow = 'hidden';
}

// 退出全屏
function exitFullscreen() {
    const overlay = document.getElementById('ojFullscreenOverlay');
    OJState.editor.setValue(OJState.fullscreenEditor.getValue());
    overlay.classList.remove('active');
    OJState.isFullscreen = false;
    document.body.style.overflow = '';
}

// 运行代码（调用后端真实接口）
async function runCode() {
    if (OJState.isRunning) return;
    const code = OJState.editor.getValue();
    const lang = OJState.currentLanguage;
    const input = document.getElementById('ojCustomInput').value;

    if (!code.trim()) {
        displayRunResult({ status: 'error', message: '代码不能为空' });
        return;
    }

    if (!OJState.currentProblem) {
        displayRunResult({ status: 'error', message: '请先选择一道题目' });
        return;
    }

    OJState.isRunning = true;
    const runBtn = document.getElementById('ojRunBtn');
    runBtn.disabled = true;
    runBtn.innerHTML = '<span class="oj-spinner" style="display:inline-block;width:12px;height:12px;border:2px solid #3c3c3c;border-top-color: #4ec9b0;border-radius:50%;animation:spin 0.8s linear infinite;vertical-align:middle;"></span> 运行中...';

    document.getElementById('ojResultBody').innerHTML = `
        <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #858585;">
            <div class="oj-spinner" style="width: 25px; height: 25px; border: 2px solid #3c3c3c; border-top-color: #4ec9b0; border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 8px;"></div>
            <span>正在编译运行...</span>
        </div>
    `;

    try {
        const curUser = getCurrentUser();
        const response = await fetch(`${API_BASE}/submissions/run`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-User-Id': String(curUser.id) },
            credentials: 'include',
            body: JSON.stringify({
                problemId: OJState.currentProblem.id,
                code: code,
                language: lang,
                input: input
            })
        });

        const data = await response.json();
        
        if (data.success) {
            const statusMap = {
                'SUCCESS': 'success',
                'CE': 'ce',
                'RE': 'error',
                'TLE': 'error'
            };
            const displayStatus = statusMap[data.status] || 'error';
            
            let result = {
                status: displayStatus,
                output: data.output || '',
                time: data.runtime || 0,
                memory: data.memory || 0
            };
            
            if (data.compileError) {
                result.message = data.compileError;
            } else if (data.error) {
                result.message = data.error;
            }
            
            displayRunResult(result);
        } else {
            displayRunResult({ status: 'error', message: data.message || '运行失败' });
        }
    } catch (error) {
        displayRunResult({ status: 'error', message: '无法连接到服务器: ' + error.message });
    } finally {
        OJState.isRunning = false;
        runBtn.disabled = false;
        runBtn.innerHTML = '<span>▶</span> 运行';
    }
}

// 提交代码（调用后端真实接口 + 轮询判题结果）
async function submitCode() {
    if (OJState.isSubmitting) return;
    const code = OJState.editor.getValue();
    const lang = OJState.currentLanguage;

    if (!code.trim()) {
        displayJudgeResult({ status: 'ce', message: '代码不能为空', results: [], passedCount: 0, totalCount: 0 });
        return;
    }

    if (!OJState.currentProblem) {
        displayJudgeResult({ status: 'ce', message: '请先选择一道题目', results: [], passedCount: 0, totalCount: 0 });
        return;
    }

    OJState.isSubmitting = true;
    const submitBtn = document.getElementById('ojSubmitBtn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="oj-spinner" style="display:inline-block;width:12px;height:12px;border:2px solid #3c3c3c;border-top-color: #4ec9b0;border-radius:50%;animation:spin 0.8s linear infinite;vertical-align:middle;"></span> 提交中...';

    document.getElementById('ojResultBody').innerHTML = `
        <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #858585;">
            <div class="oj-spinner" style="width: 25px; height: 25px; border: 2px solid #3c3c3c; border-top-color: #4ec9b0; border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 8px;"></div>
            <span>正在提交代码...</span>
        </div>
    `;

    try {
        // 1. 提交代码
        const curUser = getCurrentUser();
        const submitResponse = await fetch(`${API_BASE}/submissions`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-User-Id': String(curUser.id) },
            credentials: 'include',
            body: JSON.stringify({
                problemId: OJState.currentProblem.id,
                code: code,
                language: lang,
                userId: curUser.id,
                username: curUser.username
            })
        });

        const submitData = await submitResponse.json();
        
        if (!submitData.success) {
            displayJudgeResult({ status: 'ce', message: submitData.message || '提交失败', results: [], passedCount: 0, totalCount: 0 });
            OJState.isSubmitting = false;
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<span>📤</span> 提交';
            return;
        }

        const submissionId = submitData.submissionId;
        
        // 2. 轮询判题结果
        document.getElementById('ojResultBody').innerHTML = `
            <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #858585;">
                <div class="oj-spinner" style="width: 25px; height: 25px; border: 2px solid #3c3c3c; border-top-color: #4ec9b0; border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 8px;"></div>
                <span>判题中...</span>
            </div>
        `;

        const maxPollCount = 30; // 最多轮询30次
        let pollCount = 0;
        
        while (pollCount < maxPollCount) {
            await new Promise(resolve => setTimeout(resolve, 500)); // 等待500ms
            
            const detailResponse = await fetch(`${API_BASE}/submissions/${submissionId}`, {
                credentials: 'include',
                headers: { 'X-User-Id': String(curUser.id) }
            });
            const detailData = await detailResponse.json();
            
            if (detailData.success && detailData.submission) {
                const submission = detailData.submission;
                const status = submission.status;
                
                if (status !== 'PENDING' && status !== 'JUDGING') {
                    // 判题完成
                    const statusMap = {
                        'AC': 'accepted',
                        'WA': 'wa',
                        'CE': 'ce',
                        'RE': 're',
                        'TLE': 'tle',
                        'MLE': 'tle'
                    };
                    
                    const displayStatus = statusMap[status] || 'wa';
                    const judgeLog = submission.judgeLog || '';
                    const errorMessage = submission.errorMessage || '';
                    
                    // 解析判题日志获取通过用例数
                    let passedCount = 0;
                    let totalCount = 0;
                    const logLines = judgeLog.split('\n').filter(l => l.trim());
                    totalCount = logLines.length;
                    passedCount = (judgeLog.match(/通过/g) || []).length;
                    
                    const result = {
                        status: displayStatus,
                        results: [],
                        passedCount: passedCount,
                        totalCount: totalCount || 1,
                        time: submission.runtime || 0,
                        memory: submission.memory || 0,
                        message: errorMessage || judgeLog
                    };
                    
                    displayJudgeResult(result);
                    break;
                }
            }
            
            pollCount++;
        }
        
        if (pollCount >= maxPollCount) {
            displayJudgeResult({ status: 'wa', message: '判题超时，请稍后查看结果', results: [], passedCount: 0, totalCount: 0 });
        }
    } catch (error) {
        displayJudgeResult({ status: 'ce', message: '无法连接到服务器: ' + error.message, results: [], passedCount: 0, totalCount: 0 });
    } finally {
        OJState.isSubmitting = false;
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<span>📤</span> 提交';
    }
}

// 显示运行结果
function displayRunResult(result) {
    const body = document.getElementById('ojResultBody');
    const ok = result.status === 'success';
    const color = ok ? '#4ec9b0' : '#f14c4c';

    if (result.status === 'success') {
        body.innerHTML = `
            <div style="width: 100%; padding: 4px 0;">
                <div style="font-size: 14px; font-weight: 600; color: ${color}; margin-bottom: 10px;">✅ 运行成功</div>
                <div style="background: #2d2d30; padding: 10px; border-radius: 4px; margin-bottom: 10px;">
                    <div style="color: #858585; font-size: 11px; margin-bottom: 4px;">运行输出</div>
                    <div style="color: #d4d4d4;">${escapeHtml(String(result.output || ''))}</div>
                </div>
                <div style="color: #858585; font-size: 11px;">耗时: <strong style="color: #d4d4d4;">${result.time}ms</strong> | 内存: <strong style="color: #d4d4d4;">${result.memory}KB</strong></div>
            </div>
        `;
    } else {
        body.innerHTML = `
            <div style="width: 100%; padding: 4px 0;">
                <div style="font-size: 14px; font-weight: 600; color: ${color}; margin-bottom: 10px;">❌ ${result.status === 'ce' ? '编译错误' : '运行错误'}</div>
                <div style="background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.2); padding: 10px; border-radius: 4px; color: #f14c4c; font-size: 12px; white-space: pre-wrap;">${escapeHtml(result.message || '未知错误')}</div>
            </div>
        `;
    }
}

// 显示判题结果
function displayJudgeResult(result) {
    const body = document.getElementById('ojResultBody');

    const statusConfig = {
        'accepted': { icon: '✅', label: '通过', color: '#4ec9b0' },
        'wa': { icon: '❌', label: '答案错误', color: '#f14c4c' },
        'ce': { icon: '⚠️', label: '编译失败', color: '#cca700' },
        're': { icon: '⚠️', label: '运行崩溃', color: '#cca700' },
        'tle': { icon: '⏱️', label: '超时', color: '#cca700' }
    };

    const config = statusConfig[result.status] || { icon: '❌', label: '错误', color: '#f14c4c' };

    let html = `
        <div style="width: 100%;">
            <div style="display: flex; align-items: center; gap: 10px; padding: 10px; background: #2d2d30; border-left: 3px solid ${config.color}; border-radius: 4px; margin-bottom: 12px;">
                <span style="font-size: 18px;">${config.icon}</span>
                <div>
                    <div style="font-weight: 600; color: ${config.color}; font-size: 13px;">${config.label}</div>
                    <div style="font-size: 11px; color: #858585; margin-top: 2px;">
                        通过 ${result.passedCount}/${result.totalCount} 个测试用例 | 耗时: ${result.time}ms | 内存: ${result.memory}KB
                    </div>
                </div>
            </div>
    `;

    if (result.results && result.results.length > 0) {
        html += '<div style="margin-bottom: 12px;">';
        const progress = result.totalCount > 0 ? (result.passedCount / result.totalCount) * 100 : 0;
        html += `
            <div style="margin-bottom: 10px;">
                <div style="display: flex; justify-content: space-between; font-size: 11px; margin-bottom: 4px;">
                    <span style="color: #858585;">进度</span>
                    <span style="color: #d4d4d4;">${result.passedCount}/${result.totalCount}</span>
                </div>
                <div style="height: 6px; background: #2d2d30; border-radius: 3px; overflow: hidden;">
                    <div style="height: 100%; width: ${progress}%; background: ${config.color}; transition: width 0.3s;"></div>
                </div>
            </div>
        `;

        result.results.forEach(tc => {
            const passed = tc.status === 'passed';
            html += `
                <div style="display: flex; align-items: center; gap: 10px; padding: 8px 10px; background: #252526; border-radius: 4px; margin-bottom: 4px; font-size: 12px;">
                    <span style="color: ${passed ? '#4ec9b0' : '#f14c4c'}; font-weight: 600;">${passed ? '✓' : '✗'}</span>
                    <span>测试用例 #${tc.index}</span>
                    <span style="margin-left: auto; color: #858585; font-size: 11px;">${tc.time}ms | ${tc.memory}KB</span>
                </div>
            `;
        });
        html += '</div>';
    }

    if (result.message) {
        html += `<div style="background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.2); padding: 10px; border-radius: 4px; color: #f14c4c; font-size: 12px; white-space: pre-wrap;">${escapeHtml(result.message)}</div>`;
    }

    html += '</div>';
    body.innerHTML = html;
}

// 显示提示
function showToast(message) {
    const toast = document.createElement('div');
    toast.style.cssText = `
        position: fixed; bottom: 20px; left: 50%;
        transform: translateX(-50%);
        background: #252526; border: 1px solid #667eea;
        color: #e2e8f0; padding: 12px 24px;
        border-radius: 8px; z-index: 10000;
        font-size: 13px; box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        transition: opacity 0.3s;
    `;
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 2000);
}

// 可拖动分栏 - 水平
function initResizableLayout() {
    const leftPanel = document.getElementById('ojResizableLeft');
    const rightPanel = document.getElementById('ojResizableRight');
    const handle = document.getElementById('ojResizeHandle');

    if (!leftPanel || !rightPanel || !handle) return;

    let isResizing = false, startX = 0, startWidth = 0;

    handle.addEventListener('mousedown', e => {
        isResizing = true;
        startX = e.clientX;
        startWidth = leftPanel.offsetWidth;
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';
        e.preventDefault();
    });

    document.addEventListener('mousemove', e => {
        if (!isResizing) return;
        const deltaX = e.clientX - startX;
        const containerWidth = leftPanel.parentElement.offsetWidth;
        let newLeftWidth = startWidth + deltaX;
        newLeftWidth = Math.max(200, Math.min(newLeftWidth, containerWidth * 0.7));
        leftPanel.style.flex = `0 0 ${newLeftWidth}px`;
    });

    document.addEventListener('mouseup', () => {
        if (!isResizing) return;
        isResizing = false;
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
    });
}

// 可拖动分栏 - 垂直
function initVerticalResize() {
    const handle = document.getElementById('ojVerticalResizeHandle');
    const topPanel = document.getElementById('ojVerticalTop');
    const bottomPanel = document.getElementById('ojVerticalBottom');
    const container = document.getElementById('ojVerticalLayout');

    if (!handle || !topPanel || !bottomPanel || !container) return;

    let isResizing = false, startY = 0, startHeight = 0;

    handle.addEventListener('mousedown', e => {
        isResizing = true;
        startY = e.clientY;
        startHeight = topPanel.offsetHeight;
        document.body.style.cursor = 'row-resize';
        document.body.style.userSelect = 'none';
        e.preventDefault();
    });

    document.addEventListener('mousemove', e => {
        if (!isResizing) return;
        const deltaY = e.clientY - startY;
        let newHeight = startHeight + deltaY;
        newHeight = Math.max(80, Math.min(newHeight, container.offsetHeight * 0.85));
        topPanel.style.flex = `0 0 ${newHeight}px`;
    });

    document.addEventListener('mouseup', () => {
        if (!isResizing) return;
        isResizing = false;
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
    });
}

// 防止表单提交
document.addEventListener('submit', e => {
    e.preventDefault();
    e.stopPropagation();
    return false;
}, true);
