# Java 后端面试知识库索引

> 以下 38 本资料为 Agent 出题的核心知识来源，合计 3,549 页、5,000+ 章节条目。
> 出题时优先从对应知识模块中提取高频真题，确保题目质量对标互联网大厂面试标准。

---

## 资料清单

### 综合面试宝典（7 本）

| 序号 | 资料名称 | 页数 | 章节数 | 适用等级 |
|------|---------|------|--------|---------|
| 1 | 2021最新Java面试题及答案 | 283 | 930 | 校招/初级/中级 |
| 2 | JavaGuide面试突击版5.0 | 436 | 568 | 初级/中级/高级 |
| 3 | Java面试宝典 | 227 | 1,235 | 校招/初级/中级 |
| 4 | Java面试题2023大合集(485页) | 485 | 883 | 全等级 |
| 5 | 阿里内部资料2023版(266页) | 266 | 525 | 中级/高级/大厂压轴 |
| 6 | 阿里面经 | 114 | 14 | 高级/大厂压轴 |
| 7 | 分布式面试 | 197 | 331 | 中级/高级/大厂压轴 |

### 大厂真实面经（7 本）

| 序号 | 资料名称 | 页数 | 适用等级 | 核心价值 |
|------|---------|------|---------|---------|
| 8 | 阿里淘天一面秒挂（附答案） | 18 | 中级/高级 | 阿里真实面试题+详细答案解析 |
| 9 | 大厂四年-阿里字节蚂蚁小红书面试 | 15 | 高级 | 跨大厂横向对比，高频交叉考点 |
| 10 | 快手Java一面11个基础问题（附答案） | 20 | 初级/中级 | 基础扎实度检验标杆 |
| 11 | 美团暑期实习转面经（附答案） | 22 | 校招/初级 | 实习+校招经典面试题 |
| 12 | 上海三年半社招PDD字节证券面经 | 3 | 中级 | PDD/字节/证券多行业面试 |
| 13 | 四年半社招腾讯滴滴字节京东快手美团蚂蚁 | 6 | 中级/高级 | 最大跨度大厂面试集锦 |
| 14 | OPPO二面凉凉（附答案） | 18 | 中级 | 附详细解析的二面高频题 |

### JavaGuide 2025 最新分册（新增 5 本）

| 序号 | 资料名称 | 页数 | 章节题数 | 适用等级 |
|------|---------|------|--------|---------|
| 15 | JavaGuide2025-Java基础 | 78 | 50题 | 校招/初级/中级 |
| 16 | JavaGuide2025-集合 | 45 | 25题 | 初级/中级 |
| 17 | JavaGuide2025-JVM | 42 | 31题 | 初级/中级/高级 |
| 18 | JavaGuide2025-设计模式 | 51 | 30题 | 中级/高级 |
| 19 | JavaGuide2025-5.0暗黑版（2025升级） | 436 | 50题 | 全等级 |

> 注：序号19为 JavaGuide 5.0 的 2025 最新升级版，新增 Kafka 面试题、真实大厂面试现场章节。

### 消息队列 & 分布式深度专题（2 本）

| 序号 | 资料名称 | 页数 | 章节题数 | 适用等级 |
|------|---------|------|--------|---------|
| 20 | 消息队列常见面试题（深度解析） | 104 | 50题 | 中级/高级/大厂压轴 |
| 21 | 分布式相关面试题汇总 | 141 | 17题 | 中级/高级/大厂压轴 |

### 计算机基础专题（1 本）

| 序号 | 资料名称 | 页数 | 章节题数 | 适用等级 |
|------|---------|------|--------|---------|
| 22 | 图解计算机网络（443页详版） | 443 | 50题 | 全等级 |

### 技术栈专项面试题（15 本）

| 序号 | 资料名称 | 页数 | 核心模块 |
|------|---------|------|---------|
| 23 | MySQL面试题 | 11 | 索引优化、分库分表、B+Tree、触发器 |
| 24 | Redis面试题 | 7 | 集群、内存淘汰、场景选型 |
| 25 | Spring面试题 | 4 | IOC/AOP、SpringBoot、SpringCloud |
| 26 | Spring Cloud面试题 | 4 | 微服务设计、Feign、Gateway、断路器 |
| 27 | RocketMQ面试题 | 7 | 角色架构、消费模式、与其他MQ选型 |
| 28 | Kafka面试题 | 3 | 高吞吐原理、Zookeeper作用、流处理 |
| 29 | RabbitMQ面试题 | 5 | 路由、集群、概念体系 |
| 30 | Dubbo面试题 | 5 | 集群容错、协议选型、SPI |
| 31 | Nginx面试题 | 6 | 高并发实现、动静分离、Master/Worker |
| 32 | Docker面试题 | 4 | 镜像容器、Dockerfile、vs 虚拟机 |
| 33 | Netty面试题 | 14 | NIO模型、零拷贝、TCP粘包拆包 |
| 34 | Elasticsearch面试题 | 6 | 集群架构、索引概念、NRT原理 |
| 35 | ClickHouse面试题 | 10 | 列式存储、应用场景、架构特性 |
| 36 | MongoDB面试题 | 3 | NoSQL特性、索引、适用场景 |
| 37 | Memcached面试题 | 8 | 多线程、vs Redis、Session共享 |

---

## 一、JavaSE 核心

### 1.1 JVM（来源：资料1、2、3、4、5）

**新生代/老年代/永久代/元空间** — 资料1 p24-25、资料4 p33-34
- Eden区、SurvivorFrom、SurvivorTo
- MinorGC 过程（复制→清空→互换）
- Java8 元数据替换永久代

**垃圾回收与算法** — 资料1 p26-29、资料2 p190-210、资料4 p35-39
- 引用计数法 vs 可达性分析
- 标记清除(Mark-Sweep)、复制(Copying)、标记整理(Mark-Compact)、分代收集
- 四种引用类型（强/软/弱/虚）

**GC 收集器** — 资料1 p31-34、资料2 p210-230、资料4 p40-44
- Serial、ParNew、Parallel Scavenge
- Serial Old、Parallel Old、CMS（初始标记→并发标记→重新标记→并发清除）
- G1 收集器

**类加载机制** — 资料1 p41-44、资料2 p170-180、资料4 p45-50
- 加载→验证→准备→解析→初始化
- 双亲委派模型（Bootstrap→Extension→Application）
- 打破双亲委派场景（Tomcat、SPI）

**IO/NIO** — 资料1 p34-41
- 5 种 IO 模型（阻塞/非阻塞/多路复用/信号驱动/异步）
- Channel、Buffer、Selector
- BIO vs NIO vs AIO

### 1.2 集合（来源：资料1、2、3、4、5）

**HashMap 底层** — 资料1 p50-51、资料2 p50-70、资料4 p16-19
- Java7 数组+链表 → Java8 数组+链表+红黑树
- 扩容机制、负载因子、put/get 流程
- ConcurrentHashMap：Segment 分段锁(Java7) → CAS+synchronized(Java8)

