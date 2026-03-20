# NexusAI —— 企业级智能知识库与面试评估系统 (v1.0)

NexusAI 是一款基于 **Spring Boot 3** 和 **RAG (Retrieval-Augmented Generation)** 架构的现代化后端系统。它通过将大模型的通用智慧与私有文档的垂直领域知识相结合，有效解决了 AI “幻觉”问题，为企业提供高可靠、低延迟的知识问答能力。

## 🚀 项目核心亮点
- **工业级 RAG 架构**：不仅是 API 调用，而是完整实现了「文档解析 -> 文本切片 -> 向量化检索 -> 增强上下文生成」的全链路流程。
- **高性能预加载 (Performance Optimization)**：针对 RAG 初始化耗时长的痛点，利用 Spring 生命周期钩子实现启动预索引，确保了用户提问的**即时响应**。
- **模块化解耦设计**：采用声明式 AI Service 接口，将大模型配置（AiConfig）与业务逻辑（RAGController）彻底分离，支持分钟级切换底层模型（如 GPT-4, DeepSeek 等）。
- **生产级环境对齐**：全面基于 **Java 21 (LTS)** 构建，确保了在企业级高并发场景下的稳定性和安全性。

## 🛠️ 技术栈
- **后端框架**: Spring Boot 3.2.5
- **SDK版本**: JDK 21 (Long Term Support)
- **AI 编排**: LangChain4j 0.34.0
- **中间件**: Redis (用于后续对话上下文存储)
- **文档处理**: Apache Tika (支持 PDF/Word/TXT)
- **向量技术**: InMemoryEmbeddingStore (内存索引)
- **大语言模型**: 智谱 AI (GLM-4-Flash)

## 🏗️ 系统架构图 (RAG 流程)
1. **Documents Ingestion**: 启动时扫描 `/documents` 文件夹。
2. **Embedding**: 使用 `AllMiniLmL6V2` 模型将文本转化为 384 维向量。
3. **Storage**: 将向量数据索引至本地内存库。
4. **Retrieval**: 用户提问时，通过余弦相似度检索 Top-3 相关片段。
5. **Generation**: 构造 Prompt 发送至 LLM，输出证据凿凿的回答。

## 🏁 快速启动
1. **环境**: 安装 JDK 21 和 Redis。
2. **配置**: 在 `AiConfig.java` 中填入你的 `apiKey`。
3. **数据**: 将 PDF 资料放入根目录的 `documents/` 文件夹。
4. **接口**:
    - **基础对话**: `GET /ai/chat?message=你好`
    - **知识库问答**: `GET /ai/rag?question=公司周五下午有什么福利？`

## 📅 版本演进计划
- [x] v1.0: RAG 链路跑通与性能预加载优化。
- [ ] v2.0: 接入 **ChromaDB** 实现百万级数据的持久化存储。
- [ ] v2.1: 实现 **SSE (Server-Sent Events)** 协议，支持打字机流式响应。
- [ ] v3.0: 增加多轮对话记忆功能 (Redis 存储 Session)。

---
> **Developer**: 21-year-old Backend Learner (One-本 University)
> **Goal**: 探索 AI 技术在真实业务场景下的工程化落地。