/**
 * Dijkstra 最短路径算法可视化控制器
 */

class DijkstraVisualizer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.adjList = new Map();
        this.weights = new Map();
        this.nodePositions = {};
        this.steps = [];
        this.currentStep = 0;
        this.isPlaying = false;
        this.speed = 500;
        this.animationTimer = null;
        this.source = 'A';
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
            this.weights.set(vertex, new Map());
        }
    }

    // 添加边
    addEdge(vertex1, vertex2, weight) {
        this.addVertex(vertex1);
        this.addVertex(vertex2);
        this.adjList.get(vertex1).push(vertex2);
        this.adjList.get(vertex2).push(vertex1);
        this.weights.get(vertex1).set(vertex2, weight);
        this.weights.get(vertex2).set(vertex1, weight);
    }

    // 从边列表创建图
    fromEdges(vertices, edges) {
        this.adjList.clear();
        this.weights.clear();
        
        vertices.forEach(v => this.addVertex(v));
        edges.forEach(([v1, v2, w]) => {
            this.addEdge(v1, v2, w);
        });
        
        this.calculateNodePositions();
        this.reset();
        this.render();
    }

    // 生成随机加权图
    generateRandom(nodeCount = 6) {
        this.adjList.clear();
        this.weights.clear();
        const vertices = [];
        
        for (let i = 0; i < nodeCount; i++) {
            vertices.push(String.fromCharCode(65 + i));
        }
        
        vertices.forEach(v => this.addVertex(v));
        
        // 生成随机边和权重
        for (let i = 0; i < vertices.length; i++) {
            for (let j = i + 1; j < vertices.length; j++) {
                if (Math.random() < 0.4) {
                    const weight = Math.floor(Math.random() * 9) + 1;
                    this.addEdge(vertices[i], vertices[j], weight);
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
                const weight = Math.floor(Math.random() * 9) + 1;
                this.addEdge(randomVisited, vertex, weight);
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
        const distances = highlights.distances || {};
        
        let html = `<div class="dijkstra-container"><div class="dijkstra-visual">`;
        html += `<svg class="graph-svg" viewBox="0 0 ${width} ${height}">`;
        
        // 获取所有边
        const drawnEdges = new Set();
        
        for (const [v1, neighbors] of this.adjList) {
            for (const v2 of neighbors) {
                const key = [v1, v2].sort().join('-');
                if (drawnEdges.has(key)) continue;
                drawnEdges.add(key);
                
                const pos1 = this.nodePositions[v1];
                const pos2 = this.nodePositions[v2];
                
                if (!pos1 || !pos2) continue;
                
                const isPath = highlights.path && highlights.path.some(
                    p => (p[0] === v1 && p[1] === v2) || (p[0] === v2 && p[1] === v1)
                );
                const isEdgeHighlight = highlights.edges && highlights.edges.some(
                    e => (e[0] === v1 && e[1] === v2) || (e[0] === v2 && e[1] === v1)
                );
                
                let edgeClass = 'graph-edge';
                if (isPath) edgeClass += ' path';
                else if (isEdgeHighlight) edgeClass += ' highlight';
                
                html += `<line class="${edgeClass}" 
                        x1="${pos1.x}" y1="${pos1.y}" 
                        x2="${pos2.x}" y2="${pos2.y}"/>`;
                
                // 权重标签
                const weight = this.weights.get(v1)?.get(v2);
                if (weight !== undefined) {
                    const midX = (pos1.x + pos2.x) / 2;
                    const midY = (pos1.y + pos2.y) / 2;
                    const isWeightHighlight = highlights.currentEdge && 
                        highlights.currentEdge[0] === v1 && highlights.currentEdge[1] === v2;
                    
                    html += `<text class="graph-edge-weight" 
                            x="${midX}" y="${midY - 8}"
                            ${isWeightHighlight ? 'fill="var(--highlight-current)" font-weight="bold"' : ''}>
                            ${weight}
                            </text>`;
                }
            }
        }
        
        // 绘制节点
        for (const vertex of this.adjList.keys()) {
            const pos = this.nodePositions[vertex];
            if (!pos) continue;
            
            const isCurrent = highlights.current === vertex;
            const isVisited = highlights.visited && highlights.visited.includes(vertex);
            const isSource = vertex === this.source;
            const dist = distances[vertex];
            
            let nodeClass = 'graph-node';
            if (isCurrent) nodeClass += ' highlight-current';
            else if (isVisited) nodeClass += ' highlight-visited';
            
            html += `<g class="${nodeClass}" transform="translate(${pos.x}, ${pos.y})">`;
            html += `<circle class="graph-node-circle" r="${isSource ? 30 : 25}"/>`;
            html += `<text class="graph-node-text">${vertex}</text>`;
            
            // 显示距离
            if (dist !== undefined && dist !== Infinity) {
                html += `<text y="40" text-anchor="middle" fill="var(--text-secondary)" font-size="11">${dist}</text>`;
            }
            
            html += '</g>';
        }
        
        html += '</svg></div>'; // graph-svg, dijkstra-visual
        
        // 距离表格
        if (Object.keys(distances).length > 0) {
            html += '<table class="distance-table">';
            html += '<thead><tr><th>顶点</th><th>距离</th><th>状态</th></tr></thead>';
            html += '<tbody>';
            
            const vertices = [...this.adjList.keys()].sort();
            
            for (const vertex of vertices) {
                const dist = distances[vertex];
                const isCurrent = highlights.current === vertex;
                const isVisited = highlights.visited && highlights.visited.includes(vertex);
                
                let stateClass = '';
                if (isCurrent) stateClass = 'current';
                else if (isVisited) stateClass = 'visited';
                
                html += '<tr>';
                html += `<td><strong>${vertex}</strong></td>`;
                html += `<td class="${stateClass}">${dist === Infinity ? '∞' : dist}</td>`;
                html += `<td>${isCurrent ? '当前' : isVisited ? '已访问' : '未访问'}</td>`;
                html += '</tr>';
            }
            
            html += '</tbody></table>';
        }
        
        html += '</div>'; // dijkstra-container
        
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

    // 设置源点
    setSource(vertex) {
        this.source = vertex;
    }

    // Dijkstra 算法
    run(startVertex) {
        this.reset();
        this.source = startVertex;
        
        const vertices = [...this.adjList.keys()];
        const distances = {};
        const previous = {};
        const unvisited = new Set(vertices);
        const visited = [];
        
        // 初始化
        for (const vertex of vertices) {
            distances[vertex] = vertex === startVertex ? 0 : Infinity;
        }
        
        this.recordStep({
            current: startVertex,
            distances: { ...distances },
            visited: [...visited]
        }, this.stats);
        
        while (unvisited.size > 0) {
            // 找到未访问的最小距离顶点
            let minVertex = null;
            let minDist = Infinity;
            
            for (const vertex of unvisited) {
                if (distances[vertex] < minDist) {
                    minDist = distances[vertex];
                    minVertex = vertex;
                }
            }
            
            if (minVertex === null || distances[minVertex] === Infinity) break;
            
            // 访问该顶点
            unvisited.delete(minVertex);
            visited.push(minVertex);
            this.stats.visited++;
            
            this.recordStep({
                current: minVertex,
                distances: { ...distances },
                visited: [...visited]
            }, this.stats);
            
            // 更新邻居距离
            const neighbors = this.adjList.get(minVertex) || [];
            
            for (const neighbor of neighbors) {
                if (!unvisited.has(neighbor)) continue;
                
                const weight = this.weights.get(minVertex)?.get(neighbor) || 1;
                const newDist = distances[minVertex] + weight;
                
                if (newDist < distances[neighbor]) {
                    distances[neighbor] = newDist;
                    previous[neighbor] = minVertex;
                    
                    this.recordStep({
                        current: neighbor,
                        distances: { ...distances },
                        visited: [...visited],
                        edges: [[minVertex, neighbor]],
                        currentEdge: [minVertex, neighbor]
                    }, this.stats);
                }
            }
        }
        
        // 计算最短路径树
        const pathEdges = [];
        for (const vertex of visited) {
            if (previous[vertex]) {
                pathEdges.push([previous[vertex], vertex]);
            }
        }
        
        this.recordStep({
            distances: { ...distances },
            visited: [...visited],
            path: pathEdges
        }, this.stats);
        
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
window.DijkstraVisualizer = DijkstraVisualizer;
