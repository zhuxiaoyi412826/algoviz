/**
 * SortViz - 排序算法可视化学习平台
 * 主脚本文件
 */

// ========================================
// 全局状态管理
// ========================================
const state = {
  array: [],
  originalArray: [],
  steps: [],
  arraySnapshots: [], // 保存每一步的数组状态
  currentStep: 0,
  isPlaying: false,
  isPaused: false,
  isComplete: false,
  speed: 200,
  multiplier: 1, // 播放倍速
  baseSpeed: 200, // 基础速度
  arraySize: 20,
  currentAlgorithm: 'bubble',
  animationTimer: null,
  comparisons: 0,
  swaps: 0,
  stats: {
    comparisons: 0,
    swaps: 0,
    steps: 0
  },
  variables: {}
};

// ========================================
// 算法配置数据
// ========================================
const algorithms = {
  bubble: {
    name: '冒泡排序',
    category: '简单排序',
    timeComplexity: 'O(n²)',
    spaceComplexity: 'O(1)',
    stable: true,
    inPlace: true,
    description: '通过相邻元素的比较和交换，将较大（或较小）的元素逐步"冒泡"到序列的一端。时间复杂度固定为O(n²)，但实现简单，是初学者最熟悉的排序算法。',
    tips: '最佳情况：数组已有序，复杂度O(n)；最坏情况：数组逆序，复杂度O(n²)。',
    code: `<span class="comment">// 冒泡排序</span>
<span class="keyword">function</span> <span class="function">bubbleSort</span>(arr):
    <span class="keyword">for</span> i = <span class="number">0</span> <span class="keyword">to</span> arr.length - <span class="number">1</span>:
        <span class="keyword">for</span> j = <span class="number">0</span> <span class="keyword">to</span> arr.length - i - <span class="number">2</span>:
            <span class="keyword">if</span> arr[j] > arr[j+<span class="number">1</span>]:
                swap(arr[j], arr[j+<span class="number">1</span>])`
  },
  selection: {
    name: '选择排序',
    category: '简单排序',
    timeComplexity: 'O(n²)',
    spaceComplexity: 'O(1)',
    stable: false,
    inPlace: true,
    description: '每次从未排序区间选择最小（或最大）元素，放到已排序区间的末尾。交换次数最少为O(n)，但比较次数固定为O(n²)。',
    tips: '无论数组初始状态如何，比较次数始终是n(n-1)/2。交换次数最多为n-1次。',
    code: `<span class="comment">// 选择排序</span>
<span class="keyword">function</span> <span class="function">selectionSort</span>(arr):
    <span class="keyword">for</span> i = <span class="number">0</span> <span class="keyword">to</span> arr.length - <span class="number">1</span>:
        minIdx = i
        <span class="keyword">for</span> j = i+<span class="number">1</span> <span class="keyword">to</span> arr.length - <span class="number">1</span>:
            <span class="keyword">if</span> arr[j] < arr[minIdx]:
                minIdx = j
        swap(arr[i], arr[minIdx])`
  },
  insertion: {
    name: '插入排序',
    category: '简单排序',
    timeComplexity: 'O(n²)',
    spaceComplexity: 'O(1)',
    stable: true,
    inPlace: true,
    description: '将数组分为已排序和未排序两部分，逐个取出未排序元素，在已排序部分找到合适位置插入。适合少量数据或基本有序的数据。',
    tips: '最佳情况：数组已有序，时间复杂度O(n)；近乎有序时效率很高。',
    code: `<span class="comment">// 插入排序</span>
<span class="keyword">function</span> <span class="function">insertionSort</span>(arr):
    <span class="keyword">for</span> i = <span class="number">1</span> <span class="keyword">to</span> arr.length - <span class="number">1</span>:
        key = arr[i]
        j = i - <span class="number">1</span>
        <span class="keyword">while</span> j >= <span class="number">0</span> <span class="keyword">and</span> arr[j] > key:
            arr[j+<span class="number">1</span>] = arr[j]
            j = j - <span class="number">1</span>
        arr[j+<span class="number">1</span>] = key`
  },
  shell: {
    name: '希尔排序',
    category: '高级排序',
    timeComplexity: 'O(n^1.3)',
    spaceComplexity: 'O(1)',
    stable: false,
    inPlace: true,
    description: '插入排序的改进版，通过设置递减的增量序列，先将数组分成若干子数组进行插入排序，最后使用增量1完成最终排序。',
    tips: '增量序列的选择影响性能。常用Shell增量：n/2, n/4, ..., 1。时间复杂度与增量序列选择有关。',
    code: `<span class="comment">// 希尔排序</span>
<span class="keyword">function</span> <span class="function">shellSort</span>(arr):
    gap = arr.length / <span class="number">2</span>
    <span class="keyword">while</span> gap > <span class="number">0</span>:
        <span class="keyword">for</span> i = gap <span class="keyword">to</span> arr.length:
            temp = arr[i]
            j = i
            <span class="keyword">while</span> j >= gap <span class="keyword">and</span> arr[j-gap] > temp:
                arr[j] = arr[j-gap]
                j = j - gap
            arr[j] = temp
        gap = gap / <span class="number">2</span>`
  },
  quick: {
    name: '快速排序',
    category: '高级排序',
    timeComplexity: 'O(n log n)',
    spaceComplexity: 'O(log n)',
    stable: false,
    inPlace: true,
    description: '采用分治策略，选择一个基准元素，将数组分为两部分，小于基准的放左边，大于基准的放右边，然后递归排序两部分。',
    tips: '基准选择很重要！选择首/尾/随机/三数取中。递归深度最坏情况为O(n)。',
    code: `<span class="comment">// 快速排序</span>
<span class="keyword">function</span> <span class="function">quickSort</span>(arr, low, high):
    <span class="keyword">if</span> low < high:
        pivot = <span class="function">partition</span>(arr, low, high)
        <span class="function">quickSort</span>(arr, low, pivot-<span class="number">1</span>)
        <span class="function">quickSort</span>(arr, pivot+<span class="number">1</span>, high)

<span class="keyword">function</span> <span class="function">partition</span>(arr, low, high):
    pivot = arr[high]
    i = low - <span class="number">1</span>
    <span class="keyword">for</span> j = low <span class="keyword">to</span> high-<span class="number">1</span>:
        <span class="keyword">if</span> arr[j] <= pivot:
            i = i + <span class="number">1</span>
            swap(arr[i], arr[j])
    swap(arr[i+<span class="number">1</span>], arr[high])
    <span class="keyword">return</span> i + <span class="number">1</span>`
  },
  merge: {
    name: '归并排序',
    category: '高级排序',
    timeComplexity: 'O(n log n)',
    spaceComplexity: 'O(n)',
    stable: true,
    inPlace: false,
    description: '采用分治策略，将数组递归分成两半，分别排序后再合并。需要额外的O(n)空间存储临时数据。',
    tips: '稳定排序，时间复杂度恒定为O(n log n)。但需要额外的O(n)空间。适合链表排序。',
    code: `<span class="comment">// 归并排序</span>
<span class="keyword">function</span> <span class="function">mergeSort</span>(arr, left, right):
    <span class="keyword">if</span> left < right:
        mid = (left + right) / <span class="number">2</span>
        <span class="function">mergeSort</span>(arr, left, mid)
        <span class="function">mergeSort</span>(arr, mid+<span class="number">1</span>, right)
        <span class="function">merge</span>(arr, left, mid, right)

<span class="keyword">function</span> <span class="function">merge</span>(arr, left, mid, right):
    temp = []
    i = left, j = mid+<span class="number">1</span>
    <span class="keyword">while</span> i <= mid <span class="keyword">and</span> j <= right:
        <span class="keyword">if</span> arr[i] <= arr[j]:
            temp.append(arr[i]); i++
        <span class="keyword">else</span>:
            temp.append(arr[j]); j++
    temp.extend(arr[i:mid+<span class="number">1</span>])
    temp.extend(arr[j:right+<span class="number">1</span>])
    arr[left:right+<span class="number">1</span>] = temp`
  },
  heap: {
    name: '堆排序',
    category: '高级排序',
    timeComplexity: 'O(n log n)',
    spaceComplexity: 'O(1)',
    stable: false,
    inPlace: true,
    description: '利用堆这种数据结构进行排序。先将数组构建成最大堆，然后反复取出堆顶元素并调整堆。',
    tips: '原地排序，空间复杂度O(1)。建堆时间复杂度O(n)，取出元素调整O(n log n)。',
    code: `<span class="comment">// 堆排序</span>
<span class="keyword">function</span> <span class="function">heapSort</span>(arr):
    n = arr.length
    <span class="comment">// 构建最大堆</span>
    <span class="keyword">for</span> i = n/<span class="number">2</span>-<span class="number">1</span> <span class="keyword">downto</span> <span class="number">0</span>:
        <span class="function">heapify</span>(arr, n, i)
    <span class="comment">// 逐个取出堆顶</span>
    <span class="keyword">for</span> i = n-<span class="number">1</span> <span class="keyword">downto</span> <span class="number">1</span>:
        swap(arr[<span class="number">0</span>], arr[i])
        <span class="function">heapify</span>(arr, i, <span class="number">0</span>)

<span class="keyword">function</span> <span class="function">heapify</span>(arr, n, i):
    largest = i
    left = <span class="number">2</span>*i+<span class="number">1</span>
    right = <span class="number">2</span>*i+<span class="number">2</span>
    <span class="keyword">if</span> left < n <span class="keyword">and</span> arr[left] > arr[largest]:
        largest = left
    <span class="keyword">if</span> right < n <span class="keyword">and</span> arr[right] > arr[largest]:
        largest = right
    <span class="keyword">if</span> largest != i:
        swap(arr[i], arr[largest])
        <span class="function">heapify</span>(arr, n, largest)`
  },
  counting: {
    name: '计数排序',
    category: '线性非比较',
    timeComplexity: 'O(n + k)',
    spaceComplexity: 'O(k)',
    stable: true,
    inPlace: false,
    description: '通过统计每个元素出现的次数来排序。适合取值范围较小的整数排序，时间复杂度为线性。',
    tips: '需要知道元素取值范围k。k过大时空间消耗大。非比较排序，稳定。',
    code: `<span class="comment">// 计数排序</span>
<span class="keyword">function</span> <span class="function">countingSort</span>(arr):
    maxVal = <span class="function">max</span>(arr)
    count = array(maxVal+<span class="number">1</span>, <span class="number">0</span>)
    
    <span class="comment">// 统计每个元素出现次数</span>
    <span class="keyword">for</span> val <span class="keyword">in</span> arr:
        count[val]++
    
    <span class="comment">// 累加得到位置</span>
    <span class="keyword">for</span> i = <span class="number">1</span> <span class="keyword">to</span> maxVal:
        count[i] = count[i] + count[i-<span class="number">1</span>]
    
    <span class="comment">// 反向填充结果数组</span>
    result = array(arr.length)
    <span class="keyword">for</span> i = arr.length-<span class="number">1</span> <span class="keyword">downto</span> <span class="number">0</span>:
        result[count[arr[i]]-<span class="number">1</span>] = arr[i]
        count[arr[i]]--
    <span class="keyword">return</span> result`
  },
  radix: {
    name: '基数排序',
    category: '线性非比较',
    timeComplexity: 'O(n × k)',
    spaceComplexity: 'O(n + k)',
    stable: true,
    inPlace: false,
    description: '按位数从低到高逐位排序。使用计数排序作为稳定排序子过程。适合整数或字符串排序。',
    tips: 'k是数字的位数。非比较排序，稳定排序。适合整数范围大但位数不多的情况。',
    code: `<span class="comment">// 基数排序（LSD - 从低位开始）</span>
<span class="keyword">function</span> <span class="function">radixSort</span>(arr):
    maxVal = <span class="function">max</span>(arr)
    exp = <span class="number">1</span>
    <span class="keyword">while</span> maxVal / exp > <span class="number">0</span>:
        <span class="function">countingSortByDigit</span>(arr, exp)
        exp = exp * <span class="number">10</span>

<span class="keyword">function</span> <span class="function">countingSortByDigit</span>(arr, exp):
    n = arr.length
    output = array(n)
    count = array(<span class="number">10</span>, <span class="number">0</span>)
    
    <span class="keyword">for</span> i = <span class="number">0</span> <span class="keyword">to</span> n-<span class="number">1</span>:
        digit = (arr[i] / exp) % <span class="number">10</span>
        count[digit]++
    
    <span class="keyword">for</span> i = <span class="number">1</span> <span class="keyword">to</span> <span class="number">9</span>:
        count[i] = count[i] + count[i-<span class="number">1</span>]
    
    <span class="keyword">for</span> i = n-<span class="number">1</span> <span class="keyword">downto</span> <span class="number">0</span>:
        digit = (arr[i] / exp) % <span class="number">10</span>
        output[count[digit]-<span class="number">1</span>] = arr[i]
        count[digit]--
    
    arr = output`
  },
  bucket: {
    name: '桶排序',
    category: '线性非比较',
    timeComplexity: 'O(n + k)',
    spaceComplexity: 'O(n + k)',
    stable: true,
    inPlace: false,
    description: '将数据分散到有限数量的桶中，每个桶内分别排序，最后按顺序合并所有桶。适合均匀分布的数据。',
    tips: '桶数量和分配策略影响性能。数据分布均匀时效率高。平均时间复杂度O(n)。',
    code: `<span class="comment">// 桶排序</span>
<span class="keyword">function</span> <span class="function">bucketSort</span>(arr):
    bucketCount = <span class="number">10</span>  <span class="comment">// 桶数量</span>
    minVal = <span class="function">min</span>(arr)
    maxVal = <span class="function">max</span>(arr)
    range = (maxVal - minVal) / bucketCount + <span class="number">1</span>
    
    buckets = array(bucketCount)
    <span class="keyword">for</span> i = <span class="number">0</span> <span class="keyword">to</span> bucketCount-<span class="number">1</span>:
        buckets[i] = []
    
    <span class="comment">// 分配元素到桶</span>
    <span class="keyword">for</span> val <span class="keyword">in</span> arr:
        idx = <span class="function">floor</span>((val - minVal) / range)
        buckets[idx].append(val)
    
    <span class="comment">// 每个桶内排序</span>
    <span class="keyword">for</span> bucket <span class="keyword">in</span> buckets:
        <span class="function">insertionSort</span>(bucket)
    
    <span class="comment">// 合并桶</span>
    result = []
    <span class="keyword">for</span> bucket <span class="keyword">in</span> buckets:
        result.extend(bucket)
    <span class="keyword">return</span> result`
  }
};

