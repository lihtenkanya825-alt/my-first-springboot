package com.example.demo;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters; // [v2.3新增]
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
 * NexusAI 核心知识中台
 * 版本演进：v1.0(基础) -> v2.0(ChromaDB) -> v2.1(SSE流式) -> v2.2(前端联调) -> v2.3(精准检索与指令加固)
 */
@RestController
public class RAGController {

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private Assistant assistant;

    /**
     * [v2.3优化] 接口级指令加固 (Constraint Reinforcement)
     * 采用“三段式”提示词工程：定义角色边界、设定检索准则、强制兜底逻辑。
     * 解决了 v2.2 版本中流式接口因注解作用域问题导致的“幻觉”现象。
     */
    @SystemMessage({
            "### 角色设定 ###",
            "你现在是 NexusAI 知识库的精准提取机器人。你必须保持绝对的客观，严禁利用任何外部常识进行逻辑推演。",
            "",
            "### 检索准则 ###",
            "1. 必须【且仅能】依据下文提供的 [Context] 资料回答用户。",
            "2. 严禁回答与当前问题无关的其他章节内容。例如用户问‘周五福利’，绝对不能提到‘考勤’或‘设备补贴’。",
            "3. 回答必须精简到极致，禁止任何修辞性的废话。",
            "",
            "### 兜底规则 ###",
            "如果 [Context] 资料中未明确提到答案，请直接回答：'抱歉，在 NexusAI 知识库中未检索到相关规定。'"
    })
    interface Assistant {
        // [v1.0] 同步接口：继承上方的 SystemMessage
        String chat(String message);

        // [v2.1] 流式接口：同步继承上方的 SystemMessage
        TokenStream streamChat(String message);
    }

    public RAGController(ChatLanguageModel chatModel, StreamingChatLanguageModel streamingChatModel) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
    }

    /**
     * [v2.3调优] 生产级引擎初始化逻辑
     * 1. 引入 DocumentSplitter 实现微粒度切片，解决“章节混淆”问题。
     * 2. 更新 Collection 命名，强制数据库执行物理隔离，避免旧版脏数据干扰。
     */
    @PostConstruct
    public void initRag() {
        System.out.println("🚀 正在初始化 NexusAI v2.3 精准引擎（隔离式 Collection 架构）...");
        try {
            // [v2.3修改] 切换至新的持久化集合，确保测试环境纯净
            EmbeddingStore<TextSegment> embeddingStore = ChromaEmbeddingStore.builder()
                    .baseUrl("http://localhost:8000")
                    .collectionName("nexus_v23_precision")
                    .build();

            // [v1.0保留] 文本向量化模型配置
            EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

            // [v1.0保留] 文档加载流
            List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                    Paths.get("documents"), new ApacheTikaDocumentParser());

            /**
             * [v2.3新增] 精细化切片策略 (Chunking Strategy)
             * recursive(300, 30): 按段落逻辑切分，每块最大 300 字符，重叠 30 字符。
             * 核心价值：确保每个 Vector 记录仅包含单一知识点，从物理存储层实现降噪。
             */
            DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);

            // [v2.3优化] 注入切片器执行摄取任务
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(splitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            ingestor.ingest(documents);

            // [v2.1整合] 声明式 AI 服务构建
            this.assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(chatModel)
                    .streamingChatLanguageModel(streamingChatModel)
                    .contentRetriever(EmbeddingStoreContentRetriever.builder()
                            .embeddingStore(embeddingStore)
                            .embeddingModel(embeddingModel)
                            // [v2.3调优] 针对微缩文档设置 Top-K 为 1，实现最高精度召回
                            .maxResults(1)
                            .build())
                    .build();

            System.out.println("✅ NexusAI v2.3 精准引擎点火成功！");
        } catch (Exception e) {
            System.err.println("❌ 引擎初始化失败，请检查 Docker 或网络状态。");
            e.printStackTrace();
        }
    }

    /**
     * [v1.0保留] 标准阻塞式对话
     */
    @GetMapping("/ai/rag")
    public String ragChat(@RequestParam String question) {
        if (assistant == null) return "Engine Error.";
        return assistant.chat(question);
    }

    /**
     * [v2.1增强] SSE 流式打字机接口
     * [v2.2适配] 修复 UTF-8 编码与前端 index.html 协议解析
     */
    @GetMapping(value = "/ai/rag/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRagChat(@RequestParam String question) {
        SseEmitter emitter = new SseEmitter(120 * 1000L);
        if (assistant == null) return null;

        assistant.streamChat(question)
                .onNext(token -> {
                    try {
                        // [v2.2修复] 采用 Data 协议包装 Token，确保前端 EventSource 正确接收
                        SseEmitter.SseEventBuilder event = SseEmitter.event().data(token);
                        emitter.send(event);
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