/**
 * 链表可视化控制器
 */

class LinkedListVisualizer extends AnimationController {
    constructor(containerId) {
        super();
        this.container = document.getElementById(containerId);
        this.head = null;
        this.render();
    }

    // 链表节点类
    static Node = class {
        constructor(value) {
            this.value = value;
            this.next = null;
        }
    };

    // 从数组创建链表
    fromArray(arr) {
        if (!arr || arr.length === 0) {
            this.head = null;
            return;
        }
        
        this.head = new LinkedListVisualizer.Node(arr[0]);
        let current = this.head;
        
        for (let i = 1; i < arr.length; i++) {
            current.next = new LinkedListVisualizer.Node(arr[i]);
            current = current.next;
        }
        
        this.reset();
        this.render();
    }

    // 获取链表长度
    getLength() {
        let count = 0;
        let current = this.head;
        while (current) {
            count++;
            current = current.next;
        }
        return count;
    }

    // 获取所有节点值
    toArray() {
        const arr = [];
        let current = this.head;
        while (current) {
            arr.push(current.value);
            current = current.next;
        }
        return arr;
    }

    // 渲染链表
    render(highlights = {}) {
        if (!this.container) return;
        
        let html = '<div class="linkedlist-container">';
        
        if (!this.head) {
            html += '<div class="linkedlist-null">空链表 (Empty Linked List)</div>';
        } else {
            let current = this.head;
            let index = 0;
            
            while (current) {
                const nodeClass = this.getHighlightClass(index, highlights);
                const isCurrentNode = highlights.current === index;
                
                html += '<div class="linkedlist-node">';
                html += `<div class="linkedlist-box ${nodeClass}">`;
                html += `<div class="linkedlist-data">${current.value}</div>`;
                html += '</div>';
                
                if (current.next) {
                    html += '<div class="linkedlist-arrow"></div>';
                }
                
                // 添加指针标签
                if (highlights.pointer === index) {
                    html += '<div style="position: absolute; top: -20px; left: 10px; color: var(--highlight-current); font-size: 0.75rem; font-weight: 600;">ptr</div>';
                }
                
                html += '</div>';
                
                current = current.next;
                index++;
            }
            
            // 添加NULL
            html += '<div class="linkedlist-null">NULL</div>';
        }
        
        html += '</div>';
        this.container.innerHTML = html;
    }

    // 获取高亮类名
    getHighlightClass(index, highlights) {
        if (highlights.current === index) return 'highlight-current';
        if (highlights.visited && highlights.visited.includes(index)) return 'highlight-visited';
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

    // 遍历
    traverse() {
        this.reset();
        this.steps = [];
        
        let current = this.head;
        let index = 0;
        const visited = [];
        
        while (current) {
            visited.push(index);
            this.recordStep(
                () => {},
                { current: index, visited: [...visited] }
            );
            current = current.next;
            index++;
        }
        
        this.stepForward();
    }

    // 搜索
    search(target) {
        this.reset();
        this.steps = [];
        
        let current = this.head;
        let index = 0;
        
        while (current) {
            this.recordStep(
                () => {},
                { current: index }
            );
            
            if (current.value === target) {
                this.recordStep(
                    () => {},
                    { current: index, visited: [index] }
                );
                break;
            }
            
            current = current.next;
            index++;
        }
        
        if (!current) {
            this.recordStep(
                () => {},
                {}
            );
        }
        
        this.stepForward();
    }

    // 头部插入
    insertAtHead(value) {
        this.reset();
        this.steps = [];
        
        const newNode = new LinkedListVisualizer.Node(value);
        newNode.next = this.head;
        this.head = newNode;
        
        this.recordStep(
            () => {
                const node = new LinkedListVisualizer.Node(value);
                node.next = this.head;
                this.head = node;
            },
            { current: 0 }
        );
        
        this.stepForward();
    }

    // 尾部插入
    insertAtTail(value) {
        this.reset();
        this.steps = [];
        
        if (!this.head) {
            this.head = new LinkedListVisualizer.Node(value);
        } else {
            let current = this.head;
            let index = 0;
            const visited = [];
            
            while (current.next) {
                visited.push(index);
                this.recordStep(
                    () => {},
                    { current: index, visited: [...visited] }
                );
                current = current.next;
                index++;
            }
            
            visited.push(index);
            current.next = new LinkedListVisualizer.Node(value);
            this.recordStep(
                () => {},
                { current: index, visited: [...visited] }
            );
        }
        
        this.stepForward();
    }

    // 更新显示
    update(highlights = {}) {
        this.render(highlights);
    }
}
