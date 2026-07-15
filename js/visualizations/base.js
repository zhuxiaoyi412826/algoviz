/**
 * AlgoViz - 数据结构与算法可视化基础类
 */

// 数据结构信息配置
const DS_INFO = {
    array: {
        title: '数组 Array',
        description: '数组是最基本的数据结构，用一段连续的内存空间存储相同类型的数据元素。',
        timeComplexity: { access: 'O(1)', search: 'O(n)', insert: 'O(n)', delete: 'O(n)' },
        spaceComplexity: 'O(n)',
        features: ['连续内存空间', '支持随机访问', '插入删除效率低', '缓存友好']
    },
    linkedlist: {
        title: '链表 Linked List',
        description: '链表是一种物理存储单元上非连续、非顺序的存储结构，通过指针连接各个节点。',
        timeComplexity: { access: 'O(n)', search: 'O(n)', insert: 'O(1)', delete: 'O(1)' },
        spaceComplexity: 'O(n)',
        features: ['非连续内存', '插入删除高效', '不支持随机访问', '需要额外存储指针']
    },
    stack: {
        title: '栈 Stack',
        description: '栈是一种后进先出(LIFO)的数据结构，只允许在表尾进行插入和删除操作。',
        timeComplexity: { push: 'O(1)', pop: 'O(1)', peek: 'O(1)' },
        spaceComplexity: 'O(n)',
        features: ['后进先出(LIFO)', '只允许一端操作', '入栈出栈高效', '用于函数调用等']
    },
    queue: {
        title: '队列 Queue',
        description: '队列是一种先进先出(FIFO)的数据结构，只允许在表尾插入，表头删除。',
        timeComplexity: { enqueue: 'O(1)', dequeue: 'O(1)', front: 'O(1)' },
        spaceComplexity: 'O(n)',
        features: ['先进先出(FIFO)', '双端操作受限', '入队出队高效', '用于任务调度等']
    },
    tree: {
        title: '二叉树 Binary Tree',
        description: '二叉树是每个节点最多有两个子树的树结构，通常子树被称为"左子树"和"右子树"。',
        timeComplexity: { search: 'O(n)', insert: 'O(n)', delete: 'O(n)' },
        spaceComplexity: 'O(n)',
        features: ['层次结构', '最多两个子节点', '搜索效率高(平衡树)', '中序遍历有序']
    },
    hash: {
        title: '哈希表 Hash Table',
        description: '哈希表根据关键码值(Key)直接进行访问的数据结构，通过哈希函数计算存储位置。',
        timeComplexity: { search: 'O(1)', insert: 'O(1)', delete: 'O(1)' },
        spaceComplexity: 'O(n)',
        features: ['键值对映射', '平均查找O(1)', '哈希冲突处理', '空间换时间']
    },
    graph: {
        title: '图 Graph',
        description: '图是由顶点的有穷非空集合和顶点之间边的集合组成的数据结构。',
        timeComplexity: { DFS: 'O(V+E)', BFS: 'O(V+E)' },
        spaceComplexity: 'O(V+E)',
        features: ['顶点和边', '有向/无向', '有权/无权', '邻接矩阵/表']
    },
    heap: {
        title: '堆 Heap',
        description: '堆是一种完全二叉树，分为最大堆和最小堆，堆顶元素分别为最大或最小值。',
        timeComplexity: { insert: 'O(log n)', delete: 'O(log n)', get: 'O(1)' },
        spaceComplexity: 'O(n)',
        features: ['完全二叉树', '堆属性(最大/最小)', '优先队列实现', '适合TopK问题']
    },
    trie: {
        title: '字典树 Trie',
        description: 'Trie（前缀树）是一种树形数据结构，用于高效地存储和检索字符串键值集合。',
        timeComplexity: { insert: 'O(m)', search: 'O(m)', prefix: 'O(m)' },
        spaceComplexity: 'O(ALPHABET_SIZE * m * n)',
        features: ['前缀匹配高效', '字符串检索快', '占用空间较大', '适合自动补全']
    },
    unionfind: {
        title: '并查集 Union-Find',
        description: '并查集是一种用于处理不相交集合合并与查询的数据结构。',
        timeComplexity: { find: 'O(α(n))', union: 'O(α(n))' },
        spaceComplexity: 'O(n)',
        features: ['路径压缩', '按秩合并', '近似O(1)', '连通分量查询']
    },
    segmenttree: {
        title: '线段树 Segment Tree',
        description: '线段树是一种用于区间查询和更新的二叉树结构。',
        timeComplexity: { query: 'O(log n)', update: 'O(log n)' },
        spaceComplexity: 'O(4n)',
        features: ['区间查询', '单点更新', '区间更新', '适合大规模数据']
    },
    dp: {
        title: '动态规划 DP',
        description: '动态规划是一种通过把原问题分解为相对简单的子问题来求解的方法。',
        timeComplexity: { depends: 'O(n²) - O(n*m)' },
        spaceComplexity: 'O(n*m)',
        features: ['最优子结构', '重叠子问题', '状态转移方程', '空间优化']
    },
    kmp: {
        title: 'KMP 字符串匹配',
        description: 'KMP算法是一种高效的字符串匹配算法，利用已匹配的信息避免重复比较。',
        timeComplexity: { preprocess: 'O(m)', search: 'O(n+m)' },
        spaceComplexity: 'O(m)',
        features: ['避免回溯', '前缀函数', '高效匹配', '线性时间复杂度']
    }
};

