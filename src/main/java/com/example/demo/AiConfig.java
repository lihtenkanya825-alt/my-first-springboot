package com.example.demo;

import dev.langchain4j.memory.chat.ChatMemoryProvider; // [v3.0 新增]
import dev.langchain4j.memory.chat.MessageWindowChatMemory; // [v3.0 新增]
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.zhipu.ZhipuAiStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync; // [v3.2 新增]
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor; // [v3.2 新增]
import java.time.Duration;
import java.util.concurrent.Executor; // [v3.2 新增]

/**
 * NexusAI 核心配置类
 * 管理 AI 实例生命周期，并配置异步任务执行环境，实现高性能并发摄取
 */
@Configuration
@EnableAsync // [v3.2 新增] 开启 Spring 异步任务支持，使 @Async 注解生效
public class AiConfig {

    /**
     * [v1.0 优化] 封装 API Key 获取逻辑
     * 遵循 12-Factor App 规范，实现配置与代码脱敏
     */
    private String getApiKey() {
        String apiKey = System.getenv("ZHIPU_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("❌ 配置异常：未检测到环境变量 ZHIPU_API_KEY");
        }
        return apiKey;
    }

    /**
     * [v3.2 新增] 知识摄取专用线程池 (Ingestion Thread Pool)
     * 💡 大厂逻辑：将高耗时的 I/O 与 CPU 密集型任务（文档解析与向量化）与 Web 请求主线程隔离
     * 避免因处理大型文档而耗尽容器线程，确保系统高可用性
     */
    @Bean(name = "ingestionExecutor")
    public Executor ingestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        // 核心线程数：保持 5 个常驻“工人”
        executor.setMaxPoolSize(10);       // 最大线程数：极端负载下扩展至 10 个
        executor.setQueueCapacity(100);    // 缓冲队列：支持 100 个任务排队
        executor.setThreadNamePrefix("NexusWorker-"); // 设置线程前缀，便于监控与排错
        executor.initialize();
        return executor;
    }

    /**
     * [v3.0 新增] 全局对话记忆工厂
     * 💡 核心功能：为每个 sessionId 创建一个挂载了 Redis 的“记忆小本子”
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(RedisChatMemoryStore redisChatMemoryStore) {
        return sessionId -> MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(10)
                .chatMemoryStore(redisChatMemoryStore) // [v3.0 关键] 挂载持久化存储
                .build();
    }

    /**
     * [v1.0 基础] 阻塞式对话模型
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return ZhipuAiChatModel.builder()
                .apiKey(getApiKey())
                .model("glm-4-flash")
                .temperature(0.7) // [v2.4] 调优：恢复自然表达
                .callTimeout(Duration.ofSeconds(60)) // [v2.1 修复] 必填参数补全
                .connectTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * [v2.1 新增] 流式对话模型，驱动 SSE 打字机效果
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return ZhipuAiStreamingChatModel.builder()
                .apiKey(getApiKey())
                .model("glm-4-flash")
                .temperature(0.7) // [v2.4] 调优
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();
    }
}