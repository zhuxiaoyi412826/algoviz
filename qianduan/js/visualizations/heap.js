/**
 * 堆可视化控制器
 */

class HeapVisualizer extends AnimationController {
    constructor(containerId, type = 'max') {
        super();
        this.container = document.getElementById(containerId);
        this.heap = [];
        this.type = type; // 'max' 或 'min'
        this.nodeRadius = 22;
        this.render();
    }

    // 从数组创建堆
    fromArray(arr) {
        this.heap = [...arr];
        this.buildHeap();
        this.reset();
        this.render();
    }

    // 构建堆
    buildHeap() {
        const n = this.heap.length;
        
        // 从最后一个非叶子节点开始
        for (let i = Math.floor(n / 2) - 1; i >= 0; i--) {
            this.heapifyDown(i);
        }
    }

    // 堆化（向下）
    heapifyDown(i) {
        const n = this.heap.length;
        let largest = i;
        const left = 2 * i + 1;
        const right = 2 * i + 2;
        
        if (left < n) {
            if (this.compare(this.heap[left], this.heap[largest]) > 0) {
                largest = left;
            }
        }
        
        if (right < n) {
            if (this.compare(this.heap[right], this.heap[largest]) > 0) {
                largest = right;
            }
        }
        
        if (largest !== i) {
            [this.heap[i], this.heap[largest]] = [this.heap[largest], this.heap[i]];
            this.heapifyDown(largest);
        }
    }

    // 堆化（向上）
    heapifyUp(i) {
        while (i > 0) {
            const parent = Math.floor((i - 1) / 2);
            
            if (this.compare(this.heap[i], this.heap[parent]) > 0) {
                [this.heap[i], this.heap[parent]] = [this.heap[parent], this.heap[i]];
                i = parent;
            } else {
                break;
            }
        }
    }

    // 比较函数
    compare(a, b) {
        return this.type === 'max' ? a - b : b - a;
    }

    // 计算节点位置
    calculatePositions() {
        const positions = {};
        const n = this.heap.length;
        
        if (n === 0) return positions;
        
        const width = this.container.offsetWidth || 600;
        const height = this.container.offsetHeight || 400;
        
        const getNodePositions = (index, level, start, end) => {
            if (index >= n) return;
            
            const y = level * 60 + 40;
            const x = (start + end) / 2;
            
            positions[index] = { x, y, value: this.heap[index] };
            
            const left = 2 * index + 1;
            const right = 2 * index + 2;
            const mid = (start + end) / 2;
            
            getNodePositions(left, level + 1, start, mid);
            getNodePositions(right, level + 1, mid, end);
        };
        
        getNodePositions(0, 0, 0, width);
        
        return positions;
    }

    // 渲染堆
    render(highlights = {}) {
        if (!this.container) return;
        
        const width = this.container.offsetWidth || 600;
        const height = this.container.offsetHeight || 400;
        
        let html = `<div class="heap-container"><svg class="heap-svg" viewBox="0 0 ${width} ${height}">`;
        
        if (this.heap.length === 0) {
            html += `<text x="${width/2}" y="${height/2}" text-anchor="middle" fill="var(--text-muted)">空堆 (Empty Heap)</text>`;
        } else {
            const positions = this.calculatePositions();
            
            // 绘制边
            for (let i = 0; i < this.heap.length; i++) {
                const pos = positions[i];
                if (!pos) continue;
                
                const left = 2 * i + 1;
                const right = 2 * i + 2;
                
                if (left < this.heap.length && positions[left]) {
                    const isHighlight = highlights.edges && highlights.edges.some(
                        e => (e[0] === i && e[1] === left)
                    );
                    html += `<line class="heap-edge ${isHighlight ? 'highlight' : ''}" 
                            x1="${pos.x}" y1="${pos.y}" 
                            x2="${positions[left].x}" y2="${positions[left].y}"/>`;
                }
                
                if (right < this.heap.length && positions[right]) {
                    const isHighlight = highlights.edges && highlights.edges.some(
                        e => (e[0] === i && e[1] === right)
                    );
                    html += `<line class="heap-edge ${isHighlight ? 'highlight' : ''}" 
                            x1="${pos.x}" y1="${pos.y}" 
                            x2="${positions[right].x}" y2="${positions[right].y}"/>`;
                }
            }
            
            // 绘制节点
            for (let i = 0; i < this.heap.length; i++) {
                const pos = positions[i];
                if (!pos) continue;
                
                const isCurrent = highlights.current === i;
                const isSwapping = highlights.swapping && highlights.swapping.includes(i);
                const isVisited = highlights.visited && highlights.visited.includes(i);
                const isSorted = highlights.sorted && highlights.sorted.includes(i);
                
                let nodeClass = 'heap-node';
                if (isCurrent) nodeClass += ' highlight-current';
                else if (isSwapping) nodeClass += ' highlight-swapping';
                else if (isVisited) nodeClass += ' highlight-visited';
                
                html += `<g class="${nodeClass}" transform="translate(${pos.x}, ${pos.y})">`;
                html += `<circle class="heap-node-circle" r="${this.nodeRadius}"/>`;
                html += `<text class="heap-node-text">${this.heap[i]}</text>`;
                html += `<text class="heap-index" y="${this.nodeRadius + 15}">[${i}]</text>`;
                html += '</g>';
            }
        }
        
        html += '</svg>';
        
        // 堆信息
        html += '<div style="display: flex; gap: 2rem; justify-content: center; margin-top: 1rem;">';
        html += `<span style="color: var(--text-secondary);">类型: <strong style="color: var(--heap-color);">${this.type === 'max' ? '最大堆' : '最小堆'}</strong></span>`;
        html += `<span style="color: var(--text-secondary);">元素数: <strong style="color: var(--heap-color);">${this.heap.length}</strong></span>`;
        html += `<span style="color: var(--text-secondary);">堆顶: <strong style="color: var(--highlight-current);">${this.heap.length > 0 ? this.heap[0] : 'N/A'}</strong></span>`;
        html += '</div>';
        
        html += '</div>'; // heap-container
        
        this.container.innerHTML = html;
    }

