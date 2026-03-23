package com.example.demo;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.StringRedisTemplate; // [v3.2 新增]
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID; // [v3.2 新增]

/**
 * NexusAI 核心知识中台 - v3.2 异步状态追踪版
 * 迭代记录：v1.0(基础) -> v2.0(Chroma) -> v2.1(SSE) -> v2.4(双核) -> v3.0(记忆) -> v3.1(动态上传) -> v3.2(异步化与状态追踪)
 */
@RestController
public class RAGController {

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final ChatMemoryProvider chatMemoryProvider;
    private final IngestionService ingestionService; // [v3.2 优化] 注入异步服务
    private final StringRedisTemplate redisTemplate; // [v3.2 优化] 注入 Redis 用于状态轮询

    private EmbeddingStore<TextSegment> embeddingStore;
    private EmbeddingModel embeddingModel;
    private Assistant assistant;

    @SystemMessage({
            "你现在是 NexusAI 首席行政助理。",
            "规则：必须且仅根据 [Context] 资料回答。若资料中无答案，请回复：'抱歉，当前知识库未涵盖此信息。'"
    })
    interface Assistant {
        TokenStream streamChat(@MemoryId String chatId, @UserMessage String message);
        String chat(@MemoryId String chatId, @UserMessage String message);
    }

    // [v3.2 修改] 构造函数注入新依赖
    public RAGController(ChatLanguageModel chatModel,
                         StreamingChatLanguageModel streamingChatModel,
                         ChatMemoryProvider chatMemoryProvider,
                         IngestionService ingestionService,
                         StringRedisTemplate redisTemplate) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.chatMemoryProvider = chatMemoryProvider;
        this.ingestionService = ingestionService;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void initRag() {
        System.out.println("🚀 正在初始化 NexusAI v3.2 异步引擎...");
        try {
            this.embeddingStore = ChromaEmbeddingStore.builder()
                    .baseUrl("http://localhost:8000")
                    .collectionName("nexus_v32_clean_sync_0323")
                    .build();
            this.embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

            refreshKnowledgeBase();

            this.assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(chatModel)
                    .streamingChatLanguageModel(streamingChatModel)
                    .chatMemoryProvider(chatMemoryProvider)
                    .contentRetriever(EmbeddingStoreContentRetriever.builder()
                            .embeddingStore(embeddingStore)
                            .embeddingModel(embeddingModel)
                            .maxResults(1)
                            .build())
                    .build();
            System.out.println("✅ NexusAI v3.2 引擎启动完成。");
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * [v3.1 新增] -> [v3.2 优化] 动态 PDF 异步上传接口
     * 💡 修改点：生成 TaskId 并分发异步任务，主线程立即返回，彻底解决前端上传卡顿问题
     */
    @PostMapping("/ai/upload")
    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("EMPTY_FILE");

        try {
            // 生成唯一任务 ID
            String taskId = UUID.randomUUID().toString();

            // 1. 保存物理文件
            String filePath = "documents/" + file.getOriginalFilename();
            File dest = new File(Paths.get(filePath).toAbsolutePath().toString());
            file.transferTo(dest);

            // 2. 💡 [v3.2 核心] 异步分发：将解析重任交给后台线程池
            ingestionService.ingestAsync(taskId, dest.toPath(), embeddingModel, embeddingStore);

            // 3. 立即返回 TaskId 供前端轮询
            return ResponseEntity.ok(taskId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("UPLOAD_ERROR");
        }
    }

    /**
     * [v3.2 新增] 任务状态查询接口
     * 💡 逻辑：前端拿到 TaskId 后，每隔 1-2 秒访问此接口，从 Redis 获取处理进度
     */
    @GetMapping("/ai/upload/status/{taskId}")
    public String getUploadStatus(@PathVariable String taskId) {
        String status = redisTemplate.opsForValue().get("nexus:task:" + taskId);
        return status == null ? "NOT_FOUND" : status;
    }

    private void refreshKnowledgeBase() {
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                Paths.get("documents"), new ApacheTikaDocumentParser());
        EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(100, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(documents);
    }

    @GetMapping(value = "/ai/rag/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRagChat(@RequestParam String question, @RequestParam String chatId) {
        SseEmitter emitter = new SseEmitter(120 * 1000L);
        if (assistant == null) return null;

        assistant.streamChat(chatId, question)
                .onNext(token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .onComplete(response -> emitter.complete())
                .onError(error -> {
                    error.printStackTrace();
                    emitter.completeWithError(error);
                })
                .start();
        return emitter;
    }
}