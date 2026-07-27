const fs = require('fs');
const path = require('path');
const dirs = ['d:/1/算法数据结构可视化/AlgoVize/pages'];

function processDir(dir) {
    const files = fs.readdirSync(dir).filter(f => f.endsWith('.html'));

    const newStr = `<a href="javascript:void(0)" class="nav-link" id="profileLink">
                    <span class="nav-avatar" id="navAvatar">👤</span>
                </a>
                <a href="/pages/ai.html" class="nav-link ai-link">
                    <span>✨</span> AI助手
                </a>
            </div>
            <div class="nav-actions">
                <div class="visit-counter">访问量: <span id="visitCount">0</span></div>
                <button id="themeToggle" class="theme-switch" aria-label="切换主题">
                    <div class="theme-switch-handle">
                        <span class="theme-icon">☀️</span>
                    </div>
                </button>
            </div>`;

    files.forEach(f => {
        const filePath = path.join(dir, f);
        let content = fs.readFileSync(filePath, 'utf8');
        let originalContent = content;
        
        // 1. replace html block
        const oldRegex1 = /<a href="javascript:void\(0\)" class="nav-link" id="profileLink">[\s\S]*?<span class="nav-avatar" id="navAvatar">👤<\/span>[\s\S]*?<\/a>[\s\S]*?<\/div>[\s\S]*?<div class="nav-actions">[\s\S]*?<div class="visit-counter">访问量: <span id="visitCount">0<\/span><\/div>[\s\S]*?<button id="themeToggle"[\s\S]*?>🌙<\/button>[\s\S]*?<\/div>/;
        
        if (oldRegex1.test(content)) {
            content = content.replace(oldRegex1, newStr);
        }
        
        // 2. replace JS theme default logic
        content = content.replace(/\|\| 'dark'/g, "|| 'light'");
        
        // 3. replace JS theme logic
        const toggleLogicRegex = /btn\.textContent = theme === 'dark' \? '🌙' : '☀️';[\s\S]*?btn\.style\.borderColor = theme === 'dark' \? '#2d2d4a' : '#e2e8f0';/;
        if (toggleLogicRegex.test(content)) {
            content = content.replace(toggleLogicRegex, "var icon = btn.querySelector('.theme-icon');\n                if (icon) {\n                    icon.textContent = theme === 'dark' ? '🌙' : '☀️';\n                }");
        }
        
        if (content !== originalContent) {
            fs.writeFileSync(filePath, content, 'utf8');
            console.log('Updated ' + filePath);
        }
    });
}

dirs.forEach(processDir);
