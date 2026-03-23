package com.example.demo;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * [v3.2 新增] 知识摄取异步服务
 * 💡 职责：将 Controller 剥离出的耗时解析逻辑异步化，并利用 Redis 维护任务状态机
 */
@Service
public class IngestionService {

    private final StringRedisTemplate redisTemplate;

    public IngestionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * [v3.2] 异步向量化任务
     * @Async("ingestionExecutor"): 使用 AiConfig 中定义的专用线程池，防止主线程阻塞
     */
    @Async("ingestionExecutor")
    public void ingestAsync(String taskId, Path filePath, EmbeddingModel model, EmbeddingStore store) {
        String statusKey = "nexus:task:" + taskId;
        try {
            // 1. 在 Redis 中标记任务开始
            redisTemplate.opsForValue().set(statusKey, "PROCESSING", 10, TimeUnit.MINUTES);
            System.out.println("🧵 [" + Thread.currentThread().getName() + "] 开始解析文档，任务ID: " + taskId);

            // 2. 执行文档加载与解析 (封装 v3.1 的逻辑)
            Document document = FileSystemDocumentLoader.loadDocument(filePath, new ApacheTikaDocumentParser());

            // 3. 执行向量化摄取 (保持 v2.3/v3.1 的纳米分片精度)
            EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(100, 0))
                    .embeddingModel(model)
                    .embeddingStore(store)
                    .build()
                    .ingest(document);

            // 4. 任务成功：更新 Redis 状态
            redisTemplate.opsForValue().set(statusKey, "COMPLETED", 10, TimeUnit.MINUTES);
            System.out.println("✅ [" + Thread.currentThread().getName() + "] 解析任务完成: " + taskId);

        } catch (Exception e) {
            // 5. 任务失败：记录异常信息
            redisTemplate.opsForValue().set(statusKey, "FAILED: " + e.getMessage(), 10, TimeUnit.MINUTES);
            System.err.println("❌ 后台解析任务 [" + taskId + "] 异常: " + e.getMessage());
        }
    }
}