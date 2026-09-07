# AlgoViz - 数据结构与算法可视化学习平台

## 项目概览

AlgoViz 是一个交互式的数据结构和算法可视化学习网站，帮助用户通过动画深入理解常见数据结构和经典算法的执行过程，并提供在线 OJ、AI 答疑、面试题库（双路检索）与付费解锁等能力。

**访问地址（本地开发）**
- 前台门户：http://localhost:5500（VSCode Go Live 打开 `qianduan` 目录）
- 后台管理：http://localhost:5000（Vite 开发服）
- 后端 API ：http://localhost:80（Spring Boot；Knife4j 文档 /doc.html）

---

## 技术栈

| 层级 | 技术栈 |
| --- | --- |
| 前台门户 | 原生 HTML5 + CSS3 + JavaScript |
| 后台管理 | Vue3 + Vite + Element Plus + ECharts |
| 后端服务 | Spring Boot 3.2.0 + JDK 17 + MyBatis 3.0.3 + Sa-Token（后台 RBAC）+ Spring Security OAuth2 Client |
| 前台鉴权 | AuthInterceptor（Session + Cookie 双凭证，独立于后台 RBAC，另兼容 X-User-Id 头） |
| 向量服务 | Python FastAPI + Uvicorn + SentenceTransformers（面试题）+ Java know-qrdant（算法题） |
| 数据库 | MySQL 8.0（业务库）+ ChromaDB 1.5.9（面试题向量）+ Qdrant（算法题向量）+ Redis（缓存） |
| AI 模型 | DeepSeek（对话大模型）+ BAAI/bge-small-zh-v1.5（面试题向量 512 维）+ BAAI/bge-large-zh-v1.5（算法题向量 1024 维，ONNX Runtime） |
| 通信方式 | OpenFeign（Java ↔ Python）+ RESTful API + Dubbo 3（Java ↔ Java 子服务 know-qrdant） |

**核心组件版本**

| 技术 | 版本 | 用途 |
| --- | --- | --- |
| Spring Boot | 3.2.0 | 后端主框架 |
| JDK | 17 | 运行环境 |
| MyBatis | 3.0.3 | ORM 数据库操作 |
| Sa-Token | 1.37.0 | 后台管理员 RBAC 鉴权（双轨之一） |
| Spring Security OAuth2 Client | Boot 3.2 托管 | 前台第三方登录（GitHub + Gitee 授权码） |
| MySQL Connector | 父 pom 托管 | MySQL 8.0 连接驱动 |
| Redis | - | 缓存 / 敏感词 / 去重 / 待审核队列 |
| SentenceTransformers | - | 语句向量化（bge-small-zh-v1.5） |
| ChromaDB | 1.5.9 | 面试题语义向量库 |
| Qdrant | 1.x | 算法题向量库（HNSW m=16 + Cosine，1024 维） |
| onnxruntime | 1.17.1 | Java 端 bge-large-zh-v1.5 模型推理 |
| FastAPI + Uvicorn | - | Python 向量检索服务 |
| Dubbo | 3.2.20 | 主服务 ↔ know-qrdant 直连 |
| OpenFeign | - | Java ↔ Python 服务通信 |
| Knife4j | 4.5.0 | API 文档 |
| Apache POI | 5.2.5 | Excel 处理 |
| Jsoup | 1.17.2 | Markdown 富文本 XSS 清洗 |
| Lombok / commons-lang3 | 1.18.30 / 3.14.0 | 代码简化 / 工具 |
| Nginx | - | 前台静态页 + 后台 dist + HTTPS |
| Elasticsearch | 7.12.1 | 面试题全文检索 + 日志存储（IK 中文分词） |
| IK Analyzer | 7.12.1 | 中文分词（ik_max_word 索引 / ik_smart 搜索） |
| Kibana / Fluentd / Metricbeat | 7.12.1 / 4.3.3 / 7.12.1 | 日志可视化 / 采集 / 指标监控 |

---

## 项目结构

| 目录 | 说明 |
| --- | --- |
| qianduan | 前台门户（原生静态页 + 中转页 oauth-callback.html） |
| houtai | 后台管理系统（Vue3 + Element Plus） |
| houduan | 后端核心业务服务（含 config/security OAuth2、水印切面等） |
| algo-common-api | 公共模块：know-api（Dubbo 接口）+ know-qrdant（算法题向量检索独立子服务） |
| Agent | 智能体 Agent 开发项目（Python 面试题向量检索） |
| bin | 快捷启动文件 |
| doc | 文档与脚本：测试文档/脚本、请求流程、OAuth 实现总结、上线工具、JVM 日志方案 |

