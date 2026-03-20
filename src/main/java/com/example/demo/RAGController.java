package com.example.demo;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;
import java.util.List;

@RestController
public class RAGController {

    private final ChatLanguageModel chatModel;
    private Assistant assistant; // 将助手提炼为全局变量


    // 💡 优化：通过 @SystemMessage 告诉 AI 保持克制
    interface Assistant {
        @dev.langchain4j.service.SystemMessage("你是一个专业的公司行政助理。请严格根据提供的文档内容回答问题。如果文档中包含多个要点，请仅回答与用户问题相关的部分，不要回答无关内容。")
        String chat(String message);
    }

    public RAGController(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    // 💡 核心优化：项目启动时，只执行一次 PDF 解析
    @PostConstruct
    public void initRag() {
        System.out.println("🚀 正在初始化 NexusAI 知识库，请稍候...");
        try {
            EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
            EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

            // 1. 加载文档
            List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                    Paths.get("documents"), new ApacheTikaDocumentParser());

            // 2. 向量化并存入库
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            ingestor.ingest(documents);

            // 3. 初始化助手
            this.assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(chatModel)
                    .contentRetriever(EmbeddingStoreContentRetriever.builder()
                            .embeddingStore(embeddingStore)
                            .embeddingModel(embeddingModel)
                            .maxResults(3)
                            .build())
                    .build();
            System.out.println("✅ 知识库初始化成功！现在可以提问了。");
        } catch (Exception e) {
            System.err.println("❌ 知识库初始化失败：" + e.getMessage());
        }
    }

    @GetMapping("/ai/rag")
    public String ragChat(@RequestParam String question) {
        if (assistant == null) {
            return "系统正在初始化知识库，请刷新重试...";
        }
        // 现在这里直接回答，速度快如闪电！
        return assistant.chat(question);
    }
}



//http://localhost:8080/ai/rag?question=你的问题


