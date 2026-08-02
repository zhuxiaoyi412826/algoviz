/**
 * 数据结构页面主控制器
 */

// 可视化器实例
let visualizers = {};

// 当前选中的数据结构
let currentDS = 'array';

// 当前选中的操作
let currentOperation = null;

// 动画控制器
let animationController = null;

// DOM元素
const elements = {
    dsButtons: null,
    dataInput: null,
    randomBtn: null,
    operationButtons: null,
    playBtn: null,
    pauseBtn: null,
    stepBackBtn: null,
    stepForwardBtn: null,
    resetBtn: null,
    speedSlider: null,
    speedValue: null,
    visualizationCanvas: null,
    pseudocodeContent: null,
    vizStatus: null,
    infoPanel: null
};

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    initElements();
    initEventListeners();
    initVisualizers();
    // 不默认选择任何数据结构
    // selectDataStructure('array');
});

// 初始化DOM元素
function initElements() {
    elements.dsButtons = document.querySelectorAll('.ds-btn');
    elements.dataInput = document.getElementById('dataInput');
    elements.randomBtn = document.getElementById('randomBtn');
    elements.operationButtons = document.getElementById('operationButtons');
    elements.playBtn = document.getElementById('playBtn');
    elements.pauseBtn = document.getElementById('pauseBtn');
    elements.stepBackBtn = document.getElementById('stepBackBtn');
    elements.stepForwardBtn = document.getElementById('stepForwardBtn');
    elements.resetBtn = document.getElementById('resetBtn');
    elements.speedSlider = document.getElementById('speedSlider');
    elements.speedValue = document.getElementById('speedValue');
    elements.visualizationCanvas = document.getElementById('visualizationCanvas');
    elements.pseudocodeContent = document.getElementById('pseudocodeContent');
    elements.vizStatus = document.getElementById('vizStatus');
    elements.infoPanel = document.getElementById('infoPanel');
    elements.fileInput = document.getElementById('fileInput');
    elements.fileName = document.getElementById('fileName');
    elements.loadFileBtn = document.getElementById('loadFileBtn');
    elements.presetBtns = document.querySelectorAll('.preset-btn');
}

// 初始化事件监听
function initEventListeners() {
    // 数据结构选择
    elements.dsButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const ds = btn.dataset.ds;
            selectDataStructure(ds);
        });
    });

    // 随机数据
    elements.randomBtn.addEventListener('click', generateRandomData);

    // 文件上传
    if (elements.fileInput) {
        elements.fileInput.addEventListener('change', handleFileSelect);
    }
    if (elements.loadFileBtn) {
        elements.loadFileBtn.addEventListener('click', loadFromFile);
    }

    // 预设按钮
    elements.presetBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const preset = btn.dataset.preset;
            applyPreset(preset);
        });
    });

    // 播放控制
    elements.playBtn.addEventListener('click', play);
    elements.pauseBtn.addEventListener('click', pause);
    elements.stepBackBtn.addEventListener('click', stepBack);
    elements.stepForwardBtn.addEventListener('click', stepForward);
    elements.resetBtn.addEventListener('click', reset);

    // 速度控制
    elements.speedSlider.addEventListener('input', updateSpeed);
}

// 文件选择处理
let selectedFileData = null;

function handleFileSelect(e) {
    const file = e.target.files[0];
    if (file) {
        elements.fileName.textContent = file.name;
        elements.fileName.style.display = 'inline';
        elements.loadFileBtn.style.display = 'inline-block';
        
        const reader = new FileReader();
        reader.onload = (event) => {
            selectedFileData = event.target.result;
        };
        reader.readAsText(file);
    }
}

function loadFromFile() {
    if (selectedFileData) {
        const parsed = parseFileData(selectedFileData);
        if (parsed) {
            elements.dataInput.value = parsed.join(', ');
            generateFromInput();
        }
    }
}

function parseFileData(content) {
    try {
        // 尝试JSON格式
        if (content.trim().startsWith('[')) {
            const arr = JSON.parse(content);
            if (Array.isArray(arr)) return arr;
        }
        // 尝试CSV/普通格式
        const lines = content.split(/[\n,]+/).map(s => s.trim()).filter(s => s);
        const nums = lines.map(s => {
            const n = parseFloat(s);
            return isNaN(n) ? null : n;
        });
        if (nums.every(n => n !== null)) return nums;
        return null;
    } catch (e) {
        return null;
    }
}