---

## 部署

### 开发环境

#### 前置环境与启动顺序
```
MySQL → Redis → Elasticsearch → Java 后端 → Agent 服务（含 ChromaDB）→ 前端页面
```

- 后端/后台管理需：JDK 17、Maven、MySQL 8.0、Redis
- Agent（Python）：fastapi / uvicorn / chromadb / sentence-transformers（BAAI/bge-small-zh-v1.5）/ numpy / pydantic

#### 核心启动

**1. 后端 Java 服务（houduan）**
```
mvn spring-boot:run
```
> 第三方登录（GitHub/Gitee）需先配置环境变量再启动；未配置时主程序照常运行，登录页第三方按钮置灰：
> ```powershell
> $env:OAUTH_GITHUB_CLIENT_ID="..."; $env:OAUTH_GITHUB_CLIENT_SECRET="..."
> $env:OAUTH_GITEE_CLIENT_ID="...";  $env:OAUTH_GITEE_CLIENT_SECRET="..."
> ```
> 详细接入/回调/踩坑见 `doc/md/第三方授权登录实现总结-SpringSecurityOAuth2Client.md`

**2. 前台门户（qianduan）**
```
使用 VSCode「Go Live」直接打开 qianduan/index.html
```

**3. 后台管理系统（houtai）**
```
npm install
npm run dev
```

**4. Agent 智能体（agent/know-retrieval）**
```
python run.py
# 向量库
chroma run --path ./chroma_data --host 0.0.0.0 --port 8000
```

**5. 算法题向量子服务（algo-common-api/know-qrdant）**
```
# ① 启动 Qdrant（默认 REST 6333 / gRPC 6334）
D:\software\Qdrant\qdrant-x86_64-pc-windows-msvc\qdrant.exe

# ② 模型转换（一次性，产物写入 ~/.cache/huggingface/hub/java-bge-large-zh-v1.5/）
cd AlgoVize\algo-common-api\know-qrdant
pip install onnxscript onnx
python tools\export_onnx.py

# ③ 启动子服务（Dubbo 20999 / HTTP 8090）
cd AlgoVize\algo-common-api\know-qrdant
mvn spring-boot:run
# 探活：curl http://localhost:8090/health → {"modelReady":true,"qdrantConnected":true}

# ④ 主服务通过 Dubbo 直连 20999（check=false + mock 降级）；子服务未启动不影响核心功能
```

### 服务端部署

**1.1 基础软件安装**
```
# JDK 17
yum install -y java-17-openjdk java-17-openjdk-devel    # CentOS
# apt install -y openjdk-17-jdk                          # Ubuntu

# Maven（仅打包机需要）
yum install -y maven

# MySQL 8.0
yum install -y mysql-community-server
systemctl enable --now mysqld

# Node.js 18+（cors-proxy 与后台构建）
curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
yum install -y nodejs
npm install -g pnpm

# Nginx（托管前台静态页 + 后台 dist）
yum install -y nginx
systemctl enable --now nginx
```

**1.2 放行端口**
| 端口 | 用途 |
| --- | --- |
| 80 | 后端 Spring Boot（HTTP） |
| 443 | Nginx HTTPS 入口 |
| 3000 | cors-proxy（HTTPS 反代后端） |
| 3306 | MySQL（建议仅本机） |
| 5000 | 后台 Vite 开发服（仅本地调试，生产不开放） |
| 8000 | ChromaDB |
| 8001 | Python know-retrieval 向量/ES 检索服务 |
| 8090 | know-qrdant HTTP 探活（/health） |
| 20999 | know-qrdant Dubbo RPC（主服务直连） |
| 6333/6334 | Qdrant REST / gRPC |
| 9200 / 9300 / 5601 | Elasticsearch / 集群通信 / Kibana |

**1.3 导入 SQL**
导入 `houduan/src/main/resources/db/algovize.sql`（含历史迁移段注释，按注释顺序执行）。

**1.4 配置文件**
配置文件（含数据库密码、微信/支付密钥）手动放到服务器后，修改 yml 数据库配置；AI 等密钥注入环境变量：
```
echo 'export DEEPSEEK_API_KEY="sk-xxxxxxxxxxxx"' > /etc/profile.d/algoviz.sh
source /etc/profile.d/algoviz.sh
```

