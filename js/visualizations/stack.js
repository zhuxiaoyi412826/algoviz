/**
 * 栈可视化控制器
 */

class StackVisualizer extends AnimationController {
    constructor(containerId) {
        super();
        this.container = document.getElementById(containerId);
        this.items = [];
        this.maxSize = 10;
        this.render();
    }

    // 设置数据
    setData(arr) {
        this.items = [...arr].reverse(); // 反转使顶部在最后
        this.reset();
        this.render();
    }

    // 获取数据
    getData() {
        return [...this.items].reverse();
    }

    // 渲染栈
    render(highlights = {}) {
        if (!this.container) return;
        
        const maxH = 250;
        const itemH = 40;
        const itemW = 120;
        
        let html = '<div class="stack-container">';
        
        // 栈的可视化
        html += '<div class="stack-visual">';
        html += '<div class="stack-label">STACK</div>';
        html += '<div class="stack-frame">';
        
        if (this.items.length === 0) {
            html += '<div style="flex: 1; display: flex; align-items: center; justify-content: center; color: var(--text-muted);">空栈</div>';
        } else {
            for (let i = 0; i < this.items.length; i++) {
                const itemClass = this.getHighlightClass(i, highlights);
                html += `<div class="stack-item ${itemClass}">${this.items[i]}</div>`;
            }
        }
        
        html += '</div>'; // stack-frame
        
        // 栈顶指针
        if (this.items.length > 0 && highlights.top !== undefined) {
            html += `<div class="stack-pointer">TOP →</div>`;
        }
        
        html += '</div>'; // stack-visual
        
        // 信息面板
        html += '<div class="stack-info">';
        html += '<div class="stack-info-item">';
        html += `<span class="stack-info-label">栈大小</span>`;
        html += `<span class="stack-info-value">${this.items.length}</span>`;
        html += '</div>';
        html += '<div class="stack-info-item">';
        html += `<span class="stack-info-label">栈顶元素</span>`;
        html += `<span class="stack-info-value">${this.items.length > 0 ? this.items[this.items.length - 1] : 'N/A'}</span>`;
        html += '</div>';
        html += '<div class="stack-info-item">';
        html += `<span class="stack-info-label">状态</span>`;
        html += `<span class="stack-info-value" style="color: ${highlights.status === 'push' ? 'var(--success)' : highlights.status === 'pop' ? 'var(--danger)' : 'var(--text-secondary)'}">${highlights.status === 'push' ? '入栈中' : highlights.status === 'pop' ? '出栈中' : '就绪'}</span>`;
        html += '</div>';
        html += '</div>'; // stack-info
        
        html += '</div>'; // stack-container
        
        this.container.innerHTML = html;
    }

    // 获取高亮类名
    getHighlightClass(index, highlights) {
        const topIndex = this.items.length - 1;
        if (highlights.current === index && index === topIndex) {
            if (highlights.status === 'pop') return 'highlight-current';
            return 'highlight-current';
        }
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

    // 入栈
    push(value) {
        if (this.items.length >= this.maxSize) {
            alert('栈已满！');
            return;
        }
        
        this.recordStep(
            () => {
                this.items.push(value);
            },
            { current: this.items.length - 1, status: 'push' }
        );
        this.stepForward();
    }

    // 出栈
    pop() {
        if (this.items.length === 0) {
            alert('栈为空！');
            return;
        }
        
        const poppedValue = this.items[this.items.length - 1];
        this.recordStep(
            () => {
                this.items.pop();
            },
            { current: this.items.length - 1, status: 'pop' }
        );
        this.stepForward();
        return poppedValue;
    }

    // 获取栈顶
    peek() {
        return this.items.length > 0 ? this.items[this.items.length - 1] : null;
    }

    // 清空
    clear() {
        this.items = [];
        this.reset();
        this.render();
    }
}