**List/Set** — 资料1 p45-50、资料2 p40-49
- ArrayList（数组）、LinkedList（双向链表）、Vector（线程安全）
- HashSet（HashMap实现）、TreeSet（红黑树）、LinkedHashSet

**并发集合** — 资料1 p51-54
- ConcurrentHashMap、CopyOnWriteArrayList、BlockingQueue

### 1.3 多线程并发（来源：资料1、2、3、4、6）

**线程基础** — 资料1 p54-58、资料2 p80-100、资料4 p56-62
- 4 种创建方式（Thread、Runnable、Callable+Future、线程池）
- 线程状态（NEW→RUNNABLE→RUNNING→BLOCKED→DEAD）
- sleep vs wait、start vs run
- 4 种线程池（Cached、Fixed、Scheduled、Single）

**锁机制** — 资料1 p63-72、资料2 p120-160、资料4 p63-70
- Synchronized 原理、锁升级（偏向锁→轻量级锁→重量级锁）
- ReentrantLock（公平/非公平）、可重入性
- AQS 底层原理
- ReadWriteLock、Semaphore、CountDownLatch、CyclicBarrier

**CAS 与原子类** — 资料1 p69、资料2 p130-140、资料4 p67-68
- AtomicInteger、CAS 原理、ABA 问题
- Unsafe 类

**ThreadLocal** — 资料2 p150-160、资料4 p71
- 原理、内存泄漏问题

**锁优化** — 资料1 p71-72
- 减少锁持有时间、减小锁粒度、锁分离、锁粗化、锁消除

---

## 二、Spring 全家桶（来源：资料2、3、4、5）

### 2.1 Spring 核心
- **IOC** — 资料2 p270-290、资料4 p80-86：控制反转、依赖注入（构造器/Setter/注解）
- **AOP** — 资料2 p290-310、资料4 p84-85：动态代理（JDK/CGLIB）、AspectJ、通知类型
- **Bean 生命周期** — 资料2 p280-300、资料4 p86-88：实例化→属性注入→Aware→后置处理器→初始化→销毁
- **循环依赖** — 资料2 p300-310、资料4 p92-93：三级缓存解决（一级singletonObjects/二级earlySingletonObjects/三级singletonFactories）
- **事务** — 资料2 p320-330、资料4 p92-94：隔离级别、传播特性、编程式/声明式
- **设计模式** — 资料4 p89：单例、工厂、代理、模板方法、观察者、适配器

### 2.2 SpringMVC — 资料4 p80-84
- 全流程（DispatcherServlet→HandlerMapping→HandlerAdapter→Handler→ViewResolver）
- 常用注解（@Controller、@RequestMapping、@RequestParam、@RequestBody）

### 2.3 SpringBoot — 资料4 p101-105
- 自动装配原理（@SpringBootApplication→@EnableAutoConfiguration→spring.factories）
- Starter 自定义
- 配置加载顺序、热部署

### 2.4 SpringCloud — 资料4 p117-122、资料5 p？
- Nacos/Eureka 注册发现、Gateway 网关
- Feign/OpenFeign 远程调用、Ribbon 负载均衡
- Sentinel/Hystrix 熔断降级
- Spring Cloud Alibaba 组件

---

## 三、MySQL（来源：资料1、2、3、4、5、7）

### 3.1 引擎与索引
- **InnoDB vs MyISAM** — 资料4 p106：事务支持、锁粒度、外键、崩溃恢复
- **B+Tree 原理** — 资料2 p450-470、资料4 p107-111：聚簇索引 vs 非聚簇索引、最左前缀原则
- **索引类型** — 资料4 p113：主键、唯一、普通、联合、全文

### 3.2 事务与锁
- **事务隔离级别** — 资料1 p76-80、资料4 p109：RU/RC/RR/S，脏读/不可重复读/幻读
- **MVCC** — 资料2 p470-490、资料4 p114：undo log + ReadView，快照读 vs 当前读
- **锁机制** — 资料2 p490-510、资料4 p115：行锁/表锁/间隙锁/临键锁、死锁排查

### 3.3 SQL 优化
- **慢 SQL 排查** — 资料2 p520-540、资料4 p107-108：EXPLAIN、慢查询日志
- **三大日志** — redo log、undo log、binlog

### 3.4 分库分表 — 资料7 p174-190、资料4 p110
- 垂直拆分/水平拆分、ShardingSphere
- Snowflake 雪花算法、全局 ID 方案
- 不停机数据迁移（双写方案）

---

## 四、Redis（来源：资料1、2、4、5、7）

### 4.1 数据结构与使用
- **5 种数据类型** — 资料4 p166：String/Hash/List/Set/ZSet 及场景
- **其他结构** — BitMap、HyperLogLog、Geo、Stream

### 4.2 持久化与高可用
- **RDB vs AOF** — 资料4 p170-171、资料7 p63-69
- **主从复制** — 资料7 p76-83：全量复制、增量复制、断点续传
- **哨兵架构** — 资料7 p84-90：sdown/odown、Leader 选举、故障转移
- **Cluster 集群** — 资料7 p90-99：哈希槽(16384)、Gossip 协议、节点通信

### 4.3 缓存问题
- **缓存穿透/击穿/雪崩** — 资料4 p177-178、资料7 p99-102
- **双写一致性** — 资料7 p102-108：Cache Aside Pattern、异步串行化
- **过期策略** — 资料7 p71-73：惰性删除+定期删除、8 种内存淘汰策略

### 4.4 分布式锁 — 资料7 p140-146
- Redis 实现（SETNX+Lua）、Redisson
- vs Zookeeper 分布式锁

---

## 五、消息队列（来源：资料4、5、7）

- 为什么用 MQ：解耦、异步、削峰 — 资料7 p11-17
- RabbitMQ/RocketMQ/Kafka 区别 — 资料4 p133-135
- 消息可靠性（生产/存储/消费三阶段）— 资料7 p26-29
- 消息顺序性 — 资料7 p29-32
- 消息积压处理 — 资料7 p32-35
- 重复消费与幂等性 — 资料7 p24-26

---

## 六、分布式系统（来源：资料4、5、7）

### 6.1 分布式理论
- CAP 理论、BASE 理论
- 一致性算法（Paxos、Raft、ZAB）

### 6.2 分布式事务 — 资料7 p163-170、资料4 p184-192
- XA 两阶段提交、TCC、可靠消息最终一致性、最大努力通知

### 6.3 分布式 ID — 资料4 p194-196
- UUID、数据库自增、Snowflake 雪花算法、美团 Leaf

### 6.4 限流与熔断 — 资料4 p197-201
- 计数器、滑动窗口、漏桶、令牌桶
- Sentinel/Hystrix

### 6.5 负载均衡 — 资料4 p197
- 随机、轮询、加权、最少连接、一致性 Hash

### 6.6 RPC 框架 — 资料7 p121-126
- Dubbo 工作流程、负载均衡策略、容错策略、SPI 机制

---