// ========================================
// DOM 元素引用
// ========================================
const elements = {
  barsContainer: null,
  barsWrapper: null,
  playPauseBtn: null,
  playPauseText: null,
  stepBtn: null,
  resetBtn: null,
  generateBtn: null,
  themeToggle: null,
  dataType: null,
  manualInput: null,
  manualInputGroup: null,
  arraySize: null,
  arraySizeValue: null,
  speedSlider: null,
  speedValue: null,
  pivotType: null,
  currentAlgoName: null,
  currentAlgoCategory: null,
  comparisons: null,
  swaps: null,
  currentStep: null,
  timeComplexity: null,
  spaceComplexity: null,
  isStable: null,
  codeBlock: null,
  algoDescription: null,
  arrayValues: null,
  variables: null,
  tipModal: null,
  tipClose: null,
  copyCode: null
};

// ========================================
// 初始化
// ========================================
function init() {
  cacheElements();
  bindEvents();
  generateRandomArray();
  updateUI();
  loadTheme();
  updateSortStatus('ready');
}

function cacheElements() {
  elements.barsContainer = document.getElementById('barsContainer');
  elements.barsWrapper = document.getElementById('barsWrapper');
  elements.playPauseBtn = document.getElementById('playPauseBtn');
  elements.playPauseText = document.getElementById('playPauseText');
  elements.prevStepBtn = document.getElementById('prevStepBtn');
  elements.nextStepBtn = document.getElementById('nextStepBtn');
  elements.resetBtn = document.getElementById('resetBtn');
  elements.generateBtn = document.getElementById('generateBtn');
  elements.themeToggle = document.getElementById('themeToggle');
  elements.dataType = document.getElementById('dataType');
  elements.manualInput = document.getElementById('manualInput');
  elements.manualInputGroup = document.getElementById('manualInputGroup');
  elements.arraySize = document.getElementById('arraySize');
  elements.arraySizeValue = document.getElementById('arraySizeValue');
  elements.speedSlider = document.getElementById('speedSlider');
  elements.speedValue = document.getElementById('speedValue');
  elements.pivotType = document.getElementById('pivotType');
  elements.currentAlgoName = document.getElementById('currentAlgoName');
  elements.currentAlgoCategory = document.getElementById('currentAlgoCategory');
  elements.comparisons = document.getElementById('comparisons');
  elements.swaps = document.getElementById('swaps');
  elements.currentStepEl = document.getElementById('currentStep');
  elements.timeComplexity = document.getElementById('timeComplexity');
  elements.spaceComplexity = document.getElementById('spaceComplexity');
  elements.isStable = document.getElementById('isStable');
  elements.codeBlock = document.getElementById('codeBlock');
  elements.algoDescription = document.getElementById('algoDescription');
  elements.arrayValues = document.getElementById('arrayValues');
  elements.variables = document.getElementById('variables');
  elements.tipModal = document.getElementById('tipModal');
  elements.tipClose = document.getElementById('tipClose');
  elements.copyCode = document.getElementById('copyCode');
  elements.sortStatus = document.getElementById('sortStatus');
}

