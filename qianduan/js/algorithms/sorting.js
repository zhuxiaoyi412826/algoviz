/**
 * 排序算法可视化控制器
 */

class SortingVisualizer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.data = [];
        this.steps = [];
        this.currentStep = 0;
        this.isPlaying = false;
        this.speed = 300;
        this.animationTimer = null;
        this.onStepChange = null;
        this.onComplete = null;
        
        // 统计
        this.stats = {
            comparisons: 0,
            swaps: 0
        };
    }

    // 设置数据
    setData(arr) {
        this.data = [...arr];
        this.reset();
        this.render();
    }

    // 获取数据
    getData() {
        return [...this.data];
    }

    // 重置
    reset() {
        this.currentStep = 0;
        this.isPlaying = false;
        this.stats = { comparisons: 0, swaps: 0 };
        if (this.animationTimer) {
            clearTimeout(this.animationTimer);
            this.animationTimer = null;
        }
        this.updateStats();
    }

    // 渲染
    render(highlights = {}) {
        if (!this.container) return;
        
        const maxValue = Math.max(...this.data, 1);
        const maxHeight = 280;
        
        let html = '<div class="sorting-container">';
        html += '<div class="sorting-bars">';
        
        for (let i = 0; i < this.data.length; i++) {
            const height = (this.data[i] / maxValue) * maxHeight;
            const barClass = this.getBarClass(i, highlights);
            
            html += '<div class="sorting-bar">';
            html += `<div class="sorting-bar-value">${this.data[i]}</div>`;
            html += `<div class="sorting-bar-rect ${barClass}" style="height: ${height}px;"></div>`;
            html += '</div>';
        }
        
        html += '</div>'; // sorting-bars
        html += '</div>'; // sorting-container
        
        this.container.innerHTML = html;
    }

    // 获取柱状图类名
    getBarClass(index, highlights) {
        if (highlights.sorted && highlights.sorted.includes(index)) return 'highlight-sorted';
        if (highlights.pivot === index) return 'highlight-pivot';
        if (highlights.swapping && highlights.swapping.includes(index)) return 'highlight-swapping';
        if (highlights.comparing && highlights.comparing.includes(index)) return 'highlight-comparing';
        if (highlights.current === index) return 'highlight-current';
        return '';
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
            // 重新渲染到该步骤
            this.data = [...this.steps[0].data];
            for (let i = 0; i <= this.currentStep; i++) {
                if (this.steps[i]) {
                    this.data = [...this.steps[i].data];
                    if (this.steps[i].stats) {
                        this.stats = { ...this.steps[i].stats };
                    }
                }
            }
            this.render();
            this.updateStats();
        }
    }

    // 执行步骤
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

    // 执行单步
    executeStep(stepIndex) {
        const step = this.steps[stepIndex];
        if (step) {
            this.data = [...step.data];
            if (step.stats) {
                this.stats = { ...step.stats };
            }
            this.render(step.highlights || {});
            this.updateStats();
            
            // Dispatch event for watch panel update
            if (step.variables) {
                const event = new CustomEvent('algoVariableUpdate', { detail: step.variables });
                document.dispatchEvent(event);
            }
            
            if (this.onStepChange) this.onStepChange(step);
        }
    }

    // 记录步骤
    recordStep(data, highlights = {}, stats = null, variables = {}) {
        this.steps.push({
            data: [...data],
            highlights,
            stats: stats ? { ...stats } : { ...this.stats },
            variables: variables
        });
    }

    // 更新统计
    updateStats() {
        const progress = this.steps.length > 0 ? this.currentStep : 0;
        const total = this.steps.length;
        
        document.getElementById('statComparisons').textContent = this.stats.comparisons;
        document.getElementById('statSwaps').textContent = this.stats.swaps;
        document.getElementById('statCurrentStep').textContent = progress;
        document.getElementById('statTotalSteps').textContent = total;
    }

    // 设置速度
    setSpeed(newSpeed) {
        this.speed = newSpeed;
    }

    // 冒泡排序
    bubbleSort() {
        this.reset();
        const arr = [...this.data];
        const n = arr.length;
        
        for (let i = 0; i < n - 1; i++) {
            let swapped = false;
            
            for (let j = 0; j < n - i - 1; j++) {
                this.stats.comparisons++;
                
                // 记录比较步骤
                this.recordStep(arr, {
                    comparing: [j, j + 1],
                    current: j
                }, this.stats, {
                    'i': i,
                    'j': j,
                    [`arr[${j}]`]: arr[j],
                    [`arr[${j+1}]`]: arr[j + 1],
                    'swapped': swapped
                });
                
                if (arr[j] > arr[j + 1]) {
                    this.stats.swaps++;
                    [arr[j], arr[j + 1]] = [arr[j + 1], arr[j]];
                    swapped = true;
                    
                    // 记录交换步骤
                    this.recordStep(arr, {
                        swapping: [j, j + 1]
                    }, this.stats, {
                        'i': i,
                        'j': j,
                        [`arr[${j}]`]: arr[j],
                        [`arr[${j+1}]`]: arr[j + 1],
                        'swapped': swapped,
                        'Action': 'Swap!'
                    });
                }
            }
            
            // 标记已排序
            this.recordStep(arr, {
                sorted: [n - 1 - i]
            }, this.stats, {
                'i': i,
                'swapped': swapped
            });
            
            if (!swapped) break;
        }
        
        // 标记全部有序
        this.recordStep(arr, {
            sorted: arr.map((_, i) => i)
        }, this.stats, {
            'Status': 'Sorted'
        });
        
        this.stepForward();
    }

    // 选择排序
    selectionSort() {
        this.reset();
        const arr = [...this.data];
        const n = arr.length;
        
        for (let i = 0; i < n - 1; i++) {
            let minIdx = i;
            
            // 记录最小值位置
            this.recordStep(arr, {
                current: minIdx
            }, this.stats);
            
            for (let j = i + 1; j < n; j++) {
                this.stats.comparisons++;
                
                this.recordStep(arr, {
                    comparing: [minIdx, j],
                    current: minIdx
                }, this.stats);
                
                if (arr[j] < arr[minIdx]) {
                    minIdx = j;
                    this.recordStep(arr, {
                        current: minIdx
                    }, this.stats);
                }
            }
            
            if (minIdx !== i) {
                this.stats.swaps++;
                [arr[i], arr[minIdx]] = [arr[minIdx], arr[i]];
                
                this.recordStep(arr, {
                    swapping: [i, minIdx],
                    sorted: Array.from({ length: i }, (_, k) => k)
                }, this.stats);
            }
            
            this.recordStep(arr, {
                sorted: Array.from({ length: i + 1 }, (_, k) => k)
            }, this.stats);
        }
        
        this.recordStep(arr, {
            sorted: arr.map((_, i) => i)
        }, this.stats);
        
        this.stepForward();
    }

    // 插入排序
    insertionSort() {
        this.reset();
        const arr = [...this.data];
        const n = arr.length;
        
        this.recordStep(arr, {
            sorted: [0]
        }, this.stats);
        
        for (let i = 1; i < n; i++) {
            const key = arr[i];
            let j = i - 1;
            
            this.recordStep(arr, {
                current: i,
                sorted: Array.from({ length: i }, (_, k) => k)
            }, this.stats);
            
            while (j >= 0 && arr[j] > key) {
                this.stats.comparisons++;
                
                this.recordStep(arr, {
                    comparing: [j, j + 1],
                    current: i
                }, this.stats);
                
                arr[j + 1] = arr[j];
                this.stats.swaps++;
                
                this.recordStep(arr, {
                    swapping: [j, j + 1]
                }, this.stats);
                
                j--;
            }
            
            arr[j + 1] = key;
            
            this.recordStep(arr, {
                sorted: Array.from({ length: i + 1 }, (_, k) => k)
            }, this.stats);
        }
        
        this.recordStep(arr, {
            sorted: arr.map((_, i) => i)
        }, this.stats);
        
        this.stepForward();
    }

    // 快速排序
    quickSort() {
        this.reset();
        const arr = [...this.data];
        const n = arr.length;
        const sorted = new Set();
        
        const partition = (low, high) => {
            const pivot = arr[high];
            let i = low - 1;
            
            this.recordStep(arr, {
                pivot: high,
                current: high
            }, this.stats);
            
            for (let j = low; j < high; j++) {
                this.stats.comparisons++;
                
                this.recordStep(arr, {
                    comparing: [j, high],
                    pivot: high,
                    current: j
                }, this.stats);
                
                if (arr[j] < pivot) {
                    i++;
                    if (i !== j) {
                        this.stats.swaps++;
                        [arr[i], arr[j]] = [arr[j], arr[i]];
                        
                        this.recordStep(arr, {
                            swapping: [i, j],
                            pivot: high
                        }, this.stats);
                    }
                }
            }
            
            if (i + 1 !== high) {
                this.stats.swaps++;
                [arr[i + 1], arr[high]] = [arr[high], arr[i + 1]];
            }
            
            sorted.add(i + 1);
            
            this.recordStep(arr, {
                sorted: [...sorted],
                pivot: i + 1
            }, this.stats);
            
            return i + 1;
        };
        
        const quickSortRecursive = (low, high) => {
            if (low < high) {
                const pi = partition(low, high);
                quickSortRecursive(low, pi - 1);
                quickSortRecursive(pi + 1, high);
            } else if (low === high) {
                sorted.add(low);
                this.recordStep([...arr], {
                    sorted: [...sorted]
                }, this.stats);
            }
        };
        
        quickSortRecursive(0, n - 1);
        
        this.recordStep(arr, {
            sorted: arr.map((_, i) => i)
        }, this.stats);
        
        this.stepForward();
    }

    // 归并排序
    mergeSort() {
        this.reset();
        const arr = [...this.data];
        const sorted = new Set();
        
        const merge = (l, m, r) => {
            const left = arr.slice(l, m + 1);
            const right = arr.slice(m + 1, r + 1);
            
            let i = 0, j = 0, k = l;
            
            this.recordStep([...arr], {
                comparing: Array.from({ length: r - l + 1 }, (_, idx) => l + idx)
            }, this.stats);
            
            while (i < left.length && j < right.length) {
                this.stats.comparisons++;
                
                if (left[i] <= right[j]) {
                    arr[k] = left[i];
                    i++;
                } else {
                    arr[k] = right[j];
                    j++;
                }
                this.stats.swaps++;
                k++;
                
                this.recordStep([...arr], {
                    swapping: [k - 1]
                }, this.stats);
            }
            
            while (i < left.length) {
                arr[k] = left[i];
                this.stats.swaps++;
                k++;
                i++;
            }
            
            while (j < right.length) {
                arr[k] = right[j];
                this.stats.swaps++;
                k++;
                j++;
            }
            
            // 标记合并后的区域
            for (let idx = l; idx <= r; idx++) {
                sorted.add(idx);
            }
            
            this.recordStep([...arr], {
                sorted: [...sorted]
            }, this.stats);
        };
        
        const mergeSortRecursive = (l, r) => {
            if (l < r) {
                const m = Math.floor((l + r) / 2);
                mergeSortRecursive(l, m);
                mergeSortRecursive(m + 1, r);
                merge(l, m, r);
            }
        };
        
        mergeSortRecursive(0, arr.length - 1);
        
        this.recordStep(arr, {
            sorted: arr.map((_, i) => i)
        }, this.stats);
        
        this.stepForward();
    }

    // 堆排序
    heapSort() {
        this.reset();
        const arr = [...this.data];
        const n = arr.length;
        const sorted = new Set();
        
        const heapify = (size, i) => {
            let largest = i;
            const left = 2 * i + 1;
            const right = 2 * i + 2;
            
            if (left < size) {
                this.stats.comparisons++;
                if (arr[left] > arr[largest]) {
                    largest = left;
                }
            }
            
            if (right < size) {
                this.stats.comparisons++;
                if (arr[right] > arr[largest]) {
                    largest = right;
                }
            }
            
            if (largest !== i) {
                this.stats.swaps++;
                [arr[i], arr[largest]] = [arr[largest], arr[i]];
                
                this.recordStep([...arr], {
                    swapping: [i, largest],
                    sorted: [...sorted]
                }, this.stats);
                
                heapify(size, largest);
            }
        };
        
        // 构建最大堆
        for (let i = Math.floor(n / 2) - 1; i >= 0; i--) {
            heapify(n, i);
        }
        
        // 逐个提取
        for (let i = n - 1; i > 0; i--) {
            this.stats.swaps++;
            [arr[0], arr[i]] = [arr[i], arr[0]];
            
            this.recordStep([...arr], {
                swapping: [0, i]
            }, this.stats);
            
            sorted.add(i);
            
            this.recordStep([...arr], {
                sorted: [...sorted]
            }, this.stats);
            
            heapify(i, 0);
        }
        
        sorted.add(0);
        this.recordStep(arr, {
            sorted: [...sorted]
        }, this.stats);
        
        this.stepForward();
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

// 导出
window.SortingVisualizer = SortingVisualizer;
