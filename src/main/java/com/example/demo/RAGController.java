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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

/**
 * NexusAI 核心知识中台 - v3.1 动态热加载版 (Hotfix)
 * 迭代记录：v1.0(基础) -> v2.0(Chroma) -> v2.1(SSE) -> v2.4(双核) -> v3.0(记忆) -> v3.1(动态上传)
 */
@RestController
public class RAGController {

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final ChatMemoryProvider chatMemoryProvider;

    // [v3.1 优化] 将核心组件提炼为成员变量，供上传接口复用
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

    public RAGController(ChatLanguageModel chatModel,
                         StreamingChatLanguageModel streamingChatModel,
                         ChatMemoryProvider chatMemoryProvider) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.chatMemoryProvider = chatMemoryProvider;
    }

    @PostConstruct
    public void initRag() {
        System.out.println("🚀 正在初始化 NexusAI v3.1 动态引擎...");
        try {
            // 1. 初始化持久化存储与模型
            this.embeddingStore = ChromaEmbeddingStore.builder()
                    .baseUrl("http://localhost:8000")
                    .collectionName("nexus_v31_dynamic")
                    .build();
            this.embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

            // 2. 扫描现有的静态文档
            refreshKnowledgeBase();

            // 3. 构建 Assistant
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
            System.out.println("✅ NexusAI v3.1 引擎点火成功。");
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * [v3.1 新增] 核心业务：动态 PDF 上传与即时向量化
     */
    @PostMapping("/ai/upload")
    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("文件不能为空");

        try {
            // 1. 保存文件到本地 documents 文件夹
            String filePath = "documents/" + file.getOriginalFilename();
            File dest = new File(Paths.get(filePath).toAbsolutePath().toString());
            file.transferTo(dest);

            // 2. 实时解析并同步到 ChromaDB
            Document document = FileSystemDocumentLoader.loadDocument(dest.toPath(), new ApacheTikaDocumentParser());
            EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(100, 0))
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build()
                    .ingest(document);

            return ResponseEntity.ok("✅ 知识库已动态更新：" + file.getOriginalFilename());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("上传失败：" + e.getMessage());
        }
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

    /**
     * [v2.1 增强] SSE 流式接口
     * [v3.1 修复] 补全 onError 处理，解决 IllegalConfigurationException
     */
    @GetMapping(value = "/ai/rag/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRagChat(@RequestParam String question, @RequestParam String chatId) {
        SseEmitter emitter = new SseEmitter(120 * 1000L);
        if (assistant == null) return null;

        assistant.streamChat(chatId, question)
                .onNext(token -> {
                    try {
                        // [v2.2 优化] 使用事件包装确保协议解析正确
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .onComplete(response -> emitter.complete())
                // 💡 [v3.1-Hotfix] 必须显式处理错误，否则框架不允许启动流
                .onError(error -> {
                    error.printStackTrace();
                    emitter.completeWithError(error);
                })
                .start();
        return emitter;
    }
}