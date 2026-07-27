/**
 * 线段树 (Segment Tree) 可视化器
 */

class SegmentTreeVisualizer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.tree = [];
        this.originalArray = [];
        this.steps = [];
        this.currentStep = 0;
        this.init();
    }

    init() {
        this.tree = [];
        this.originalArray = [];
        this.steps = [];
        this.currentStep = 0;
        this.render();
    }

    reset() {
        this.init();
    }

    // 从数组构建线段树
    buildFromArray(arr) {
        this.originalArray = [...arr];
        this.steps = [];
        this.steps.push({
            type: 'build-start',
            arr: [...arr],
            description: `从数组 [${arr.join(', ')}] 构建线段树`
        });
        
        const n = arr.length;
        const size = 4 * n;
        this.tree = new Array(size).fill(0);
        
        const build = (node, start, end) => {
            if (start === end) {
                this.tree[node] = arr[start];
                this.steps.push({
                    type: 'leaf',
                    node,
                    index: start,
                    value: arr[start],
                    description: `叶节点 [${start}] = ${arr[start]}`
                });
                return arr[start];
            }
            
            const mid = Math.floor((start + end) / 2);
            this.steps.push({
                type: 'split',
                node,
                start, mid, end,
                description: `节点 ${node} 范围 [${start}, ${end}]，分裂为 [${start}, ${mid}] 和 [${mid+1}, ${end}]`
            });
            
            build(node * 2, start, mid);
            build(node * 2 + 1, mid + 1, end);
            
            this.tree[node] = this.tree[node * 2] + this.tree[node * 2 + 1];
            this.steps.push({
                type: 'combine',
                node,
                left: node * 2,
                right: node * 2 + 1,
                value: this.tree[node],
                description: `合并节点 ${node*2}(${this.tree[node*2]}) + ${node*2+1}(${this.tree[node*2+1]}) = ${this.tree[node]}`
            });
        };
        
        build(1, 0, n - 1);
        
        this.steps.push({
            type: 'build-end',
            description: `线段树构建完成！`
        });
        
        this.render();
    }

    // 区间查询
    query(left, right) {
        this.steps = [];
        this.steps.push({
            type: 'query-start',
            left, right,
            array: [...this.originalArray],
            description: `查询区间 [${left}, ${right}] 的和`
        });
        
        let result = 0;
        
        const queryHelper = (node, start, end) => {
            // 完全在范围外
            if (right < start || end < left) {
                this.steps.push({
                    type: 'out-of-range',
                    node,
                    start, end,
                    description: `节点 ${node} 范围 [${start}, ${end}] 完全在查询范围外，返回 0`
                });
                return 0;
            }
            
            // 完全在范围内
            if (left <= start && end <= right) {
                this.steps.push({
                    type: 'in-range',
                    node,
                    start, end,
                    value: this.tree[node],
                    description: `节点 ${node} 范围 [${start}, ${end}] 完全在查询范围内，返回 ${this.tree[node]}`
                });
                return this.tree[node];
            }
            
            // 部分重叠
            const mid = Math.floor((start + end) / 2);
            this.steps.push({
                type: 'partial',
                node,
                start, mid, end,
                description: `节点 ${node} 范围 [${start}, ${end}] 与查询范围部分重叠，继续分裂`
            });
            
            const leftSum = queryHelper(node * 2, start, mid);
            const rightSum = queryHelper(node * 2 + 1, mid + 1, end);
            
            this.steps.push({
                type: 'query-combine',
                node,
                leftSum, rightSum,
                total: leftSum + rightSum,
                description: `合并结果: ${leftSum} + ${rightSum} = ${leftSum + rightSum}`
            });
            
            return leftSum + rightSum;
        };
        
        result = queryHelper(1, 0, this.originalArray.length - 1);
        
        this.steps.push({
            type: 'query-end',
            left, right,
            result,
            description: `区间 [${left}, ${right}] 的和为 ${result}`
        });
        
        this.render();
        return result;
    }

    // 单点更新
    update(index, value) {
        this.steps = [];
        this.steps.push({
            type: 'update-start',
            index, value,
            oldValue: this.originalArray[index],
            description: `将位置 ${index} 的值从 ${this.originalArray[index]} 更新为 ${value}`
        });
        
        const updateHelper = (node, start, end) => {
            if (start === end) {
                this.originalArray[start] = value;
                this.tree[node] = value;
                this.steps.push({
                    type: 'update-leaf',
                    node,
                    index: start,
                    value,
                    description: `更新叶节点 [${start}] = ${value}`
                });
                return;
            }
            
            const mid = Math.floor((start + end) / 2);
            
            if (index <= mid) {
                this.steps.push({
                    type: 'go-left',
                    node,
                    child: node * 2,
                    description: `更新目标在左子树，继续向下`
                });
                updateHelper(node * 2, start, mid);
            } else {
                this.steps.push({
                    type: 'go-right',
                    node,
                    child: node * 2 + 1,
                    description: `更新目标在右子树，继续向下`
                });
                updateHelper(node * 2 + 1, mid + 1, end);
            }
            
            this.tree[node] = this.tree[node * 2] + this.tree[node * 2 + 1];
            this.steps.push({
                type: 'update-combine',
                node,
                value: this.tree[node],
                description: `更新父节点 ${node} = ${this.tree[node]}`
            });
        };
        
        updateHelper(1, 0, this.originalArray.length - 1);
        
        this.steps.push({
            type: 'update-end',
            description: `更新完成！`
        });
        
        this.render();
    }

    render(highlightNode = null) {
        if (!this.container) return;
        
        const theme = document.documentElement.getAttribute('data-theme') || 'dark';
        const bgColor = theme === 'dark' ? '#1a1a2e' : '#ffffff';
        const textColor = theme === 'dark' ? '#ffffff' : '#1a202c';
        const cardBg = theme === 'dark' ? '#252542' : '#f0f2f5';
        const primaryColor = '#667eea';
        const highlightColor = '#fcd34d';
        const successColor = '#48c78e';
        
        if (this.originalArray.length === 0) {
            this.container.innerHTML = `<div style="padding:40px;text-align:center;color:${textColor};">点击"构建"开始演示</div>`;
            return;
        }
        
        let html = `<div style="margin-bottom:20px;padding:15px;background:${cardBg};border-radius:8px;">`;
        html += `<div style="color:${textColor};font-size:12px;margin-bottom:8px;">原数组:</div>`;
        html += `<div style="display:flex;gap:8px;flex-wrap:wrap;">`;
        this.originalArray.forEach((val, i) => {
            html += `<div style="width:40px;height:40px;display:flex;align-items:center;justify-content:center;background:${primaryColor};color:white;border-radius:6px;font-weight:bold;">${val}</div>`;
        });
        html += `</div></div>`;
        
        // 树形可视化
        html += `<div class="segment-tree-container" style="position:relative;width:100%;min-height:300px;background:${bgColor};border-radius:8px;padding:20px;overflow:auto;">`;
        
        // 计算树节点位置
        const levels = Math.ceil(Math.log2(this.originalArray.length)) + 1;
        const nodePositions = {};
        
        const calculatePositions = (node, start, end, level, left, right) => {
            if (node >= this.tree.length || this.tree[node] === 0 && node > 1 && start !== end) return;
            
            const y = 30 + level * 60;
            const x = left + (right - left) / 2;
            nodePositions[node] = { x, y, start, end };
            
            if (start !== end) {
                const mid = Math.floor((start + end) / 2);
                calculatePositions(node * 2, start, mid, level + 1, left, x);
                calculatePositions(node * 2 + 1, mid + 1, end, level + 1, x, right);
            }
        };
        
        calculatePositions(1, 0, this.originalArray.length - 1, 0, 0, 800);
        
        // 绘制连线
        html += `<svg style="position:absolute;top:0;left:0;width:100%;height:100%;pointer-events:none;">`;
        Object.entries(nodePositions).forEach(([node, pos]) => {
            const numNode = parseInt(node);
            if (numNode !== 1 && nodePositions[Math.floor(numNode / 2)]) {
                const parent = nodePositions[Math.floor(numNode / 2)];
                html += `<line x1="${pos.x}" y1="${pos.y}" x2="${parent.x}" y2="${parent.y}" stroke="${theme==='dark'?'#4a5568':'#a0a0b8'}" stroke-width="2"/>`;
            }
        });
        html += `</svg>`;
        
        // 绘制节点
        Object.entries(nodePositions).forEach(([node, pos]) => {
            const isHighlight = highlightNode === parseInt(node);
            const val = this.tree[parseInt(node)];
            
            html += `
                <div style="position:absolute;left:${pos.x}px;top:${pos.y}px;transform:translateX(-50%);text-align:center;">
                    <div style="min-width:50px;height:40px;display:flex;align-items:center;justify-content:center;background:${isHighlight ? highlightColor : primaryColor};color:white;border-radius:8px;font-weight:bold;box-shadow:0 2px 8px rgba(0,0,0,0.3);">
                        ${val}
                    </div>
                    <div style="font-size:10px;color:${theme==='dark'?'#a0a0b8':'#6b6b80'};margin-top:4px;">[${pos.start}, ${pos.end}]</div>
                </div>
            `;
        });
        
        html += `</div>`;
        
        // 当前步骤说明
        if (this.steps.length > 0 && this.currentStep < this.steps.length) {
            const step = this.steps[this.currentStep];
            html += `
                <div style="margin-top:15px;padding:12px;background:${cardBg};border-radius:8px;color:${textColor};font-size:13px;">
                    <strong>步骤 ${this.currentStep + 1}/${this.steps.length}:</strong> ${step.description}
                </div>
            `;
        }
        
        this.container.innerHTML = html;
    }

    getSteps() {
        return this.steps;
    }

    // 设置数据
    setData(arr) {
        this.init();
        this.buildFromArray(arr);
        this.render();
    }
}