function bindEvents() {
  // 算法选择
  document.querySelectorAll('.algo-btn').forEach(btn => {
    btn.addEventListener('click', () => selectAlgorithm(btn.dataset.algo));
  });

  // 播放控制
  elements.playPauseBtn.addEventListener('click', togglePlayPause);
  elements.prevStepBtn.addEventListener('click', prevStep);
  elements.nextStepBtn.addEventListener('click', nextStep);
  elements.resetBtn.addEventListener('click', reset);
  elements.generateBtn.addEventListener('click', generateNewArray);

  // 主题切换
  elements.themeToggle.addEventListener('click', toggleTheme);

  // 数据配置
  elements.dataType.addEventListener('change', handleDataTypeChange);
  elements.arraySize.addEventListener('input', updateArraySize);
  elements.speedSlider.addEventListener('input', updateSpeed);

  // 速度预设
  document.querySelectorAll('.speed-btn').forEach(btn => {
    btn.addEventListener('click', () => setSpeed(parseInt(btn.dataset.speed)));
  });

  // 倍速按钮
  document.querySelectorAll('.speed-multiplier-btn').forEach(btn => {
    btn.addEventListener('click', () => setMultiplier(parseInt(btn.dataset.multiplier)));
  });

  // 进度条
  const progressBar = document.getElementById('progressBar');
  const progressFill = document.getElementById('progressFill');
  const progressHandle = document.getElementById('progressHandle');
  if (progressBar) {
    progressBar.addEventListener('click', handleProgressClick);
    let isDragging = false;
    progressBar.addEventListener('mousedown', (e) => {
      isDragging = true;
      handleProgressClick(e);
    });
    document.addEventListener('mousemove', (e) => {
      if (isDragging) handleProgressClick(e);
    });
    document.addEventListener('mouseup', () => {
      isDragging = false;
    });
  }

  // 键盘快捷键
  document.addEventListener('keydown', handleKeyboard);

  // 复制代码
  elements.copyCode.addEventListener('click', copyCodeToClipboard);

  // 关闭提示弹窗
  elements.tipClose.addEventListener('click', () => {
    elements.tipModal.classList.remove('active');
  });

  elements.tipModal.addEventListener('click', (e) => {
    if (e.target === elements.tipModal) {
      elements.tipModal.classList.remove('active');
    }
  });
}

// ========================================
// 主题管理
// ========================================
function loadTheme() {
  const theme = localStorage.getItem('sortviz-theme') || 'dark';
  document.body.classList.toggle('light-theme', theme === 'light');
}

function toggleTheme() {
  const isLight = document.body.classList.toggle('light-theme');
  localStorage.setItem('sortviz-theme', isLight ? 'light' : 'dark');
}

// ========================================
// 数组生成
// ========================================
function generateRandomArray() {
  const size = state.arraySize;
  state.array = [];
  
  for (let i = 0; i < size; i++) {
    state.array.push(Math.floor(Math.random() * 95) + 5);
  }
  
  state.originalArray = [...state.array];
}

function generateSortedArray() {
  state.array = [];
  for (let i = 1; i <= state.arraySize; i++) {
    state.array.push(i);
  }
  state.originalArray = [...state.array];
}

function generateReversedArray() {
  state.array = [];
  for (let i = state.arraySize; i >= 1; i--) {
    state.array.push(i);
  }
  state.originalArray = [...state.array];
}

function generateNearlySortedArray() {
  state.array = [];
  for (let i = 1; i <= state.arraySize; i++) {
    state.array.push(i);
  }
  // 随机交换几对元素
  const swaps = Math.floor(state.arraySize * 0.1);
  for (let i = 0; i < swaps; i++) {
    const idx1 = Math.floor(Math.random() * state.arraySize);
    const idx2 = Math.floor(Math.random() * state.arraySize);
    [state.array[idx1], state.array[idx2]] = [state.array[idx2], state.array[idx1]];
  }
  state.originalArray = [...state.array];
}

function generateFromManualInput() {
  const input = elements.manualInput.value.trim();
  if (!input) {
    showToast('请输入数组数据', true);
    return false;
  }
  
  const nums = input.split(/[,，\s]+/).map(s => parseInt(s.trim())).filter(n => !isNaN(n));
  
  if (nums.length < 2) {
    showToast('请输入至少2个有效数字', true);
    return false;
  }
  
  if (nums.some(n => n < 1 || n > 100)) {
    showToast('数组元素必须在1-100之间', true);
    return false;
  }
  
  state.array = nums;
  state.arraySize = nums.length;
  elements.arraySize.value = nums.length;
  elements.arraySizeValue.textContent = nums.length;
  state.originalArray = [...state.array];
  return true;
}

