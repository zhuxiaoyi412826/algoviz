const fs = require('fs');
const path = require('path');

const dirs = ['d:/1/算法数据结构可视化/AlgoVize/pages', 'd:/1/算法数据结构可视化/AlgoVize'];

function addOjNav(dir) {
    if (!fs.existsSync(dir)) return;
    
    const files = fs.readdirSync(dir).filter(f => f.endsWith('.html'));

    files.forEach(f => {
        const filePath = path.join(dir, f);
        let content = fs.readFileSync(filePath, 'utf8');
        let originalContent = content;
        
        // Find algo nav link
        const algoLinkRegex = /(<a href="\/pages\/algorithms\.html"[^>]*>算法演示<\/a>)/;
        
        // If OJ link is not already there, add it
        if (algoLinkRegex.test(content) && !content.includes('href="/pages/oj.html"')) {
            content = content.replace(algoLinkRegex, '$1\n                <a href="/pages/oj.html" class="nav-link">在线OJ</a>');
        }
        
        if (content !== originalContent) {
            fs.writeFileSync(filePath, content, 'utf8');
            console.log('Updated nav in ' + filePath);
        }
    });
}

dirs.forEach(addOjNav);