// 算法信息配置
const ALGO_INFO = {
    bubbleSort: {
        title: '冒泡排序 Bubble Sort',
        description: '通过相邻元素的比较和交换，把最大的元素逐步"冒泡"到数组末端。',
        timeComplexity: { best: 'O(n)', average: 'O(n²)', worst: 'O(n²)' },
        spaceComplexity: 'O(1)',
        coreIdea: '重复遍历数组，比较相邻元素并在必要时交换位置，直到没有更多交换为止。'
    },
    selectionSort: {
        title: '选择排序 Selection Sort',
        description: '在未排序序列中找到最小(或最大)元素，放到排序序列的起始位置。',
        timeComplexity: { best: 'O(n²)', average: 'O(n²)', worst: 'O(n²)' },
        spaceComplexity: 'O(1)',
        coreIdea: '每一轮在未排序部分选择最小值，与未排序部分的第一个元素交换位置。'
    },
    insertionSort: {
        title: '插入排序 Insertion Sort',
        description: '将数组分为已排序和未排序两部分，逐个将未排序元素插入到已排序部分的正确位置。',
        timeComplexity: { best: 'O(n)', average: 'O(n²)', worst: 'O(n²)' },
        spaceComplexity: 'O(1)',
        coreIdea: '从第二个元素开始，将每个元素插入到前面已排序序列中的正确位置。'
    },
    quickSort: {
        title: '快速排序 Quick Sort',
        description: '采用分治策略，选择一个基准元素，将数组分为两部分，递归排序。',
        timeComplexity: { best: 'O(n log n)', average: 'O(n log n)', worst: 'O(n²)' },
        spaceComplexity: 'O(log n)',
        coreIdea: '选择基准元素，将小于基准的放左边，大于基准的放右边，递归处理左右两部分。'
    },
    mergeSort: {
        title: '归并排序 Merge Sort',
        description: '采用分治策略，将数组递归分成两半，分别排序后合并。',
        timeComplexity: { best: 'O(n log n)', average: 'O(n log n)', worst: 'O(n log n)' },
        spaceComplexity: 'O(n)',
        coreIdea: '递归地将数组分成两半，分别排序后使用额外空间合并成一个有序数组。'
    },
    heapSort: {
        title: '堆排序 Heap Sort',
        description: '利用堆这种数据结构设计的排序算法，先建立最大堆，然后逐个取出堆顶。',
        timeComplexity: { best: 'O(n log n)', average: 'O(n log n)', worst: 'O(n log n)' },
        spaceComplexity: 'O(1)',
        coreIdea: '将数组构建成最大堆，交换堆顶和末尾元素，然后调整堆，重复此过程。'
    },
    binarySearch: {
        title: '二分查找 Binary Search',
        description: '在有序数组中，通过每次将搜索范围缩小一半来查找目标元素。',
        timeComplexity: { best: 'O(1)', average: 'O(log n)', worst: 'O(log n)' },
        spaceComplexity: 'O(1)',
        coreIdea: '比较中间元素与目标值，若相等则找到，若目标较小则在左半部分继续查找。'
    },
    treeInorder: {
        title: '中序遍历 Inorder Traversal',
        description: '先遍历左子树，再访问根节点，最后遍历右子树。',
        timeComplexity: 'O(n)',
        spaceComplexity: 'O(h)',
        coreIdea: '对于二叉搜索树，中序遍历会得到有序的节点序列。'
    },
    treePreorder: {
        title: '前序遍历 Preorder Traversal',
        description: '先访问根节点，再遍历左子树，最后遍历右子树。',
        timeComplexity: 'O(n)',
        spaceComplexity: 'O(h)',
        coreIdea: '常用于复制二叉树，或获取前缀表达式。'
    },
    treePostorder: {
        title: '后序遍历 Postorder Traversal',
        description: '先遍历左子树，再遍历右子树，最后访问根节点。',
        timeComplexity: 'O(n)',
        spaceComplexity: 'O(h)',
        coreIdea: '常用于删除二叉树，或获取后缀表达式。'
    },
    graphDFS: {
        title: '深度优先搜索 DFS',
        description: '沿着一条路径走到底，然后回溯尝试其他路径。',
        timeComplexity: 'O(V+E)',
        spaceComplexity: 'O(V)',
        coreIdea: '使用栈或递归，沿着深度方向尽可能深入，然后回溯。'
    },
    graphBFS: {
        title: '广度优先搜索 BFS',
        description: '从起点开始，一层一层地遍历图中所有节点。',
        timeComplexity: 'O(V+E)',
        spaceComplexity: 'O(V)',
        coreIdea: '使用队列，先访问起点的所有邻居，再访问邻居的邻居。'
    },
    dijkstra: {
        title: 'Dijkstra 最短路径',
        description: '计算从源点到其他所有顶点的最短路径长度。',
        timeComplexity: 'O((V+E) log V)',
        spaceComplexity: 'O(V)',
        coreIdea: '贪心策略，每次选择当前距离最小的未访问顶点，直到所有顶点都被访问。'
    }
};