## 七、计算机网络（来源：资料4、6）

- TCP 三次握手/四次挥手 — 资料4 p204-209、资料6 p66-85
- TCP 可靠性保证
- HTTP/HTTPS、HTTP1.0/1.1/2.0 区别 — 资料4 p216
- OSI 七层模型、TCP/IP 四层
- 跨域（JSONP/CORS/代理）

---

## 八、Linux / Git / DevOps（来源：资料4）

### Linux — 资料4 p139-140
- 常用排查命令（top、ps、netstat、lsof、df、free）
- 日志分析（grep、awk、sed、tail）
- 权限管理（chmod、chown）

### Git — 资料4 p249-257
- 分支策略（GitFlow）
- git merge vs git rebase
- git cherry-pick、git stash
- 版本回退（reset/revert）

---

## 九、大厂压轴专题（来源：资料6、7）

### 9.1 性能优化 — 资料6 p18-43
- JVM 调优（GC 日志分析、堆内存配置）
- 接口优化（缓存、异步、批量、池化）
- 数据库优化（索引、SQL、连接池、读写分离）
- 代码层面优化（对象复用、线程池、算法优化）

### 9.2 高并发系统设计 — 资料7 p171-173
- 系统拆分、缓存、MQ、读写分离、分库分表
- CDN、静态化、限流降级

### 9.3 线程池深度 — 资料6 p44-65
- 核心参数、拒绝策略、调优实践

### 9.4 设计模式实战 — 资料4 p223-229
- 单例（DCL、枚举）、代理、工厂、观察者、责任链、策略

---

## 十、阿里终面专题（来源：资料6）

- 简历与面试策略 — 资料6 p86-108
- 终面官考察重点（学习能力、技术深度、业务理解、团队协作）
- 应届生 vs 社招面试侧重点

---

## 十一、JVM 调优（来源：资料1、2、4、5、6）

### 11.1 内存模型与区域
- **运行时数据区** — 资料2 p180-190、资料4 p33-35：堆/方法区/虚拟机栈/本地方法栈/程序计数器
- **新生代/老年代** — 资料1 p24-25：Eden/Survivor From/Survivor To、对象晋升机制
- **元空间 vs 永久代** — 资料4 p34：Java8 移除永久代、元空间使用本地内存

### 11.2 GC 算法与收集器
- **GC 算法** — 资料2 p190-210：标记清除/复制/标记整理/分代收集
- **收集器选型** — 资料1 p31-34、资料2 p210-230：Serial/ParNew/Parallel/CMS/G1/ZGC
- **CMS 四阶段** — 资料4 p40-42：初始标记→并发标记→重新标记→并发清除、CMS 缺陷
- **G1 原理** — 资料4 p42-44：Region 划分、Remembered Set、Mixed GC、Pause Prediction Model
- **GC 日志分析** — 资料6 p18-30：-XX:+PrintGCDetails、Full GC 频率与耗时、GC 停顿时间分析

### 11.3 类加载机制
- **加载流程** — 资料1 p41-44、资料2 p170-180：加载→验证→准备→解析→初始化
- **双亲委派** — 资料4 p45-47：Bootstrap→Extension→Application、SPI 打破场景
- **Tomcat 类加载** — 资料4 p48：WebAppClassLoader 打破双亲委派

### 11.4 JMM 与线程
- **JMM 内存模型** — 资料4 p63-65：主内存/工作内存、happens-before 八大规则
- **volatile 语义** — 资料2 p140-150：可见性、禁止指令重排、不保证原子性
- **synchronized 锁升级** — 资料1 p63-68：偏向锁→轻量级锁→重量级锁

### 11.5 线上排查实战
- **CPU 飙升排查** — 资料6 p25-30：top→top -Hp→jstack→线程状态分析
- **OOM 排查** — 资料6 p30-35：jmap dump、MAT 分析大对象、堆内存泄漏定位
- **JVM 参数调优** — 资料6 p18-25：-Xms/-Xmx/-Xmn、MetaspaceSize、GC 参数

### 11.6 出题方向
- 简答题：G1 和 CMS 的区别？ZGC 为什么能做到亚毫秒停顿？
- 场景题：线上 Full GC 频繁，每次停顿 3 秒，如何排查和调优？
- 代码题：手写一段触发 OOM 的代码并分析堆内存变化

---

## 十二、Agent 开发（Java 工程视角）

### 12.1 Agent 架构模式
- **ReAct 模式**：Reasoning + Acting 循环，Thought→Action→Observation→Thought
- **Plan-Execute 模式**：先规划任务分解再逐步执行，适用于复杂多步任务
- **多 Agent 协作**：Supervisor-Worker、Debate、Hierarchical 等编排模式
- **Agent vs 传统 Workflow**：动态决策 vs 固定流程，适用场景对比

### 12.2 Function Calling 与 Tool Use
- **Function Calling 机制**：大模型结构化输出函数名+参数、后端执行函数并回传结果
- **Tool 定义规范**：JSON Schema 描述工具入参/出参、OpenAI/Anthropic 工具格式差异
- **Tool 编排策略**：单工具调用 vs 并行工具调用 vs 工具链式调用
- **安全边界**：工具权限控制、执行超时、沙箱隔离、用户确认机制

### 12.3 Agent 记忆与状态管理
- **短期记忆**：对话上下文窗口管理、滑动窗口/摘要压缩
- **长期记忆**：向量数据库存储历史交互、语义检索召回
- **状态持久化**：Agent 会话状态存储到 Redis/DB、断点续传与恢复
- **多轮上下文裁剪**：Token 预算管理、消息重要性排序

### 12.4 Java Agent 框架
- **Spring AI**：ChatClient、Advisor 链、Tool Callback、RAG 支持
- **LangChain4j**：AiServices、Tools、Memory、RAG、Structured Output
- **自定义 Agent 框架**：基于 Function Calling 手搭 Agent Loop、状态机管理
- **与 Spring 生态集成**：Spring Boot Starter 封装、配置化管理、Actuator 监控

### 12.5 Agent 后端落地场景
- **智能客服**：多轮对话+知识库检索+工单系统集成
- **代码审查 Agent**：PR 自动审查、代码规范检查、安全漏洞扫描
- **自动化运维 Agent**：日志异常检测→根因分析→自动修复脚本生成
- **数据分析 Agent**：自然语言转 SQL、自动生成报表、数据洞察

### 12.6 出题方向
- 简答题：ReAct 模式和 Plan-Execute 模式各自适用什么场景？多 Agent 协作有哪些编排模式？
- 场景题：设计一个智能客服 Agent，要求支持多轮对话、知识库检索和工单创建，请给出完整架构
- 代码题：用 Spring AI / LangChain4j 实现一个带 Function Calling 的简单 Agent

---

## 十三、RAG 架构（检索增强生成）

### 13.1 文档处理与分块
- **分块策略**：固定长度分块、按语义/段落分块、递归字符分块、Markdown 结构化分块
- **Chunk 大小选择**：过小导致语义割裂、过大导致检索精度下降、常见 256-1024 token
- **元数据保留**：来源文件/页码/章节标题，用于检索结果溯源