// 预设数据
function applyPreset(preset) {
    let data = [];
    switch (preset) {
        case 'random10':
            data = Array.from({length: 10}, () => Math.floor(Math.random() * 100) + 1);
            break;
        case 'random20':
            data = Array.from({length: 20}, () => Math.floor(Math.random() * 100) + 1);
            break;
        case 'nearlySorted':
            data = [1, 2, 3, 5, 4, 6, 7, 8, 9, 10];
            break;
        case 'reversed':
            data = [10, 9, 8, 7, 6, 5, 4, 3, 2, 1];
            break;
    }
    elements.dataInput.value = data.join(', ');
    generateFromInput();
}

// 初始化可视化器
function initVisualizers() {
    visualizers.array = new ArrayVisualizer('visualizationCanvas');
    visualizers.linkedlist = new LinkedListVisualizer('visualizationCanvas');
    visualizers.stack = new StackVisualizer('visualizationCanvas');
    visualizers.queue = new QueueVisualizer('visualizationCanvas');
    visualizers.tree = new TreeVisualizer('visualizationCanvas');
    visualizers.hash = new HashTableVisualizer('visualizationCanvas');
    visualizers.graph = new GraphVisualizer('visualizationCanvas');
    visualizers.heap = new HeapVisualizer('visualizationCanvas');
    
    // 暂时不初始化 trie、unionfind、segmenttree、dp、kmp 等可视化器
    // 只保留核心数据结构

    // 设置默认数据
    visualizers.array.setData([23, 45, 12, 67, 34, 89, 56]);
    visualizers.linkedlist.fromArray([10, 20, 30, 40, 50]);
    visualizers.stack.setData([15, 25, 35]);
    visualizers.queue.setData([5, 10, 15, 20]);
    visualizers.tree.fromArray([50, 30, 70, 20, 40, 60, 80]);
    visualizers.hash.setData([[10, 'A'], [25, 'B'], [35, 'C'], [50, 'D']]);
    visualizers.graph.generateRandom(6, 0.4);
    visualizers.heap.fromArray([15, 10, 20, 8, 25, 30, 12]);
}

// 选择数据结构
function selectDataStructure(ds) {
    // 数据结构页面映射
    const pageMap = {
        array: '/html/DS/arr/index.html',
        linkedlist: '/html/DS/linklist/index.html',
        stack: '/html/DS/stack/index.html',
        queue: '/html/DS/queue/index.html',
        tree: '/html/DS/tree/index.html',
        hash: '/html/DS/hash/index.html',
        graph: '/html/DS/graph/index.html',
        heap: '/html/DS/heap/index.html'
    };
    
    // 检查是否有对应的独立页面
    if (pageMap[ds]) {
        // 跳转到独立页面
        window.location.href = pageMap[ds];
        return;
    }
    
    // 对于没有独立页面的数据结构，保持原有的页面内可视化功能
    // 更新按钮状态
    elements.dsButtons.forEach(btn => {
        btn.classList.toggle('active', btn.dataset.ds === ds);
    });

    currentDS = ds;

    // 更新信息面板
    updateInfoPanel(ds);

    // 更新操作按钮
    updateOperationButtons(ds);

    // 更新伪代码
    updatePseudocode(ds);

    // 更新可视化
    const viz = visualizers[ds];
    if (viz) {
        viz.render();
    }

    // 重置状态
    updateStatus('ready');
}

// 更新信息面板
function updateInfoPanel(ds) {
    const info = DS_INFO[ds];
    if (!info) return;

    document.getElementById('dsTitle').textContent = info.title;
    document.getElementById('dsDescription').textContent = info.description;
    document.getElementById('dsFeatures').innerHTML = info.features.map(f => `- ${f}`).join('<br>');

    // 更新复杂度
    const complexityContainer = elements.infoPanel.querySelector('.info-value:last-of-type');
    if (complexityContainer) {
        let complexityHtml = '';
        for (const [key, value] of Object.entries(info.timeComplexity)) {
            complexityHtml += `<span class="complexity-badge">${key} ${value}</span>`;
        }
        complexityContainer.innerHTML = complexityHtml;
    }

    // 更新空间复杂度
    const spaceEl = document.querySelector('#infoPanel .info-item:nth-child(3) .info-value');
    if (spaceEl) {
        spaceEl.innerHTML = `<span class="complexity-badge">${info.spaceComplexity}</span>`;
    }
}

