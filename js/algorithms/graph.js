/**
 * 图遍历算法可视化控制器
 */

class GraphTraversalVisualizer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.adjList = new Map();
        this.nodePositions = {};
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

    // 添加顶点
    addVertex(vertex) {
        if (!this.adjList.has(vertex)) {
            this.adjList.set(vertex, []);
        }
    }

    // 添加边
    addEdge(vertex1, vertex2) {
        this.addVertex(vertex1);
        this.addVertex(vertex2);
        this.adjList.get(vertex1).push(vertex2);
        this.adjList.get(vertex2).push(vertex1);
    }

    // 从边列表创建图
    fromEdges(vertices, edges) {
        this.adjList.clear();
        vertices.forEach(v => this.addVertex(v));
        edges.forEach(([v1, v2]) => {
            this.addEdge(v1, v2);
        });
        this.calculateNodePositions();
        this.reset();
        this.render();
    }

    // 生成随机图
    generateRandom(nodeCount = 6) {
        this.adjList.clear();
        const vertices = [];
        
        for (let i = 0; i < nodeCount; i++) {
            vertices.push(String.fromCharCode(65 + i));
        }
        
        vertices.forEach(v => this.addVertex(v));
        
        // 生成随机边
        for (let i = 0; i < vertices.length; i++) {
            for (let j = i + 1; j < vertices.length; j++) {
                if (Math.random() < 0.5) {
                    this.addEdge(vertices[i], vertices[j]);
                }
            }
        }
        
        // 确保连通
        const visited = new Set();
        const queue = [vertices[0]];
        visited.add(vertices[0]);
        
        while (queue.length > 0) {
            const current = queue.shift();
            const neighbors = this.adjList.get(current);
            
            for (const neighbor of neighbors) {
                if (!visited.has(neighbor)) {
                    visited.add(neighbor);
                    queue.push(neighbor);
                }
            }
        }
        
        for (const vertex of vertices) {
            if (!visited.has(vertex)) {
                const randomVisited = [...visited][Math.floor(Math.random() * visited.size)];
                this.addEdge(randomVisited, vertex);
                visited.add(vertex);
            }
        }
        
        this.calculateNodePositions();
        this.reset();
        this.render();
    }

    // 计算节点位置
    calculateNodePositions() {
        const vertices = [...this.adjList.keys()];
        const count = vertices.length;
        
        if (count === 0) return;
        
        const width = this.container.offsetWidth || 600;
        const height = this.container.offsetHeight || 400;
        const centerX = width / 2;
        const centerY = height / 2;
        const radius = Math.min(width, height) / 2 - 60;
        
        vertices.forEach((vertex, index) => {
            const angle = (2 * Math.PI * index) / count - Math.PI / 2;
            this.nodePositions[vertex] = {
                x: centerX + radius * Math.cos(angle),
                y: centerY + radius * Math.sin(angle)
            };
        });
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

    // 渲染
    render(highlights = {}) {
        if (!this.container) return;
        
        const width = this.container.offsetWidth || 600;
        const height = this.container.offsetHeight || 400;
        
        let html = `<div class="graph-traversal-container"><div class="graph-traversal-visual">`;
        html += `<svg class="graph-svg" viewBox="0 0 ${width} ${height}">`;
        
        // 获取所有边
        const edges = new Set();
        const edgeList = [];
        
        for (const [v1, neighbors] of this.adjList) {
            for (const v2 of neighbors) {
                const key = [v1, v2].sort().join('-');
                if (!edges.has(key)) {
                    edges.add(key);
                    edgeList.push([v1, v2]);
                }
            }
        }
        
        // 绘制边
        for (const [v1, v2] of edgeList) {
            const pos1 = this.nodePositions[v1];
            const pos2 = this.nodePositions[v2];
            
            if (!pos1 || !pos2) continue;
            
            const isHighlight = highlights.edges && highlights.edges.some(
                e => (e[0] === v1 && e[1] === v2) || (e[0] === v2 && e[1] === v1)
            );
            
            html += `<line class="graph-edge ${isHighlight ? 'highlight' : ''}" 
                    x1="${pos1.x}" y1="${pos1.y}" x2="${pos2.x}" y2="${pos2.y}"/>`;
        }
        
        // 绘制节点
        for (const vertex of this.adjList.keys()) {
            const pos = this.nodePositions[vertex];
            if (!pos) continue;
            
            const isCurrent = highlights.current === vertex;
            const isVisited = highlights.visited && highlights.visited.includes(vertex);
            
            let nodeClass = 'graph-node';
            if (isCurrent) nodeClass += ' highlight-current';
            else if (isVisited) nodeClass += ' highlight-visited';
            
            html += `<g class="${nodeClass}" transform="translate(${pos.x}, ${pos.y})">`;
            html += `<circle class="graph-node-circle" r="25"/>`;
            html += `<text class="graph-node-text">${vertex}</text>`;
            html += '</g>';
        }
        
        html += '</svg></div>'; // graph-svg, graph-traversal-visual
        
        // 队列/栈显示
        if (highlights.queue && highlights.queue.length > 0) {
            html += '<div class="graph-traversal-queue">';
            html += '<span class="graph-queue-label">队列:</span>';
            highlights.queue.forEach(v => {
                html += `<span class="graph-queue-item">${v}</span>`;
            });
            html += '</div>';
        }
        
        if (highlights.stack && highlights.stack.length > 0) {
            html += '<div class="graph-traversal-stack">';
            html += '<span class="graph-stack-label">栈:</span>';
            highlights.stack.forEach(v => {
                html += `<span class="graph-stack-item">${v}</span>`;
            });
            html += '</div>';
        }
        
        // 访问顺序
        if (highlights.result && highlights.result.length > 0) {
            html += '<div class="graph-visited-list">';
            html += '<span class="graph-visited-label">访问顺序:</span>';
            highlights.result.forEach((v, idx) => {
                const delay = idx * 100;
                html += `<div class="traversal-order-item" style="animation-delay: ${delay}ms">${v}</div>`;
            });
            html += '</div>';
        }
        
        html += '</div>'; // graph-traversal-container
        
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

    // 深度优先搜索
    dfs(startVertex) {
        this.reset();
        const visited = new Set();
        const result = [];
        const stack = [startVertex];
        
        this.recordStep({
            current: startVertex,
            stack: [...stack],
            visited: [...visited],
            result: [...result]
        }, this.stats);
        
        while (stack.length > 0) {
            const vertex = stack.pop();
            
            if (visited.has(vertex)) continue;
            
            visited.add(vertex);
            result.push(vertex);
            this.stats.visited++;
            
            this.recordStep({
                current: vertex,
                visited: [...visited],
                result: [...result],
                stack: [...stack]
            }, this.stats);
            
            const neighbors = this.adjList.get(vertex) || [];
            
            // 按逆序添加邻居，以便按字母顺序访问
            const sortedNeighbors = [...neighbors].sort().reverse();
            
            for (const neighbor of sortedNeighbors) {
                if (!visited.has(neighbor)) {
                    stack.push(neighbor);
                    
                    this.recordStep({
                        current: vertex,
                        visited: [...visited],
                        result: [...result],
                        stack: [...stack],
                        edges: [[vertex, neighbor]]
                    }, this.stats);
                }
            }
        }
        
        this.stepForward();
    }

    // 广度优先搜索
    bfs(startVertex) {
        this.reset();
        const visited = new Set();
        const result = [];
        const queue = [startVertex];
        visited.add(startVertex);
        
        this.recordStep({
            current: startVertex,
            queue: [...queue],
            visited: [...visited],
            result: [...result]
        }, this.stats);
        
        while (queue.length > 0) {
            const vertex = queue.shift();
            result.push(vertex);
            this.stats.visited++;
            
            this.recordStep({
                current: vertex,
                visited: [...visited],
                result: [...result],
                queue: [...queue]
            }, this.stats);
            
            const neighbors = this.adjList.get(vertex) || [];
            
            for (const neighbor of neighbors.sort()) {
                if (!visited.has(neighbor)) {
                    visited.add(neighbor);
                    queue.push(neighbor);
                    
                    this.recordStep({
                        current: neighbor,
                        visited: [...visited],
                        result: [...result],
                        queue: [...queue],
                        edges: [[vertex, neighbor]]
                    }, this.stats);
                }
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
window.GraphTraversalVisualizer = GraphTraversalVisualizer;
