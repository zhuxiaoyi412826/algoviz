/**
 * 算法页面主控制器
 */

// 可视化器实例
let visualizers = {};

// 当前选中的算法
let currentAlgo = 'bubbleSort';

// 动画控制器
let animationController = null;

// DOM元素
const elements = {};

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initElements();
    initEventListeners();
    initVisualizers();
    selectAlgorithm('bubbleSort');
});

// 初始化DOM元素
function initElements() {
    elements.algoButtons = document.querySelectorAll('.algo-btn');
    elements.arraySizeSlider = document.getElementById('arraySizeSlider');
    elements.arraySizeValue = document.getElementById('arraySizeValue');
    elements.algoRandomBtn = document.getElementById('algoRandomBtn');
    elements.algoDataInput = document.getElementById('algoDataInput');
    elements.searchTargetSection = document.getElementById('searchTargetSection');
    elements.searchTarget = document.getElementById('searchTarget');
    elements.graphConfigSection = document.getElementById('graphConfigSection');
    elements.generateGraphBtn = document.getElementById('generateGraphBtn');
    elements.algoPlayBtn = document.getElementById('algoPlayBtn');
    elements.algoPauseBtn = document.getElementById('algoPauseBtn');
    elements.algoStepBackBtn = document.getElementById('algoStepBackBtn');
    elements.algoStepForwardBtn = document.getElementById('algoStepForwardBtn');
    elements.algoResetBtn = document.getElementById('algoResetBtn');
    elements.algoSpeedSlider = document.getElementById('algoSpeedSlider');
    elements.algoSpeedValue = document.getElementById('algoSpeedValue');
    elements.algoVisualizationCanvas = document.getElementById('algoVisualizationCanvas');
    elements.algoPseudocodeContent = document.getElementById('algoPseudocodeContent');
    elements.algoVizStatus = document.getElementById('algoVizStatus');
    elements.algoTitle = document.getElementById('algoTitle');
    elements.algoDescription = document.getElementById('algoDescription');
    elements.algoTimeBest = document.getElementById('algoTimeBest');
    elements.algoTimeAvg = document.getElementById('algoTimeAvg');
    elements.algoTimeWorst = document.getElementById('algoTimeWorst');
    elements.algoSpace = document.getElementById('algoSpace');
    elements.algoCoreIdea = document.getElementById('algoCoreIdea');
    elements.algoFileInput = document.getElementById('algoFileInput');
    elements.algoFileName = document.getElementById('algoFileName');
    elements.algoLoadFileBtn = document.getElementById('algoLoadFileBtn');
    elements.algoPresetBtns = document.querySelectorAll('#algoInputPresets .preset-btn');
}

// 初始化事件监听
    function initEventListeners() {
        // Variable Watch Update Event Listener
        document.addEventListener('algoVariableUpdate', (e) => {
            updateVariableWatch(e.detail);
        });

        // 算法选择
    if (elements.algoButtons) {
        elements.algoButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                const algo = btn.dataset.algo;
                if (algo) selectAlgorithm(algo);
            });
        });
    }

    // 数组大小
    if (elements.arraySizeSlider) {
        elements.arraySizeSlider.addEventListener('input', () => {
            const size = parseInt(elements.arraySizeSlider.value);
            elements.arraySizeValue.textContent = size;
            generateRandomData();
        });
    }

    // 随机数据
    if (elements.algoRandomBtn) {
        elements.algoRandomBtn.addEventListener('click', generateRandomData);
    }

    // 文件上传
    if (elements.algoFileInput) {
        elements.algoFileInput.addEventListener('change', handleAlgoFileSelect);
    }
    if (elements.algoLoadFileBtn) {
        elements.algoLoadFileBtn.addEventListener('click', loadAlgoFromFile);
    }

    // 预设按钮
    if (elements.algoPresetBtns) {
        elements.algoPresetBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const preset = btn.dataset.preset;
                applyAlgoPreset(preset);
            });
        });
    }

    // 生成图
    elements.generateGraphBtn?.addEventListener('click', () => {
        if (visualizers.graphTraversal) {
            visualizers.graphTraversal.generateRandom();
        }
        if (visualizers.dijkstra) {
            visualizers.dijkstra.generateRandom();
        }
    });

    // 播放控制
    if (elements.algoPlayBtn) elements.algoPlayBtn.addEventListener('click', play);
    if (elements.algoPauseBtn) elements.algoPauseBtn.addEventListener('click', pause);
    if (elements.algoStepBackBtn) elements.algoStepBackBtn.addEventListener('click', stepBack);
    if (elements.algoStepForwardBtn) elements.algoStepForwardBtn.addEventListener('click', stepForward);
    if (elements.algoResetBtn) elements.algoResetBtn.addEventListener('click', reset);

    // 速度控制
    if (elements.algoSpeedSlider) elements.algoSpeedSlider.addEventListener('input', updateSpeed);

    // 导出GIF
    const exportBtn = document.getElementById('exportGifBtn');
    if (exportBtn) {
        exportBtn.addEventListener('click', exportAnimationGif);
    }
}

