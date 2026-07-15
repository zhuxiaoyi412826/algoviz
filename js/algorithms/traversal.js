/**
 * 遍历算法可视化控制器
 */

class TraversalVisualizer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.root = null;
        this.steps = [];
        this.currentStep = 0;
        this.isPlaying = false;
        this.speed = 500;
        this.animationTimer = null;
        this.onStepChange = null;
        this.onComplete = null;
        
        // 统计
        this.stats = {
            visited: 0
        };
    }

    // 树节点类
    static TreeNode = class {
        constructor(value) {
            this.value = value;
            this.left = null;
            this.right = null;
        }
    };

    // 从数组创建树
    fromArray(arr) {
        if (!arr || arr.length === 0) {
            this.root = null;
            return;
        }
        
        this.root = new TraversalVisualizer.TreeNode(arr[0]);
        const queue = [this.root];
        let i = 1;
        
        while (queue.length > 0 && i < arr.length) {
            const node = queue.shift();
            
            if (i < arr.length) {
                node.left = new TraversalVisualizer.TreeNode(arr[i]);
                queue.push(node.left);
                i++;
            }
            
            if (i < arr.length) {
                node.right = new TraversalVisualizer.TreeNode(arr[i]);
                queue.push(node.right);
                i++;
            }
        }
        
        this.reset();
        this.render();
    }

    // 重置
    reset() {
        this.currentStep = 0;
        this.isPlaying = false;
        this.stats = { visited: 0 };
        if (this.animationTimer) {
            clearTimeout(this.animationTimer);
            this.animationTimer = null;
        }
        this.updateStats();
    }

    // 计算节点位置
    calculatePositions(node, positions = {}, level = 0, offset = 0, spread = 1) {
        if (!node) return;
        
        const width = this.container.offsetWidth || 600;
        const levelWidth = (width - 100) / Math.pow(2, level);
        const x = width / 2 + offset * levelWidth;
        const y = level * 70 + 50;
        
        positions[node.value] = { x, y, node };
        
        this.calculatePositions(node.left, positions, level + 1, offset - 1, spread * 0.5);
        this.calculatePositions(node.right, positions, level + 1, offset + 1, spread * 0.5);
    }

    // 渲染
    render(highlights = {}) {
        if (!this.container) return;
        
        const width = this.container.offsetWidth || 600;
        const height = this.container.offsetHeight || 350;
        
        let html = `<div class="tree-traversal-container"><div class="tree-traversal-visual">`;
        html += `<svg class="tree-svg" viewBox="0 0 ${width} ${height}">`;
        
        if (!this.root) {
            html += `<text x="${width/2}" y="${height/2}" text-anchor="middle" fill="var(--text-muted)">空树</text>`;
        } else {
            const positions = {};
            this.calculatePositions(this.root, positions);
            
            // 绘制边
            const drawEdges = (node) => {
                if (!node) return;
                
                const pos = positions[node.value];
                const edgeClass = highlights.edges ? 
                    (highlights.edges.some(e => e[0] === node.value && e[1] === node.left?.value) ? 'highlight' : '') : '';
                
                if (node.left) {
                    const childPos = positions[node.left.value];
                    const edgeHighlight = highlights.edges && highlights.edges.some(
                        e => (e[0] === node.value && e[1] === node.left.value)
                    );
                    html += `<line class="tree-edge ${edgeHighlight ? 'highlight' : ''}" 
                            x1="${pos.x}" y1="${pos.y}" x2="${childPos.x}" y2="${childPos.y}"/>`;
                    drawEdges(node.left);
                }
                
                if (node.right) {
                    const childPos = positions[node.right.value];
                    const edgeHighlight = highlights.edges && highlights.edges.some(
                        e => (e[0] === node.value && e[1] === node.right.value)
                    );
                    html += `<line class="tree-edge ${edgeHighlight ? 'highlight' : ''}" 
                            x1="${pos.x}" y1="${pos.y}" x2="${childPos.x}" y2="${childPos.y}"/>`;
                    drawEdges(node.right);
                }
            };
            
            drawEdges(this.root);
            
            // 绘制节点
            for (const value in positions) {
                const { x, y, node } = positions[value];
                
                const isCurrent = highlights.current === node.value;
                const isVisited = highlights.visited && highlights.visited.includes(node.value);
                
                let nodeClass = 'tree-node';
                if (isCurrent) nodeClass += ' highlight-current';
                else if (isVisited) nodeClass += ' highlight-visited';
                
                html += `<g class="${nodeClass}" transform="translate(${x}, ${y})">`;
                html += `<circle class="tree-node-circle" r="25"/>`;
                html += `<text class="tree-node-text">${node.value}</text>`;
                html += '</g>';
            }
        }
        
        html += '</svg></div>'; // tree-svg, tree-traversal-visual
        
        // 遍历结果
        if (highlights.result && highlights.result.length > 0) {
            html += '<div class="tree-traversal-order">';
            html += `<span class="traversal-order-label">遍历顺序:</span>`;
            highlights.result.forEach((val, idx) => {
                const delay = idx * 100;
                html += `<div class="traversal-order-item" style="animation-delay: ${delay}ms">${val}</div>`;
            });
            html += '</div>';
        }
        
        html += '</div>'; // tree-traversal-container
        
        this.container.innerHTML = html;
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
            this.executeStep(this.currentStep);
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
        
        document.getElementById('statComparisons').textContent = '-';
        document.getElementById('statSwaps').textContent = this.stats.visited;
        document.getElementById('statCurrentStep').textContent = progress;
        document.getElementById('statTotalSteps').textContent = total;
    }

    // 设置速度
    setSpeed(newSpeed) {
        this.speed = newSpeed;
    }

    // 前序遍历: 根-左-右
    preorder() {
        this.reset();
        const visited = [];
        const edges = [];
        
        const traverse = (node) => {
            if (!node) return;
            
            visited.push(node.value);
            this.stats.visited++;
            
            this.recordStep({
                current: node.value,
                visited: [...visited],
                result: [...visited],
                edges: [...edges]
            }, this.stats);
            
            if (node.left) {
                edges.push([node.value, node.left.value]);
                traverse(node.left);
            }
            
            if (node.right) {
                edges.push([node.value, node.right.value]);
                traverse(node.right);
            }
        };
        
        traverse(this.root);
        this.stepForward();
    }

    // 中序遍历: 左-根-右
    inorder() {
        this.reset();
        const visited = [];
        
        const traverse = (node) => {
            if (!node) return;
            
            if (node.left) {
                traverse(node.left);
            }
            
            visited.push(node.value);
            this.stats.visited++;
            
            this.recordStep({
                current: node.value,
                visited: [...visited],
                result: [...visited]
            }, this.stats);
            
            if (node.right) {
                traverse(node.right);
            }
        };
        
        traverse(this.root);
        this.stepForward();
    }

    // 后序遍历: 左-右-根
    postorder() {
        this.reset();
        const visited = [];
        
        const traverse = (node) => {
            if (!node) return;
            
            if (node.left) {
                traverse(node.left);
            }
            
            if (node.right) {
                traverse(node.right);
            }
            
            visited.push(node.value);
            this.stats.visited++;
            
            this.recordStep({
                current: node.value,
                visited: [...visited],
                result: [...visited]
            }, this.stats);
        };
        
        traverse(this.root);
        this.stepForward();
    }

    // 层序遍历
    levelorder() {
        this.reset();
        const visited = [];
        
        if (!this.root) {
            this.stepForward();
            return;
        }
        
        const queue = [this.root];
        
        while (queue.length > 0) {
            const node = queue.shift();
            
            visited.push(node.value);
            this.stats.visited++;
            
            this.recordStep({
                current: node.value,
                visited: [...visited],
                result: [...visited]
            }, this.stats);
            
            if (node.left) {
                queue.push(node.left);
            }
            if (node.right) {
                queue.push(node.right);
            }
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
window.TraversalVisualizer = TraversalVisualizer;
