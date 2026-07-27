/**
 * 队列可视化控制器
 */

class QueueVisualizer extends AnimationController {
    constructor(containerId) {
        super();
        this.container = document.getElementById(containerId);
        this.items = [];
        this.maxSize = 8;
        this.render();
    }

    // 设置数据
    setData(arr) {
        this.items = [...arr];
        this.reset();
        this.render();
    }

    // 获取数据
    getData() {
        return [...this.items];
    }

    // 渲染队列
    render(highlights = {}) {
        if (!this.container) return;
        
        let html = '<div class="queue-container">';
        
        // 队列可视化
        html += '<div class="queue-visual">';
        html += '<div class="queue-frame">';
        html += '<div class="queue-items">';
        
        if (this.items.length === 0) {
            html += '<div style="flex: 1; display: flex; align-items: center; justify-content: center; color: var(--text-muted);">空队列</div>';
        } else {
            for (let i = 0; i < this.items.length; i++) {
                const itemClass = this.getHighlightClass(i, highlights);
                html += `<div class="queue-item ${itemClass}">${this.items[i]}</div>`;
            }
        }
        
        html += '</div>'; // queue-items
        html += '</div>'; // queue-frame
        
        // 指针标签
        if (this.items.length > 0) {
            html += '<div class="queue-pointer front">← front</div>';
            html += '<div class="queue-pointer rear">rear →</div>';
        }
        
        html += '</div>'; // queue-visual
        
        // 标签
        html += '<div class="queue-labels">';
        html += '<span class="queue-label">← 队首 (Front)</span>';
        html += '<span class="queue-label">队尾 (Rear) →</span>';
        html += '</div>';
        
        // 信息面板
        html += '<div class="stack-info" style="width: 100%;">';
        html += '<div class="stack-info-item">';
        html += `<span class="stack-info-label">队列长度</span>`;
        html += `<span class="stack-info-value">${this.items.length}</span>`;
        html += '</div>';
        html += '<div class="stack-info-item">';
        html += `<span class="stack-info-label">队首元素</span>`;
        html += `<span class="stack-info-value">${this.items.length > 0 ? this.items[0] : 'N/A'}</span>`;
        html += '</div>';
        html += '<div class="stack-info-item">';
        html += `<span class="stack-info-label">队尾元素</span>`;
        html += `<span class="stack-info-value">${this.items.length > 0 ? this.items[this.items.length - 1] : 'N/A'}</span>`;
        html += '</div>';
        html += '<div class="stack-info-item">';
        html += `<span class="stack-info-label">状态</span>`;
        html += `<span class="stack-info-value" style="color: ${highlights.status === 'enqueue' ? 'var(--success)' : highlights.status === 'dequeue' ? 'var(--danger)' : 'var(--text-secondary)'}">${highlights.status === 'enqueue' ? '入队中' : highlights.status === 'dequeue' ? '出队中' : '就绪'}</span>`;
        html += '</div>';
        html += '</div>'; // stack-info
        
        html += '</div>'; // queue-container
        
        this.container.innerHTML = html;
    }

    // 获取高亮类名
    getHighlightClass(index, highlights) {
        if (highlights.front && index === 0) return 'highlight-current';
        if (highlights.rear && index === this.items.length - 1) return 'highlight-current';
        return '';
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

    // 入队
    enqueue(value) {
        if (this.items.length >= this.maxSize) {
            alert('队列已满！');
            return;
        }
        
        this.recordStep(
            () => {
                this.items.push(value);
            },
            { rear: true, status: 'enqueue' }
        );
        this.stepForward();
    }

    // 出队
    dequeue() {
        if (this.items.length === 0) {
            alert('队列为空！');
            return null;
        }
        
        this.recordStep(
            () => {
                this.items.shift();
            },
            { front: true, status: 'dequeue' }
        );
        this.stepForward();
        return this.items[0];
    }

    // 获取队首
    front() {
        return this.items.length > 0 ? this.items[0] : null;
    }

    // 获取队尾
    rear() {
        return this.items.length > 0 ? this.items[this.items.length - 1] : null;
    }

    // 清空
    clear() {
        this.items = [];
        this.reset();
        this.render();
    }
}