// 文件选择处理
let algoSelectedFileData = null;

function handleAlgoFileSelect(e) {
    const file = e.target.files[0];
    if (file) {
        elements.algoFileName.textContent = file.name;
        elements.algoFileName.style.display = 'inline';
        elements.algoLoadFileBtn.style.display = 'inline-block';
        
        const reader = new FileReader();
        reader.onload = (event) => {
            algoSelectedFileData = event.target.result;
        };
        reader.readAsText(file);
    }
}

function loadAlgoFromFile() {
    if (algoSelectedFileData) {
        const parsed = parseAlgoFileData(algoSelectedFileData);
        if (parsed) {
            elements.algoDataInput.value = parsed.join(', ');
            if (visualizers.sorting) {
                visualizers.sorting.setData(parsed);
            }
        }
    }
}

function parseAlgoFileData(content) {
    try {
        if (content.trim().startsWith('[')) {
            const arr = JSON.parse(content);
            if (Array.isArray(arr)) return arr;
        }
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
function applyAlgoPreset(preset) {
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
    elements.algoDataInput.value = data.join(', ');
    if (visualizers.sorting) {
        visualizers.sorting.setData(data);
    }
}

// 初始化可视化器
function initVisualizers() {
    const canvas = document.getElementById('algoVisualizationCanvas');
    if (!canvas) return; // 容错处理：如果当前页面没有可视化画布则不初始化

    visualizers.sorting = new SortingVisualizer('algoVisualizationCanvas');
    visualizers.searching = new SearchingVisualizer('algoVisualizationCanvas');
    visualizers.treeTraversal = new TraversalVisualizer('algoVisualizationCanvas');
    visualizers.graphTraversal = new GraphTraversalVisualizer('algoVisualizationCanvas');
    visualizers.dijkstra = new DijkstraVisualizer('algoVisualizationCanvas');

    // 设置默认数据
    visualizers.sorting.setData([23, 45, 12, 67, 34, 89, 56, 78, 90, 15]);
    visualizers.searching.setData([10, 20, 30, 40, 50, 60, 70, 80, 90]);
    visualizers.treeTraversal.fromArray([50, 30, 70, 20, 40, 60, 80]);
    visualizers.graphTraversal.generateRandom(6);
    visualizers.dijkstra.generateRandom(6);
}

// 选择算法
function selectAlgorithm(algo) {
    if (!document.getElementById('algoVisualizationCanvas')) return; // 如果没有画布则不执行后续渲染逻辑

    // 更新按钮状态
    if (elements.algoButtons) {
        elements.algoButtons.forEach(btn => {
            btn.classList.toggle('active', btn.dataset.algo === algo);
        });
    }

    currentAlgo = algo;

    // 更新信息面板
    updateInfoPanel(algo);

    // 更新伪代码
    updatePseudocode(algo);

    // 更新配置选项
    updateConfigOptions(algo);

    // 更新可视化器
    updateVisualizer(algo);

    // 重置状态
    updateStatus('ready');
}

// 更新信息面板
function updateInfoPanel(algo) {
    const info = ALGO_INFO[algo];
    if (!info) return;

    elements.algoTitle.textContent = info.title;
    elements.algoDescription.textContent = info.description;
    elements.algoCoreIdea.textContent = info.coreIdea;

    if (info.timeComplexity.best) {
        elements.algoTimeBest.textContent = `最优 ${info.timeComplexity.best}`;
        elements.algoTimeAvg.textContent = `平均 ${info.timeComplexity.average}`;
        elements.algoTimeWorst.textContent = `最坏 ${info.timeComplexity.worst}`;
    } else {
        elements.algoTimeBest.textContent = `时间 ${info.timeComplexity}`;
        elements.algoTimeAvg.textContent = '';
        elements.algoTimeWorst.textContent = '';
    }

    elements.algoSpace.textContent = `空间 ${info.spaceComplexity}`;
}

// 更新伪代码
function updatePseudocode(algo) {
    const code = PSEUDOCODES[algo] || PSEUDOCODES.bubbleSort;
    
    elements.algoPseudocodeContent.innerHTML = code.map((line, index) => `
        <div class="pseudocode-line" data-line="${index}">
            ${line}
        </div>
    `).join('');
}

// 高亮伪代码行
function highlightPseudocodeLine(lineIndex) {
    document.querySelectorAll('#algoPseudocodeContent .pseudocode-line').forEach((el, i) => {
        el.classList.toggle('highlight', i === lineIndex);
    });
}

// 更新配置选项
function updateConfigOptions(algo) {
    // 隐藏所有特殊配置
    elements.searchTargetSection.style.display = 'none';
    elements.graphConfigSection.style.display = 'none';
    
    // 显示相关配置
    if (algo === 'binarySearch') {
        elements.searchTargetSection.style.display = 'block';
    }
    
    if (algo === 'graphDFS' || algo === 'graphBFS' || algo === 'dijkstra') {
        elements.graphConfigSection.style.display = 'block';
    }
}

// 更新可视化器
function updateVisualizer(algo) {
    // 隐藏所有可视化器
    Object.values(visualizers).forEach(viz => {
        if (viz.container) {
            viz.container.style.display = 'none';
        }
    });

    // 显示当前算法对应的可视化器
    let viz = null;
    
    switch (algo) {
        case 'bubbleSort':
        case 'selectionSort':
        case 'insertionSort':
        case 'quickSort':
        case 'mergeSort':
        case 'heapSort':
            viz = visualizers.sorting;
            break;
        case 'binarySearch':
            viz = visualizers.searching;
            break;
        case 'treeInorder':
        case 'treePreorder':
        case 'treePostorder':
            viz = visualizers.treeTraversal;
            break;
        case 'graphDFS':
        case 'graphBFS':
            viz = visualizers.graphTraversal;
            break;
        case 'dijkstra':
            viz = visualizers.dijkstra;
            break;
    }

    if (viz && viz.container) {
        viz.container.style.display = 'block';
        viz.render();
        animationController = viz;
        
        viz.onStepChange = () => {
            updateStatus('running');
        };
        viz.onComplete = () => {
            updateStatus('completed');
            updateControlButtons(false);
        };
    }
}

// 生成随机数据
function generateRandomData() {
    const size = parseInt(elements.arraySizeSlider.value);
    
    // 检查是否是排序算法
    if (['bubbleSort', 'selectionSort', 'insertionSort', 'quickSort', 'mergeSort', 'heapSort'].includes(currentAlgo)) {
        visualizers.sorting.setData(Utils.randomArray(size, 1, 99));
    } else if (currentAlgo === 'binarySearch') {
        visualizers.searching.setData(Utils.randomArray(size, 1, 99).sort((a, b) => a - b));
    } else if (['treeInorder', 'treePreorder', 'treePostorder'].includes(currentAlgo)) {
        visualizers.treeTraversal.fromArray(Utils.randomArray(size, 1, 99));
    } else if (currentAlgo === 'graphDFS' || currentAlgo === 'graphBFS') {
        visualizers.graphTraversal.generateRandom(6);
    } else if (currentAlgo === 'dijkstra') {
        visualizers.dijkstra.generateRandom(6);
    }

    updateStatus('ready');
}

// 运行算法
function runAlgorithm() {
    switch (currentAlgo) {
        case 'bubbleSort':
            visualizers.sorting.bubbleSort();
            break;
        case 'selectionSort':
            visualizers.sorting.selectionSort();
            break;
        case 'insertionSort':
            visualizers.sorting.insertionSort();
            break;
        case 'quickSort':
            visualizers.sorting.quickSort();
            break;
        case 'mergeSort':
            visualizers.sorting.mergeSort();
            break;
        case 'heapSort':
            visualizers.sorting.heapSort();
            break;
        case 'binarySearch':
            const target = parseInt(elements.searchTarget.value) || Math.floor(Math.random() * 100);
            elements.searchTarget.value = target;
            visualizers.searching.binarySearch(target);
            break;
        case 'treeInorder':
            visualizers.treeTraversal.inorder();
            break;
        case 'treePreorder':
            visualizers.treeTraversal.preorder();
            break;
        case 'treePostorder':
            visualizers.treeTraversal.postorder();
            break;
        case 'graphDFS':
            visualizers.graphTraversal.dfs('A');
            break;
        case 'graphBFS':
            visualizers.graphTraversal.bfs('A');
            break;
        case 'dijkstra':
            visualizers.dijkstra.run('A');
            break;
    }
}

// 播放
function play() {
    if (animationController) {
        animationController.play();
        updateStatus('running');
        updateControlButtons(true);
    } else {
        runAlgorithm();
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
    const value = parseInt(elements.algoSpeedSlider.value);
    const speed = 1500 - value + 50;
    
    if (animationController) {
        animationController.setSpeed(speed);
    }
    
    const speedText = (value / 500).toFixed(1) + 'x';
    elements.algoSpeedValue.textContent = speedText;
}

// 更新状态显示
function updateStatus(status) {
    const statusEl = elements.algoVizStatus;
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
    elements.algoPlayBtn.disabled = isPlaying;
    elements.algoPauseBtn.disabled = !isPlaying;
}

// ===================================
// GIF Export Functionality
// ===================================

async function exportAnimationGif() {
    const exportBtn = document.getElementById('exportGifBtn');
    const canvas = elements.algoVisualizationCanvas;
    
    if (!canvas || !animationController) {
        alert('请先运行一次动画');
        return;
    }

    // 禁用按钮
    exportBtn.disabled = true;
    exportBtn.innerHTML = '<span>⏳</span> 导出中...';

    // 创建进度模态框
    const overlay = createExportOverlay();
    document.body.appendChild(overlay);

    try {
        const gif = new GIF({
            workers: 2,
            quality: 10,
            width: canvas.offsetWidth,
            height: canvas.offsetHeight,
            workerScript: 'https://cdnjs.cloudflare.com/ajax/libs/gif.js/0.2.0/gif.worker.js'
        });

        // 重置到开始
        animationController.reset();
        animationController.render();

        // 捕获初始帧
        await captureFrame(gif, canvas);
        updateProgress(5, '正在录制...');

        const steps = animationController.getSteps();
        const totalSteps = steps.length;
        const frameInterval = Math.max(1, Math.floor(totalSteps / 30)); // 最多30帧

        // 录制每一帧
        for (let i = 0; i < totalSteps; i++) {
            animationController.stepForward();
            animationController.render();

            if (i % frameInterval === 0 || i === totalSteps - 1) {
                await captureFrame(gif, canvas);
                const progress = 5 + Math.floor((i / totalSteps) * 85);
                updateProgress(progress, `录制中 ${i + 1}/${totalSteps}`);
            }
        }

        updateProgress(90, '正在生成GIF...');

        // 添加最后一帧
        await captureFrame(gif, canvas);

        // 生成GIF
        gif.on('finished', function(blob) {
            updateProgress(100, '完成!');
            
            // 下载
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `algoviz-${currentAlgo}-${Date.now()}.gif`;
            a.click();
            URL.revokeObjectURL(url);

            // 移除模态框
            setTimeout(() => {
                overlay.remove();
                exportBtn.disabled = false;
                exportBtn.innerHTML = '<span>🎬</span> 导出动画';
            }, 1000);
        });

        gif.render();
    } catch (error) {
        console.error('Export failed:', error);
        alert('导出失败，请重试');
        overlay.remove();
        exportBtn.disabled = false;
        exportBtn.innerHTML = '<span>🎬</span> 导出动画';
    }
}

function createExportOverlay() {
    const overlay = document.createElement('div');
    overlay.className = 'export-overlay';
    overlay.innerHTML = `
        <div class="export-modal">
            <h3>导出动画</h3>
            <div class="export-progress">
                <div class="export-progress-bar" id="exportProgressBar" style="width: 0%"></div>
            </div>
            <div class="export-status" id="exportStatus">准备中...</div>
        </div>
    `;
    return overlay;
}

function updateProgress(percent, text) {
    const bar = document.getElementById('exportProgressBar');
    const status = document.getElementById('exportStatus');
    if (bar) bar.style.width = percent + '%';
    if (status) status.textContent = text;
}

async function captureFrame(gif, element) {
    try {
        const canvas = await html2canvas(element, {
            backgroundColor: getComputedStyle(document.documentElement).getPropertyValue('--bg-card').trim() || '#1a1a2e',
            scale: 1,
            logging: false,
            useCORS: true
        });
        gif.addFrame(canvas, { delay: 100, copy: true });
    } catch (e) {
        console.error('Frame capture failed:', e);
    }
}

// ���±���������
function updateVariableWatch(variables) {
    const panel = document.getElementById('variableWatchPanel');
    if (!panel) return;
    
    let html = '<h4>�������</h4><div class="vars-grid">';
    for (const [key, value] of Object.entries(variables)) {
        html += <div class="var-item"><span class="var-name"></span>: <span class="var-value"></span></div>;
    }
    html += '</div>';
    panel.innerHTML = html;
}