**1.5 下载代码并打包**
```
git clone https://github.com/zhuxiaoyi412826/algoviz.git
# Java 打成 jar；前端文件与 Vue(dist) 部署到 Nginx
```
后台启动示例：
```
# cors-proxy
nohup node server.js > cors-proxy.log 2>&1 &
# 后端
nohup java -jar backend-1.0.0.jar > app.log 2>&1 &
# 说明：--spring.config.additional-location=file:./config/ 可加载外部 application.yml 覆盖 jar 内默认值
```

**1.6 验证启动**
```
tail -f app.log    # 看到 "Started BackendApplication" 即成功
curl http://127.0.0.1:80/api/xxx
```

**1.7 部署前台静态页 + Nginx**
```
scp -r qianduan/* root@server:/usr/share/nginx/html/
```
```nginx
# /etc/nginx/conf.d/dsaol.conf
server {
    listen 443 ssl;
    server_name dsaol.asia;
    ssl_certificate     /home/99/dsaol.asia_bundle.pem;
    ssl_certificate_key /home/99/dsaol.asia.key;
    root /usr/share/nginx/html;
    index index.html index.htm;
    location / { try_files $uri $uri/ /index.html; }
    location /admin/ {
        alias /usr/share/nginx/admin/;
        try_files $uri $uri/ /admin/index.html;
    }
}
# 80 强制跳 443
server { listen 80; server_name dsaol.asia; return 301 https://$host$request_uri; }
```

### Docker
详细步骤见 `doc/txt/Docker部署流程.txt`。要点：后端 8080 由 Nginx(80/443) 统一接管；MySQL 首次启动通过 volume 挂载 `algovize.sql` 初始化；SSL 证书手动放入 `docker/ssl/` 且文件名与 nginx.conf 一致。

---

## 功能特性

### 前台

#### 支付
1. **权限分级**：基础可视化免费；高清 GIF 导出、大文件解析、高阶算法模块（线段树、图复杂演示）需付费解锁
2. **支付流程**：前端生成微信支付二维码并轮询状态；后端对接微信支付 API 生成预支付单、校验回调
3. **订单管理**：保存订单号/支付时间/过期时长，支付成功自动解锁权限（绑定账号）
4. **回调与异常**：处理超时、取消、重复回调；失败可重新唤起支付；成功后实时解锁无需刷新

#### 数据结构可视化
- 数组、链表、栈、队列、哈希表、堆、树、图、线段树、并查集、Trie、KMP、UnionFind 等
- 支持 .txt / .json / .csv 上传、随机生成、近乎有序/倒序预设、手动输入
- 逐步动画演示；执行过程导出 GIF（带进度条）

#### 算法可视化演示
- 排序：冒泡/选择/插入/希尔/归并/快速/堆/桶/计数/基数
- 经典：动态规划、BFS/DFS、贪心、递归、字符串匹配、Dijkstra、KMP 等

#### 在线 OJ
- CodeMirror 编辑器（Java/Python/C++/JavaScript 高亮），格式化/清空/复制/重置
- 模拟判题：AC/WA/CE/RE/TLE/MLE，展示执行时间/内存/结果对比
- 统一分页条（≤8 全显示、首尾省略号、当前页高亮、跳页钳制）+ 状态记忆（localStorage 保存分页/排序/筛选，返回自动恢复）

#### 🤖 智能 AI 助手（DeepSeek）
- 多轮对话（新建/历史切换）、代码块高亮与一键复制、流式输出可中断、常用 Prompt 胶囊

#### 暗色模式
- 全局主题切换（☀️/🌙），LocalStorage 持久化，跨页一致

#### 面试题库（双模式搜索）
- 关键词搜索（ES IK 分词）：多字段加权（标题 4.0 > 标签 3.0 > 分类 1.5 > 描述 1.0）+ 命中高亮 + 降级 MySQL LIKE
- 语义搜索：bge-small-zh 句向量近邻召回
- 做题状态（待做/已做/收藏/点赞）、Markdown 题解、统一分页条与状态持久化（单一真源 `algovize:interview-list.state`）

#### 用户系统
- 微信公众号验证码登录（6 位验证码 + 2 秒轮询）
- 个人中心、金币系统、产品购买、主题切换

#### 登录方式
- **微信公众号登录**：验证码登录 + 实时轮询 + 消息签名/加密解密
- **账号密码登录**（图形验证码）
- **邮箱验证码登录**
- **第三方授权登录（GitHub / Gitee）**：Spring Security OAuth2 Client 授权码模式；首次授权自动注册（`user_oauth` 绑定：bind_scene=1、登录计数、raw_profile 快照），再次授权直接登录；未配置平台优雅降级（按钮置灰）；成功经中转页直达首页

