# NexusAI —— 工业级双核智能知识引擎 (v2.4)

NexusAI 是一款基于 **Spring Boot 3**、**LangChain4j** 和 **RAG** 架构的现代化 AI 后端系统。它通过“通用逻辑”与“私有知识”的双核驱动，实现了兼具博学性与严谨性的生产级问答体验。

## 🌟 v2.4 核心突破
- **双核心智力入口 (Dual-Core Architecture)**：
   - **专业知识库 (Professional RAG)**：基于 ChromaDB 持久化索引，锁定私有文档边界，实现零幻觉的精准检索（Blue UI）。
   - **通用助手 (General Intelligence)**：注入 2026 年时空感知补丁，具备实时时间观念，处理复杂的通用逻辑咨询（Purple UI）。
- **全链路异步流式交互**：基于 **SSE (Server-Sent Events)** 协议实现打字机效果。配合 **Smooth Auto-scrolling** 算法，首字节响应时间 (TTFB) 缩短至毫秒级。
- **高阶 UI/UX 设计**：对标 Apple / 影视飓风审美，采用 **Glassmorphism (玻璃拟态)** 视觉风格，支持双端界面无缝导航切换。
- **数据治理与降噪**：采用 **Micro-Chunking (100字/片)** 纳米级分片技术，结合数据清洗策略，彻底根治 RAG 系统中常见的“跨章节干扰”问题。

## 🛠️ 技术栈（企业标准版）
- **后端核心**: Java 21 (LTS), Spring Boot 3.2.5
- **AI 编排**: LangChain4j 0.34.0, 智谱 AI (GLM-4-Flash)
- **存储中心**: ChromaDB 0.5.20 (Docker 容器化), Redis (Session准备)
- **协议/交互**: SSE 异步协议, JS EventSource, CSS GPU 硬件加速

## 🏗️ 架构概览
1. **感知层**: 双前端 UI 分别对接 `/ai/rag/stream` 与 `/ai/chat/stream` 接口。
2. **逻辑层**:
   - `AiConfig` 统一管理 Bean 生命周期与安全策略。
   - `RAGController` 负责分片、向量化存储与精准召回。
   - `AIController` 负责通用指令集注入与时空同步。
3. **物理层**: Docker 挂载数据卷实现向量索引物理持久化（F盘）。

## 🏁 快速启动
1. **环境准备**:
   - 确保 Docker 容器 `my-chroma` (v0.5.20) 正常运行。
   - 确保系统环境变量 `ZHIPU_API_KEY` 已正确配置。
2. **知识投喂**: 将 PDF 文档放入根目录 `/documents` 文件夹。
3. **访问体验**:
   - **知识库模式**: [http://localhost:8080/index.html](http://localhost:8080/index.html)
   - **通用模式**: [http://localhost:8080/chat.html](http://localhost:8080/chat.html)

## 📅 迭代记录
- [x] v1.0: 基础 RAG 链路跑通。
- [x] v2.0: 引入 ChromaDB 持久化存储。
- [x] v2.2: 全面落地 SSE 流式打字机交互。
- [x] v2.4: **[当前版本]** 实现双大脑 UI 闭环、时空感知优化与数据降噪治理。
- [ ] v3.0: 计划接入 Redis 分布式会话管理。

---
> **Project Owner**: 21-year-old Backend Learner
> **Vision**: 探索 AI 基础设施在垂直业务场景下的工程化极限。