function generateNewArray() {
  reset();
  
  const dataType = elements.dataType.value;
  
  switch (dataType) {
    case 'random':
      generateRandomArray();
      break;
    case 'sorted':
      generateSortedArray();
      break;
    case 'reversed':
      generateReversedArray();
      break;
    case 'nearly':
      generateNearlySortedArray();
      break;
    case 'manual':
      if (!generateFromManualInput()) return;
      break;
  }
  
  renderBars();
  updateArrayDisplay();
  showToast('数据已生成');
}

function handleDataTypeChange() {
  const isManual = elements.dataType.value === 'manual';
  elements.manualInputGroup.style.display = isManual ? 'block' : 'none';
  
  if (isManual) {
    elements.manualInput.focus();
  }
}

function updateArraySize() {
  state.arraySize = parseInt(elements.arraySize.value);
  elements.arraySizeValue.textContent = state.arraySize;
}

function updateSpeed() {
  state.speed = parseInt(elements.speedSlider.value);
  elements.speedValue.textContent = state.speed;
  
  // 更新速度预设按钮状态
  document.querySelectorAll('.speed-btn').forEach(btn => {
    btn.classList.toggle('active', parseInt(btn.dataset.speed) === state.speed);
  });
}

function setSpeed(speed) {
  state.speed = speed;
  state.baseSpeed = speed;
  state.speed = Math.round(speed / state.multiplier);
  elements.speedSlider.value = state.baseSpeed;
  elements.speedValue.textContent = state.speed;
  
  document.querySelectorAll('.speed-btn').forEach(btn => {
    btn.classList.toggle('active', parseInt(btn.dataset.speed) === state.baseSpeed);
  });
}

// 设置播放倍速
function setMultiplier(multiplier) {
  state.multiplier = multiplier;
  state.speed = Math.round(state.baseSpeed / multiplier);
  elements.speedValue.textContent = state.speed;
  
  document.querySelectorAll('.speed-multiplier-btn').forEach(btn => {
    btn.classList.toggle('active', parseInt(btn.dataset.multiplier) === multiplier);
  });
}

// 处理进度条点击
function handleProgressClick(e) {
  const progressBar = document.getElementById('progressBar');
  if (!progressBar || state.steps.length === 0) return;
  
  const rect = progressBar.getBoundingClientRect();
  const percent = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
  const targetStep = Math.round(percent * (state.steps.length - 1));
  
  // 跳转到指定步骤
  jumpToStep(targetStep);
}

// 跳转到指定步骤
function jumpToStep(targetStep) {
  if (state.isPlaying) {
    pause();
  }
  
  if (state.steps.length === 0) return;
  
  // 重置状态
  state.currentStep = 0;
  state.comparisons = 0;
  state.swaps = 0;
  state.array = [...state.originalArray];
  
  // 重新生成步骤并执行到目标步骤
  state.steps = [];
  stepHistory = [];
  
  const arr = [...state.originalArray];
  switch (state.currentAlgorithm) {
    case 'bubble': bubbleSortSteps(arr); break;
    case 'selection': selectionSortSteps(arr); break;
    case 'insertion': insertionSortSteps(arr); break;
    case 'shell': shellSortSteps(arr); break;
    case 'quick': quickSortSteps(arr, 0, arr.length - 1); break;
    case 'merge': mergeSortSteps(arr, 0, arr.length - 1); break;
    case 'heap': heapSortSteps(arr); break;
    case 'counting': countingSortSteps(arr); break;
    case 'radix': radixSortSteps(arr); break;
    case 'bucket': bucketSortSteps(arr); break;
  }
  
  // 添加完成步骤
  state.steps.push({ type: 'sorted', indices: state.array.map((_, i) => i) });
  
  // 重置数组
  state.array = [...state.originalArray];
  
  // 执行到目标步骤
  for (let i = 0; i <= targetStep && i < state.steps.length; i++) {
    saveSnapshot();
    executeStepAnimation(state.steps[i]);
    state.currentStep++;
  }
  
  // 更新显示
  updateStats();
  updateProgress();
  renderBars();
  updateArrayDisplay();
  
  // 更新状态
  if (state.currentStep >= state.steps.length) {
    state.isComplete = true;
    updateSortStatus('completed');
  } else {
    state.isComplete = false;
    updateSortStatus('sorting');
  }
}

// 更新进度条
function updateProgress() {
  const progressFill = document.getElementById('progressFill');
  const progressHandle = document.getElementById('progressHandle');
  const progressStepStart = document.getElementById('progressStepStart');
  const progressStepEnd = document.getElementById('progressStepEnd');
  
  if (!progressFill || !progressHandle) return;
  
  const percent = state.steps.length > 0 ? (state.currentStep / state.steps.length) * 100 : 0;
  progressFill.style.width = `${percent}%`;
  progressHandle.style.left = `${percent}%`;
  
  if (progressStepStart) progressStepStart.textContent = 0;
  if (progressStepEnd) progressStepEnd.textContent = state.steps.length;
}

// 键盘快捷键处理
function handleKeyboard(e) {
  // 忽略输入框中的按键
  if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
  
  switch (e.code) {
    case 'Space':
      e.preventDefault();
      togglePlayPause();
      break;
    case 'ArrowLeft':
      e.preventDefault();
      prevStep();
      break;
    case 'ArrowRight':
      e.preventDefault();
      nextStep();
      break;
    case 'KeyR':
      e.preventDefault();
      reset();
      break;
  }
}

// ========================================
// 算法选择
// ========================================
function selectAlgorithm(algo) {
  if (state.isPlaying) {
    showToast('动画播放中，请先暂停', true);
    return;
  }
  
  state.currentAlgorithm = algo;
  
  // 更新按钮状态
  document.querySelectorAll('.algo-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.algo === algo);
  });
  
  updateUI();
  reset();
}

// ========================================
// UI 更新
// ========================================
function updateUI() {
  const algo = algorithms[state.currentAlgorithm];
  
  // 更新算法名称
  elements.currentAlgoName.textContent = algo.name;
  elements.currentAlgoCategory.textContent = algo.category;
  
  // 更新复杂度信息
  elements.timeComplexity.textContent = algo.timeComplexity;
  elements.spaceComplexity.textContent = algo.spaceComplexity;
  elements.isStable.textContent = algo.stable ? '是' : '否';
  
  // 更新代码
  elements.codeBlock.innerHTML = algo.code;
  
  // 更新算法简介
  elements.algoDescription.textContent = algo.description;
  
  // 更新参数配置显示
  const needsPivotConfig = state.currentAlgorithm === 'quick';
  document.getElementById('algoParamsCard').style.display = needsPivotConfig ? 'block' : 'none';
}

// ========================================
// 柱状图渲染
// ========================================
function renderBars() {
  elements.barsWrapper.innerHTML = '';
  const containerWidth = elements.barsContainer.clientWidth - 40;
  const barWidth = Math.max(8, Math.floor((containerWidth - (state.array.length - 1) * 2) / state.array.length));
  const maxValue = Math.max(...state.array);
  const containerHeight = elements.barsContainer.clientHeight - 80;
  
  state.array.forEach((value, index) => {
    const bar = document.createElement('div');
    bar.className = 'bar';
    bar.style.width = `${barWidth}px`;
    bar.style.height = `${containerHeight}px`;
    
    const barInner = document.createElement('div');
    barInner.className = 'bar-inner';
    barInner.style.height = `${(value / maxValue) * (containerHeight - 30)}px`;
    
    const barValue = document.createElement('span');
    barValue.className = 'bar-value';
    barValue.textContent = value;
    
    barInner.appendChild(barValue);
    
    const barIndex = document.createElement('span');
    barIndex.className = 'bar-index';
    barIndex.textContent = index + 1;
    
    bar.appendChild(barInner);
    bar.appendChild(barIndex);
    
    elements.barsWrapper.appendChild(bar);
  });
}