    // 执行步骤
    executeStep(stepIndex) {
        const step = this.steps[stepIndex];
        if (step) {
            step.action();
            this.render(step.highlights || {});
            if (this.onStepChange) this.onStepChange(step);
        }
    }

    // 记录步骤
    recordStep(action, highlights = {}) {
        this.steps.push({ action, highlights });
    }

    // 插入
    insert(value) {
        this.recordStep(
            () => {
                this.heap.push(value);
                this.heapifyUp(this.heap.length - 1);
            },
            { current: this.heap.length - 1 }
        );
        this.stepForward();
    }

    // 提取堆顶
    extractRoot() {
        if (this.heap.length === 0) return null;
        
        const root = this.heap[0];
        
        this.recordStep(
            () => {},
            { current: 0 }
        );
        
        this.recordStep(
            () => {
                this.heap[0] = this.heap[this.heap.length - 1];
                this.heap.pop();
                this.heapifyDown(0);
            },
            { current: 0, swapping: [0, this.heap.length - 1] }
        );
        
        this.stepForward();
        return root;
    }

    // 堆排序
    heapSort() {
        this.reset();
        this.steps = [];
        
        const arr = [...this.heap];
        const sorted = [];
        
        // 构建最大堆
        for (let i = Math.floor(arr.length / 2) - 1; i >= 0; i--) {
            this.heapifyDownIndex(i, arr.length);
        }
        
        // 逐个提取
        for (let i = arr.length - 1; i > 0; i--) {
            // 记录交换
            this.recordStep(
                () => {},
                { 
                    current: 0, 
                    swapping: [0, i],
                    sorted: [...sorted],
                    edges: [[0, i]]
                }
            );
            
            // 交换堆顶和末尾
            [arr[0], arr[i]] = [arr[i], arr[0]];
            sorted.unshift(arr[i]);
            
            this.recordStep(
                () => {},
                { 
                    sorted: [...sorted],
                    edges: [[0, i]]
                }
            );
            
            // 堆化
            this.heapifyDownIndex(0, i);
        }
        
        sorted.unshift(arr[0]);
        this.recordStep(
            () => {
                this.heap = arr;
            },
            { sorted }
        );
        
        this.stepForward();
    }

    // 指定范围的堆化
    heapifyDownIndex(i, n) {
        let largest = i;
        const left = 2 * i + 1;
        const right = 2 * i + 2;
        
        if (left < n && this.heap[left] > this.heap[largest]) {
            largest = left;
        }
        
        if (right < n && this.heap[right] > this.heap[largest]) {
            largest = right;
        }
        
        if (largest !== i) {
            [this.heap[i], this.heap[largest]] = [this.heap[largest], this.heap[i]];
            this.heapifyDownIndex(largest, n);
        }
    }

    // 获取堆数组
    getHeap() {
        return [...this.heap];
    }
}
