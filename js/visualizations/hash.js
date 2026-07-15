/**
 * 哈希表可视化控制器
 */

class HashTableVisualizer extends AnimationController {
    constructor(containerId, size = 8) {
        super();
        this.container = document.getElementById(containerId);
        this.size = size;
        this.table = new Array(size).fill(null).map(() => []);
        this.render();
    }

    // 简单的哈希函数
    hash(key) {
        return key % this.size;
    }

    // 设置数据
    setData(pairs) {
        this.table = new Array(this.size).fill(null).map(() => []);
        
        if (Array.isArray(pairs)) {
            pairs.forEach(([key, value]) => {
                const index = this.hash(key);
                this.table[index].push({ key, value });
            });
        }
        
        this.reset();
        this.render();
    }

    // 渲染哈希表
    render(highlights = {}) {
        if (!this.container) return;
        
        let html = '<div class="hash-container">';
        
        // 哈希函数显示
        if (highlights.hashCalc) {
            html += '<div class="hash-function-info">';
            html += `<span class="hash-calc">h(key) = key mod ${this.size}</span>`;
            if (highlights.searching !== undefined) {
                html += ` → <span class="hash-calc-highlight">${highlights.searching} mod ${this.size} = ${this.hash(highlights.searching)}</span>`;
            }
            html += '</div>';
        }
        
        // 哈希表
        html += '<div class="hash-table">';
        
        for (let i = 0; i < this.size; i++) {
            const bucket = this.table[i];
            const isHighlight = highlights.current === i;
            
            html += `<div class="hash-bucket ${isHighlight ? 'highlight' : ''}">`;
            html += `<div class="hash-index">${i}</div>`;
            html += '<div class="hash-values">';
            
            if (bucket.length === 0) {
                html += '<span class="hash-empty">empty</span>';
            } else {
                bucket.forEach(item => {
                    const isItemHighlight = highlights.current === i && 
                        highlights.highlightItem === item.key;
                    html += `<span class="hash-value ${isItemHighlight ? 'highlight' : ''}">${item.key}: ${item.value}</span>`;
                });
            }
            
            html += '</div>'; // hash-values
            html += '</div>'; // hash-bucket
        }
        
        html += '</div>'; // hash-table
        
        // 冲突信息
        if (highlights.collision) {
            html += `<div style="text-align: center; padding: 1rem; background: rgba(251, 191, 36, 0.1); border-radius: 0.5rem; margin-top: 1rem; color: var(--warning);">`;
            html += `⚠️ 哈希冲突！位置 ${highlights.collision.index} 已存在元素，将使用链地址法解决`;
            html += '</div>';
        }
        
        html += '</div>'; // hash-container
        
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
    insert(key, value) {
        const index = this.hash(key);
        const bucket = this.table[index];
        
        // 检查是否已存在
        for (let i = 0; i < bucket.length; i++) {
            if (bucket[i].key === key) {
                this.recordStep(
                    () => {},
                    { current: index, highlightItem: key, collision: { index } }
                );
                this.stepForward();
                return;
            }
        }
        
        // 记录查找冲突的步骤
        for (let i = 0; i < this.size; i++) {
            if (this.table[i].length > 0 && i !== index) {
                this.recordStep(
                    () => {},
                    { current: i, hashCalc: true, collision: { index, probing: i } }
                );
            }
        }
        
        this.recordStep(
            () => {
                this.table[index].push({ key, value });
            },
            { current: index, collision: { index } }
        );
        
        this.stepForward();
    }

    // 搜索
    search(key) {
        this.reset();
        this.steps = [];
        
        const index = this.hash(key);
        
        // 先显示哈希计算
        this.recordStep(
            () => {},
            { hashCalc: true, searching: key }
        );
        
        // 检查所有桶
        for (let i = 0; i < this.size; i++) {
            if (i === index) {
                const bucket = this.table[i];
                
                if (bucket.length === 0) {
                    this.recordStep(
                        () => {},
                        { current: i, found: false, hashCalc: true, searching: key }
                    );
                } else {
                    // 检查桶中的元素
                    for (let j = 0; j < bucket.length; j++) {
                        if (bucket[j].key === key) {
                            this.recordStep(
                                () => {},
                                { current: i, highlightItem: key, found: true, hashCalc: true, searching: key }
                            );
                            break;
                        }
                    }
                }
            } else {
                this.recordStep(
                    () => {},
                    { current: i, found: false, hashCalc: true, searching: key }
                );
            }
        }
        
        this.stepForward();
    }

    // 删除
    delete(key) {
        const index = this.hash(key);
        const bucket = this.table[index];
        
        for (let i = 0; i < bucket.length; i++) {
            if (bucket[i].key === key) {
                this.recordStep(
                    () => {
                        this.table[index].splice(i, 1);
                    },
                    { current: index, highlightItem: key }
                );
                this.stepForward();
                return;
            }
        }
        
        this.recordStep(
            () => {},
            { found: false }
        );
        this.stepForward();
    }

    // 获取统计信息
    getStats() {
        let totalElements = 0;
        let nonEmptyBuckets = 0;
        let maxChainLength = 0;
        
        for (let i = 0; i < this.size; i++) {
            const bucket = this.table[i];
            totalElements += bucket.length;
            if (bucket.length > 0) nonEmptyBuckets++;
            maxChainLength = Math.max(maxChainLength, bucket.length);
        }
        
        return {
            totalElements,
            nonEmptyBuckets,
            maxChainLength,
            loadFactor: (totalElements / this.size).toFixed(2)
        };
    }
}