### 13.2 Embedding 与向量化
- **Embedding 模型选型**：OpenAI text-embedding-3、BGE、Cohere、本地模型
- **维度与存储**：768/1024/1536 维向量、量化压缩（PQ/SQ）
- **多语言支持**：跨语言 Embedding、中英混合检索

### 13.3 向量数据库
- **Milvus**：分布式架构、IVF/HNSW 索引、标量过滤
- **Pgvector**：PostgreSQL 扩展、HNSW/IVFFlat 索引、与业务数据联合查询
- **Elasticsearch KNN**：稀疏+稠密混合检索、BM25 + KNN 融合
- **Redis Vector**：RediSearch 模块、轻量级向量检索
- **选型对比**：吞吐量/延迟/数据量/运维成本/与现有架构集成度

### 13.4 检索策略
- **稠密检索**：语义相似度搜索、cosine/L2 距离
- **稀疏检索**：BM25/TF-IDF 关键词匹配
- **混合检索**：稠密+稀疏融合（RRF 融合策略）、加权排序
- **Rerank 重排序**：Cross-Encoder 模型二次排序、Cohere Rerank、bge-reranker

### 13.5 RAG 全链路优化
- **Query 改写**：同义改写、Query 分解多子问题、HyDE 假设文档检索
- **上下文管理**：Top-K 选择、上下文窗口溢出处理、去重
- **引用溯源**：回答中标注来源片段、用户可点击跳转原文
- **RAG 评估**：Faithfulness（忠实度）、Answer Relevance（答案相关性）、Context Recall/Precision

### 13.6 Java 集成 RAG
- **Spring AI**：VectorStore 抽象、ETL pipeline（DocumentReader/Transformer/Writer）
- **LangChain4j**：EmbeddingStore、ContentRetriever、RAG Chat Assistant
- **全链路实现**：文档上传→分块→Embedding→存入向量库→检索→Rerank→拼 Prompt→调 LLM

### 13.7 常见问题与优化
- **幻觉问题**：Prompt 约束"仅基于检索内容回答"、引用溯源验证
- **召回率低**：优化分块策略、Query 改写、混合检索
- **响应延迟**：异步 Embedding、缓存高频 Query 结果、Rerank 仅对 Top-N

### 13.8 出题方向
- 简答题：RAG 中稠密检索和稀疏检索各自的优缺点？混合检索的 RRF 融合是什么原理？
- 场景题：公司内部知识库有 10 万篇文档，用户检索召回率只有 40%，如何系统性优化 RAG 链路？
- 代码题：用 LangChain4j 实现一个简单的 RAG 流程：文档加载→分块→Embedding→检索→生成

---

## 十四、项目经验（行为面试 + 技术深挖）

### 14.1 STAR 方法
- **Situation**：项目背景、业务场景、团队规模、你的角色
- **Task**：你负责的具体任务、面临的技术挑战
- **Action**：你的技术方案、决策过程、具体实现
- **Result**：量化成果（QPS 提升 xx%、响应时间降低 xx%、节省 xx 成本）

### 14.2 技术选型与 Trade-off
- **选型决策**：为什么选 A 不选 B？对比维度（性能/成本/可维护性/团队熟悉度/社区活跃度）
- **Trade-off 分析**：CAP 取舍、一致性 vs 可用性、性能 vs 成本、灵活性 vs 复杂度
- **技术债管理**：何时引入技术债、如何量化、如何规划偿还

### 14.3 项目难点深挖
- **高并发场景**：如何发现瓶颈？方案设计→压测验证→逐步优化过程
- **数据一致性**：跨服务/跨库数据一致性方案、补偿机制、对账设计
- **线上故障**：故障发现→定位→止血→根因分析→预防措施（监控/告警/预案）
- **架构演进**：单体→微服务、单库→分库分表、手动→自动化，演进驱动因素

### 14.4 系统设计能力
- **架构图描述**：能清晰画出系统架构、说清数据流向、组件职责
- **容量估算**：日活/QPS/存储量/带宽估算，基于估算做技术选型
- **高可用设计**：冗余/熔断/降级/限流、故障演练、多机房容灾

### 14.5 团队协作与软实力
- **Code Review**：Review 标准与流程、如何处理分歧、Review 文化建设
- **技术分享与文档**：技术方案文档规范、Wiki 维护、新人 Onboarding
- **跨团队协作**：需求对齐、接口定义、联调排期、冲突解决

### 14.6 出题方向
- 简答题：请用 STAR 方法描述你做过最有挑战的一个项目。你在项目中最大的技术收获是什么？
- 场景题：你的项目 QPS 从 1000 涨到 10000，你做了哪些架构改造？请描述你的决策过程和遇到的坑。
- 场景题：线上发生了一次 P0 故障，请从发现到复盘完整描述你的处理过程，以及后续改进了什么。

---

## 十五、多线程并发（来源：资料1、2、4、5、6）

### 15.1 线程基础
- **创建方式** — 资料1 p54-58：Thread/Runnable/Callable+Future/线程池
- **线程状态** — 资料4 p56-62：NEW→RUNNABLE→BLOCKED→WAITING→TIMED_WAITING→TERMINATED
- **核心方法** — 资料2 p80-100：sleep vs wait、start vs run、yield/join、interrupt

### 15.2 线程池
- **四种线程池** — 资料1 p58-62：Cached/Fixed/Scheduled/Single
- **核心参数** — 资料6 p44-65：corePoolSize/maxPoolSize/queue/keepAliveTime/factory/handler
- **拒绝策略** — AbortPolicy/CallerRunsPolicy/DiscardOldestPolicy/DiscardPolicy
- **线程池调优** — 资料6 p50-55：CPU 密集型 vs IO 密集型、动态参数调整、线程池监控

### 15.3 并发工具
- **CountDownLatch vs CyclicBarrier** — 资料1 p69-70
- **Semaphore** — 限流场景
- **CompletableFuture** — 异步编排、thenApply/thenCompose/allOf/anyOf
- **Fork/Join 框架** — 分治思想、工作窃取算法

### 15.4 ThreadLocal
- 原理（ThreadLocalMap、弱引用 Key）、内存泄漏与清理、InheritableThreadLocal

### 15.5 出题方向
- 简答题：线程池 execute() 和 submit() 的区别？核心线程能否被回收？
- 场景题：线上接口偶发超时，排查发现线程池队列堆积 10 万任务，如何处理？
- 代码题：手写一个简单的线程池实现

---

## 十六、锁机制（来源：资料1、2、4、5）

### 16.1 synchronized
- **底层原理** — 资料1 p63-68：monitor 对象、monitorenter/monitorexit
- **锁升级** — 偏向锁→轻量级锁（自旋）→重量级锁、锁撤销与锁膨胀
- **synchronized vs Lock** — 资料4 p63-65