// 更新操作按钮
function updateOperationButtons(ds) {
    const operations = getOperations(ds);
    
    elements.operationButtons.innerHTML = operations.map(op => `
        <button class="btn btn-secondary" onclick="executeOperation('${op.id}')">
            ${op.icon} ${op.name}
        </button>
    `).join('');
}

// 获取数据结构操作
function getOperations(ds) {
    const operations = {
        array: [
            { id: 'search', name: '查询', icon: '🔍' },
            { id: 'insert', name: '增加', icon: '➕' },
            { id: 'delete', name: '删除', icon: '➖' },
            { id: 'expand', name: '扩容', icon: '📏' }
        ],
        linkedlist: [
            { id: 'traverse', name: '遍历', icon: '🚶' },
            { id: 'search', name: '查找', icon: '🔍' },
            { id: 'insertHead', name: '头部插入', icon: '⬆️' },
            { id: 'insertTail', name: '尾部插入', icon: '⬇️' }
        ],
        stack: [
            { id: 'push', name: '入栈', icon: '⬇️' },
            { id: 'pop', name: '出栈', icon: '⬆️' },
            { id: 'peek', name: '查看栈顶', icon: '👁️' }
        ],
        queue: [
            { id: 'enqueue', name: '入队', icon: '➡️' },
            { id: 'dequeue', name: '出队', icon: '⬅️' },
            { id: 'front', name: '查看队首', icon: '👁️' }
        ],
        tree: [
            { id: 'traverseInorder', name: '中序遍历', icon: '↔️' },
            { id: 'traversePreorder', name: '前序遍历', icon: '➡️' },
            { id: 'traversePostorder', name: '后序遍历', icon: '⬅️' },
            { id: 'traverseLevel', name: '层序遍历', icon: '⬇️' }
        ],
        hash: [
            { id: 'insert', name: '插入', icon: '➕' },
            { id: 'search', name: '查找', icon: '🔍' },
            { id: 'delete', name: '删除', icon: '➖' }
        ],
        graph: [
            { id: 'dfs', name: 'DFS遍历', icon: '🧭' },
            { id: 'bfs', name: 'BFS遍历', icon: '🌊' }
        ],
        heap: [
            { id: 'insert', name: '插入', icon: '➕' },
            { id: 'extract', name: '提取堆顶', icon: '⬆️' },
            { id: 'sort', name: '堆排序', icon: '📊' }
        ],
        trie: [
            { id: 'insert', name: '插入单词', icon: '➕' },
            { id: 'search', name: '查找单词', icon: '🔍' },
            { id: 'prefix', name: '前缀匹配', icon: '🔎' }
        ],
        unionfind: [
            { id: 'union', name: '合并集合', icon: '🔗' },
            { id: 'find', name: '查找根', icon: '🔍' },
            { id: 'connected', name: '连通判断', icon: '✅' }
        ],
        segmenttree: [
            { id: 'query', name: '区间查询', icon: '🔍' },
            { id: 'update', name: '单点更新', icon: '✏️' },
            { id: 'build', name: '构建树', icon: '🌲' }
        ],
        dp: [
            { id: 'lcs', name: '最长公共子序列', icon: '📊' },
            { id: 'knapsack', name: '0-1背包', icon: '🎒' },
            { id: 'fibonacci', name: '斐波那契', icon: '🔢' }
        ],
        kmp: [
            { id: 'match', name: '模式匹配', icon: '🎯' },
            { id: 'lps', name: 'LPS构建', icon: '📝' }
        ]
    };

    return operations[ds] || [];
}

// 更新伪代码
function updatePseudocode(ds) {
    const code = PSEUDOCODES[ds] || ['// No pseudocode available'];
    
    elements.pseudocodeContent.innerHTML = code.map((line, index) => `
        <div class="pseudocode-line" data-line="${index}">
            ${line}
        </div>
    `).join('');
}

