/**
 * KMP 字符串匹配可视化器
 */

class KMPVisualizer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.pattern = '';
        this.text = '';
        this.next = [];
        this.steps = [];
        this.currentStep = 0;
        this.init();
    }

    init() {
        this.pattern = '';
        this.text = '';
        this.next = [];
        this.steps = [];
        this.currentStep = 0;
        this.render();
    }

    reset() {
        this.init();
    }

    // 计算 next 数组（前缀函数）
    computeNext(pattern) {
        this.pattern = pattern;
        this.next = new Array(pattern.length).fill(0);
        this.steps = [];
        
        this.steps.push({
            type: 'next-start',
            pattern,
            description: `计算模式串 "${pattern}" 的 next 数组`
        });
        
        this.next[0] = 0;
        this.steps.push({
            type: 'next-init',
            index: 0,
            value: 0,
            description: `next[0] = 0（首字符无真前缀）`
        });
        
        for (let i = 1; i < pattern.length; i++) {
            let j = this.next[i - 1];
            
            this.steps.push({
                type: 'next-iterate',
                i,
                j,
                description: `计算 next[${i}]，当前 j = ${j}`
            });
            
            while (j > 0 && pattern[i] !== pattern[j]) {
                this.steps.push({
                    type: 'next-fail',
                    i, j,
                    charI: pattern[i],
                    charJ: pattern[j],
                    description: `${pattern[i]} != ${pattern[j]}，j 回退到 next[${j-1}] = ${this.next[j-1]}`
                });
                j = this.next[j - 1];
            }
            
            if (pattern[i] === pattern[j]) {
                j++;
            }
            
            this.next[i] = j;
            this.steps.push({
                type: 'next-set',
                i,
                value: j,
                description: `next[${i}] = ${j}`
            });
        }
        
        this.steps.push({
            type: 'next-end',
            next: [...this.next],
            description: `next 数组计算完成: [${this.next.join(', ')}]`
        });
        
        this.currentStep = this.steps.length - 1;
        this.render();
    }

    // KMP 匹配
    match(text, pattern) {
        this.text = text;
        this.pattern = pattern;
        this.steps = [];
        
        // 先计算 next 数组
        this.computeNextSilent(pattern);
        
        this.steps = [];
        this.steps.push({
            type: 'match-start',
            text,
            pattern,
            description: `在文本 "${text}" 中查找模式 "${pattern}"`
        });
        
        let j = 0;
        let found = -1;
        
        for (let i = 0; i < text.length; i++) {
            while (j > 0 && text[i] !== pattern[j]) {
                this.steps.push({
                    type: 'mismatch',
                    i, j,
                    charText: text[i],
                    charPattern: pattern[j],
                    nextJ: this.next[j - 1],
                    description: `文本[${i}]='${text[i]}' != 模式[${j}]='${pattern[j]}'，j 回退到 ${this.next[j-1]}`
                });
                j = this.next[j - 1];
            }
            
            if (text[i] === pattern[j]) {
                j++;
                this.steps.push({
                    type: 'match-char',
                    i, j,
                    char: text[i],
                    description: `匹配成功！文本[${i}]='${text[i]}' == 模式[${j-1}]='${pattern[j-1]}'`
                });
            }
            
            if (j === pattern.length) {
                found = i - pattern.length + 1;
                this.steps.push({
                    type: 'found',
                    index: found,
                    description: `在位置 ${found} 处找到匹配！`
                });
                break;
            }
        }
        
        if (found === -1) {
            this.steps.push({
                type: 'not-found',
                description: `未找到匹配`
            });
        }
        
        this.currentStep = this.steps.length - 1;
        this.render();
    }

    computeNextSilent(pattern) {
        this.pattern = pattern;
        this.next = new Array(pattern.length).fill(0);
        
        for (let i = 1; i < pattern.length; i++) {
            let j = this.next[i - 1];
            while (j > 0 && pattern[i] !== pattern[j]) {
                j = this.next[j - 1];
            }
            if (pattern[i] === pattern[j]) {
                j++;
            }
            this.next[i] = j;
        }
    }

    render(highlightI = -1, highlightJ = -1, matchRange = null) {
        if (!this.container) return;
        
        const theme = document.documentElement.getAttribute('data-theme') || 'dark';
        const bgColor = theme === 'dark' ? '#1a1a2e' : '#ffffff';
        const textColor = theme === 'dark' ? '#ffffff' : '#1a202c';
        const cardBg = theme === 'dark' ? '#252542' : '#f0f2f5';
        const primaryColor = '#667eea';
        const highlightColor = '#fcd34d';
        const successColor = '#48c78e';
        const matchBg = '#48c78e40';
        
        let html = '';
        
        // 模式串
        if (this.pattern) {
            html += `<div style="margin-bottom:20px;padding:15px;background:${cardBg};border-radius:8px;">`;
            html += `<div style="color:${textColor};font-size:12px;margin-bottom:8px;">模式串:</div>`;
            html += `<div style="display:flex;gap:4px;">`;
            for (let i = 0; i < this.pattern.length; i++) {
                const isHighlight = i === highlightJ;
                const bg = isHighlight ? highlightColor : primaryColor;
                html += `<div style="width:35px;height:40px;display:flex;flex-direction:column;align-items:center;justify-content:center;background:${bg};color:white;border-radius:6px;font-weight:bold;">
                    <span>${this.pattern[i]}</span>
                    <span style="font-size:9px;opacity:0.7;">${i}</span>
                </div>`;
            }
            html += `</div>`;
            
            // next 数组
            if (this.next.length > 0) {
                html += `<div style="display:flex;gap:4px;margin-top:15px;">`;
                for (let i = 0; i < this.next.length; i++) {
                    html += `<div style="width:35px;height:25px;display:flex;align-items:center;justify-content:center;background:${cardBg};color:${textColor};border-radius:4px;font-size:11px;">next[${i}]=${this.next[i]}</div>`;
                }
                html += `</div>`;
            }
            html += `</div>`;
        }
        
        // 文本串
        if (this.text) {
            html += `<div style="margin-bottom:20px;padding:15px;background:${cardBg};border-radius:8px;">`;
            html += `<div style="color:${textColor};font-size:12px;margin-bottom:8px;">文本串:</div>`;
            html += `<div style="display:flex;gap:4px;flex-wrap:wrap;">`;
            for (let i = 0; i < this.text.length; i++) {
                let bg = primaryColor;
                if (matchRange && i >= matchRange.start && i <= matchRange.end) {
                    bg = successColor;
                } else if (i === highlightI) {
                    bg = highlightColor;
                }
                html += `<div style="width:35px;height:40px;display:flex;flex-direction:column;align-items:center;justify-content:center;background:${bg};color:white;border-radius:6px;font-weight:bold;">
                    <span>${this.text[i]}</span>
                    <span style="font-size:9px;opacity:0.7;">${i}</span>
                </div>`;
            }
            html += `</div></div>`;
        }
        
        // 匹配指示器
        if (this.pattern && this.text) {
            const chars = this.pattern.split('');
            const maxLen = Math.max(chars.length, this.text.length);
            html += `<div style="padding:15px;background:${cardBg};border-radius:8px;margin-bottom:15px;">`;
            html += `<div style="color:${textColor};font-size:12px;margin-bottom:8px;">对齐指示:</div>`;
            html += `<div style="position:relative;height:30px;background:${bgColor};border-radius:4px;overflow:hidden;">`;
            html += `<div style="position:absolute;top:0;left:0;width:100%;height:4px;background:linear-gradient(90deg, ${primaryColor}, ${successColor});"></div>`;
            html += `</div></div>`;
        }
        
        // 当前步骤说明
        if (this.steps.length > 0 && this.currentStep < this.steps.length) {
            const step = this.steps[this.currentStep];
            html += `
                <div style="padding:12px;background:${cardBg};border-radius:8px;color:${textColor};font-size:13px;">
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
    setData(text, pattern) {
        this.init();
        this.match(text || 'ABABDABACDABABCABAB', pattern || 'ABABCABAB');
        this.render();
    }
}