### 16.2 ReentrantLock 与 AQS
- **ReentrantLock** — 资料2 p120-140：公平/非公平、lock/unlock/tryLock
- **AQS 源码** — 资料2 p130-145：state、CLH 变体队列、独占/共享模式、ConditionObject
- **ReentrantReadWriteLock** — 读读共享/读写互斥/写写互斥

### 16.3 CAS 与原子类
- **CAS 原理** — 资料1 p69：Unsafe.compareAndSwap、自旋
- **ABA 问题** — AtomicStampedReference
- **原子类** — Basic/Array/Reference/FieldUpdater/Accumulator

### 16.4 锁优化
- 锁粗化、锁消除（逃逸分析）、减小锁粒度、读写分离

### 16.5 死锁
- **死锁四条件** — 互斥/占有等待/不可抢占/循环等待
- **排查** — jstack 死锁检测、Arthas thread -b

### 16.6 出题方向
- 简答题：synchronized 锁升级过程？AQS 的 CLH 队列如何实现？
- 场景题：线上 CPU 100%，jstack 发现已锁死，如何排查解决？
- 代码题：用 AQS 手写一个不可重入锁

---

## 十七、分布式事务（来源：资料4、5、7）

### 17.1 理论基础
- **CAP** — 一致性/可用性/分区容错、三选二
- **BASE** — 基本可用/软状态/最终一致性

### 17.2 分布式事务方案
- **2PC** — 资料7 p163-165：协调者/参与者、同步阻塞、单点故障
- **3PC** — CanCommit/PreCommit/DoCommit、降低阻塞
- **TCC** — 资料7 p165-168：Try/Confirm/Cancel、业务侵入
- **Saga** — 长事务拆分+补偿、正向/反向操作
- **可靠消息最终一致性** — 本地消息表/事务消息
- **最大努力通知** — 通知重试 + 对账兜底

### 17.3 Seata 框架
- **角色** — TC（事务协调者）/TM（事务管理器）/RM（资源管理器）
- **AT 模式** — undo_log 自动补偿、全局锁、两阶段
- **TCC 模式** — 手动 Try/Confirm/Cancel
- **SAGA 模式** — 状态机引擎、补偿服务
- **XA 模式** — 数据库 XA 协议、强一致性
- **XID 传播** — 跨服务 RootContext 传递
- **高可用** — TC 集群部署、注册中心（Nacos/Eureka）

### 17.4 出题方向
- 简答题：Seata AT 模式如何保证数据一致性？TCC 和 AT 各自的适用场景？
- 场景题：电商下单涉及订单服务、库存服务、账户服务，如何设计分布式事务方案？
- 场景题：Seata AT 模式出现脏写问题，如何排查和解决？

---

## 十八、分布式缓存（来源：资料4、7）

### 18.1 多级缓存
- **本地缓存** — Caffeine（W-TinyLFU）、Guava Cache、Spring Cache
- **多级架构** — 本地缓存 → Redis → DB、缓存一致性保证

### 18.2 缓存三大问题
- **穿透** — 布隆过滤器/空值缓存/接口校验
- **击穿** — 互斥锁/逻辑过期/热点预热
- **雪崩** — 随机 TTL/多级缓存/限流降级/熔断

### 18.3 缓存与数据库一致性
- **Cache Aside** — 先更新 DB 再删缓存、延迟双删
- **Canal 订阅 binlog** — 异步同步缓存、解耦
- **最终一致性** — 消息队列补偿、定时对账

### 18.4 热点 Key
- **发现** — 实时统计、QPS 监控
- **处理** — 本地缓存兜底、多副本分散、读写分离

### 18.5 出题方向
- 简答题：Cache Aside 模式为什么是"先更新 DB 再删缓存"而不是"先删缓存再更新 DB"？
- 场景题：设计一个支持百万 QPS 的热点商品缓存方案
- 代码题：手写一个基于 Caffeine + Redis 的多级缓存工具类

---

## 十九、分布式锁（来源：资料7）

### 19.1 Redis 分布式锁
- **基础实现** — SET key value NX PX timeout
- **解锁原子性** — Lua 脚本验证 value + DEL
- **Redisson** — 看门狗自动续期、可重入锁（Hash 结构）、公平锁、读写锁
- **Redlock** — 多节点半数成功、争议与适用场景

### 19.2 Zookeeper 分布式锁
- **临时顺序节点** — 创建节点→获取前序节点→监听前序节点→获锁
- **优势** — 天然避免锁超时、CP 一致性
- **对比** — Redis（AP/性能高）vs ZK（CP/可靠性高）

### 19.3 生产级考虑
- 锁误删、锁不可重入、锁超时与续期、锁自旋与重试策略、锁粒度控制、死锁检测

### 19.4 出题方向
- 简答题：Redisson 看门狗机制原理？Redlock 算法有什么争议？
- 场景题：秒杀场景下分布式锁性能瓶颈如何突破？
- 代码题：手写 Redis 分布式锁加锁解锁（含 Lua 脚本）

---

## 二十、微服务架构（来源：资料4、5、7）

### 20.1 服务拆分
- **拆分原则** — DDD 领域驱动、单一职责、数据库独立、适度拆分
- **拆分粒度** — 过粗（单体）vs 过细（分布式单体）、团队 Conway 法则

### 20.2 服务治理
- **注册发现** — 服务上线/下线、健康检查、负载均衡
- **配置管理** — 集中配置、动态推送、灰度发布
- **API 网关** — 统一入口、路由、鉴权、限流、日志
- **服务通信** — REST（Feign）/RPC（Dubbo）/消息驱动（MQ）

### 20.3 可观测性
- **链路追踪** — SkyWalking/Zipkin/Jaeger、TraceId/SpanId 传播
- **指标监控** — Prometheus + Grafana、Micrometer
- **日志聚合** — ELK/EFK、日志 TraceId 关联

### 20.4 出题方向
- 简答题：微服务拆分有哪些原则？REST 和 RPC 在微服务中各自的优缺点？
- 场景题：从单体迁移到微服务，数据一致性如何保证？如何做不停机数据迁移？
- 场景题：微服务链路追踪如何实现？TraceId 如何在异步线程中传递？

---

## 二十一、SpringCloudAlibaba（来源：资料4、5）

### 21.1 全家桶概览
- Nacos（注册+配置）、Sentinel（流控熔断）、Seata（分布式事务）、RocketMQ（消息总线）、Dubbo（RPC）

### 21.2 vs SpringCloud Netflix
- Netflix 组件停止维护、Alibaba 活跃更新
- Nacos vs Eureka、Sentinel vs Hystrix、Seata 独有分布式事务

### 21.3 整合架构
- SpringBoot + Nacos + Gateway + Feign/Dubbo + Sentinel + Seata + RocketMQ

### 21.4 出题方向
- 简答题：SpringCloudAlibaba 相比 SpringCloud Netflix 有哪些优势？
- 场景题：基于 SpringCloudAlibaba 设计一个电商微服务架构，涉及订单、库存、支付、用户服务

---

