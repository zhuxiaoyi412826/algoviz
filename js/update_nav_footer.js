const fs = require('fs');
const path = require('path');
const dirs = ['d:/1/算法数据结构可视化/AlgoVize/pages', 'd:/1/算法数据结构可视化/AlgoVize'];

function processDir(dir) {
    if (!fs.existsSync(dir)) return;
    
    const files = fs.readdirSync(dir).filter(f => f.endsWith('.html') && f !== 'ai.html');

    const newFooter = `
    <!-- Footer -->
    <footer class="footer">
        <div class="container">
            <div class="footer-content">
                <div class="footer-slogan">AlgoViz - 数据结构与算法可视化学习平台 · 让学习变得更有趣</div>
                <div class="footer-bottom-row">
                    <span>AlgoViz © 2026</span>
                    <span class="footer-divider">|</span>
                    <span class="footer-icp">ICP备案：<a href="https://beian.miit.gov.cn/" target="_blank">豫ICP备12345678号</a></span>
                    <span class="footer-divider">|</span>
                    <a href="/pages/changelog.html" class="footer-link">更新日志</a>
                    <span class="footer-divider">|</span>
                    <a href="/pages/suggestion.html" class="footer-link">建议我</a>
                </div>
            </div>
        </div>
    </footer>`;

    files.forEach(f => {
        const filePath = path.join(dir, f);
        let content = fs.readFileSync(filePath, 'utf8');
        let originalContent = content;
        
        // Replace footer
        const footerRegex = /<!-- Footer -->[\s\S]*?<\/footer>/;
        
        if (footerRegex.test(content)) {
            content = content.replace(footerRegex, newFooter);
        }
        
        if (content !== originalContent) {
            fs.writeFileSync(filePath, content, 'utf8');
            console.log('Updated ' + filePath);
        }
    });
}

dirs.forEach(processDir);