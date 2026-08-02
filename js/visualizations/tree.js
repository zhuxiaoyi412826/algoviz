/**
 * 二叉树可视化控制器
 */

class TreeVisualizer extends AnimationController {
    constructor(containerId) {
        super();
        this.container = document.getElementById(containerId);
        this.root = null;
        this.nodeRadius = 25;
        this.render();
    }

    // 树节点类
    static TreeNode = class {
        constructor(value) {
            this.value = value;
            this.left = null;
            this.right = null;
        }
    };

    // 从数组创建二叉树（层序）
    fromArray(arr) {
        if (!arr || arr.length === 0) {
            this.root = null;
            this.reset();
            this.render();
            return;
        }
        
        // 使用队列层序创建
        this.root = new TreeVisualizer.TreeNode(arr[0]);
        const queue = [this.root];
        let i = 1;
        
        while (queue.length > 0 && i < arr.length) {
            const node = queue.shift();
            
            // 左子节点
            if (i < arr.length) {
                node.left = new TreeVisualizer.TreeNode(arr[i]);
                queue.push(node.left);
                i++;
            }
            
            // 右子节点
            if (i < arr.length) {
                node.right = new TreeVisualizer.TreeNode(arr[i]);
                queue.push(node.right);
                i++;
            }
        }
        
        this.reset();
        this.render();
    }

    // 从数组创建BST
    fromBSTArray(arr) {
        arr.sort((a, b) => a - b);
        this.root = this.buildBST(arr, 0, arr.length - 1);
        this.reset();
        this.render();
    }

    // 构建BST
    buildBST(arr, start, end) {
        if (start > end) return null;
        
        const mid = Math.floor((start + end) / 2);
        const node = new TreeVisualizer.TreeNode(arr[mid]);
        node.left = this.buildBST(arr, start, mid - 1);
        node.right = this.buildBST(arr, mid + 1, end);
        return node;
    }

    // 计算树节点位置
    calculatePositions(node, level = 0, positions = {}, offset = 0, spread = 1) {
        if (!node) return;
        
        const x = offset;
        const y = level * 80 + 50;
        
        positions[node.value] = { x, y, node };
        
        const newSpread = spread * 0.6;
        const levelWidth = 600 / (level + 1);
        
        if (node.left) {
            this.calculatePositions(node.left, level + 1, positions, offset - levelWidth / 2, newSpread);
        }
        if (node.right) {
            this.calculatePositions(node.right, level + 1, positions, offset + levelWidth / 2, newSpread);
        }
    }

    // 渲染树
    render(highlights = {}) {
        if (!this.container) return;
        
        const width = this.container.offsetWidth || 800;
        const height = this.container.offsetHeight || 350;
        
        let html = `<div class="tree-container"><svg class="tree-svg" viewBox="0 0 ${width} ${height}">`;
        
        // 箭头标记定义
        html += `
            <defs>
                <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                    <polygon points="0 0, 10 3.5, 0 7" fill="var(--text-muted)"/>
                </marker>
            </defs>
        `;
        
        if (!this.root) {
            html += `<text x="${width/2}" y="${height/2}" text-anchor="middle" fill="var(--text-muted)">空树 (Empty Tree)</text>`;
        } else {
            // 计算所有节点位置
            const positions = {};
            const padding = 50;
            const availableWidth = width - padding * 2;
            
            // 计算树的宽度和节点数
            const nodeCount = this.countNodes(this.root);
            const levelCount = this.getHeight(this.root);
            
            // 使用中序遍历获取节点顺序
            const inorderNodes = [];
            this.inorderTraversal(this.root, inorderNodes);
            
            // 水平分配位置
            inorderNodes.forEach((node, idx) => {
                const x = padding + (idx + 0.5) * (availableWidth / inorderNodes.length);
                positions[node.value] = { x, y: 60, node };
            });
            
            // 为每个节点计算垂直位置
            this.assignLevels(positions, this.root);
            
            // 绘制边
            this.renderEdges(html, this.root, positions, highlights);
            
            // 绘制节点
            this.renderNodes(html, positions, highlights);
        }
        
        html += '</svg></div>';
        
        // 遍历结果面板
        if (highlights.traversalResult && highlights.traversalResult.length > 0) {
            html += '<div class="traversal-order">';
            html += `<span class="traversal-order-label">遍历结果:</span>`;
            highlights.traversalResult.forEach((val, idx) => {
                const delay = idx * 100;
                html += `<div class="traversal-order-item" style="animation-delay: ${delay}ms">${val}</div>`;
            });
            html += '</div>';
        }
        
        this.container.innerHTML = html;
    }

    // 分配层级
    assignLevels(positions, node, level = 0) {
        if (!node) return;
        
        if (positions[node.value]) {
            positions[node.value].y = level * 80 + 60;
        }
        
        this.assignLevels(positions, node.left, level + 1);
        this.assignLevels(positions, node.right, level + 1);
    }