function updateBar(index, value, className = '') {
  const bars = elements.barsWrapper.children;
  if (index < 0 || index >= bars.length) return;
  
  const bar = bars[index];
  const barInner = bar.querySelector('.bar-inner');
  const barValue = bar.querySelector('.bar-value');
  
  bar.className = 'bar';
  if (className) {
    bar.classList.add(className);
  }
  
  barInner.style.height = `${(value / Math.max(...state.array)) * (elements.barsContainer.clientHeight - 110)}px`;
  barValue.textContent = value;
}

function highlightBars(indices, className) {
  const bars = elements.barsWrapper.children;
  indices.forEach(index => {
    if (index >= 0 && index < bars.length) {
      bars[index].classList.add(className);
    }
  });
}

function clearHighlights() {
  const bars = elements.barsWrapper.children;
  Array.from(bars).forEach(bar => {
    bar.classList.remove('comparing', 'swapping', 'pivot', 'boundary');
  });
}

function markAsSorted(indices) {
  const bars = elements.barsWrapper.children;
  indices.forEach(index => {
    if (index >= 0 && index < bars.length) {
      bars[index].classList.remove('comparing', 'swapping', 'pivot', 'boundary');
      bars[index].classList.add('sorted');
    }
  });
}

// ========================================
// 数组显示
// ========================================
function updateArrayDisplay() {
  elements.arrayValues.innerHTML = state.array.map((val, idx) => {
    return `<span class="array-value" data-index="${idx}">${val}</span>`;
  }).join('');
}

function highlightArrayValues(indices) {
  const values = elements.arrayValues.querySelectorAll('.array-value');
  values.forEach((el, idx) => {
    el.classList.toggle('highlight', indices.includes(idx));
  });
}

function updateVariablesDisplay(variables) {
  if (!variables || Object.keys(variables).length === 0) {
    elements.variables.innerHTML = '<div class="variable-hint" style="color:var(--text-secondary); font-size:0.9rem; font-style:italic;">等待变量更新...</div>';
    return;
  }
  elements.variables.innerHTML = Object.entries(variables).map(([key, value]) => {
    return `<div class="variable" style="display: flex; justify-content: space-between; padding: 4px 8px; background: rgba(168,85,247,0.1); border-radius: 4px; margin-bottom: 4px; font-family: monospace;">
      <span class="variable-name" style="color: var(--primary-light); font-weight: 600;">${key}</span>
      <span class="variable-value" style="color: var(--text-primary); font-weight: bold;">${value}</span>
    </div>`;
  }).join('');
}

// 更新排序步骤描述
let stepHistory = [];

function updateStepDescription(step) {
  const stepDescList = document.getElementById('stepDescList');
  if (!stepDescList) return;
  
  let stepText = '';
  
  switch (step.type) {
    case 'compare':
      stepText = `比较 ${step.indices[0] + 1} 号元素 (${state.array[step.indices[0]]}) 与 ${step.indices[1] + 1} 号元素 (${state.array[step.indices[1]]})`;
      if (state.array[step.indices[0]] > state.array[step.indices[1]]) {
        stepText += `，<span class="step-desc-action">需要交换</span>`;
      } else {
        stepText += `，<span class="step-desc-action">无需交换</span>`;
      }
      break;
      
    case 'swap':
      stepText = `<span class="step-desc-action">交换</span> ${step.i + 1} 号元素与 ${step.j + 1} 号元素的位置`;
      break;
      
    case 'move':
      stepText = `<span class="step-desc-action">移动</span> ${step.from + 1} 号元素 (${step.value}) 到位置 ${step.to + 1}`;
      break;
      
    case 'pivot':
      stepText = `选择 ${step.index + 1} 号元素 (<span class="step-desc-value">${state.array[step.index]}</span>) 作为<span class="step-desc-action">基准值</span>`;
      break;
      
    case 'boundary':
      stepText = `标记分区范围：<span class="step-desc-value">${step.indices[0] + 1}</span> ~ <span class="step-desc-value">${step.indices[step.indices.length - 1] + 1}</span>`;
      break;
      
    case 'sorted':
      stepText = `<span class="step-desc-action">已排序</span> ${step.indices.length} 个元素`;
      break;
      
    case 'merge':
      stepText = `<span class="step-desc-action">合并</span>区间 [${step.startIdx + 1}~${step.startIdx + step.indices.length}]`;
      break;
      
    case 'bucket':
      stepText = `将 <span class="step-desc-value">${step.value}</span> (位置 ${step.index + 1}) 分配到<span class="step-desc-action">桶${step.bucket}</span>`;
      break;
      
    default:
      stepText = `<span class="step-desc-action">执行中</span>`;
  }
  
  // 添加到历史
  stepHistory.push({
    number: stepHistory.length + 1,
    text: stepText
  });
  
  // 渲染所有步骤
  renderStepList();
}

// 渲染步骤列表
function renderStepList() {
  const stepDescList = document.getElementById('stepDescList');
  if (!stepDescList) return;
  
  if (stepHistory.length === 0) {
    stepDescList.innerHTML = '<div class="step-desc-hint">点击"开始"按钮开始排序</div>';
    return;
  }
  
  // 显示所有历史步骤
  let html = '';
  stepHistory.forEach((item, idx) => {
    const isLast = idx === stepHistory.length - 1;
    html += `<div class="step-item${isLast ? ' active' : ''}">
      <span class="step-number">步骤 ${item.number}:</span>
      <span class="step-text">${item.text}</span>
    </div>`;
  });
  
  stepDescList.innerHTML = html;
  
  // 滚动到底部显示最新步骤
  stepDescList.scrollTop = stepDescList.scrollHeight;
}

// 重置步骤历史
function resetStepHistory() {
  stepHistory = [];
  renderStepList();
}

// ========================================
// 播放控制
// ========================================
function togglePlayPause() {
  if (state.isComplete) {
    reset();
    return;
  }
  
  if (state.isPlaying) {
    pause();
  } else {
    play();
  }
}

function play() {
  if (state.steps.length === 0) {
    generateSteps();
  }
  
  state.isPlaying = true;
  state.isPaused = false;
  updateSortStatus('sorting');
  
  // 更新按钮状态
  elements.playPauseBtn.querySelector('.icon-play').style.display = 'none';
  elements.playPauseBtn.querySelector('.icon-pause').style.display = 'block';
  elements.playPauseText.textContent = '暂停';
  
  // 禁用配置按钮
  document.querySelectorAll('.algo-btn').forEach(btn => btn.disabled = true);
  elements.generateBtn.disabled = true;
  
  playNextStep();
}

function pause() {
  state.isPlaying = false;
  state.isPaused = true;
  updateSortStatus('paused');
  
  if (state.animationTimer) {
    clearTimeout(state.animationTimer);
    state.animationTimer = null;
  }
  
  elements.playPauseBtn.querySelector('.icon-play').style.display = 'block';
  elements.playPauseBtn.querySelector('.icon-pause').style.display = 'none';
  elements.playPauseText.textContent = '继续';
}

// 下一步
function nextStep() {
  if (state.isPlaying) {
    pause();
  }
  
  if (state.isComplete) {
    reset();
    return;
  }
  
  if (state.steps.length === 0) {
    generateSteps();
  }
  
  if (state.currentStep < state.steps.length) {
    // 保存当前状态快照
    saveSnapshot();
    executeStepAnimation(state.steps[state.currentStep]);
    state.currentStep++;
    updateStats();
    updateProgress(); // 更新进度条
    updateSortStatus(state.currentStep >= state.steps.length ? 'completed' : 'sorting');
  }
  
  if (state.currentStep >= state.steps.length) {
    complete();
  }
}

