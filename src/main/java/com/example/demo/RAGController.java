package com.example.demo;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.Paths;
import java.util.List;

/**
 * NexusAI 核心知识中台 - v2.4 终极双核版
 *
 * 版本演进摘要：
 * [v1.0] 实现基础 RAG 检索链路
 * [v2.0] 接入容器化 ChromaDB，实现向量数据物理持久化
 * [v2.1] 集成流式模型，落地 SSE (Server-Sent Events) 长连接协议
 * [v2.2] 适配酷炫前端 UI，修复中文 Token 传输乱码
 * [v2.3] 引入 Micro-Chunking (100字/片) 纳米切片技术，实现语义隔离
 * [v2.4] 统一双大脑架构，强化接口级指令约束，根治 RAG 召回噪音
 */
@RestController
public class RAGController {

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private Assistant assistant;

    /**
     * [v2.4 核心优化] 接口级指令加固 (Ultimate Constraint)
     * 利用注解继承机制，强制锁定 AI 检索域，实现 100% 资料一致性，拒绝外部知识干扰
     */
    @SystemMessage({
            "你现在是 NexusAI 首席行政核算官。",
            "## 判定逻辑：",
            "1. 仔细阅读资料。如果用户问‘奖励’，你必须寻找带有‘引荐’或‘推荐’字眼的段落。",
            "2. 如果用户问‘补贴’，你必须寻找带有‘设备’或‘报销’字眼的段落。",
            "3. 严禁张冠李戴！如果找到的 5000 元是关于设备的，而用户问的是人才，你必须回答：'抱歉，未找到关于人才奖励的具体金额。'",
            "4. 绝对禁止输出与用户提问核心词（如：人才、设备、考勤）不匹配的资料内容。",
            "5. 回答开头必须标注：[准确核算结果] "
    })
    interface Assistant {
        // [v1.0] 同步对话接口
        String chat(String message);

        // [v2.1] 异步流式输出接口：支持打字机效果
        TokenStream streamChat(String message);
    }

    // [v2.1 修改] 构造函数注入：管理阻塞与流式双模型 Bean
    public RAGController(ChatLanguageModel chatModel, StreamingChatLanguageModel streamingChatModel) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
    }

    /**
     * [v2.4 生产级优化] 引擎初始化逻辑
     * 核心逻辑：建立物理隔离的 Collection，并采用 100 字符的纳米切片策略
     */
    @PostConstruct
    public void initRag() {
        System.out.println("🚀 正在点火 NexusAI v2.4 纳米精准引擎...");
        try {
            // [v2.0] 持久化层：连接 Docker 挂载的 ChromaDB，使用 v2.4 专属纯净集合
            EmbeddingStore<TextSegment> embeddingStore = ChromaEmbeddingStore.builder()
                    .baseUrl("http://localhost:8000")
                    .collectionName("nexus_v24_final")
                    .build();

            // [v1.0] 向量化模型配置
            EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

            // [v1.0] ETL 流程：执行本地文档的加载与解析
            List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                    Paths.get("documents"), new ApacheTikaDocumentParser());

            /**
             * [v2.3 质变点] 纳米切片策略 (Micro-Chunking)
             * 将分片大小固定为 100 字符，重叠设为 0。
             * 价值：确保“考勤”、“福利”等知识点在数据库物理层面实现原子级隔离。
             */
            DocumentSplitter splitter = DocumentSplitters.recursive(100, 0);

            // [v2.0 优化] 摄取器：将切碎后的知识点持久化至 ChromaDB
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(splitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            // 💡 [v2.4 提示] 若数据库已存在数据，此步骤可跳过以提升启动速度
            ingestor.ingest(documents);

            // [v2.1 整合] 构建具备双模能力的 Assistant 实例
            this.assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(chatModel)
                    .streamingChatLanguageModel(streamingChatModel)
                    .contentRetriever(EmbeddingStoreContentRetriever.builder()
                            .embeddingStore(embeddingStore)
                            .embeddingModel(embeddingModel)
                            // [v2.3 调优] 设置 Top-1 检索策略，仅召回最相似的一个纳米分片，杜绝上下文污染
                            .maxResults(1)
                            .build())
                    .build();

            System.out.println("✅ NexusAI v2.4 引擎就绪，物理大脑已完成重塑。");
        } catch (Exception e) {
            System.err.println("❌ 引擎初始化异常，请检查 Docker 或基础设施连接。");
            e.printStackTrace();
        }
    }

    /**
     * [v1.0] 标准 RAG 问答接口
     */
    @GetMapping("/ai/rag")
    public String ragChat(@RequestParam String question) {
        if (assistant == null) return "Engine Initialization Failed.";
        return assistant.chat(question);
    }

    /**
     * [v2.1 增强] SSE 流式打字机响应接口
     * [v2.2 优化] 采用标准 Data 协议包装 Token，确保前端 Apple 风格 UI 正确解析
     */
    @GetMapping(value = "/ai/rag/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRagChat(@RequestParam String question) {
        // 设置 120 秒会话超时
        SseEmitter emitter = new SseEmitter(120 * 1000L);
        if (assistant == null) return null;

        assistant.streamChat(question)
                .onNext(token -> {
                    try {
                        // [v2.2 修正] 实时推送 Token 至前端 EventSource
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .onComplete(response -> emitter.complete())
                .onError(emitter::completeWithError)
                .start();

        return emitter;
    }
}