#### 用户中心（个人中心）
- 修改邮箱 / 昵称 / 性别 / 头像（emoji 或图片 URL，URL 实时校验预览）
- 无密码账号首次「设置密码」/ 已有密码「修改密码」/ 「忘记密码」找回
- 只读：用户名、硬币余额、注册时间、最后登录

### 后台

#### 1. 内容管理
- 算法/数据结构、动画配置、OJ 题目、测试用例、判题配置
- 面试题：CRUD、JSON/Excel 批量导入、AI 批量生成
- 向量库管理（VectorManage）：数量/维度监控、全量同步、分页查看
- ES 索引管理（EsManage）：统计/mapping、全量同步、重建（IK）、分词测试
- 算法题目向量管理（AlgorithmVector）：全量同步/取消/清空 + 进度条
- Qdrant 实时检测（QdrantMonitor）：集合信息 + 向量检索查看
- OJ 语义搜索链路

#### 2. AI 配置
- AIConfig（模型接口）、AIPrompt（快捷提示词）

#### 3. 金币 / 订单系统
- 商品管理、购买记录、微信支付管理；全量订单、状态统计、付费记录、订单导出

#### 4. 用户管理
- 用户列表、行为分析、登录记录、管理员账号（RBAC 多角色分级）
- 账号状态三态（1 正常 / 0 封禁 / -1 注销）+ 逻辑删除（is_deleted），注销/删除即时踢下线，后台三种状态均可见
- 用户访问统计独立 `user_visit_stat` 表（高频计数器拆表，解耦 user 主表）

#### 5. 统计与监控
- Dashboard / 数据导出 / OJ 分析 / 可视化分析
- 系统与资源监控、告警、操作日志、登录日志
- Dashboard 首屏优化：去除低选择性索引、只读 stat 表 `COUNT(*)` 聚合，消除首访 3s 卡顿

#### 6. 系统扩展
- 公告、反馈、数据备份、第三方配置、系统配置

#### 7. 安全与合规
- **后台页面全局水印**：登录后全屏 Canvas 防截图水印，侧栏不覆盖、折叠联动、MutationObserver 防删防隐藏自动恢复，样式可配，super_admin 同样生效
- **登录页合规条款**：左侧保密条例（3 条）+ 员工守则（2 条）
- **接口水印（@ResponseWatermark + ResponseBodyAdvice）**：敏感接口 JSON 根节点追加 `_watermark`（userId/username/tenantId/clientIp/traceId/accessTime）事后溯源；未标注接口零开销
- 操作审计日志（OperationLogAspect 自动记录后台写操作）

---

## 核心业务模块

### 1. 业务模块（40+ Controller，按功能分域）
- 认证：账号密码/邮箱/微信扫码/第三方 OAuth（GitHub、Gitee）
- 面试题：管理端 CRUD + 用户端查询/点赞/收藏/浏览记录
- OJ：题目管理、提交判题、代码运行
- AI：对话、AI 生成题目
- 支付：微信支付、订单、金币
- 内容：算法/数据结构/公告/反馈/文件上传导出
- 用户：个人资料、注销、访问统计上报
- 向量：Chroma 语义 + ES 关键词 + Qdrant 算法题检索、同步进度监控、索引管理

### 2. 特色技术点
- **双轨鉴权**：前台 AuthInterceptor(Session/Cookie/X-User-Id) 与后台 Sa-Token RBAC 相互独立；Spring Security OAuth2 Client 仅接管第三方授权端点（permitAll 兼容层 + 优雅降级 + 可空仓库）
- **页面 + 接口双层水印**：Canvas 页面水印 + ResponseBodyAdvice `_watermark` 接口水印
- TraceId 全链路：日志 / 响应头 / 接口水印三处一致
- XSS 过滤：Jsoup 清洗 Markdown 富文本；Excel 导入导出（POI）
- AOP 操作审计日志；API 文档（Knife4j）；文件上传 10MB 本地存储
- EFK 日志链路（Logstash → ES → Kibana）+ 敏感词 DFA 审核体系

---

## 功能实现

### 向量检索服务（双路检索架构）

```
┌──────────┐        ┌──────────────────┐  OpenFeign  ┌────────────────────┐
│ 前端      │ ──────→│ Java Spring Boot │ ──────────→ │ Python know-retrieval│
│ 双搜索框  │        │ :80              │             │ :8001               │
└──────────┘        │ VectorSearchService│            └─────────┬──────────┘
                    └──────────────────┘                      │
                              │                     ┌─────────┴──────────┐
                              │ MySQL 回源查详情     ▼                    ▼
                              │              ChromaDB(语义)      Elasticsearch(关键词)
                              │              bge-small-zh 512维   IK 分词 + BM25
                              └←─── 按相关性顺序返回题目 ID ────┘
```

