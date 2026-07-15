/**
 * 图可视化控制器
 */

class GraphVisualizer extends AnimationController {
    constructor(containerId) {
        super();
        this.container = document.getElementById(containerId);
        this.adjList = new Map();
        this.nodePositions = {};
        this.isDirected = false;
        this.isWeighted = false;
        this.nodeRadius = 25;
        this.render();
    }

    // 添加顶点
    addVertex(vertex) {
        if (!this.adjList.has(vertex)) {
            this.adjList.set(vertex, []);
        }
    }

    // 添加边
    addEdge(vertex1, vertex2, weight = 1) {
        this.addVertex(vertex1);
        this.addVertex(vertex2);
        this.adjList.get(vertex1).push({ node: vertex2, weight });
        
        if (!this.isDirected) {
            this.adjList.get(vertex2).push({ node: vertex1, weight });
        }
    }

    // 从边列表创建图
    fromEdges(vertices, edges, directed = false, weighted = false) {
        this.adjList.clear();
        this.isDirected = directed;
        this.isWeighted = weighted;
        
        vertices.forEach(v => this.addVertex(v));
        edges.forEach(([v1, v2, w]) => {
            if (weighted) {
                this.addEdge(v1, v2, w);
            } else {
                this.addEdge(v1, v2);
            }
        });
        
        this.calculateNodePositions();
        this.reset();
        this.render();
    }

    // 生成随机图
    generateRandom(nodeCount = 6, edgeDensity = 0.4) {
        this.adjList.clear();
        this.isDirected = false;
        this.isWeighted = true;
        
        const vertices = [];
        for (let i = 0; i < nodeCount; i++) {
            vertices.push(String.fromCharCode(65 + i)); // A, B, C, ...
        }
        
        vertices.forEach(v => this.addVertex(v));
        
        // 添加边
        for (let i = 0; i < vertices.length; i++) {
            for (let j = i + 1; j < vertices.length; j++) {
                if (Math.random() < edgeDensity) {
                    const weight = Math.floor(Math.random() * 9) + 1; // 1-9
                    this.addEdge(vertices[i], vertices[j], weight);
                }
            }
        }
        
        // 确保图连通
        this.ensureConnected(vertices);
        
        this.calculateNodePositions();
        this.reset();
        this.render();
    }

    // 确保图连通
    ensureConnected(vertices) {
        const visited = new Set();
        const queue = [vertices[0]];
        visited.add(vertices[0]);
        
        while (queue.length > 0) {
            const current = queue.shift();
            const neighbors = this.adjList.get(current).map(e => e.node);
            
            for (const neighbor of neighbors) {
                if (!visited.has(neighbor)) {
                    visited.add(neighbor);
                    queue.push(neighbor);
                }
            }
        }
        
        // 连接未访问的顶点
        for (const vertex of vertices) {
            if (!visited.has(vertex)) {
                const randomVisited = [...visited][Math.floor(Math.random() * visited.size)];
                const weight = Math.floor(Math.random() * 9) + 1;
                this.addEdge(randomVisited, vertex, weight);
                visited.add(vertex);
            }
        }
    }

    // 计算节点位置（圆形布局）
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

    // 获取边列表
    getEdges() {
        const edges = new Set();
        const result = [];
        
        for (const [v1, neighbors] of this.adjList) {
            for (const { node: v2, weight } of neighbors) {
                const edgeKey = this.isDirected ? `${v1}-${v2}` : [v1, v2].sort().join('-');
                if (!edges.has(edgeKey)) {
                    edges.add(edgeKey);
                    result.push([v1, v2, weight]);
                }
            }
        }
        
        return result;
    }

