# AlgoViz - 数据结构与算法可视化学习平台

## 项目概览

AlgoViz 是一个交互式的数据结构和算法可视化学习网站，帮助用户通过动画深入理解常见数据结构和经典算法的执行过程。

## 技术栈

- **前端**: 原生 HTML5 + CSS3 + JavaScript (ES6+)
- **样式**: 自定义 CSS (无框架依赖)
- **字体**: Inter (正文) + Fira Code (代码)
- **服务器**: Python SimpleHTTPServer (端口 5000)

## 目录结构

```
/workspace/projects/
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
└── js/
    ├── main.js                   # 首页脚本
    ├── ai-chat.js                # AI 助手对话逻辑
    ├── datastructures.js         # 数据结构页控制器
    ├── algorithms.js             # 算法页控制器
    ├── oj.js                     # 在线OJ前端逻辑
    ├── visualizations/
    │   ├── base.js               # 基类和配置
    │   ├── array.js              # 数组可视化
    │   ├── linkedlist.js         # 链表可视化
    │   ├── stack.js              # 栈可视化
    │   ├── queue.js              # 队列可视化
    │   ├── tree.js               # 二叉树可视化
    │   ├── hash.js               # 哈希表可视化
    │   ├── graph.js              # 图可视化
    │   └── heap.js               # 堆可视化
    └── algorithms/
        ├── sorting.js            # 排序算法
        ├── searching.js          # 查找算法
        ├── traversal.js          # 树遍历
        ├── graph.js              # 图遍历
        └── dijkstra.js            # Dijkstra算法
```

## 功能特性

### 数据结构可视化

1. **数组 (Array)** - 支持访问、查找、插入、删除操作
2. **链表 (Linked List)** - 单链表，支持遍历、搜索、插入
3. **栈 (Stack)** - LIFO，支持入栈、出栈、查看栈顶
4. **队列 (Queue)** - FIFO，支持入队、出队、查看队首
5. **二叉树 (Binary Tree)** - 支持前序、中序、后序、层序遍历
6. **哈希表 (Hash Table)** - 链地址法解决冲突
7. **图 (Graph)** - 支持 DFS/BFS 遍历
8. **堆 (Heap)** - 最大堆/最小堆，支持插入、提取、排序

### 算法演示

1. **排序算法**:
   - 冒泡排序 (Bubble Sort)
   - 选择排序 (Selection Sort)
   - 插入排序 (Insertion Sort)
   - 快速排序 (Quick Sort)
   - 归并排序 (Merge Sort)
   - 堆排序 (Heap Sort)

2. **查找算法**:
   - 二分查找 (Binary Search)

3. **遍历算法**:
   - 树的遍历 (前序、中序、后序、层序)
   - 图的遍历 (DFS、BFS)
   - 最短路径 (Dijkstra)

### 动画控制

- 播放 / 暂停
- 单步执行 (前进/后退)
- 速度调节
- 重置

### 信息展示

- 算法/数据结构简介
- 时间/空间复杂度
- 核心思路说明
- 伪代码高亮

### 在线OJ功能

- **代码编辑器**: 基于 CodeMirror，支持多语言语法高亮（Java、Python、C++、JavaScript）
- **题目管理**: 题目列表、难度筛选、标签筛选
- **判题系统**: 模拟判题逻辑，支持 AC/WA/CE/RE/TLE/MLE 等状态反馈
- **编辑器功能**: 格式化、清空、复制、重置代码
- **结果展示**: 执行时间、内存占用、运行结果对比

**当前 OJ 题目列表**:
1. 1001 - 两数之和 (简单)
2. 1002 - 合并两个有序链表 (简单)
3. 1003 - 有效的括号 (简单)
4. 2001 - 买卖股票最佳时机 (中等)
5. 2002 - 全排列 (中等)
6. 3001 - 最长公共子序列 (困难)
7. 3002 - 滑动窗口最大值 (困难)

## 启动方式

```bash
# 开发环境已自动启动在端口 5000
# 访问 http://localhost:5000
```

## 访问地址

- 首页: `http://localhost:5000/`
- 数据结构: `http://localhost:5000/pages/datastructures.html`
- 算法演示: `http://localhost:5000/pages/algorithms.html`
- 在线OJ: `http://localhost:5000/pages/oj.html`
- AI 助手: `http://localhost:5000/pages/ai.html`

## 使用说明

1. **数据结构页面**: 选择数据结构类型 → 输入或随机生成数据 → 选择操作 → 观看动画
2. **算法页面**: 选择算法演示模块 → 调整数据 → 点击播放 → 观看执行过程
3. **在线OJ**: 进入独立"在线OJ"页面 → 选择题目 → 编写代码 → 运行/提交 → 查看结果
4. **AI 助手**: 点击导航栏✨进入 AI 对话界面，随时提问算法原理和代码实现

## 新增功能 (交互体验优化)

### 🤖 智能 AI 助手 (DeepSeek)
- **多轮对话管理**: 左侧侧边栏支持创建新对话和历史记录切换。
- **强大的代码支持**: 支持代码块语言高亮识别，提供一键复制功能。
- **实时流式输出**: 接入真实的 DeepSeek 接口，支持打字机效果及生成中断。
- **快捷提问**: 提供常用的算法学习 Prompt 胶囊按钮。

### 暗色模式
- 导航栏右侧提供主题切换按钮（☀️/🌙）
- 支持亮色/暗色模式切换，自动保存到 LocalStorage
- 所有页面保持一致的主题偏好

### 自定义数据输入
- **文件上传**: 支持 .txt、.json、.csv 格式文件上传
- **快速预设**: 提供随机10个、随机20个、近乎有序、倒序等快捷预设
- 支持逗号分隔的手动输入

### 动画导出
- 支持将算法执行过程导出为 GIF 动画
- 导出时显示进度条（录制中→生成GIF→完成）
- 自动下载生成的 GIF 文件