| 能力 | 语义搜索 | 关键词搜索 |
| --- | --- | --- |
| 底层 | ChromaDB HNSW + sentence-transformers | ES 7.12.1 + IK 分词器 |
| 向量/分词 | bge-small-zh-v1.5，512 维 float32 | ik_max_word 索引 / ik_smart 搜索 |
| 原理 | 句向量余弦近邻召回 | 多字段加权（标题 4.0 / 标签 3.0 / 分类 1.5 / 描述 1.0 / content 0.5）+ match_phrase + BM25 |
| 适用 | “意思相近但用词不同” | 术语精确命中（单调栈/红黑树） |

**全量同步与实时进度**
- 触发：管理页「全量同步」（向量 100 条/批、ES 200 条/批）与导入后增量同步
- 进度：`SyncProgressHolder` + `GET /api/vector/admin/sync/progress`，前端 1 秒轮询展示进度/失败/剩余/耗时
- 幂等：ES `_id` / Chroma ID 均取 MySQL 主键，重复同步覆盖写
- 降级：ES/向量异常自动降级 MySQL LIKE
- 落盘：`Agent/know-retrieval/data/chroma_db/`，备份需整目录拷贝

> 详细设计见 `doc/txt/ES分词搜索与索引同步全流程文档.md`

### 算法题语义检索（Qdrant + Dubbo 3 子服务）

```
┌──────────┐      ┌──────────────────┐  Dubbo 3 直连   ┌────────────────────────┐
│ 前台 oj-list │ ──→│ Java 主服务 :80     │ ──(20999)──→ │ know-qrdant 子服务      │
│ 语义搜索    │    │ OJProblemController │              │  (HTTP 8090 探活)       │
└──────────┘      └──────────────────┘              └──────────┬─────────────┘
                                        ONNX Runtime 推理        │ REST 6333
                                        bge-large-zh-v1.5       ▼
                                        文本 → 1024 维向量   Qdrant（algorithm_knowledge）
                                                               HNSW + Cosine
```

- 索引：后台「算法题目向量管理」手动全量同步（只向量化题目标题），稳定点 id 幂等覆盖，支持进度/取消/清空
- 检索：输入题目名称/标签/自然语言 → 向量化 → Qdrant 余弦近邻 → 按 ID 回查 MySQL
- 通信：`@DubboReference(check=false, mock降级, url=dubbo://127.0.0.1:20999)`；子服务未启动不影响核心功能
- 模型：bge-large-zh-v1.5（1024 维），`tools/export_onnx.py` 一次性转换（IR9 兼容 onnxruntime 1.17）
- 详细设计见 `doc/算法题目向量检索子服务know-qrdant方案总结.md`

### EFK 日志与敏感词审核

**架构总览**
```
┌─────────────────────────────────────────────────────────────────┐
│                        SpringBoot 应用层                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────────┐ │
│  │ 业务模块 │  │日志埋点  │  │MDC链路ID │  │关键词过滤服务   │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────────┬────────┘ │
└───────┼─────────────┼─────────────┼───────────────────┼──────────┘
        └─────────────┴──────┬──────┴───────────────────┘
                             │ Logback TCP
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        ELK 日志处理层                             │
│  ┌──────────┐    ┌──────────────┐    ┌──────────────────────┐  │
│  │ Logstash │───▶│ Elasticsearch│───▶│    Kibana 可视化     │  │
│  │ 日志采集  │    │ 存储+分词检索 │    │    看板+查询         │  │
│  └──────────┘    └──────┬───────┘    └──────────────────────┘  │
│                         │ IK分词器                               │
└─────────────────────────┼───────────────────────────────────────┘
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      业务功能层                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │分词日志查询  │  │关键词屏蔽系统│  │定时分析+人工审核     │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘  │
└─────────┼─────────────────┼──────────────────────┼──────────────┘
          ▼                 ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                        MySQL 数据层                               │
│  关键词库表 | 审核记录表 | 推送任务表 | 系统配置表                │
└─────────────────────────────────────────────────────────────────┘
```

**敏感词审核闭环**：DFA 实时过滤器 → 命中审计 → 候选词/置信度 → 人工审核 → 正式词库热加载；ES 高频可疑词定时分析触发告警，与 Kibana 看板联动。

> 图：![敏感词审核流程](https://zhuxiaoyi-1300958454.cos.ap-guangzhou.myqcloud.com/img/image-20260816221302185.png)