// 高亮伪代码行
function highlightPseudocodeLine(lineIndex) {
    document.querySelectorAll('.pseudocode-line').forEach((el, i) => {
        el.classList.toggle('highlight', i === lineIndex);
    });
}

// 生成随机数据
function generateRandomData() {
    const viz = visualizers[currentDS];
    if (!viz) return;

    switch (currentDS) {
        case 'array':
            viz.setData(Utils.randomArray(8, 1, 99));
            break;
        case 'linkedlist':
            viz.fromArray(Utils.randomArray(6, 1, 99));
            break;
        case 'stack':
            viz.setData(Utils.randomArray(5, 1, 99));
            break;
        case 'queue':
            viz.setData(Utils.randomArray(5, 1, 99));
            break;
        case 'tree':
            viz.fromArray(Utils.randomArray(7, 1, 99));
            break;
        case 'hash':
            const pairs = Utils.randomArray(5, 1, 10).map((v, i) => [v * 7, String.fromCharCode(65 + i)]);
            viz.setData(pairs);
            break;
        case 'graph':
            viz.generateRandom(6, 0.4);
            break;
        case 'heap':
            viz.fromArray(Utils.randomArray(7, 1, 99));
            break;
        case 'trie':
            const words = ['apple', 'app', 'apply', 'application', 'banana', 'band', 'cat', 'car'];
            const shuffled = words.sort(() => Math.random() - 0.5);
            viz.setData(shuffled.slice(0, 5));
            break;
        case 'unionfind':
            viz.setData(6);
            break;
        case 'segmenttree':
            viz.setData(Utils.randomArray(6, 1, 20));
            break;
        case 'dp':
            viz.setData(Utils.randomArray(4, 5, 50), Utils.randomArray(4, 5, 50));
            break;
        case 'kmp':
            const text = 'ABABDABACDABABCABAB';
            const patterns = ['ABABCABAB', 'ABA', 'DABACDABAB'];
            viz.setData(text, patterns[Math.floor(Math.random() * patterns.length)]);
            break;
    }

    viz.render();
}