// 上一步
function prevStep() {
  if (state.isPlaying) {
    pause();
  }
  
  if (state.arraySnapshots.length === 0 || state.currentStep === 0) {
    return;
  }
  
  // 获取上一步的数组快照
  const prevSnapshot = state.arraySnapshots[state.currentStep - 1];
  if (!prevSnapshot) return;
  
  state.array = [...prevSnapshot.array];
  state.comparisons = prevSnapshot.comparisons;
  state.swaps = prevSnapshot.swaps;
  state.currentStep--;
  state.isComplete = false;
  
  // 清除所有高亮
  clearHighlights();
  
  // 重新渲染柱状图
  renderBars();
  updateStats();
  updateArrayDisplay();
  updateSortStatus('sorting');
}

// 更新排序状态显示
function updateSortStatus(status) {
  const statusText = elements.sortStatus.querySelector('.status-text');
  elements.sortStatus.className = 'sort-status ' + status;
  
  switch (status) {
    case 'ready':
    case 'waiting':
      statusText.textContent = '准备就绪';
      break;
    case 'sorting':
      statusText.textContent = '排序中';
      break;
    case 'paused':
      statusText.textContent = '已暂停';
      break;
    case 'completed':
      statusText.textContent = '排序完成';
      break;
  }
}

function reset() {
  state.isPlaying = false;
  state.isPaused = false;
  state.isComplete = false;
  state.currentStep = 0;
  state.steps = [];
  state.arraySnapshots = [];
  state.comparisons = 0;
  state.swaps = 0;
  updateSortStatus('ready');
  
  if (state.animationTimer) {
    clearTimeout(state.animationTimer);
    state.animationTimer = null;
  }
  
  // 恢复原始数组
  state.array = [...state.originalArray];
  
  // 更新UI
  elements.playPauseBtn.querySelector('.icon-play').style.display = 'block';
  elements.playPauseBtn.querySelector('.icon-pause').style.display = 'none';
  elements.playPauseText.textContent = '开始';
  
  document.querySelectorAll('.algo-btn').forEach(btn => btn.disabled = false);
  elements.generateBtn.disabled = false;
  
  // 重置柱状图
  renderBars();
  
  // 重置进度条
  updateProgress();
  
  // 重置步骤历史
  stepHistory = [];
  renderStepList();
  updateStats();
  updateArrayDisplay();
  updateVariablesDisplay({});
  resetStepHistory();
}

function complete() {
  state.isPlaying = false;
  state.isComplete = true;
  updateSortStatus('completed');
  
  if (state.animationTimer) {
    clearTimeout(state.animationTimer);
    state.animationTimer = null;
  }
  
  // 标记所有为已排序
  for (let i = 0; i < state.array.length; i++) {
    markAsSorted([i]);
  }
  
  elements.playPauseBtn.querySelector('.icon-play').style.display = 'block';
  elements.playPauseBtn.querySelector('.icon-pause').style.display = 'none';
  elements.playPauseText.textContent = '完成';
  
  document.querySelectorAll('.algo-btn').forEach(btn => btn.disabled = false);
  elements.generateBtn.disabled = false;
  
  // 更新进度条
  updateProgress();
  
  // 更新步骤描述为完成状态
  const stepDescList = document.getElementById('stepDescList');
  if (stepDescList) {
    stepDescList.innerHTML = `<div class="step-item">
      <span class="step-number">完成:</span>
      <span class="step-text"><span class="step-desc-action">排序完成</span> - 共比较 <span class="step-desc-value">${state.comparisons}</span> 次，交换 <span class="step-desc-value">${state.swaps}</span> 次</span>
    </div>`;
  }
  
  showToast(`${algorithms[state.currentAlgorithm].name} 完成！`);
}

function updateStats() {
  elements.comparisons.textContent = state.comparisons;
  elements.swaps.textContent = state.swaps;
  elements.currentStepEl.textContent = `${state.currentStep}/${state.steps.length}`;
}

// ========================================
// 步骤执行
// ========================================
function playNextStep() {
  if (!state.isPlaying || state.isPaused) return;
  
  if (state.currentStep >= state.steps.length) {
    complete();
    return;
  }
  
  // 保存当前状态快照
  saveSnapshot();
  
  executeStepAnimation(state.steps[state.currentStep]);
  state.currentStep++;
  updateStats();
  updateProgress(); // 更新进度条
  
  state.animationTimer = setTimeout(playNextStep, state.speed);
}

// 保存当前步骤的数组快照
function saveSnapshot() {
  state.arraySnapshots.push({
    array: [...state.array],
    comparisons: state.comparisons,
    swaps: state.swaps
  });
}

function executeStepAnimation(step) {
  clearHighlights();
  updateStepDescription(step); // 更新步骤描述
  
  switch (step.type) {
    case 'compare':
      state.comparisons += step.count || 1;
      highlightBars(step.indices, 'comparing');
      if (step.variables) updateVariablesDisplay(step.variables);
      highlightArrayValues(step.indices);
      break;
      
    case 'swap':
      state.swaps++;
      state.array[step.i] = step.val1;
      state.array[step.j] = step.val2;
      highlightBars([step.i, step.j], 'swapping');
      updateBar(step.i, step.val1);
      updateBar(step.j, step.val2);
      break;
      
    case 'move':
      state.array[step.to] = step.value;
      highlightBars([step.from, step.to], 'swapping');
      updateBar(step.to, step.value);
      break;
      
    case 'pivot':
      highlightBars([step.index], 'pivot');
      break;
      
    case 'boundary':
      highlightBars(step.indices, 'boundary');
      break;
      
    case 'sorted':
      markAsSorted(step.indices);
      break;
      
    case 'merge':
      step.indices.forEach((val, idx) => {
        const realIdx = step.startIdx + idx;
        state.array[realIdx] = val;
        updateBar(realIdx, val, 'swapping');
      });
      break;
      
    case 'bucket':
      updateBar(step.index, step.value, 'pivot');
      break;
  }
  
  updateArrayDisplay();
}

// ========================================
// 排序步骤生成
// ========================================
function generateSteps() {
  state.steps = [];
  state.comparisons = 0;
  state.swaps = 0;
  state.currentStep = 0;
  
  // 深拷贝当前数组
  const arr = [...state.originalArray];
  
  switch (state.currentAlgorithm) {
    case 'bubble':
      bubbleSortSteps(arr);
      break;
    case 'selection':
      selectionSortSteps(arr);
      break;
    case 'insertion':
      insertionSortSteps(arr);
      break;
    case 'shell':
      shellSortSteps(arr);
      break;
    case 'quick':
      quickSortSteps(arr, 0, arr.length - 1);
      break;
    case 'merge':
      mergeSortSteps(arr, 0, arr.length - 1);
      break;
    case 'heap':
      heapSortSteps(arr);
      break;
    case 'counting':
      countingSortSteps(arr);
      break;
    case 'radix':
      radixSortSteps(arr);
      break;
    case 'bucket':
      bucketSortSteps(arr);
      break;
  }
  
  // 添加最终排序完成步骤
  state.steps.push({
    type: 'sorted',
    indices: arr.map((_, i) => i)
  });
}

