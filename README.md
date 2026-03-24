# NexusAI —— 工业级双核智能知识引擎 (v3.2)

NexusAI 是一款基于 **Spring Boot 3**、**LangChain4j** 和 **RAG** 架构的生产级 AI 后端系统。系统通过“异步流水线”与“状态机”重构，实现了具备高性能并发摄取与实时任务监测能力的智能问答体验。

## 🌟 v3.2 核心特性
- **异步非阻塞架构 (Async Ingestion)**：引入 `ThreadPoolTaskExecutor` 建立 `NexusWorker` 线程池，实现高耗时向量化任务与 Web 主线程的物理隔离。
- **任务状态可观测性 (Task State Machine)**：利用 **Redis** 存储任务生命周期状态，支持前端通过 TaskID 实时轮询处理进度（PROCESSING -> COMPLETED）。
- **动态知识热加载 (Hot-loading)**：无需重启服务，通过 `/ai/upload` 接口实现知识库的秒级在线更新。

## 🛠️ 技术栈
- **后端核心**: Java 21 (LTS), Spring Boot 3.2.5
- **并发控制**: Spring TaskExecutor (自定义线程池隔离)
- **存储中心**: ChromaDB 0.5.20, Redis (会话记忆+任务状态)
- **AI 编排**: LangChain4j 0.34.0

## 📈 版本深度演进历程

### v3.2 (当前版本) - 异步化重构
- **[3.2.1] 线程池隔离**：配置自定义 Executor，解决大文件上传阻塞 Tomcat 线程池的隐患。
- **[3.2.2] 任务状态机**：自研 `IngestionService`，利用 Redis 实现跨线程的任务状态同步。
- **[3.2.3] 轮询机制落地**：在 `index.html` 实现前端 Polling 逻辑，增强异步处理的用户反馈感。

### v3.1 - 交互闭环与稳定性加固
- **[3.1.1] 动态摄取**：实现基于 `MultipartFile` 的文件上传链路。
- **[3.1.2] 健壮性 Hotfix**：补全 `TokenStream.onError()` 监听，修复 0.34.0 版配置校验报错。
- **[3.1.3] 布局重构**：将对话框升级为 **1100px 沉浸式宽屏**，解决容器塌陷问题。

### v3.0 - 分布式会话记忆
- **[3.0.1] Redis 存储集成**：自研 `RedisChatMemoryStore` 实现对话持久化，解决重启失忆问题。
- **[3.0.2] 指代消解能力**：引入 `@MemoryId` 机制，支持多轮连续对话上下文理解。

### v2.4 - UI 联动与时空同步
- **[2.4.1] 双大脑 UI**：构建「极光紫」与「苹果蓝」互通界面。
- **[2.4.2] 时空补丁**：注入 SystemMessage 同步 2026 年时间线感知。

## 🚀 快速启动
1. **基础设施**: 确保 Docker `my-chroma` (v0.5.20) 和 Redis 正常运行。
2. **环境配置**: 操作系统环境变量中设置 `ZHIPU_API_KEY`。
3. **功能体验**: 访问 `index.html` 体验异步上传与精准 RAG。

---
> **Project Owner**: 21-year-old Backend Learner
> **Milestone**: 成功攻克异步并发与分布式任务调度核心逻辑。