// 伪代码模板
const PSEUDOCODES = {
    array: [
        'function search(arr, target):',
        '    for i from 0 to length(arr) - 1:',
        '        if arr[i] == target:',
        '            return i  // Found at index i',
        '    return -1  // Not found',
        '',
        'function insert(arr, index, value):',
        '    for i from length(arr) - 1 down to index:',
        '        arr[i + 1] = arr[i]  // Shift elements',
        '    arr[index] = value  // O(n) time complexity',
        '',
        'function delete(arr, index):',
        '    if index < 0 or index >= length(arr):',
        '        return "Index out of bounds"',
        '    for i from index to length(arr) - 2:',
        '        arr[i] = arr[i + 1]  // Shift elements',
        '    arr.pop()  // Remove last element',
        '',
        'function expand(arr):',
        '    newCapacity = length(arr) * 2',
        '    newArray = new Array(newCapacity)',
        '    for i from 0 to length(arr) - 1:',
        '        newArray[i] = arr[i]',
        '    return newArray  // Double capacity'
    ],
    linkedlist: [
        'class Node:',
        '    data, next',
        '',
        'function insertAtHead(head, value):',
        '    newNode = Node(value)',
        '    newNode.next = head  // Link new node',
        '    return newNode  // O(1) insertion',
        '',
        'function search(head, target):',
        '    current = head',
        '    while current != null:',
        '        if current.data == target:',
        '            return current  // O(n) search',
        '        current = current.next',
        '    return null  // Not found'
    ],
    stack: [
        'class Stack:',
        '    items = []',
        '',
        'function push(stack, value):',
        '    stack.items.append(value)  // O(1)',
        '',
        'function pop(stack):',
        '    if isEmpty(stack):',
        '        return "Stack Underflow"',
        '    return stack.items.pop()  // O(1)',
        '',
        'function peek(stack):',
        '    return stack.items[-1]  // Top element'
    ],
    queue: [
        'class Queue:',
        '    items = []',
        '',
        'function enqueue(queue, value):',
        '    queue.items.append(value)  // O(1)',
        '',
        'function dequeue(queue):',
        '    if isEmpty(queue):',
        '        return "Queue Underflow"',
        '    return queue.items.pop(0)  // O(n)',
        '',
        'function front(queue):',
        '    return queue.items[0]  // First element'
    ],
    bubbleSort: [
        'procedure bubbleSort(arr):',
        '    n = length(arr)',
        '    for i from 0 to n - 1:',
        '        swapped = false',
        '        for j from 0 to n - i - 2:',
        '            if arr[j] > arr[j + 1]:',
        '                swap(arr[j], arr[j + 1])',
        '                swapped = true',
        '        if not swapped:',
        '            break  // Already sorted'
    ],
    selectionSort: [
        'procedure selectionSort(arr):',
        '    n = length(arr)',
        '    for i from 0 to n - 1:',
        '        minIdx = i',
        '        for j from i + 1 to n - 1:',
        '            if arr[j] < arr[minIdx]:',
        '                minIdx = j',
        '        swap(arr[i], arr[minIdx])'
    ],
    insertionSort: [
        'procedure insertionSort(arr):',
        '    for i from 1 to length(arr) - 1:',
        '        key = arr[i]',
        '        j = i - 1',
        '        while j >= 0 and arr[j] > key:',
        '            arr[j + 1] = arr[j]',
        '            j = j - 1',
        '        arr[j + 1] = key'
    ],
    quickSort: [
        'procedure quickSort(arr, low, high):',
        '    if low < high:',
        '        pi = partition(arr, low, high)',
        '        quickSort(arr, low, pi - 1)',
        '        quickSort(arr, pi + 1, high)',
        '',
        'procedure partition(arr, low, high):',
        '    pivot = arr[high]',
        '    i = low - 1',
        '    for j from low to high - 1:',
        '        if arr[j] <= pivot:',
        '            i = i + 1',
        '            swap(arr[i], arr[j])',
        '    swap(arr[i + 1], arr[high])',
        '    return i + 1'
    ],
    mergeSort: [
        'procedure mergeSort(arr, left, right):',
        '    if left < right:',
        '        mid = (left + right) / 2',
        '        mergeSort(arr, left, mid)',
        '        mergeSort(arr, mid + 1, right)',
        '        merge(arr, left, mid, right)',
        '',
        'procedure merge(arr, l, m, r):',
        '    // Merge two sorted subarrays'
    ],
    heapSort: [
        'procedure heapSort(arr):',
        '    n = length(arr)',
        '    // Build max heap',
        '    for i from n/2 - 1 down to 0:',
        '        heapify(arr, n, i)',
        '    // Extract elements',
        '    for i from n - 1 down to 0:',
        '        swap(arr[0], arr[i])',
        '        heapify(arr, i, 0)',
        '',
        'procedure heapify(arr, n, i):',
        '    largest = i',
        '    left = 2*i + 1',
        '    right = 2*i + 2'
    ],
    binarySearch: [
        'procedure binarySearch(arr, target):',
        '    left = 0',
        '    right = length(arr) - 1',
        '    while left <= right:',
        '        mid = (left + right) / 2',
        '        if arr[mid] == target:',
        '            return mid  // Found!',
        '        else if arr[mid] < target:',
        '            left = mid + 1',
        '        else:',
        '            right = mid - 1',
        '    return -1  // Not found'
    ],
    trie: [
        'class TrieNode:',
        '    children = {}  // Map<char, TrieNode>',
        '    isEndOfWord = false',
        '',
        'procedure insert(word):',
        '    node = root',
        '    for each char in word:',
        '        if char not in node.children:',
        '            node.children[char] = TrieNode()',
        '        node = node.children[char]',
        '    node.isEndOfWord = true  // Mark end',
        '',
        'procedure search(word):',
        '    node = root',
        '    for each char in word:',
        '        if char not in node.children:',
        '            return false',
        '        node = node.children[char]',
        '    return node.isEndOfWord',
        '',
        'procedure startsWith(prefix):',
        '    node = root',
        '    for each char in prefix:',
        '        if char not in node.children:',
        '            return false',
        '        node = node.children[char]',
        '    return true  // Prefix exists'
    ],
    unionfind: [
        'class UnionFind:',
        '    parent[], rank[]',
        '',
        'procedure find(x):',
        '    if parent[x] != x:',
        '        parent[x] = find(parent[x])  // Path compression',
        '    return parent[x]',
        '',
        'procedure union(x, y):',
        '    rootX = find(x)',
        '    rootY = find(y)',
        '    if rootX != rootY:',
        '        if rank[rootX] > rank[rootY]:',
        '            parent[rootY] = rootX',
        '        elif rank[rootX] < rank[rootY]:',
        '            parent[rootX] = rootY',
        '        else:',
        '            parent[rootY] = rootX',
        '            rank[rootX] += 1  // Increase rank'
    ],
    segmenttree: [
        'class SegmentTree:',
        '    tree[]  // Segment tree array',
        '',
        'procedure build(arr, node, l, r):',
        '    if l == r:',
        '        tree[node] = arr[l]',
        '    else:',
        '        mid = (l + r) / 2',
        '        build(arr, 2*node, l, mid)',
        '        build(arr, 2*node+1, mid+1, r)',
        '        tree[node] = tree[2*node] + tree[2*node+1]',
        '',
        'procedure query(node, l, r, ql, qr):',
        '    if ql <= l and r <= qr:',
        '        return tree[node]  // Fully inside',
        '    if r < ql or l > qr:',
        '        return 0  // Outside range',
        '    mid = (l + r) / 2',
        '    return query(2*node, l, mid, ql, qr)',
        '           + query(2*node+1, mid+1, r, ql, qr)'
    ],
    dp: [
        'procedure LCS(s1, s2):',
        '    n = length(s1), m = length(s2)',
        '    dp[n+1][m+1] initialized to 0',
        '',
        '    for i from 1 to n:',
        '        for j from 1 to m:',
        '            if s1[i-1] == s2[j-1]:',
        '                dp[i][j] = dp[i-1][j-1] + 1',
        '            else:',
        '                dp[i][j] = max(dp[i-1][j], dp[i][j-1])',
        '',
        '    return dp[n][m]  // Length of LCS'
    ],
    kmp: [
        'procedure computeLPS(pat, m, lps):',
        '    length = 0  // Length of previous prefix',
        '    lps[0] = 0',
        '    i = 1',
        '    while i < m:',
        '        if pat[i] == pat[length]:',
        '            length += 1',
        '            lps[i] = length',
        '            i += 1',
        '        else:',
        '            if length != 0:',
        '                length = lps[length-1]',
        '            else:',
        '                lps[i] = 0',
        '                i += 1',
        '',
        'procedure KMPSearch(pat, txt):',
        '    n = length(txt), m = length(pat)',
        '    lps[] = computeLPS(pat)',
        '    i = 0, j = 0  // Indexes for txt and pat',
        '    while i < n:',
        '        if pat[j] == txt[i]:',
        '            i += 1, j += 1',
        '            if j == m:',
        '                return i - j  // Pattern found!',
        '        else:',
        '            if j != 0:',
        '                j = lps[j-1]',
        '            else:',
        '                i += 1'
    ]
};