// 冒泡排序步骤
function bubbleSortSteps(arr) {
  const n = arr.length;
  for (let i = 0; i < n - 1; i++) {
    let swapped = false;
    for (let j = 0; j < n - i - 1; j++) {
      state.steps.push({
        type: 'compare',
        indices: [j, j + 1],
        variables: { i, j, [`arr[${j}]`]: arr[j], [`arr[${j+1}]`]: arr[j+1] }
      });
      
      if (arr[j] > arr[j + 1]) {
        state.steps.push({
          type: 'swap',
          i: j,
          j: j + 1,
          val1: arr[j + 1],
          val2: arr[j],
          variables: { i, j, [`arr[${j}]`]: arr[j+1], [`arr[${j+1}]`]: arr[j] }
        });
        [arr[j], arr[j + 1]] = [arr[j + 1], arr[j]];
        swapped = true;
      }
    }
    
    state.steps.push({
      type: 'sorted',
      indices: [n - i - 1]
    });
    
    if (!swapped) break;
  }
}

// 选择排序步骤
function selectionSortSteps(arr) {
  const n = arr.length;
  for (let i = 0; i < n - 1; i++) {
    let minIdx = i;
    
    for (let j = i + 1; j < n; j++) {
      state.steps.push({
        type: 'compare',
        indices: [minIdx, j],
        variables: { i, j, minIdx, [`arr[j]`]: arr[j], [`arr[minIdx]`]: arr[minIdx] }
      });
      
      if (arr[j] < arr[minIdx]) {
        minIdx = j;
      }
    }
    
    if (minIdx !== i) {
      state.steps.push({
        type: 'swap',
        i: i,
        j: minIdx,
        val1: arr[minIdx],
        val2: arr[i],
        variables: { i, minIdx, [`arr[i]`]: arr[minIdx], [`arr[minIdx]`]: arr[i] }
      });
      [arr[i], arr[minIdx]] = [arr[minIdx], arr[i]];
    }
    
    state.steps.push({
      type: 'sorted',
      indices: [i]
    });
  }
  
  state.steps.push({
    type: 'sorted',
    indices: [n - 1]
  });
}

// 插入排序步骤
function insertionSortSteps(arr) {
  const n = arr.length;
  for (let i = 1; i < n; i++) {
    const key = arr[i];
    let j = i - 1;
    
    state.steps.push({
      type: 'compare',
      indices: [j, i],
      variables: { i, key, j, [`arr[j]`]: arr[j] }
    });
    
    while (j >= 0 && arr[j] > key) {
      state.steps.push({
        type: 'move',
        from: j,
        to: j + 1,
        value: arr[j],
        variables: { i, key, j, [`arr[${j+1}]`]: arr[j] }
      });
      arr[j + 1] = arr[j];
      j--;
      
      if (j >= 0) {
        state.steps.push({
          type: 'compare',
          indices: [j, j + 1],
          variables: { i, key, j, [`arr[j]`]: arr[j] }
        });
      }
    }
    
    arr[j + 1] = key;
    if (j + 1 !== i) {
      state.steps.push({
        type: 'move',
        from: i,
        to: j + 1,
        value: key,
        variables: { i, key, j, [`arr[${j+1}]`]: key }
      });
    }
    
    state.steps.push({
      type: 'sorted',
      indices: [i]
    });
  }
}

// 希尔排序步骤
function shellSortSteps(arr) {
  const n = arr.length;
  let gap = Math.floor(n / 2);
  let step = 0;
  
  while (gap > 0) {
    for (let i = gap; i < n; i++) {
      const temp = arr[i];
      let j = i;
      
      while (j >= gap) {
        state.steps.push({
          type: 'compare',
          indices: [j - gap, j],
          variables: { gap, i, j }
        });
        
        if (arr[j - gap] > temp) {
          state.steps.push({
            type: 'move',
            from: j - gap,
            to: j,
            value: arr[j - gap]
          });
          arr[j] = arr[j - gap];
          j -= gap;
        } else {
          break;
        }
      }
      
      arr[j] = temp;
      if (j !== i) {
        state.steps.push({
          type: 'move',
          from: i,
          to: j,
          value: temp
        });
      }
    }
    
    gap = Math.floor(gap / 2);
  }
}

// 快速排序步骤
function quickSortSteps(arr, low, high) {
  if (low < high) {
    const pivotType = elements.pivotType.value;
    let pivotIndex;
    
    // 基准选择
    switch (pivotType) {
      case 'first':
        pivotIndex = low;
        break;
      case 'last':
        pivotIndex = high;
        break;
      case 'random':
        pivotIndex = low + Math.floor(Math.random() * (high - low + 1));
        break;
      case 'median':
        const mid = Math.floor((low + high) / 2);
        const candidates = [[arr[low], low], [arr[mid], mid], [arr[high], high]];
        candidates.sort((a, b) => a[0] - b[0]);
        pivotIndex = candidates[1][1];
        break;
    }
    
    // 将基准移到末尾
    if (pivotIndex !== high) {
      state.steps.push({
        type: 'swap',
        i: pivotIndex,
        j: high,
        val1: arr[high],
        val2: arr[pivotIndex],
        variables: { low, high, pivotIndex, [`arr[${high}]`]: arr[pivotIndex], [`arr[${pivotIndex}]`]: arr[high] }
      });
      [arr[pivotIndex], arr[high]] = [arr[high], arr[pivotIndex]];
      pivotIndex = high;
    }
    
    state.steps.push({
      type: 'pivot',
      index: pivotIndex,
      variables: { low, high, pivot: arr[pivotIndex] }
    });
    
    // 分区
    let i = low - 1;
    for (let j = low; j < high; j++) {
      state.steps.push({
        type: 'compare',
        indices: [j, pivotIndex],
        variables: { low, high, i, j, pivot: arr[pivotIndex], [`arr[${j}]`]: arr[j] }
      });
      
      if (arr[j] <= arr[pivotIndex]) {
        i++;
        if (i !== j) {
          state.steps.push({
            type: 'swap',
            i: i,
            j: j,
            val1: arr[j],
            val2: arr[i],
            variables: { i, j, [`arr[${i}]`]: arr[j], [`arr[${j}]`]: arr[i] }
          });
          [arr[i], arr[j]] = [arr[j], arr[i]];
        }
      }
    }
    
    // 将基准放到正确位置
    const pivotPos = i + 1;
    state.steps.push({
      type: 'swap',
      i: pivotPos,
      j: high,
      val1: arr[high],
      val2: arr[pivotPos],
      variables: { pivotPos, high, [`arr[${pivotPos}]`]: arr[high], [`arr[${high}]`]: arr[pivotPos] }
    });
    [arr[pivotPos], arr[high]] = [arr[high], arr[pivotPos]];
    
    state.steps.push({
      type: 'sorted',
      indices: [pivotPos]
    });
    
    // 递归排序左右两部分
    quickSortSteps(arr, low, pivotPos - 1);
    quickSortSteps(arr, pivotPos + 1, high);
  } else if (low === high) {
    state.steps.push({
      type: 'sorted',
      indices: [low]
    });
  }
}

// 归并排序步骤
function mergeSortSteps(arr, left, right) {
  if (left < right) {
    const mid = Math.floor((left + right) / 2);
    
    mergeSortSteps(arr, left, mid);
    mergeSortSteps(arr, mid + 1, right);
    mergeSteps(arr, left, mid, right);
  }
}

