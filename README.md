# NexusAI —— 工业级双核智能知识引擎 (v2.4)

NexusAI 是一款基于 **Spring Boot 3**、**LangChain4j** 和 **RAG** 架构的现代化 AI 后端系统。它通过“通用逻辑”与“私有知识”的双核驱动，实现了兼具博学性与严谨性的生产级问答体验。

## 🌟 核心特性
- **双核心智力入口**：专业 RAG (Blue UI) 与 通用助手 (Purple UI) 并行。
- **持久化存储**：集成 Docker 版 ChromaDB，实现数据物理留存。
- **全链路 SSE 响应**：打字机式流式交互，毫秒级首字节反馈。
- **时空补丁**：通过指令注入实现模型对 2026 年的实时感知。

## 🛠️ 技术栈
- **后端**: Java 21 (LTS), Spring Boot 3.2.5, LangChain4j 0.34.0
- **数据库**: ChromaDB 0.5.20 (Docker), Redis (准备中)
- **文档解析**: Apache Tika (PDF/Word/TXT)

## 📈 版本深度演进历程 (Version Timeline)

### v2.4 (当前版本) - 交互闭环与时空同步
- **[2.4.1] UI 全链路联调**：构建 `chat.html`（极光紫）与 `index.html`（苹果蓝）双端界面，实现一键跳转功能闭环。
- **[2.4.2] 时空幻觉修复**：针对大模型 2023 年知识截止日期问题，在 `AIController` 层级注入「2026年3月」时间感知补丁，大幅提升对话真实感。

### v2.3 - 数据治理与检索精度调优
- **[2.3.1] 纳米级切片 (Micro-Chunking)**：引入 `DocumentSplitters.recursive(100, 0)` 策略，将文档强行切割为 100 字符原子块，解决章节混淆问题。
- **[2.3.2] 指令加固**：利用 Java 注解继承机制，将 `@SystemMessage` 提升至接口级别，彻底锁定流式与非流式接口的回答边界。

### v2.2 - 交互协议革新
- **[2.2.1] SSE 协议落地**：全面废弃阻塞式返回，采用 Server-Sent Events 实现后端向前端的 Token 级实时推送。
- **[2.2.2] 视觉体验优化**：引入 JS 平滑追踪滚动算法与 CSS 硬件加速，实现对标顶级 AI 产品的打字机动效。

### v2.1 - 基础设施可靠性补全
- **[2.1.1] 驱动深度适配**：攻克 LangChain4j 0.34.0 版智谱驱动的参数陷阱，闭环配置 4 项超时控制（Call/Connect/Read/Write），消除启动 Null 异常。
- **[2.1.2] 全局配置中心**：重构 `AiConfig` 实现双模型 Bean 统一生命周期管理。

### v2.0 - 存储层持久化转型
- **[2.0.1] 向量库容器化**：引入 Docker 版 ChromaDB 0.5.20，解决 `InMemory` 重启失忆痛点。
- **[2.0.2] 物理挂载**：通过 Docker Volume 实现 F 盘数据落盘，实现数据与计算的物理隔离。

### v1.0 - RAG 链路原型点火
- 实现 Spring Boot 3 对接智谱 API，打通 PDF 加载与基础向量检索全流程。

## 🚀 快速启动
1. **环境**: 启动 Docker `my-chroma` (v0.5.20) 及本地 Redis。
2. **配置**: 操作系统环境变量中设置 `ZHIPU_API_KEY`。
3. **数据**: PDF 文档放入 `/documents` 文件夹。

---
> **Project Owner**: 21-year-old Backend Learner
> **Goal**: 探索 AI 基础设施在垂直业务场景下的工程化极限。