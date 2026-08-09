# AlgoViz - 数据结构与算法可视化学习平台

## 项目概览

AlgoViz 是一个交互式的数据结构和算法可视化学习网站，帮助用户通过动画深入理解常见数据结构和经典算法的执行过程。
**访问地址**:

后台管理访问地址:

## 技术栈

- **前台**: 原生 HTML5 + CSS3 + JavaScript (ES6+)
- **后台管理:** VUE+Vite
- **样式**: 自定义 CSS (无框架依赖)
- **字体**: Inter (正文) + Fira Code (代码)
- **后端**: Spring Boot 3.2.0  jdk 17
- **数据库**: MySQL 8.0
- **部署环境**: Apache Tomcat (前端) + Java JAR (后端) + Node.js CORS代理

| 技术            | 版本        | 用途                                 |
| --------------- | ----------- | ------------------------------------ |
| Spring Boot     | 3.2.0       | 后端主框架                           |
| JDK             | 17          | 运行环境                             |
| MyBatis         | 3.0.3       | ORM 数据库操作                       |
| MySQL Connector | 父 pom 托管 | MySQL8.0 连接驱动                    |
| Knife4j         | 4.5.0       | API 文档                             |
| Lombok          | 1.18.30     | 代码简化                             |
| commons-lang3   | 3.14.0      | 通用工具                             |
| POI             | 5.2.5       | Excel 处理                           |
| Maven Compiler  | 3.11.0      | 编译                                 |
| Spring AI       | 1.0.0-M6    | 大模型接入、向量检索、RAG、Embedding |
| Nginx           |             |                                      |

## 项目结构

```

```



### 整体目录结构

```
AlgoVize/
├── index.html                    # 首页
├── pages/
│   ├── datastructures.html       # 数据结构页面
│   ├── algorithms.html           # 算法演示页面
│   ├── oj.html                   # 在线OJ独立页面
│   ├── ai.html                   # AI 智能助手页面
│   ├── login.html                # 用户登录页面
│   └── profile.html              # 个人中心页面
├── styles/
│   ├── main.css                  # 全局样式
│   ├── datastructures.css        # 数据结构页样式
│   ├── algorithms.css            # 算法页样式
│   └── oj.css                    # 在线OJ样式
├── js/
│   ├── main.js                   # 首页脚本
│   ├── ai-chat.js                # AI 助手对话逻辑
│   ├── datastructures.js         # 数据结构页控制器
│   ├── algorithms.js             # 算法页控制器
│   ├── oj.js                     # 在线OJ前端逻辑
│   ├── visualizations/
│   │   ├── base.js               # 基类和配置
│   │   ├── array.js              # 数组可视化
│   │   ├── linkedlist.js         # 链表可视化
│   │   ├── stack.js              # 栈可视化
│   │   ├── queue.js              # 队列可视化
│   │   ├── tree.js               # 二叉树可视化
│   │   ├── hash.js               # 哈希表可视化
│   │   ├── graph.js              # 图可视化
│   │   └── heap.js               # 堆可视化
│   └── algorithms/
│       ├── sorting.js            # 排序算法
│       ├── searching.js          # 查找算法
│       ├── traversal.js          # 树遍历
│       ├── graph.js              # 图遍历
│       └── dijkstra.js            # Dijkstra算法
├── houduan/                      # 后端 Spring Boot 项目
│   ├── src/main/java/com/algoviz/
│   │   ├── controller/           # 控制器
│   │   ├── service/              # 服务层
│   │   ├── repository/           # 数据访问层
│   │   ├── entity/               # 实体类
│   │   ├── dto/                  # 数据传输对象
│   │   └── config/               # 配置类
│   ├── src/main/resources/
│   │   └── application.yml        # 应用配置
│   └── pom.xml                   # Maven 配置
└── cors-proxy/                   # CORS代理服务器
    ├── server.js                 # 代理服务器代码
    └── package.json              # Node.js 依赖配置
```

## 部署

### 开发环境

前台  使用Go live 访问idnex.html 

后台管理系统  npm install npm run dev 

后台管理系统  mvn spring-boot:run

mysql jdk maven

### 服务端部署

**1.1基础软件按照**

```
# JDK 17
yum install -y java-17-openjdk java-17-openjdk-devel   # CentOS
# 或 apt install -y openjdk-17-jdk                      # Ubuntu

# Maven（仅打包机需要，服务器可省略）
yum install -y maven

# MySQL 8.0
yum install -y mysql-community-server
systemctl enable --now mysqld

# Node.js 18+（用于 cors-proxy 与后台构建）
curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
yum install -y nodejs
npm install -g pnpm

# Nginx（托管前台静态页 + 后台 dist）
yum install -y nginx
systemctl enable --now nginx
# git 下载代码
```

**1.2 放行端口**

| 端口 | 用途                                         |
| ---- | -------------------------------------------- |
| 80   | 后端 Spring Boot（HTTP）                     |
| 443  | Nginx HTTPS 入口                             |
| 3000 | cors-proxy (HTTPS 反代后端)                  |
| 3306 | MySQL（建议仅本机）                          |
| 5000 | 后台 Vite 开发服（仅本地调试，生产无需开放） |