    // 渲染边
    renderEdges(html, node, positions, highlights, parentPos = null) {
        if (!node) return;
        
        const pos = positions[node.value];
        
        if (parentPos) {
            const isHighlight = highlights.edges && 
                highlights.edges.some(e => 
                    (e.parent === parentPos.node.value && e.child === node.value) ||
                    (e.parent === node.value && e.child === parentPos.node.value)
                );
            
            html += `<line class="tree-edge ${isHighlight ? 'highlight' : ''}" 
                    x1="${parentPos.x}" y1="${parentPos.y}" 
                    x2="${pos.x}" y2="${pos.y}"/>`;
        }
        
        this.renderEdges(html, node.left, positions, highlights, pos);
        this.renderEdges(html, node.right, positions, highlights, pos);
    }

    // 渲染节点
    renderNodes(html, positions, highlights) {
        for (const value in positions) {
            const { x, y, node } = positions[value];
            
            const isCurrent = highlights.current === value || highlights.current === node;
            const isVisited = highlights.visited && highlights.visited.includes(node.value);
            
            const nodeClass = isCurrent ? 'highlight-current' : isVisited ? 'highlight-visited' : '';
            
            html += `<g class="tree-node ${nodeClass}" transform="translate(${x}, ${y})">`;
            html += `<circle class="tree-node-circle" r="${this.nodeRadius}"/>`;
            html += `<text class="tree-node-text">${node.value}</text>`;
            html += '</g>';
        }
    }

    // 计算节点数
    countNodes(node) {
        if (!node) return 0;
        return 1 + this.countNodes(node.left) + this.countNodes(node.right);
    }

    // 获取高度
    getHeight(node) {
        if (!node) return 0;
        return 1 + Math.max(this.getHeight(node.left), this.getHeight(node.right));
    }

    // 中序遍历收集节点
    inorderTraversal(node, result) {
        if (!node) return;
        this.inorderTraversal(node.left, result);
        result.push(node);
        this.inorderTraversal(node.right, result);
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

    // 前序遍历
    preorder() {
        this.reset();
        this.steps = [];
        const visited = [];
        const edges = [];
        
        const traverse = (node) => {
            if (!node) return;
            visited.push(node.value);
            this.recordStep(() => {}, { 
                current: node, 
                visited: [...visited],
                edges: [...edges],
                traversalResult: [...visited]
            });
            
            if (node.left) {
                edges.push({ parent: node.value, child: node.left.value });
                traverse(node.left);
            }
            if (node.right) {
                edges.push({ parent: node.value, child: node.right.value });
                traverse(node.right);
            }
        };
        
        traverse(this.root);
        this.stepForward();
    }

    // 中序遍历
    inorder() {
        this.reset();
        this.steps = [];
        const visited = [];
        const edges = [];
        
        const traverse = (node) => {
            if (!node) return;
            
            if (node.left) {
                edges.push({ parent: node.value, child: node.left.value });
                traverse(node.left);
            }
            
            visited.push(node.value);
            this.recordStep(() => {}, { 
                current: node, 
                visited: [...visited],
                edges: [...edges],
                traversalResult: [...visited]
            });
            
            if (node.right) {
                edges.push({ parent: node.value, child: node.right.value });
                traverse(node.right);
            }
        };
        
        traverse(this.root);
        this.stepForward();
    }

    // 后序遍历
    postorder() {
        this.reset();
        this.steps = [];
        const visited = [];
        const edges = [];
        
        const traverse = (node) => {
            if (!node) return;
            
            if (node.left) {
                edges.push({ parent: node.value, child: node.left.value });
                traverse(node.left);
            }
            if (node.right) {
                edges.push({ parent: node.value, child: node.right.value });
                traverse(node.right);
            }
            
            visited.push(node.value);
            this.recordStep(() => {}, { 
                current: node, 
                visited: [...visited],
                edges: [...edges],
                traversalResult: [...visited]
            });
        };
        
        traverse(this.root);
        this.stepForward();
    }

    // 层序遍历
    levelorder() {
        this.reset();
        this.steps = [];
        
        if (!this.root) {
            this.stepForward();
            return;
        }
        
        const queue = [this.root];
        const visited = [];
        const edges = [];
        
        while (queue.length > 0) {
            const node = queue.shift();
            visited.push(node.value);
            
            this.recordStep(() => {}, { 
                current: node, 
                visited: [...visited],
                edges: [...edges],
                traversalResult: [...visited]
            });
            
            if (node.left) {
                edges.push({ parent: node.value, child: node.left.value });
                queue.push(node.left);
            }
            if (node.right) {
                edges.push({ parent: node.value, child: node.right.value });
                queue.push(node.right);
            }
        }
        
        this.stepForward();
    }

    // 搜索
    search(target) {
        this.reset();
        this.steps = [];
        const visited = [];
        
        const traverse = (node) => {
            if (!node) return false;
            
            visited.push(node.value);
            this.recordStep(() => {}, { 
                current: node, 
                visited: [...visited],
                traversalResult: [...visited]
            });
            
            if (node.value === target) {
                return true;
            }
            
            if (target < node.value) {
                return traverse(node.left);
            } else {
                return traverse(node.right);
            }
        };
        
        const found = traverse(this.root);
        this.recordStep(() => {}, { 
            found,
            traversalResult: [...visited]
        });
        
        this.stepForward();
        return found;
    }
}