// 执行操作
function executeOperation(opId) {
    const viz = visualizers[currentDS];
    if (!viz) return;

    // 获取输入数据
    const inputValue = parseInt(elements.dataInput.value.split(',')[0]) || Utils.randomInt(1, 99);

    switch (currentDS) {
        case 'array':
            switch (opId) {
                case 'search':
                    viz.search(inputValue);
                    break;
                case 'insert':
                    viz.insert(viz.getData().length, inputValue);
                    break;
                case 'delete':
                    viz.delete(viz.getData().length - 1);
                    break;
                case 'expand':
                    // 扩容操作：将数组容量翻倍
                    const currentLength = viz.getData().length;
                    const expandedArray = [...viz.getData()];
                    for (let i = 0; i < currentLength; i++) {
                        expandedArray.push(0);
                    }
                    viz.setData(expandedArray);
                    break;
            }
            break;

        case 'linkedlist':
            switch (opId) {
                case 'traverse':
                    viz.traverse();
                    break;
                case 'search':
                    viz.search(inputValue);
                    break;
                case 'insertHead':
                    viz.insertAtHead(inputValue);
                    break;
                case 'insertTail':
                    viz.insertAtTail(inputValue);
                    break;
            }
            break;

        case 'stack':
            switch (opId) {
                case 'push':
                    viz.push(inputValue);
                    break;
                case 'pop':
                    viz.pop();
                    break;
                case 'peek':
                    alert(`栈顶元素: ${viz.peek()}`);
                    break;
            }
            break;

        case 'queue':
            switch (opId) {
                case 'enqueue':
                    viz.enqueue(inputValue);
                    break;
                case 'dequeue':
                    viz.dequeue();
                    break;
                case 'front':
                    alert(`队首元素: ${viz.front()}`);
                    break;
            }
            break;

        case 'tree':
            switch (opId) {
                case 'traverseInorder':
                    viz.inorder();
                    break;
                case 'traversePreorder':
                    viz.preorder();
                    break;
                case 'traversePostorder':
                    viz.postorder();
                    break;
                case 'traverseLevel':
                    viz.levelorder();
                    break;
            }
            break;

        case 'hash':
            const key = inputValue;
            const value = String.fromCharCode(65 + (key % 26));
            switch (opId) {
                case 'insert':
                    viz.insert(key, value);
                    break;
                case 'search':
                    viz.search(key);
                    break;
                case 'delete':
                    viz.delete(key);
                    break;
            }
            break;

        case 'graph':
            switch (opId) {
                case 'dfs':
                    viz.dfs('A');
                    break;
                case 'bfs':
                    viz.bfs('A');
                    break;
            }
            break;

        case 'heap':
            switch (opId) {
                case 'insert':
                    viz.insert(inputValue);
                    break;
                case 'extract':
                    viz.extractRoot();
                    break;
                case 'sort':
                    viz.heapSort();
                    break;
            }
            break;

        case 'trie':
            const wordInput = elements.dataInput.value.trim() || 'test';
            switch (opId) {
                case 'insert':
                    viz.insert(wordInput);
                    break;
                case 'search':
                    viz.search(wordInput);
                    break;
                case 'prefix':
                    viz.startsWith(wordInput);
                    break;
            }
            break;

        case 'unionfind':
            const a = inputValue % 6;
            const b = (inputValue + 3) % 6;
            switch (opId) {
                case 'union':
                    viz.union(a, b);
                    break;
                case 'find':
                    viz.find(a);
                    break;
                case 'connected':
                    viz.connected(a, b);
                    break;
            }
            break;

        case 'segmenttree':
            const arr = viz.getData();
            const l = 0;
            const r = Math.floor(arr.length / 2);
            switch (opId) {
                case 'query':
                    viz.query(l, r);
                    break;
                case 'update':
                    viz.update(2, inputValue);
                    break;
                case 'build':
                    viz.build();
                    break;
            }
            break;

        case 'dp':
            switch (opId) {
                case 'lcs':
                    viz.lcs();
                    break;
                case 'knapsack':
                    viz.knapsack();
                    break;
                case 'fibonacci':
                    viz.fibonacci();
                    break;
            }
            break;

        case 'kmp':
            switch (opId) {
                case 'match':
                    viz.kmpSearch();
                    break;
                case 'lps':
                    viz.computeLPS();
                    break;
            }
            break;
    }

    // 设置当前可视化器的动画回调
    animationController = viz;
    viz.onStepChange = (step) => {
        updateStatus('running');
    };
    viz.onComplete = () => {
        updateStatus('completed');
        updateControlButtons(false);
    };
}

// 播放
function play() {
    if (animationController) {
        animationController.play();
        updateStatus('running');
        updateControlButtons(true);
    }
}

// 暂停
function pause() {
    if (animationController) {
        animationController.pause();
        updateStatus('paused');
        updateControlButtons(false);
    }
}

// 上一步
function stepBack() {
    if (animationController) {
        animationController.stepBack();
        updateStatus('paused');
    }
}

// 下一步
function stepForward() {
    if (animationController) {
        animationController.stepForward();
    }
}

// 重置
function reset() {
    if (animationController) {
        animationController.reset();
        animationController.render();
        updateStatus('ready');
        updateControlButtons(false);
    }
}

// 更新速度
function updateSpeed() {
    const value = parseInt(elements.speedSlider.value);
    const speed = 2000 - value; // 反转，使滑块向右更快
    
    if (animationController) {
        animationController.setSpeed(speed);
    }
    
    const speedText = (value / 1000).toFixed(1) + 'x';
    elements.speedValue.textContent = speedText;
}

// 更新状态显示
function updateStatus(status) {
    const statusEl = elements.vizStatus;
    statusEl.className = 'viz-status';
    
    switch (status) {
        case 'ready':
            statusEl.textContent = '就绪';
            break;
        case 'running':
            statusEl.textContent = '运行中';
            statusEl.classList.add('running');
            break;
        case 'paused':
            statusEl.textContent = '已暂停';
            statusEl.classList.add('running');
            break;
        case 'completed':
            statusEl.textContent = '完成';
            statusEl.classList.add('completed');
            break;
    }
}

// 更新控制按钮状态
function updateControlButtons(isPlaying) {
    elements.playBtn.disabled = isPlaying;
    elements.pauseBtn.disabled = !isPlaying;
}
