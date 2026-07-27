/**
 * Trie (字典树) 可视化器
 */

class TrieVisualizer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.root = null;
        this.steps = [];
        this.currentStep = 0;
        this.animationController = null;
        this.nodePositions = [];
        this.init();
    }

    init() {
        this.root = { children: {}, isEnd: false, char: 'root' };
        this.steps = [];
        this.currentStep = 0;
        this.render();
    }

    reset() {
        this.root = { children: {}, isEnd: false, char: 'root' };
        this.steps = [];
        this.currentStep = 0;
        this.render();
    }

    // 插入字符串
    insert(word) {
        this.steps = [];
        let node = this.root;
        this.steps.push({ type: 'start', word, description: `开始插入单词: "${word}"` });
        
        for (let i = 0; i < word.length; i++) {
            const char = word[i];
            
            if (!node.children[char]) {
                node.children[char] = { children: {}, isEnd: false, char };
                this.steps.push({ 
                    type: 'create', 
                    char, 
                    parent: node.char,
                    description: `创建新节点 '${char}'`
                });
            } else {
                this.steps.push({ 
                    type: 'exists', 
                    char, 
                    description: `节点 '${char}' 已存在，沿用`
                });
            }
            node = node.children[char];
        }
        
        if (!node.isEnd) {
            node.isEnd = true;
            this.steps.push({ 
                type: 'end', 
                word,
                description: `单词 "${word}" 插入完成，标记为结束`
            });
        }
        
        this.calculatePositions();
        this.render();
    }

    // 搜索字符串
    search(word) {
        this.steps = [];
        let node = this.root;
        this.steps.push({ type: 'search-start', word, description: `开始搜索: "${word}"` });
        
        for (let i = 0; i < word.length; i++) {
            const char = word[i];
            
            if (!node.children[char]) {
                this.steps.push({ 
                    type: 'not-found', 
                    char,
                    description: `未找到 '${char}'，搜索失败`
                });
                this.calculatePositions();
                this.render();
                return false;
            }
            
            this.steps.push({ 
                type: 'visit', 
                char, 
                path: word.substring(0, i + 1),
                description: `访问节点 '${char}'`
            });
            node = node.children[char];
        }
        
        if (node.isEnd) {
            this.steps.push({ 
                type: 'found', 
                word,
                description: `找到单词 "${word}"！`
            });
        } else {
            this.steps.push({ 
                type: 'prefix', 
                word,
                description: `"${word}" 是某单词的前缀，但本身不是完整单词`
            });
        }
        
        this.calculatePositions();
        this.render();
        return node.isEnd;
    }

    // 前缀搜索
    startsWith(prefix) {
        this.steps = [];
        let node = this.root;
        this.steps.push({ type: 'prefix-start', prefix, description: `搜索前缀: "${prefix}"` });
        
        for (let i = 0; i < prefix.length; i++) {
            const char = prefix[i];
            
            if (!node.children[char]) {
                this.steps.push({ 
                    type: 'no-prefix', 
                    description: `没有以 "${prefix}" 开头的单词`
                });
                this.calculatePositions();
                this.render();
                return [];
            }
            
            this.steps.push({ 
                type: 'prefix-visit', 
                char, 
                description: `访问 '${char}'，前缀匹配中...`
            });
            node = node.children[char];
        }
        
        this.steps.push({ 
            type: 'prefix-found', 
            prefix,
            description: `前缀 "${prefix}" 匹配成功！`
        });
        
        this.calculatePositions();
        this.render();
        return true;
    }

    // 计算节点位置
    calculatePositions() {
        this.nodePositions = {};
        const visited = new Set();
        
        const traverse = (node, x, y, level) => {
            if (!node) return;
            
            const key = node.char + (node.isEnd ? '_end' : '');
            if (!visited.has(key)) {
                visited.add(key);
                this.nodePositions[key] = { x, y };
            }
            
            const children = Object.values(node.children);
            const spacing = Math.max(40, 120 / (level + 1));
            const startX = x - (children.length - 1) * spacing / 2;
            
            children.forEach((child, i) => {
                traverse(child, startX + i * spacing, y + 70, level + 1);
            });
        };
        
        traverse(this.root, 400, 30, 0);
    }

    render(highlightKey = null) {
        if (!this.container) return;
        
        const theme = document.documentElement.getAttribute('data-theme') || 'dark';
        const bgColor = theme === 'dark' ? '#1a1a2e' : '#ffffff';
        const textColor = theme === 'dark' ? '#ffffff' : '#1a202c';
        const edgeColor = theme === 'dark' ? '#4a5568' : '#a0a0b8';
        const nodeColor = theme === 'dark' ? '#667eea' : '#667eea';
        const endNodeColor = '#48c78e';
        const highlightColor = '#fcd34d';
        
        let html = `<div class="trie-container" style="position:relative;width:100%;height:400px;background:${bgColor};border-radius:8px;overflow:hidden;">`;
        
        // 绘制边
        const drawEdges = (node, parentKey = null, parentPos = null) => {
            if (!node) return '';
            let edges = '';
            
            const key = node.char + (node.isEnd ? '_end' : '');
            const pos = this.nodePositions[key];
            
            if (parentPos && pos) {
                const isHighlight = highlightKey && (highlightKey.includes(node.char));
                edges += `<line x1="${parentPos.x}" y1="${parentPos.y + 15}" x2="${pos.x}" y2="${pos.y - 15}" stroke="${isHighlight ? highlightColor : edgeColor}" stroke-width="2"/>`;
                const midX = (parentPos.x + pos.x) / 2;
                const midY = (parentPos.y + pos.y) / 2;
                edges += `<text x="${midX + 8}" y="${midY}" fill="${textColor}" font-size="12">${node.char}</text>`;
            }
            
            Object.values(node.children).forEach(child => {
                edges += drawEdges(child, key, pos);
            });
            
            return edges;
        };
        
        html += `<svg style="position:absolute;top:0;left:0;width:100%;height:100%;">`;
        html += drawEdges(this.root);
        html += `</svg>`;
        
        // 绘制节点
        const drawNodes = (node) => {
            if (!node) return '';
            let nodes = '';
            
            const key = node.char + (node.isEnd ? '_end' : '');
            const pos = this.nodePositions[key];
            
            if (pos) {
                const isHighlight = highlightKey && (highlightKey.includes(node.char));
                const radius = node.char === 'root' ? 20 : 15;
                const fillColor = isHighlight ? highlightColor : (node.isEnd ? endNodeColor : nodeColor);
                
                nodes += `<div style="position:absolute;left:${pos.x - radius}px;top:${pos.y - radius}px;width:${radius*2}px;height:${radius*2}px;border-radius:50%;background:${fillColor};display:flex;align-items:center;justify-content:center;font-weight:bold;color:white;font-size:12px;border:2px solid ${isHighlight ? '#fff' : 'transparent'};box-shadow:0 2px 8px rgba(0,0,0,0.3);">`;
                nodes += node.char === 'root' ? 'R' : node.char.toUpperCase();
                nodes += `</div>`;
                
                if (node.isEnd) {
                    nodes += `<div style="position:absolute;left:${pos.x + radius - 6}px;top:${pos.y + radius - 6}px;width:12px;height:12px;background:#48c78e;border-radius:50%;border:2px solid ${bgColor};"></div>`;
                }
            }
            
            Object.values(node.children).forEach(child => {
                nodes += drawNodes(child);
            });
            
            return nodes;
        };
        
        html += drawNodes(this.root);
        
        // 图例
        html += `
            <div style="position:absolute;bottom:10px;left:10px;display:flex;gap:15px;font-size:11px;color:${textColor};">
                <span><span style="display:inline-block;width:12px;height:12px;border-radius:50%;background:${nodeColor};vertical-align:middle;"></span> 普通节点</span>
                <span><span style="display:inline-block;width:12px;height:12px;border-radius:50%;background:${endNodeColor};vertical-align:middle;"></span> 结束节点</span>
                <span><span style="display:inline-block;width:12px;height:12px;border-radius:50%;background:${highlightColor};vertical-align:middle;"></span> 当前操作</span>
            </div>
        `;
        
        html += `</div>`;
        
        // 当前步骤说明
        if (this.steps.length > 0 && this.currentStep < this.steps.length) {
            const step = this.steps[this.currentStep];
            html += `
                <div class="trie-step-info" style="margin-top:15px;padding:12px;background:${theme==='dark'?'#252542':'#f0f2f5'};border-radius:8px;color:${textColor};font-size:13px;">
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
    setData(words) {
        this.init();
        words.forEach(word => {
            this.insert(word);
        });
        this.render();
    }
}