## 二十二、Nacos（来源：资料4、5）

### 22.1 注册中心
- 服务注册、健康检查（心跳/TCP/HTTP）、服务发现（推拉模型）
- 临时实例 vs 永久实例

### 22.2 配置中心
- 动态配置、命名空间/分组/Data ID、灰度发布、配置监听

### 22.3 CAP 模式
- **AP 模式（Distro）** — 临时实例、最终一致性、适合服务发现
- **CP 模式（Raft）** — 永久实例、强一致性、适合配置管理
- 模式切换条件

### 22.4 集群部署
- 3 节点起步、数据持久化（MySQL）、Distro 协议数据同步

### 22.5 Nacos 2.x
- gRPC 长连接替代 HTTP 短连接、性能提升、连接管理

### 22.6 出题方向
- 简答题：Nacos AP 和 CP 模式切换原理？Nacos 2.x 为什么要用 gRPC？
- 场景题：Nacos 集群某节点宕机，服务发现是否受影响？如何保证高可用？

---

## 二十三、Sentinel（来源：资料4、5）

### 23.1 流控规则
- **阈值类型** — QPS / 线程数
- **流控模式** — 直接 / 关联 / 链路
- **流控效果** — 快速失败 / Warm Up / 排队等待

### 23.2 熔断降级
- **慢调用比例** — RT 阈值 + 比例阈值
- **异常比例** — 异常比例 + 最小请求数
- **异常数** — 异常数阈值

### 23.3 系统规则
- 自适应限流（BBR）、CPU 使用率、Load、入口 QPS、入口线程数、平均 RT

### 23.4 规则持久化
- 原始模式（内存）、文件模式、Nacos/Apollo 模式（推送）

### 23.5 vs Hystrix
- Hystrix 停止维护、Sentinel 流控更丰富、Sentinel 支持热点参数限流

### 23.6 出题方向
- 简答题：Sentinel 的 Warm Up 预热原理？Hystrix 和 Sentinel 的核心区别？
- 场景题：秒杀场景如何用 Sentinel 做热点参数限流？规则如何持久化到 Nacos？

---

## 二十四、Gateway（来源：资料4、5）

### 24.1 核心概念
- **Route** — ID + URI + Predicate + Filter
- **Predicate** — Path/Header/Method/Host/Query/Cookie/After/Before
- **Filter** — pre/post 过滤、GatewayFilterFactory

### 24.2 全局过滤器
- GlobalFilter vs GatewayFilter、@Order 排序、鉴权过滤器实现

### 24.3 动态路由
- 从 Nacos/Redis 加载路由配置、热更新路由

### 24.4 限流
- RequestRateLimiter + Redis 令牌桶

### 24.5 vs Zuul
- Zuul 1.x 阻塞 IO、Gateway 基于 Netty 非阻塞、Gateway 功能更丰富

### 24.6 出题方向
- 简答题：Gateway 的 Predicate 和 Filter 有什么区别？动态路由如何实现？
- 场景题：设计一个统一鉴权 + 限流的 Gateway 过滤器
- 代码题：手写一个 Gateway 全局鉴权过滤器

---

## 二十五、Dubbo（来源：资料7）

### 25.1 架构
- Provider/Consumer/Registry/Monitor、服务注册/订阅/调用流程

### 25.2 SPI 机制
- Java SPI vs Dubbo SPI、自适应扩展（@Adaptive）、Wrapper 包装类、IOC 注入

### 25.3 负载均衡
- Random（加权随机）、RoundRobin（加权轮询）、LeastActive（最少活跃数）、ConsistentHash（一致性哈希）、ShortestResponse（最短响应）

### 25.4 集群容错
- Failover（失败重试）、Failfast（快速失败）、Failsafe（失败忽略）、Failback（异步重试）、Forking（并行调用）、Broadcast（广播）

### 25.5 Dubbo 3.x
- Triple 协议（HTTP/2 + Protobuf）、应用级注册、Service Mesh 对接

### 25.6 vs Feign
- Feign 基于 HTTP、Dubbo 基于 TCP（自定义协议）、Dubbo 性能更高、Feign 更简单

### 25.7 出题方向
- 简答题：Dubbo SPI 和 Java SPI 的区别？Dubbo 四种负载均衡策略？
- 场景题：Dubbo 调用超时，如何排查？服务提供者下线，消费者如何感知？

---

## 二十六、RocketMQ（来源：资料4、5、7）

### 26.1 架构
- NameServer（路由注册/发现）、Broker（消息存储）、Producer/Consumer

### 26.2 消息类型
- **普通消息** — 同步/异步/单向发送
- **顺序消息** — 全局有序（单队列）/分区有序（MessageQueueSelector）
- **延迟消息** — 18 个延迟级别、5.x 任意延迟
- **事务消息** — 半消息→本地事务→Commit/Rollback→回查
- **批量消息** — 同 Topic 同 Tag 批量发送

### 26.3 消息存储
- CommitLog（全量消息顺序写）、ConsumeQueue（逻辑索引）、IndexFile（索引查询）
- 刷盘策略（同步/异步）、主从复制（同步/异步）

### 26.4 消费者
- 集群消费 vs 广播消费、Push vs Pull（长轮询）、消费位点管理、重复消费幂等

### 26.5 高级特性
- 消息重试、死信队列、消息轨迹、事务回查机制

### 26.6 vs Kafka vs RabbitMQ
- Kafka（高吞吐/日志场景）、RocketMQ（事务/延迟/顺序）、RabbitMQ（路由灵活/低延迟）

### 26.7 出题方向
- 简答题：RocketMQ 事务消息原理？CommitLog 和 ConsumeQueue 的关系？
- 场景题：RocketMQ 消息积压 100 万条，如何快速处理？
- 场景题：如何用 RocketMQ 实现分布式事务最终一致性？

---

## 二十七、Docker

### 27.1 基础
- 镜像 vs 容器 vs 仓库、Docker 引擎架构、容器 vs 虚拟机

### 27.2 Dockerfile
- FROM/RUN/COPY/ADD/WORKDIR/ENV/CMD/ENTRYPOINT/EXPOSE/VOLUME
- 多阶段构建、构建缓存优化

### 27.3 Docker Compose
- 多容器编排、service/networks/volumes 定义、depends_on 健康检查

### 27.4 网络
- bridge/host/none/overlay/macvlan、容器间通信、端口映射

### 27.5 数据持久化
- Volume（Docker 管理）/Bind Mount（宿主目录）/tmpfs（内存）

### 27.6 镜像优化
- 减小镜像体积（Alpine 基础镜像/多阶段构建/合并 RUN 层/.dockerignore）

### 27.7 出题方向
- 简答题：CMD 和 ENTRYPOINT 的区别？Docker 镜像分层原理？
- 场景题：Java 应用容器化部署，JVM 参数如何传入？如何优化镜像体积？
- 代码题：编写一个 SpringBoot 应用的多阶段 Dockerfile

---

## 二十八、Maven

