# 算法可视化平台 - 管理后台

## 项目概述

基于 Vue 3 + TypeScript + Vite 构建的管理后台系统，用于管理算法可视化平台的所有功能模块。

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue | 3.5 |
| 语言 | TypeScript | 6.0 |
| 构建工具 | Vite | 8.0 |
| 路由 | Vue Router | 4.6 |
| 状态管理 | Pinia | 3.0 |
| UI组件 | Element Plus | 2.13 |
| 图表 | ECharts | 6.0 |
| 样式 | SCSS | 1.99 |

## 功能模块

### 1. 系统管理模块
- 管理员账户管理（多角色、密码重置、权限分配）
- 登录日志（IP/时间/设备/异常告警）
- 操作日志（增删改查全记录）
- 系统配置（网站标题/主题/CORS代理）
- 安全设置（频率限制/验证码/SSL配置）

### 2. 内容管理模块
- 数据结构可视化配置
- 算法可视化配置
- 动画参数配置
- OJ题目管理
- 测试用例管理
- 判题规则配置
- AI快捷提示词管理
- AI接口配置

### 3. 用户管理模块
- 用户列表
- 登录记录查询
- 行为数据统计

### 4. 数据统计模块
- 核心指标看板（DAU/WAU/MAU）
- OJ运营分析
- 可视化使用分析
- 数据导出（Excel/CSV）

### 5. 运维监控模块
- 服务状态监控
- 资源占用监控
- 异常告警
- 日志管理

### 6. 扩展功能模块
- 公告管理
- 反馈管理
- 数据备份/恢复
- 第三方集成

## 目录结构

```
src/
├── api/                 # API接口模块
│   ├── request.ts       # Axios请求封装
│   ├── system.ts        # 系统管理接口
│   ├── content.ts       # 内容管理接口
│   ├── user.ts          # 用户管理接口
│   ├── statistics.ts    # 统计接口
│   ├── monitor.ts       # 运维监控接口
│   └── extension.ts     # 扩展功能接口
├── components/          # 公共组件
├── layout/              # 布局组件
│   ├── index.vue        # 主布局
│   ├── header/          # 顶部导航
│   └── sider/           # 侧边栏
├── router/              # 路由配置
├── stores/              # Pinia状态管理
│   └── user.ts          # 用户状态
├── styles/              # 全局样式
├── types/               # TypeScript类型定义
└── views/               # 页面组件
    ├── system/          # 系统管理模块
    ├── content/         # 内容管理模块
    ├── user/            # 用户管理模块
    ├── statistics/      # 数据统计模块
    ├── operation/       # 运维监控模块
    └── extension/       # 扩展功能模块
```

## 开发命令

```bash
# 安装依赖
pnpm install

# 开发模式
pnpm run dev

# 构建生产版本
pnpm run build

# 预览生产版本
pnpm run preview
```

## 开发规范

### 组件规范
- 使用 Composition API (`<script setup>`)
- 组件名使用 PascalCase
- Props 使用 TypeScript 类型定义

### 样式规范
- 使用 SCSS 预处理器
- CSS 变量定义在 `styles/variables.scss`
- 组件样式使用 `scoped`

### API 规范
- 使用 RESTful 风格
- 统一使用 `request.ts` 封装
- 包含请求/响应拦截器

## 登录信息

- 默认账号: `admin`
- 默认密码: `admin123`
- 登录页面: `/login`

## 部署指南

### 1. 构建生产版本

```bash
pnpm install
pnpm run build
```

### 2. Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /path/to/houtai/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api/ {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 3. 启动服务

```bash
# 启动后端服务
java -jar backend-1.0.0.jar

# 启动Nginx
# Windows: start nginx
# Linux: sudo systemctl start nginx
```

## 环境变量

可通过 `.env` 文件配置：

```env
VITE_API_BASE_URL=http://localhost/api
```

## 技术亮点

1. **响应式布局**: 支持多种屏幕尺寸
2. **权限控制**: 基于角色的访问控制
3. **性能优化**: 懒加载、缓存策略
4. **代码规范**: ESLint + Prettier
5. **类型安全**: 完整的TypeScript类型定义

## License

MIT
