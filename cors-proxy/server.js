const express = require('express');
const https = require('https');
const fs = require('fs');
const { createProxyMiddleware } = require('http-proxy-middleware');

const app = express();

const sslOptions = {
    key: fs.readFileSync('/home/99/dsaol.asia.key'),
    cert: fs.readFileSync('/home/99/dsaol.asia_bundle.pem')
};

// 解决跨域 + 伪装来源，解决403
app.use((req, res, next) => {
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
    res.header("Access-Control-Allow-Headers", "*");
    if (req.method === "OPTIONS") return res.sendStatus(200);
    next();
});

// 日志
app.use((req, res, next) => {
    console.log("\n======================================");
    console.log("📥 转入:", req.originalUrl);
    next();
});

// ✅ 核心：伪装成 dsaol.asia 访问后端，解决403
app.use('/api', createProxyMiddleware({
    target: 'http://127.0.0.1:80',
    changeOrigin: true,
    headers: {
        Host: 'dsaol.asia',
        Origin: 'http://dsaol.asia'
    },
    pathRewrite: (path) => {
        const full = '/api' + path;
        console.log("📤 转出:", full);
        return full;
    }
}));

// 启动
https.createServer(sslOptions, app).listen(3000, () => {
    console.log('✅ 代理启动成功：3000端口');
});