### 28.1 POM 与坐标
- groupId/artifactId/version、POM 继承（parent）、dependencyManagement 版本统管

### 28.2 依赖管理
- 依赖范围（compile/test/provided/runtime/system/import）
- 依赖传递、依赖冲突（最短路径优先/先声明优先）、exclusions 排除

### 28.3 生命周期
- clean/validate/compile/test/package/verify/install/deploy/site
- 三套生命周期（clean/default/site）、phase 与 goal（插件目标）

### 28.4 多模块
- 聚合（modules）、继承（parent）、版本统一管理、reactor 构建顺序

### 28.5 私服
- Nexus/Artifactory、proxy/hosted/group 仓库、SNAPSHOT vs RELEASE

### 28.6 vs Gradle
- Maven（约定优于配置/XML/成熟稳定）、Gradle（Groovy DSL/灵活/构建快）

### 28.7 出题方向
- 简答题：Maven 依赖冲突如何解决？<dependencyManagement> 和 <dependencies> 的区别？
- 场景题：多模块项目版本升级，如何高效统一管理依赖版本？

---

## 二十九、Nginx

### 29.1 核心功能
- 反向代理、负载均衡、动静分离、HTTP 服务器

### 29.2 负载均衡
- 轮询（默认）、加权轮询（weight）、ip_hash（会话保持）、least_conn（最少连接）、fair（响应时间）
- upstream 健康检查（max_fails/fail_timeout/backup）

### 29.3 location 匹配
- 精确匹配（=）、前缀匹配（^~）、正则匹配（~ / ~*）、默认匹配
- 匹配优先级顺序

### 29.4 限流
- limit_req（令牌桶/请求频率）、limit_conn（并发连接数）、limit_rate（带宽限速）

### 29.5 高可用
- Keepalived + VIP 双机热备、VRRP 协议、主备/主主模式

### 29.6 性能调优
- worker_processes/auto、worker_connections、epoll 事件模型、gzip 压缩、缓存配置

### 29.7 OpenResty
- Nginx + Lua、动态路由、WAF 防火墙、API 网关

### 29.8 出题方向
- 简答题：Nginx location 匹配优先级？ip_hash 和一致性哈希在负载均衡中的区别？
- 场景题：用 Nginx 实现灰度发布（按 IP/Header 路由到不同后端）
- 代码题：编写 Nginx 配置实现反向代理 + 负载均衡 + 限流

---

## 三十、计算机网络（来源：资料4、6）

### 30.1 网络模型
- OSI 七层 vs TCP/IP 四层、各层职责与协议

### 30.2 TCP
- **三次握手** — SYN/SYN-ACK/ACK、为什么不是两次/四次
- **四次挥手** — FIN/ACK/FIN/ACK、TIME_WAIT（2MSL）/CLOSE_WAIT 问题
- **可靠性** — 序列号/确认应答、滑动窗口、拥塞控制（慢启动/拥塞避免/快重传/快恢复）、超时重传
- TCP vs UDP

### 30.3 HTTP/HTTPS
- **HTTP 1.0/1.1/2.0/3.0** — 持久连接/管线化/多路复用/Server Push/QUIC
- **HTTPS** — TLS 握手流程、对称+非对称加密、证书链验证
- **中间人攻击** — 证书伪造、CA 信任链

### 30.4 跨域
- 同源策略、CORS（预检请求/简单请求）、JSONP、Nginx 反向代理

### 30.5 CDN
- 边缘节点缓存、DNS 智能解析、回源策略、缓存刷新

### 30.6 出题方向
- 简答题：TCP 三次握手为什么不是两次？HTTPS 握手过程？
- 场景题：服务器出现大量 TIME_WAIT，如何排查和优化？
- 场景题：前后端分离项目跨域问题如何解决？

---

## 三十一、大厂真实面经专题（来源：资料8-14）

### 31.1 阿里淘天一面（资料8，18页）
- GET vs POST 区别、MySQL 查询优化
- Spring @Component 原理、@Value 读取配置机制
- 项目介绍方法论、技术难点应答策略

### 31.2 阿里/字节/蚂蚁/小红书（资料9，15页）
- Kafka 如何保证高吞吐与消息不丢失
- Redis 节点间通信、Redis 为什么快、CAP 理解
- Redis Geo 底层原理
- 项目设计：双链路幂等保证
- 乐观锁/悲观锁深入

### 31.3 快手一面（资料10，20页）
- Long 长度范围、HashMap 底层与线程不安全原因
- 线程池无界队列 + 拒绝策略
- Redis zset 底层数据结构（ziplist/skiplist）
- Redis 除缓存外的其他用途

### 31.4 美团实习面经（资料11，22页）
- 接口 vs 抽象类、死锁预防与避免
- 乐观锁 vs 悲观锁
- Spring @Component/@Value 原理

### 31.5 四年半社招大厂集锦（资料13，6页）
- 对象深拷贝方法、Java 对象内存结构（Mark Word）
- 静态内部类单例（线程安全+延迟加载）
- 弱引用 vs 虚引用、三色标记法（漏标/错标解决）
- JIT 编译、逃逸分析
- synchronized 底层实现与锁释放通知机制

### 31.6 OPPO二面（资料14，18页）
- String 不可变性、线程创建方式与状态
- 线程池使用（内置 vs 自定义）
- 慢 SQL 定位与分析、索引使用与联合索引
- SQL 性能分析（EXPLAIN）

### 31.7 出题方向
- 场景题：一面面试官问"你项目中最大的难点是什么"，你如何用 STAR 方法回答？（参考淘天面经）
- 简答题：ConcurrentHashMap 如何保证线程安全？（参考快手一面）
- 场景题：你的设计中有两条互为旁路的链路，如何保证幂等？（参考阿里面经）
- 简答题：Redis Geo 底层是什么数据结构？如何实现附近的人？（参考阿里面经）
- 代码题：手写静态内部类单例并解释为何线程安全且延迟加载（参考四年半面经）

---

## 三十二、Elasticsearch（来源：资料26）

### 32.1 集群架构
- **Node 角色** — Master/Data/Coordinating/Ingest
- **索引（Index）** — 逻辑命名空间、分片（Shard）、副本（Replica）
- **集群选举** — 脑裂问题、minimum_master_nodes
- **NRT** — 近实时搜索原理（refresh interval、translog）

### 32.2 核心概念
- **倒排索引** — Term Dictionary + Posting List
- **分析器** — Standard/IK/拼音、分词/过滤/归一化
- **Mapping** — 字段类型（text/keyword/date/nested）、动态映射

### 32.3 查询与优化
- **查询类型** — match/term/range/bool/aggregation
- **深度分页** — from+size 上限、Scroll、Search After
- **写入优化** — 批量写入、减少 refresh、合理分片数
- **X-Pack** — 安全/监控/告警/机器学习

### 32.4 出题方向
- 简答题：Elasticsearch 为什么能实现近实时搜索？倒排索引原理？
- 场景题：ES 集群出现脑裂，如何排查和预防？
- 场景题：ES 深度分页性能差，如何优化？