function mergeSteps(arr, left, mid, right) {
  const leftArr = arr.slice(left, mid + 1);
  const rightArr = arr.slice(mid + 1, right + 1);
  
  let i = 0, j = 0, k = left;
  
  state.steps.push({
    type: 'boundary',
    indices: Array.from({ length: right - left + 1 }, (_, idx) => left + idx)
  });
  
  while (i < leftArr.length && j < rightArr.length) {
    state.steps.push({
      type: 'compare',
      indices: [left + i, mid + 1 + j],
      variables: { left, mid, right, i, j, [`leftArr[${i}]`]: leftArr[i], [`rightArr[${j}]`]: rightArr[j] }
    });
    
    if (leftArr[i] <= rightArr[j]) {
      state.steps.push({
        type: 'move',
        from: left + i,
        to: k,
        value: leftArr[i],
        variables: { k, i, [`arr[${k}]`]: leftArr[i] }
      });
      arr[k] = leftArr[i];
      i++;
    } else {
      state.steps.push({
        type: 'move',
        from: mid + 1 + j,
        to: k,
        value: rightArr[j],
        variables: { k, j, [`arr[${k}]`]: rightArr[j] }
      });
      arr[k] = rightArr[j];
      j++;
    }
    k++;
  }
  
  while (i < leftArr.length) {
    state.steps.push({
      type: 'move',
      from: left + i,
      to: k,
      value: leftArr[i]
    });
    arr[k] = leftArr[i];
    i++;
    k++;
  }
  
  while (j < rightArr.length) {
    state.steps.push({
      type: 'move',
      from: mid + 1 + j,
      to: k,
      value: rightArr[j]
    });
    arr[k] = rightArr[j];
    j++;
    k++;
  }
  
  state.steps.push({
    type: 'merge',
    indices: arr.slice(left, right + 1),
    startIdx: left
  });
}

// 堆排序步骤
function heapSortSteps(arr) {
  const n = arr.length;
  
  // 构建最大堆
  for (let i = Math.floor(n / 2) - 1; i >= 0; i--) {
    heapifySteps(arr, n, i);
  }
  
  // 逐个取出堆顶
  for (let i = n - 1; i > 0; i--) {
    state.steps.push({
      type: 'swap',
      i: 0,
      j: i,
      val1: arr[i],
      val2: arr[0],
      variables: { i, [`arr[0]`]: arr[i], [`arr[${i}]`]: arr[0] }
    });
    [arr[0], arr[i]] = [arr[i], arr[0]];
    
    state.steps.push({
      type: 'sorted',
      indices: [i]
    });
    
    heapifySteps(arr, i, 0);
  }
  
  state.steps.push({
    type: 'sorted',
    indices: [0]
  });
}

function heapifySteps(arr, n, i) {
  let largest = i;
  const left = 2 * i + 1;
  const right = 2 * i + 2;
  
  if (left < n) {
    state.steps.push({
      type: 'compare',
      indices: [largest, left],
      variables: { i, largest, left, [`arr[${left}]`]: arr[left], [`arr[${largest}]`]: arr[largest] }
    });
    if (arr[left] > arr[largest]) {
      largest = left;
    }
  }
  
  if (right < n) {
    state.steps.push({
      type: 'compare',
      indices: [largest, right],
      variables: { i, largest, right, [`arr[${right}]`]: arr[right], [`arr[${largest}]`]: arr[largest] }
    });
    if (arr[right] > arr[largest]) {
      largest = right;
    }
  }
  
  if (largest !== i) {
    state.steps.push({
      type: 'swap',
      i: i,
      j: largest,
      val1: arr[largest],
      val2: arr[i],
      variables: { i, largest, [`arr[${i}]`]: arr[largest], [`arr[${largest}]`]: arr[i] }
    });
    [arr[i], arr[largest]] = [arr[largest], arr[i]];
    heapifySteps(arr, n, largest);
  }
}

// 计数排序步骤
function countingSortSteps(arr) {
  const max = Math.max(...arr);
  const min = Math.min(...arr);
  const range = max - min + 1;
  const count = new Array(range).fill(0);
  const output = new Array(arr.length);
  
  // 统计
  for (let i = 0; i < arr.length; i++) {
    count[arr[i] - min]++;
    state.steps.push({
      type: 'compare',
      indices: [i],
      variables: { val: arr[i], count: count[arr[i] - min] }
    });
  }
  
  // 累加
  for (let i = 1; i < range; i++) {
    count[i] += count[i - 1];
  }
  
  // 反向填充
  for (let i = arr.length - 1; i >= 0; i--) {
    output[count[arr[i] - min] - 1] = arr[i];
    count[arr[i] - min]--;
  }
  
  // 复制回原数组
  for (let i = 0; i < arr.length; i++) {
    state.steps.push({
      type: 'move',
      from: arr.length - 1 - i,
      to: i,
      value: output[i]
    });
    arr[i] = output[i];
  }
}

// 基数排序步骤
function radixSortSteps(arr) {
  const max = Math.max(...arr);
  let exp = 1;
  
  while (Math.floor(max / exp) > 0) {
    countingSortByDigit(arr, exp);
    exp *= 10;
  }
}

function countingSortByDigit(arr, exp) {
  const n = arr.length;
  const output = new Array(n);
  const count = new Array(10).fill(0);
  
  for (let i = 0; i < n; i++) {
    const digit = Math.floor(arr[i] / exp) % 10;
    count[digit]++;
    state.steps.push({
      type: 'compare',
      indices: [i],
      variables: { exp, digit, val: arr[i] }
    });
  }
  
  for (let i = 1; i < 10; i++) {
    count[i] += count[i - 1];
  }
  
  for (let i = n - 1; i >= 0; i--) {
    const digit = Math.floor(arr[i] / exp) % 10;
    output[count[digit] - 1] = arr[i];
    count[digit]--;
  }
  
  for (let i = 0; i < n; i++) {
    state.steps.push({
      type: 'move',
      from: i,
      to: i,
      value: output[i]
    });
    arr[i] = output[i];
  }
}

// 桶排序步骤
function bucketSortSteps(arr) {
  const bucketCount = Math.ceil(Math.sqrt(arr.length));
  const maxVal = Math.max(...arr);
  const minVal = Math.min(...arr);
  const range = (maxVal - minVal) / bucketCount + 1;
  const buckets = Array.from({ length: bucketCount }, () => []);
  
  // 分配到桶
  for (let i = 0; i < arr.length; i++) {
    const idx = Math.floor((arr[i] - minVal) / range);
    buckets[idx].push(arr[i]);
    state.steps.push({
      type: 'bucket',
      index: i,
      value: arr[i],
      bucket: idx
    });
  }
  
  // 每个桶内排序
  let index = 0;
  for (let i = 0; i < bucketCount; i++) {
    buckets[i].sort((a, b) => a - b);
    for (const val of buckets[i]) {
      state.steps.push({
        type: 'move',
        from: index,
        to: index,
        value: val
      });
      arr[index] = val;
      index++;
    }
  }
}

// ========================================
// 工具函数
// ========================================
function showToast(message, isError = false) {
  const toast = document.createElement('div');
  toast.className = `toast${isError ? ' error' : ''}`;
  toast.textContent = message;
  document.body.appendChild(toast);
  
  setTimeout(() => toast.classList.add('show'), 10);
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300);
  }, 2000);
}

function copyCodeToClipboard() {
  const codeText = elements.codeBlock.textContent;
  navigator.clipboard.writeText(codeText).then(() => {
    showToast('代码已复制到剪贴板');
  }).catch(() => {
    showToast('复制失败', true);
  });
}

// ========================================
// 窗口大小变化时重绘
// ========================================
window.addEventListener('resize', () => {
  if (!state.isPlaying) {
    renderBars();
  }
});

// ========================================
// 页面加载完成后初始化
// ========================================
document.addEventListener('DOMContentLoaded', init);