**1.3 导入SQL文件** 

**1.4 配置文件**

（含明文密码、微信密钥），需手动放置到服务器： 修改yml 文件数据库配置  AI密钥注入环境变量

```
echo 'export DEEPSEEK_API_KEY="sk-xxxxxxxxxxxx"' > /etc/profile.d/algoviz.sh
source /etc/profile.d/algoviz.sh
```

**1.5 下载代码打包**

也可以上传直接打包好的jar包

```
https://github.com/zhuxiaoyi412826/algoviz.git
git@github.com:zhuxiaoyi412826/algoviz.git
```

java 文件打成jar包  前端文件和vue部署在nginx中

```
1 nohup node server.js > cors-proxy.log 2>&1 &    后台启动日志在 cors-proxy.log    前台启动 
2 nohup java -jar backend-1.0.0.jar > app.log 2>&1 &                             后台启动
 - nohup ... & ：脱离终端后台运行
- --spring.config.additional-location ：加载外部 application.yml 覆盖 jar 内默认值
- > app.log 2>&1 ：标准输出与错误流都写入 app.log
```

**1.6 验证是否启动成功**

```
tail -f app.log          # 看到 "Started BackendApplication" 即成功
curl http://127.0.0.1:80/api/xxx    # 接口可达
```

**1.7 部署前台服务**

上传静态文件

```
# 本地
scp -r qianduan/* root@server:/usr/share/nginx/html/
```

配置nginx

```
# /etc/nginx/conf.d/dsaol.conf
server {
    listen 443 ssl;
    server_name dsaol.asia;

    ssl_certificate     /home/99/dsaol.asia_bundle.pem;
    ssl_certificate_key /home/99/dsaol.asia.key;

    # 前台静态页
    root /usr/share/nginx/html;
    index index.html index.htm;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后台管理界面 dist
    location /admin/ {
        alias /usr/share/nginx/admin/;
        try_files $uri $uri/ /admin/index.html;
    }
}

# 80 强制跳 443
server {
    listen 80;
    server_name dsaol.asia;
    return 301 https://$host$request_uri;
}
```

### Docker

## 功能特性

### 前台

#### 数据结构可视化

**自定义数据输入**

- **文件上传**: 支持 .txt、.json、.csv 格式文件上传
- **快速预设**: 提供随机10个、随机20个、近乎有序、倒序等快捷预设
- 支持逗号分隔的手动输入

**动画导出**

- 支持将算法执行过程导出为 GIF 动画
- 导出时显示进度条（录制中→生成GIF→完成）
- 自动下载生成的 GIF 文件

#### 算法可视化演示

#### 在线OJ功能

- **代码编辑器**: 基于 CodeMirror，支持多语言语法高亮（Java、Python、C++、JavaScript）
- **题目管理**: 题目列表、难度筛选、标签筛选
- **判题系统**: 模拟判题逻辑，支持 AC/WA/CE/RE/TLE/MLE 等状态反馈
- **编辑器功能**: 格式化、清空、复制、重置代码
- **结果展示**: 执行时间、内存占用、运行结果对比

**使用说明**

1. **数据结构页面**: 选择数据结构类型 → 输入或随机生成数据 → 选择操作 → 观看动画
2. **算法页面**: 选择算法演示模块 → 调整数据 → 点击播放 → 观看执行过程
3. **在线OJ**: 进入独立"在线OJ"页面 → 选择题目 → 编写代码 → 运行/提交 → 查看结果
4. **AI 助手**: 点击导航栏✨进入 AI 对话界面，随时提问算法原理和代码实现

**自定义数据输入**

- **文件上传**: 支持 .txt、.json、.csv 格式文件上传
- **快速预设**: 提供随机10个、随机20个、近乎有序、倒序等快捷预设
- 支持逗号分隔的手动输入

**动画导出**

- 支持将算法执行过程导出为 GIF 动画
- 导出时显示进度条（录制中→生成GIF→完成）
- 自动下载生成的 GIF 文件

#### 🤖 智能 AI 助手 (DeepSeek)

- **多轮对话管理**: 左侧侧边栏支持创建新对话和历史记录切换。
- **强大的代码支持**: 支持代码块语言高亮识别，提供一键复制功能。
- **实时流式输出**: 接入真实的 DeepSeek 接口，支持打字机效果及生成中断。
- **快捷提问**: 提供常用的算法学习 Prompt 胶囊按钮。

**暗色模式**

- 导航栏右侧提供主题切换按钮（☀️/🌙）
- 支持亮色/暗色模式切换，自动保存到 LocalStorage
- 所有页面保持一致的主题偏好

#### 登录

**🔐 微信公众号登录**

- **验证码登录**: 用户在网页获取6位数字验证码，在微信公众号输入验证码完成登录
- **实时状态轮询**: 前端每2秒轮询一次登录状态
- **安全验证**: 支持微信消息签名验证和加密消息解密

### 后台

## 未来更新