/**
 * 查找算法可视化控制器
 */

class SearchingVisualizer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.data = [];
        this.sortedData = [];
        this.steps = [];
        this.currentStep = 0;
        this.isPlaying = false;
        this.speed = 300;
        this.animationTimer = null;
        this.target = null;
        this.onStepChange = null;
        this.onComplete = null;
        
        // 统计
        this.stats = {
            comparisons: 0,
            iterations: 0
        };
    }

    // 设置数据
    setData(arr) {
        this.data = [...arr];
        this.sortedData = [...arr].sort((a, b) => a - b);
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
        this.stats = { comparisons: 0, iterations: 0 };
        if (this.animationTimer) {
            clearTimeout(this.animationTimer);
            this.animationTimer = null;
        }
        this.updateStats();
    }

    // 渲染
    render(highlights = {}) {
        if (!this.container) return;
        
        const arr = this.sortedData;
        
        let html = '<div class="searching-container">';
        html += '<div class="searching-array">';
        
        for (let i = 0; i < arr.length; i++) {
            const itemClass = this.getItemClass(i, highlights);
            const isInRange = highlights.range && i >= highlights.range[0] && i <= highlights.range[1];
            
            html += `<div class="searching-item ${itemClass}" data-index="${i}" ${isInRange ? 'style="border: 2px solid var(--primary-light);"' : ''}>`;
            html += `${arr[i]}`;
            
            // 添加指针
            if (highlights.mid === i) {
                html += '<span class="searching-pointer">mid</span>';
            }
            if (highlights.left === i && i !== highlights.mid) {
                html += '<span class="searching-pointer">L</span>';
            }
            if (highlights.right === i && i !== highlights.mid) {
                html += '<span class="searching-pointer">R</span>';
            }
            
            html += '</div>';
        }
        
        html += '</div>'; // searching-array
        
        // 结果信息
        if (highlights.result !== undefined) {
            html += '<div class="searching-info">';
            if (highlights.result === -1) {
                html += `<div class="searching-result not-found">未找到目标值 ${this.target}</div>`;
            } else {
                html += `<div class="searching-result found">找到目标值 ${this.target}，位于索引 ${highlights.result}</div>`;
            }
            html += '</div>';
        }
        
        // 搜索范围信息
        if (highlights.range) {
            html += `<div style="text-align: center; padding: 0.5rem; color: var(--text-secondary); font-size: 0.9rem;">`;
            html += `搜索范围: [${highlights.range[0]}, ${highlights.range[1]}]`;
            html += '</div>';
        }
        
        html += '</div>'; // searching-container
        
        this.container.innerHTML = html;
    }

    // 获取项类名
    getItemClass(index, highlights) {
        if (highlights.result !== undefined) {
            if (highlights.result === index) return 'highlight-found';
            return 'highlight-not-found';
        }
        if (highlights.mid === index) return 'highlight-current';
        if (highlights.searching === index) return 'highlight-searching';
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
            const step = this.steps[this.currentStep];
            if (step) {
                this.render(step.highlights || {});
                if (step.stats) {
                    this.stats = { ...step.stats };
                }
                this.updateStats();
            }
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
            this.render(step.highlights || {});
            if (step.stats) {
                this.stats = { ...step.stats };
            }
            this.updateStats();
            if (this.onStepChange) this.onStepChange(step);
        }
    }

    // 记录步骤
    recordStep(highlights = {}, stats = null) {
        this.steps.push({
            highlights,
            stats: stats ? { ...stats } : { ...this.stats }
        });
    }

    // 更新统计
    updateStats() {
        const progress = this.steps.length > 0 ? this.currentStep : 0;
        const total = this.steps.length;
        
        document.getElementById('statComparisons').textContent = this.stats.comparisons;
        document.getElementById('statSwaps').textContent = this.stats.iterations;
        document.getElementById('statCurrentStep').textContent = progress;
        document.getElementById('statTotalSteps').textContent = total;
    }

    // 设置速度
    setSpeed(newSpeed) {
        this.speed = newSpeed;
    }

    // 二分查找
    binarySearch(target) {
        this.reset();
        this.target = target;
        const arr = this.sortedData;
        let left = 0;
        let right = arr.length - 1;
        
        this.recordStep({
            range: [left, right]
        }, this.stats);
        
        while (left <= right) {
            this.stats.iterations++;
            
            const mid = Math.floor((left + right) / 2);
            this.stats.comparisons++;
            
            this.recordStep({
                left,
                right,
                mid,
                searching: mid,
                range: [left, right]
            }, this.stats);
            
            if (arr[mid] === target) {
                this.recordStep({
                    result: mid,
                    mid
                }, this.stats);
                break;
            } else if (arr[mid] < target) {
                left = mid + 1;
                this.recordStep({
                    range: [left, right]
                }, this.stats);
            } else {
                right = mid - 1;
                this.recordStep({
                    range: [left, right]
                }, this.stats);
            }
        }
        
        if (left > right) {
            this.recordStep({
                result: -1
            }, this.stats);
        }
        
        this.stepForward();
    }

    // 线性查找
    linearSearch(target) {
        this.reset();
        this.target = target;
        const arr = this.sortedData;
        
        for (let i = 0; i < arr.length; i++) {
            this.stats.iterations++;
            this.stats.comparisons++;
            
            this.recordStep({
                searching: i,
                range: [0, i]
            }, this.stats);
            
            if (arr[i] === target) {
                this.recordStep({
                    result: i,
                    searching: i
                }, this.stats);
                break;
            }
        }
        
        if (this.currentStep >= this.steps.length) {
            this.recordStep({
                result: -1
            }, this.stats);
        }
        
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
window.SearchingVisualizer = SearchingVisualizer;
