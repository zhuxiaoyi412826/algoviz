/**
 * 数组可视化控制器
 */

class ArrayVisualizer extends AnimationController {
    constructor(containerId) {
        super();
        this.container = document.getElementById(containerId);
        this.data = [];
        this.maxValue = 100;
        this.render();
    }

    // 设置数据
    setData(arr) {
        this.data = [...arr];
        this.maxValue = Math.max(...arr, 100);
        this.reset();
        this.render();
    }

    // 获取数据
    getData() {
        return [...this.data];
    }

    // 渲染数组
    render(highlights = {}) {
        if (!this.container) return;
        
        const maxH = 300;
        const barWidth = Math.max(30, Math.min(50, (this.container.offsetWidth - 100) / this.data.length - 4));

        let html = '<div class="array-container">';
        html += '<div class="array-indices">';
        for (let i = 0; i < this.data.length; i++) {
            html += `<div class="array-index">${i}</div>`;
        }
        html += '</div>';
        html += '<div class="array-elements">';
        
        for (let i = 0; i < this.data.length; i++) {
            const height = (this.data[i] / this.maxValue) * maxH;
            const highlightClass = this.getHighlightClass(i, highlights);
            html += `<div class="array-element ${highlightClass}" style="height: ${height + 20}px; width: ${barWidth}px;">`;
            html += `${this.data[i]}`;
            
            // 添加指针
            if (highlights.pointer && highlights.pointer === i) {
                html += '<span class="array-pointer top">ptr</span>';
            }
            if (highlights.pointer2 && highlights.pointer2 === i) {
                html += '<span class="array-pointer bottom">ptr</span>';
            }
            
            html += '</div>';
        }
        html += '</div></div>';

        this.container.innerHTML = html;
    }

    // 获取高亮类名
    getHighlightClass(index, highlights) {
        if (highlights.current === index) return 'highlight-current';
        if (highlights.comparing && highlights.comparing.includes(index)) return 'highlight-comparing';
        if (highlights.swapping && highlights.swapping.includes(index)) return 'highlight-swapping';
        if (highlights.visited && highlights.visited.includes(index)) return 'highlight-visited';
        return 'element-default';
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

    // 回退
    revertToStep(stepIndex) {
        if (stepIndex < 0) return;
        // 重新执行到指定步骤
        const originalData = [...this.data];
        this.data = [...this.steps[0]?.initialData || this.data];
        
        for (let i = 0; i <= stepIndex; i++) {
            const step = this.steps[i];
            if (step && step.reverse) {
                step.reverse();
            } else if (step) {
                step.action();
            }
        }
        
        this.render({});
    }

    // 记录步骤
    recordStep(action, highlights = {}, reverse = null) {
        this.steps.push({
            action,
            highlights,
            reverse,
            initialData: [...this.data]
        });
    }

    // 访问元素
    access(index) {
        this.recordStep(
            () => {},
            { current: index }
        );
        this.stepForward();
    }

    // 查找元素
    search(target) {
        this.reset();
        this.steps = [];
        
        for (let i = 0; i < this.data.length; i++) {
            this.recordStep(
                () => {},
                { current: i, comparing: [i] }
            );
            
            if (this.data[i] === target) {
                this.recordStep(
                    () => {},
                    { current: i }
                );
                break;
            }
        }
        
        this.stepForward();
    }

    // 插入元素
    insert(index, value) {
        if (index < 0 || index > this.data.length) return;
        
        this.recordStep(
            () => {
                this.data.splice(index, 0, value);
                this.maxValue = Math.max(...this.data, this.maxValue);
            },
            { current: index },
            () => {
                this.data.splice(index, 1);
            }
        );
        this.stepForward();
    }

    // 删除元素
    delete(index) {
        if (index < 0 || index >= this.data.length) return;
        
        const deletedValue = this.data[index];
        this.recordStep(
            () => {
                this.data.splice(index, 1);
            },
            { current: index },
            () => {
                this.data.splice(index, 0, deletedValue);
            }
        );
        this.stepForward();
    }

    // 更新显示
    update(highlights = {}) {
        this.render(highlights);
    }

    // 获取最大高度
    getMaxBarHeight() {
        return 300;
    }
}