/**
 * 动画控制器基类
 */
class AnimationController {
    constructor() {
        this.steps = [];
        this.currentStep = 0;
        this.isPlaying = false;
        this.speed = 500;
        this.animationTimer = null;
        this.onStepChange = null;
        this.onComplete = null;
    }

    // 添加动画步骤
    addStep(action) {
        this.steps.push(action);
    }

    // 重置
    reset() {
        this.currentStep = 0;
        this.isPlaying = false;
        if (this.animationTimer) {
            clearTimeout(this.animationTimer);
            this.animationTimer = null;
        }
    }

    // 播放
    play() {
        if (this.currentStep >= this.steps.length) {
            this.reset();
        }
        this.isPlaying = true;
        this.executeNextStep();
    }

    // 暂停
    pause() {
        this.isPlaying = false;
        if (this.animationTimer) {
            clearTimeout(this.animationTimer);
            this.animationTimer = null;
        }
    }

    // 下一步
    stepForward() {
        if (this.currentStep < this.steps.length) {
            this.executeStep(this.currentStep);
            this.currentStep++;
            if (this.currentStep >= this.steps.length && this.onComplete) {
                this.onComplete();
            }
        }
    }

    // 上一步
    stepBack() {
        if (this.currentStep > 0) {
            this.currentStep--;
            this.revertToStep(this.currentStep);
        }
    }