---

## 三十三、ClickHouse（来源：资料27）

### 33.1 核心特性
- **列式存储** — 按列存储与压缩、向量化执行
- **应用场景** — 实时数据分析、OLAP、用户行为分析、BI 报表
- **缺点** — 不擅长点查询/更新、不适合高频写入、无事务

### 33.2 架构与数据模型
- **MergeTree 引擎** — 数据分区（PARTITION BY）、排序键（ORDER BY）、主键索引
- **逻辑数据模型** — 数据库→表→分区→数据块
- **分布式表** — Distributed 引擎、分片规则

### 33.3 使用注意
- 批量写入优于单条、避免频繁小批量插入
- 避免 SELECT *、合理使用物化视图
- Nullable 类型性能损耗

### 33.4 出题方向
- 简答题：ClickHouse 为什么查询这么快？列式存储和向量化执行的原理？
- 简答题：ClickHouse 和 MySQL/ES 各自适用什么场景？
- 场景题：每日 10 亿条用户行为日志，如何用 ClickHouse 做实时分析？

---

## 三十四、MongoDB（来源：资料28）

### 34.1 基础概念
- **NoSQL vs RDBMS** — 文档型、无 Schema、水平扩展
- **集合（Collection）** — 类比表、BSON 文档
- **索引** — 单字段/复合/地理空间/全文/TTL

### 34.2 适用场景
- **适用**：日志存储、IoT 数据、内容管理、用户画像、灵活 Schema
- **不适用**：强事务、多表关联、固定 Schema 场景

### 34.3 出题方向
- 简答题：MongoDB 和 MySQL 的区别？什么场景适合用 MongoDB？
- 场景题：用户行为日志日均 5000 万条，如何设计 MongoDB 存储方案？

---

## 三十五、Netty（来源：资料25）

### 35.1 NIO 基础
- **BIO/NIO/AIO** — 阻塞/非阻塞/异步、Selector 多路复用
- **Channel/Buffer/Selector** — NIO 三大核心组件

### 35.2 Netty 核心
- **线程模型** — Boss Group + Worker Group、EventLoop
- **核心组件** — Channel、ChannelPipeline、ChannelHandler、ByteBuf
- **TCP 粘包/拆包** — 原因（Nagle 算法/缓冲区）、解决方案（长度域/分隔符/定长）
- **零拷贝** — DirectBuffer、CompositeByteBuf、FileRegion.transferTo

### 35.3 出题方向
- 简答题：Netty 的 Boss 和 Worker 线程组如何分工？零拷贝怎么实现的？
- 场景题：Netty 服务端收到半包问题，如何设计解码器解决？
- 代码题：用 Netty 手写一个简单的 TCP 服务端

---

## 三十六、Kafka（来源：资料20）

### 36.1 核心原理
- **高吞吐** — 顺序写磁盘、PageCache、零拷贝 sendfile、批量压缩
- **Zookeeper 作用** — Broker 注册、Controller 选举、Topic 配置存储
- **消费者 API vs 流 API** — Consumer 拉取消息、Streams 实时处理转换

### 36.2 负载均衡
- Partition 分配策略（Range/RoundRobin/Sticky/CooperativeSticky）
- Rebalance 机制与优化

### 36.3 出题方向
- 简答题：Kafka 为什么能达到百万 QPS 吞吐？零拷贝和顺序写是什么原理？
- 场景题：Kafka 消费组 Rebalance 频繁导致消费中断，如何排查优化？

---

## 三十七、RabbitMQ（来源：资料21）

### 37.1 核心概念
- **Producer/Consumer/Broker**
- **Exchange/Queue/Binding** — 路由机制
- **消息路由** — Direct/Topic/Fanout/Headers

### 37.2 集群与高可用
- **集群限制** — 跨数据中心延迟、脑裂风险
- **镜像队列** — 主从同步、ha-mode
- **Basic.Reject** — 单条拒绝与死信队列

### 37.3 出题方向
- 简答题：RabbitMQ 的 Exchange 类型有哪些？各自适用什么场景？
- 简答题：RabbitMQ 和 RocketMQ 的核心区别？如何选型？

---

## 三十八、Memcached（来源：资料29）

### 38.1 核心特性
- **多线程** — 请求处理用 worker 线程、支持多核
- **vs Redis** — 仅 String、纯内存无持久化、多线程 vs 单线程、过期策略不同

### 38.2 使用场景
- Session 共享存储
- 热点数据缓存（简单 KV）

### 38.3 出题方向
- 简答题：Memcached 和 Redis 的核心区别？各自适用什么场景？
- 场景题：分布式 Session 共享有哪些方案？各自的优缺点？

---

## 三十九、设计模式（来源：资料18 JavaGuide2025-设计模式，51页）

### 39.1 软件设计原则
- **SOLID 原则** — 单一职责、开闭原则、里氏替换、接口隔离、依赖倒置
- **DRY / KISS / YAGNI** — 不重复、保持简单、不过度设计

### 39.2 创建型模式
- **单例模式** — 饿汉式（类加载保证线程安全）、懒汉式（synchronized/DCL）、静态内部类（延迟加载+线程安全）、枚举（最安全）
- **工厂方法 vs 抽象工厂** — 单一产品等级 vs 产品族
- **建造者模式** — 链式调用、Lombok @Builder

### 39.3 结构型模式
- **代理模式** — JDK 动态代理（InvocationHandler）、CGLIB 代理
- **适配器模式** — 类适配器 vs 对象适配器、Spring MVC HandlerAdapter
- **装饰器模式** — JDK IO 流（BufferedInputStream）、vs 代理模式对比

### 39.4 行为型模式
- **观察者模式** — 事件监听、Spring Event 机制
- **责任链模式** — Servlet Filter 链、Spring Interceptor
- **策略模式** — 算法族、Spring 资源访问 Resource
- **状态模式** — 状态机、订单状态流转
- **模板方法模式** — AbstractList、JdbcTemplate、AQS

### 39.5 框架中的设计模式
- **Spring 中的设计模式** — 工厂（BeanFactory）、单例（Bean 默认）、代理（AOP）、模板（JdbcTemplate）、观察者（ApplicationListener）
- **JDK 中的设计模式** — 装饰器（IO 流）、迭代器（Iterator）、适配器（Arrays.asList）
- **Netty 中的设计模式** — 责任链（Pipeline）、观察者（ChannelFuture）、策略（EventExecutorChooser）
- **Dubbo 中的设计模式** — SPI（扩展点）、代理（RPC 代理）、集群容错策略

### 39.6 出题方向
- 简答题：单例模式的几种写法，哪种最安全？为什么？
- 简答题：JDK 动态代理和 CGLIB 代理的区别？Spring AOP 如何选择？
- 简答题：Spring 框架中使用了哪些设计模式？举例说明。
- 场景题：如何用责任链模式设计一个多级审批系统？
- 代码题：手写线程安全的懒汉式单例（DCL + volatile）