    // 渲染图
    render(highlights = {}) {
        if (!this.container) return;
        
        const width = this.container.offsetWidth || 600;
        const height = this.container.offsetHeight || 400;
        
        let html = `<div class="graph-container"><svg class="graph-svg" viewBox="0 0 ${width} ${height}">`;
        
        // 获取所有边
        const edges = this.getEdges();
        
        // 绘制边
        edges.forEach(([v1, v2, weight]) => {
            const pos1 = this.nodePositions[v1];
            const pos2 = this.nodePositions[v2];
            
            if (!pos1 || !pos2) return;
            
            // 检查是否高亮
            const isEdgeHighlight = highlights.edges && highlights.edges.some(
                e => (e[0] === v1 && e[1] === v2) || (e[0] === v2 && e[1] === v1)
            );
            const isPath = highlights.path && highlights.path.some(
                p => (p[0] === v1 && p[1] === v2) || (p[0] === v2 && p[1] === v1)
            );
            
            let edgeClass = 'graph-edge';
            if (isPath) edgeClass += ' path';
            else if (isEdgeHighlight) edgeClass += ' highlight';
            
            // 绘制边
            html += `<line class="${edgeClass}" x1="${pos1.x}" y1="${pos1.y}" x2="${pos2.x}" y2="${pos2.y}"/>`;
            
            // 绘制权重
            if (this.isWeighted) {
                const midX = (pos1.x + pos2.x) / 2;
                const midY = (pos1.y + pos2.y) / 2;
                html += `<text class="graph-edge-weight" x="${midX}" y="${midY - 8}">${weight}</text>`;
            }
        });
        
        // 绘制节点
        for (const vertex of this.adjList.keys()) {
            const pos = this.nodePositions[vertex];
            if (!pos) continue;
            
            const isCurrent = highlights.current === vertex;
            const isVisited = highlights.visited && highlights.visited.includes(vertex);
            const isInQueue = highlights.queue && highlights.queue.includes(vertex);
            const isInStack = highlights.stack && highlights.stack.includes(vertex);
            
            let nodeClass = 'graph-node';
            if (isCurrent) nodeClass += ' highlight-current';
            else if (isVisited) nodeClass += ' highlight-visited';
            
            html += `<g class="${nodeClass}" transform="translate(${pos.x}, ${pos.y})">`;
            html += `<circle class="graph-node-circle" r="${this.nodeRadius}"/>`;
            html += `<text class="graph-node-text">${vertex}</text>`;
            html += '</g>';
        }
        
        html += '</svg>';
        
        // 图例
        html += '<div class="graph-legend">';
        html += '<div class="graph-legend-item">';
        html += `<div class="graph-legend-color" style="background: var(--graph-color)"></div>`;
        html += '<span>未访问</span>';
        html += '</div>';
        html += '<div class="graph-legend-item">';
        html += `<div class="graph-legend-color" style="background: var(--highlight-current)"></div>`;
        html += '<span>当前节点</span>';
        html += '</div>';
        html += '<div class="graph-legend-item">';
        html += `<div class="graph-legend-color" style="background: var(--highlight-visited)"></div>`;
        html += '<span>已访问</span>';
        html += '</div>';
        if (highlights.path) {
            html += '<div class="graph-legend-item">';
            html += `<div class="graph-legend-color" style="background: var(--highlight-comparing)"></div>`;
            html += '<span>最短路径</span>';
            html += '</div>';
        }
        html += '</div>';
        
        html += '</div>'; // graph-container
        
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

    // DFS
    dfs(startVertex) {
        this.reset();
        this.steps = [];
        
        const visited = new Set();
        const result = [];
        
        const dfsRecursive = (vertex) => {
            if (!vertex || visited.has(vertex)) return;
            
            visited.add(vertex);
            result.push(vertex);
            
            this.recordStep(
                () => {},
                { 
                    current: vertex, 
                    visited: [...visited],
                    stack: [...result],
                    edges: this.getAdjacentEdges(vertex, visited)
                }
            );
            
            const neighbors = this.adjList.get(vertex) || [];
            for (const { node } of neighbors) {
                if (!visited.has(node)) {
                    dfsRecursive(node);
                }
            }
        };
        
        dfsRecursive(startVertex);
        
        this.recordStep(
            () => {},
            { visited: [...visited], result }
        );
        
        this.stepForward();
    }

    // BFS
    bfs(startVertex) {
        this.reset();
        this.steps = [];
        
        const visited = new Set();
        const queue = [startVertex];
        const result = [];
        visited.add(startVertex);
        
        this.recordStep(
            () => {},
            { current: startVertex, visited: [...visited], queue: [...queue] }
        );
        
        while (queue.length > 0) {
            const vertex = queue.shift();
            result.push(vertex);
            
            const neighbors = this.adjList.get(vertex) || [];
            for (const { node } of neighbors) {
                if (!visited.has(node)) {
                    visited.add(node);
                    queue.push(node);
                    
                    this.recordStep(
                        () => {},
                        { 
                            current: node,
                            visited: [...visited], 
                            queue: [...queue],
                            edges: [[vertex, node]]
                        }
                    );
                }
            }
        }
        
        this.recordStep(
            () => {},
            { visited: [...visited], result }
        );
        
        this.stepForward();
    }

    // 获取相邻边
    getAdjacentEdges(vertex, visited) {
        const edges = [];
        const neighbors = this.adjList.get(vertex) || [];
        
        for (const { node } of neighbors) {
            if (!visited.has(node)) {
                edges.push([vertex, node]);
            }
        }
        
        return edges;
    }

    // 获取节点度数
    getDegree(vertex) {
        return this.adjList.get(vertex)?.length || 0;
    }
}