    // 执行下一步
    executeNextStep() {
        if (!this.isPlaying || this.currentStep >= this.steps.length) {
            this.isPlaying = false;
            if (this.onComplete) this.onComplete();
            return;
        }

        this.executeStep(this.currentStep);
        this.currentStep++;

        this.animationTimer = setTimeout(() => {
            this.executeNextStep();
        }, this.speed);
    }

    // 执行步骤（子类实现）
    executeStep(stepIndex) {
        // 子类实现
    }

    // 回退到指定步骤（子类实现）
    revertToStep(stepIndex) {
        // 子类实现
    }

    // 设置速度
    setSpeed(newSpeed) {
        this.speed = newSpeed;
    }

    // 获取进度
    getProgress() {
        return {
            current: this.currentStep,
            total: this.steps.length,
            percentage: this.steps.length > 0 ? (this.currentStep / this.steps.length) * 100 : 0
        };
    }
}

/**
 * 工具函数
 */
const Utils = {
    // 随机数组
    randomArray(size, min = 1, max = 99) {
        const arr = [];
        for (let i = 0; i < size; i++) {
            arr.push(Math.floor(Math.random() * (max - min + 1)) + min);
        }
        return arr;
    },

    // 生成随机整数
    randomInt(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    },

    // 等待
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    },

    // 解析输入数据
    parseInput(inputStr) {
        return inputStr.split(/[,，\s]+/).map(s => parseInt(s.trim())).filter(n => !isNaN(n));
    },

    // 交换数组元素
    swap(arr, i, j) {
        const temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    },

    // 创建节点类
    createTreeNode(value) {
        return {
            value,
            left: null,
            right: null
        };
    }
};
