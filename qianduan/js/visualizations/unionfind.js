/**
 * 并查集 (Union-Find) 可视化器
 */

class UnionFindVisualizer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.parent = {};
        this.rank = {};
        this.steps = [];
        this.currentStep = 0;
        this.init();
    }

    init() {
        this.parent = {};
        this.rank = {};
        this.steps = [];
        this.currentStep = 0;
        this.render();
    }

    reset() {
        this.init();
    }

    // 初始化元素
    makeSet(elements) {
        this.steps = [];
        this.steps.push({ 
            type: 'init', 
            elements,
            description: `初始化 ${elements.length} 个元素的并查集`
        });
        
        elements.forEach(e => {
            this.parent[e] = e;
            this.rank[e] = 0;
        });
        
        this.render();
    }

    // 查找（带路径压缩）
    find(x) {
        this.steps = [];
        const path = [];
        this.steps.push({ 
            type: 'find-start', 
            x,
            description: `查找元素 ${x} 的根节点`
        });
        
        let current = x;
        while (current !== this.parent[current]) {
            path.push(current);
            this.steps.push({ 
                type: 'traverse', 
                current,
                parent: this.parent[current],
                path: [...path],
                description: `${current} 不是根节点，继续向上查找`
            });
            current = this.parent[current];
        }
        
        // 路径压缩
        this.steps.push({ 
            type: 'compress-start', 
            x,
            path: [...path],
            description: `找到根节点 ${current}，开始路径压缩`
        });
        
        // 将路径上的所有节点直接指向根
        path.forEach(node => {
            this.steps.push({ 
                type: 'compress', 
                node,
                root: current,
                description: `将节点 ${node} 直接指向根节点 ${current}`
            });
            this.parent[node] = current;
        });
        
        this.steps.push({ 
            type: 'find-end', 
            x,
            root: current,
            description: `查找完成，${x} 的根节点是 ${current}`
        });
        
        this.render();
        return current;
    }

    // 合并（带按秩合并）
    union(x, y) {
        this.steps = [];
        this.steps.push({ 
            type: 'union-start', 
            x, y,
            description: `合并集合 ${x} 和 ${y}`
        });
        
        // 查找两个元素的根
        const rootX = this.findSilent(x);
        const rootY = this.findSilent(y);
        
        this.steps.push({ 
            type: 'roots-found', 
            x, y,
            rootX, rootY,
            description: `${x} 的根是 ${rootX}，${y} 的根是 ${rootY}`
        });
        
        if (rootX === rootY) {
            this.steps.push({ 
                type: 'same-set', 
                x, y,
                root: rootX,
                description: `${x} 和 ${y} 已经在同一个集合中，无需合并`
            });
            this.render();
            return;
        }
        
        // 按秩合并
        const rankX = this.rank[rootX];
        const rankY = this.rank[rootY];
        
        this.steps.push({ 
            type: 'compare-rank', 
            rootX, rootY,
            rankX, rankY,
            description: `比较秩: ${rootX}(${rankX}) vs ${rootY}(${rankY})`
        });
        
        if (rankX < rankY) {
            this.steps.push({ 
                type: 'union', 
                child: rootX,
                parent: rootY,
                description: `${rootY} 的秩更大，${rootX} 合并到 ${rootY}`
            });
            this.parent[rootX] = rootY;
        } else if (rankX > rankY) {
            this.steps.push({ 
                type: 'union', 
                child: rootY,
                parent: rootX,
                description: `${rootX} 的秩更大，${rootY} 合并到 ${rootX}`
            });
            this.parent[rootY] = rootX;
        } else {
            this.steps.push({ 
                type: 'union-equal', 
                rootX, rootY,
                description: `秩相等，任选一个作为父节点，秩+1`
            });
            this.parent[rootY] = rootX;
            this.rank[rootX]++;
        }
        
        this.steps.push({ 
            type: 'union-end', 
            x, y,
            description: `合并完成！`
        });
        
        this.render();
    }

    // 静默查找（不记录步骤）
    findSilent(x) {
        if (this.parent[x] !== x) {
            this.parent[x] = this.findSilent(this.parent[x]);
        }
        return this.parent[x];
    }

    // 批量合并演示
    demoUnions(pairs) {
        this.steps = [];
        this.steps.push({ 
            type: 'demo-start',
            description: '开始批量合并演示'
        });
        
        pairs.forEach(([x, y]) => {
            const rootX = this.findSilent(x);
            const rootY = this.findSilent(y);
            
            this.steps.push({ 
                type: 'demo-union',
                x, y,
                rootX, rootY,
                description: `合并 ${x} 和 ${y}`
            });
            
            if (rootX !== rootY) {
                if (this.rank[rootX] < this.rank[rootY]) {
                    this.parent[rootX] = rootY;
                } else if (this.rank[rootX] > this.rank[rootY]) {
                    this.parent[rootY] = rootX;
                } else {
                    this.parent[rootY] = rootX;
                    this.rank[rootX]++;
                }
            }
        });
        
        this.steps.push({ 
            type: 'demo-end',
            description: '批量合并完成！'
        });
        
        this.render();
    }

    render(highlightNodes = [], highlightPath = []) {
        if (!this.container) return;
        
        const theme = document.documentElement.getAttribute('data-theme') || 'dark';
        const bgColor = theme === 'dark' ? '#1a1a2e' : '#ffffff';
        const textColor = theme === 'dark' ? '#ffffff' : '#1a202c';
        const cardBg = theme === 'dark' ? '#252542' : '#f0f2f5';
        const edgeColor = theme === 'dark' ? '#4a5568' : '#a0a0b8';
        const primaryColor = '#667eea';
        const successColor = '#48c78e';
        
        const elements = Object.keys(this.parent);
        if (elements.length === 0) {
            this.container.innerHTML = `<div style="padding:40px;text-align:center;color:${textColor};">点击"初始化"开始演示</div>`;
            return;
        }
        
        // 计算节点位置（圆形布局）
        const centerX = 400;
        const centerY = 150;
        const radius = 120;
        const positions = {};
        
        elements.forEach((el, i) => {
            const angle = (2 * Math.PI * i) / elements.length - Math.PI / 2;
            positions[el] = {
                x: centerX + radius * Math.cos(angle),
                y: centerY + radius * Math.sin(angle)
            };
        });
        
        let html = `<div class="unionfind-container" style="position:relative;width:100%;height:350px;background:${bgColor};border-radius:8px;overflow:hidden;">`;
        
        // 绘制连接线
        html += `<svg style="position:absolute;top:0;left:0;width:100%;height:100%;">`;
        
        elements.forEach(el => {
            const parent = this.parent[el];
            if (parent !== el && positions[parent]) {
                const isHighlight = highlightNodes.includes(el) || highlightNodes.includes(parent);
                const isPath = highlightPath.includes(el);
                html += `<line x1="${positions[el].x}" y1="${positions[el].y}" x2="${positions[parent].x}" y2="${positions[parent].y}" stroke="${isHighlight ? '#fcd34d' : isPath ? '#48c78e' : edgeColor}" stroke-width="${isHighlight ? 3 : 2}" stroke-dasharray="${isPath ? '5,5' : ''}"/>`;
            }
        });
        
        html += `</svg>`;
        
        // 绘制节点
        elements.forEach(el => {
            const pos = positions[el];
            const isHighlight = highlightNodes.includes(el);
            const isPath = highlightPath.includes(el);
            const isRoot = this.parent[el] === el;
            
            let bgColor = primaryColor;
            if (isPath) bgColor = successColor;
            if (isHighlight) bgColor = '#fcd34d';
            
            html += `
                <div style="position:absolute;left:${pos.x - 25}px;top:${pos.y - 25}px;width:50px;height:50px;border-radius:50%;background:${bgColor};display:flex;flex-direction:column;align-items:center;justify-content:center;color:white;font-weight:bold;box-shadow:0 3px 10px rgba(0,0,0,0.3);border:3px solid ${isRoot ? '#fff' : 'transparent'};">
                    <span style="font-size:14px;">${el}</span>
                    <span style="font-size:9px;opacity:0.8;">rank:${this.rank[el]}</span>
                </div>
            `;
        });
        
        html += `</div>`;
        
        // 集合信息
        const sets = {};
        elements.forEach(el => {
            const root = this.findSilent(el);
            if (!sets[root]) sets[root] = [];
            sets[root].push(el);
        });
        
        html += `<div style="margin-top:15px;display:flex;gap:10px;flex-wrap:wrap;">`;
        Object.entries(sets).forEach(([root, members]) => {
            html += `
                <div style="padding:8px 15px;background:${cardBg};border-radius:20px;color:${textColor};font-size:12px;">
                    集合{${members.join(', ')}} <span style="color:${successColor};">根:${root}</span>
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
    setData(size) {
        this.init();
        const elements = Array.from({length: size}, (_, i) => i);
        this.makeSet(elements);
        this.render();
    }
}
