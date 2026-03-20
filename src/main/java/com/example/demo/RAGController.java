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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;
import java.util.List;

@RestController
public class RAGController {

    private final ChatLanguageModel chatModel;

    // 定义助手接口
    interface Assistant {
        String chat(String message);
    }

    public RAGController(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/ai/rag")
    public String ragChat(@RequestParam String question) {
        try {
            // 1. 初始化内存向量库和嵌入模型
            EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
            EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

            // 2. 加载 documents 文件夹下的 PDF
            List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                    Paths.get("documents"), new ApacheTikaDocumentParser());

            // 3. 将文档存入向量库
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();
            ingestor.ingest(documents);

            // 4. 构建 AI 服务
            Assistant assistant = AiServices.builder(Assistant.class)
                    .chatLanguageModel(chatModel)
                    .contentRetriever(EmbeddingStoreContentRetriever.builder()
                            .embeddingStore(embeddingStore)
                            .embeddingModel(embeddingModel)
                            .maxResults(3)
                            .build())
                    .build();

            return assistant.chat(question);

        } catch (Exception e) {
            e.printStackTrace();
            return "RAG系统出错：" + e.getMessage();
        }
    }
}