/**
 * 动态规划 (DP) 可视化器
 */

class DPVisualizer {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.dp = [];
        this.text1 = '';
        this.text2 = '';
        this.steps = [];
        this.currentStep = 0;
        this.type = 'lcs'; // 'lcs' or 'knapsack'
        this.init();
    }

    init() {
        this.dp = [];
        this.steps = [];
        this.currentStep = 0;
        this.render();
    }

    reset() {
        this.init();
    }

    // 最长公共子序列
    lcs(text1, text2) {
        this.type = 'lcs';
        this.text1 = text1;
        this.text2 = text2;
        this.steps = [];
        
        const m = text1.length;
        const n = text2.length;
        
        this.dp = Array(m + 1).fill(null).map(() => Array(n + 1).fill(0));
        
        this.steps.push({
            type: 'init',
            description: `初始化 ${m}x${n} 的 DP 表`,
            dp: this.dp.map(row => [...row])
        });
        
        for (let i = 1; i <= m; i++) {
            for (let j = 1; j <= n; j++) {
                if (text1[i - 1] === text2[j - 1]) {
                    this.dp[i][j] = this.dp[i - 1][j - 1] + 1;
                    this.steps.push({
                        type: 'match',
                        i, j,
                        char1: text1[i - 1],
                        char2: text2[j - 1],
                        value: this.dp[i][j],
                        description: `text1[${i-1}]='${text1[i-1]}' == text2[${j-1}]='${text2[j-1]}'，dp[${i}][${j}] = ${this.dp[i][j]}`
                    });
                } else {
                    this.dp[i][j] = Math.max(this.dp[i - 1][j], this.dp[i][j - 1]);
                    this.steps.push({
                        type: 'skip',
                        i, j,
                        char1: text1[i - 1],
                        char2: text2[j - 1],
                        value: this.dp[i][j],
                        maxFrom: this.dp[i - 1][j] >= this.dp[i][j - 1] ? `dp[${i-1}][${j}]` : `dp[${i}][${j-1}]`,
                        description: `不匹配，取 max(dp[${i-1}][${j}], dp[${i}][${j-1}]) = ${this.dp[i][j]}`
                    });
                }
            }
        }
        
        // 回溯找最长公共子序列
        const lcs = [];
        let i = m, j = n;
        while (i > 0 && j > 0) {
            if (text1[i - 1] === text2[j - 1]) {
                lcs.unshift(text1[i - 1]);
                i--; j--;
            } else if (this.dp[i - 1][j] > this.dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        
        this.steps.push({
            type: 'result',
            lcs: lcs.join(''),
            length: this.dp[m][n],
            description: `最长公共子序列: "${lcs.join('')}"，长度: ${this.dp[m][n]}`
        });
        
        this.currentStep = this.steps.length - 1;
        this.render();
    }

    // 0-1 背包问题
    knapsack(weights, values, capacity) {
        this.type = 'knapsack';
        this.steps = [];
        
        const n = weights.length;
        this.dp = Array(n + 1).fill(null).map(() => Array(capacity + 1).fill(0));
        
        this.steps.push({
            type: 'init',
            description: `初始化 ${n}x${capacity + 1} 的 DP 表`,
            dp: this.dp.map(row => [...row])
        });
        
        for (let i = 1; i <= n; i++) {
            const w = weights[i - 1];
            const v = values[i - 1];
            
            this.steps.push({
                type: 'item-start',
                i, w, v,
                description: `考虑物品 ${i}: 重量=${w}, 价值=${v}`
            });
            
            for (let c = 0; c <= capacity; c++) {
                if (c < w) {
                    this.dp[i][c] = this.dp[i - 1][c];
                    this.steps.push({
                        type: 'skip',
                        i, c,
                        reason: '容量不足',
                        value: this.dp[i][c],
                        description: `容量 ${c} < 物品重量 ${w}，不能装入，dp[${i}][${c}] = dp[${i-1}][${c}] = ${this.dp[i][c]}`
                    });
                } else {
                    const notTake = this.dp[i - 1][c];
                    const take = this.dp[i - 1][c - w] + v;
                    this.dp[i][c] = Math.max(notTake, take);
                    
                    this.steps.push({
                        type: 'choice',
                        i, c,
                        notTake, take,
                        value: this.dp[i][c],
                        description: `容量 ${c}: 不装=${notTake} vs 装=${take}，取最大值 ${this.dp[i][c]}`
                    });
                }
            }
        }
        
        this.steps.push({
            type: 'result',
            maxValue: this.dp[n][capacity],
            description: `最大价值: ${this.dp[n][capacity]}`
        });
        
        this.currentStep = this.steps.length - 1;
        this.render();
    }

    render() {
        if (!this.container) return;
        
        const theme = document.documentElement.getAttribute('data-theme') || 'dark';
        const bgColor = theme === 'dark' ? '#1a1a2e' : '#ffffff';
        const textColor = theme === 'dark' ? '#ffffff' : '#1a202c';
        const cardBg = theme === 'dark' ? '#252542' : '#f0f2f5';
        const primaryColor = '#667eea';
        const highlightColor = '#fcd34d';
        const successColor = '#48c78e';
        
        let html = '';
        
        if (this.type === 'lcs' && this.text1 && this.text2) {
            // LCS 可视化
            html += `<div style="margin-bottom:20px;padding:15px;background:${cardBg};border-radius:8px;">`;
            html += `<div style="display:flex;gap:20px;color:${textColor};font-size:13px;">`;
            html += `<div>字符串1: <span style="color:${primaryColor};font-weight:bold;">${this.text1}</span></div>`;
            html += `<div>字符串2: <span style="color:${successColor};font-weight:bold;">${this.text2}</span></div>`;
            html += `</div></div>`;
            
            // DP 表
            html += `<div style="overflow-x:auto;padding:15px;background:${bgColor};border-radius:8px;">`;
            html += `<table style="border-collapse:collapse;margin:0 auto;">`;
            
            // 表头
            html += `<tr><td style="padding:8px;border:1px solid ${cardBg};"></td>`;
            html += `<td style="padding:8px;border:1px solid ${cardBg};color:${textColor};font-size:11px;text-align:center;">ε</td>`;
            for (let j = 0; j < this.text2.length; j++) {
                html += `<td style="padding:8px;border:1px solid ${cardBg};background:${successColor};color:white;font-weight:bold;min-width:35px;text-align:center;">${this.text2[j]}</td>`;
            }
            html += `</tr>`;
            
            // 表格内容
            for (let i = 0; i <= this.text1.length; i++) {
                html += `<tr>`;
                if (i === 0) {
                    html += `<td style="padding:8px;border:1px solid ${cardBg};"></td>`;
                } else {
                    html += `<td style="padding:8px;border:1px solid ${cardBg};background:${primaryColor};color:white;font-weight:bold;min-width:35px;text-align:center;">${this.text1[i-1]}</td>`;
                }
                
                for (let j = 0; j <= this.text2.length; j++) {
                    const isHighlight = this.dp[i] && this.dp[i][j] > 0 && i > 0 && j > 0;
                    const isMax = this.dp[i] && this.dp[i][j] === Math.max(...this.dp.map(row => Math.max(...row)));
                    
                    let bg = theme === 'dark' ? '#1a1a2e' : '#ffffff';
                    if (isMax && i > 0 && j > 0) bg = successColor + '40';
                    if (isHighlight && i === this.dp.length - 1 && j === this.dp[0].length - 1) bg = highlightColor;
                    
                    html += `<td style="padding:8px;border:1px solid ${cardBg};background:${bg};color:${textColor};min-width:35px;text-align:center;font-weight:bold;">${this.dp[i] ? this.dp[i][j] : ''}</td>`;
                }
                html += `</tr>`;
            }
            html += `</table></div>`;
        }
        
        if (this.type === 'knapsack' && this.dp.length > 0) {
            html += `<div style="overflow-x:auto;padding:15px;background:${bgColor};border-radius:8px;">`;
            html += `<table style="border-collapse:collapse;margin:0 auto;">`;
            
            // 表头
            html += `<tr><td style="padding:8px;border:1px solid ${cardBg};"></td>`;
            for (let c = 0; c < this.dp[0].length; c++) {
                html += `<td style="padding:8px;border:1px solid ${cardBg};color:${textColor};font-size:11px;min-width:35px;text-align:center;">${c}</td>`;
            }
            html += `</tr>`;
            
            // 表格内容
            for (let i = 0; i < this.dp.length; i++) {
                html += `<tr>`;
                html += `<td style="padding:8px;border:1px solid ${cardBg};color:${textColor};font-size:11px;background:${cardBg};">${i === 0 ? 'ε' : '物品' + i}</td>`;
                
                for (let j = 0; j < this.dp[i].length; j++) {
                    const isLastRow = i === this.dp.length - 1;
                    const bg = isLastRow && this.dp[i][j] === Math.max(...this.dp[this.dp.length - 1]) ? highlightColor : (theme === 'dark' ? '#1a1a2e' : '#ffffff');
                    html += `<td style="padding:8px;border:1px solid ${cardBg};background:${bg};color:${textColor};min-width:35px;text-align:center;font-weight:bold;">${this.dp[i][j]}</td>`;
                }
                html += `</tr>`;
            }
            html += `</table></div>`;
        }
        
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
    setData(values, weights) {
        this.init();
        this.lcs('ABCBDAB', 'BDCABA');
        this.render();
    }
}
