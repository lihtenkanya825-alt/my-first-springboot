package com.example.demo;

import dev.langchain4j.memory.chat.ChatMemoryProvider; // [v3.0 新增]
import dev.langchain4j.memory.chat.MessageWindowChatMemory; // [v3.0 新增]
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.zhipu.ZhipuAiStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

/**
 * NexusAI 核心配置类
 * 管理 AI 实例生命周期，实现配置与业务逻辑的解耦
 */
@Configuration
public class AiConfig {

    /**
     * [v1.0 优化] 环境变量注入 API Key，实现脱敏
     */
    private String getApiKey() {
        String apiKey = System.getenv("ZHIPU_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("❌ 配置异常：未检测到环境变量 ZHIPU_API_KEY");
        }
        return apiKey;
    }

    /**
     * [v3.0 新增] 全局对话记忆工厂
     * 💡 核心功能：为每个 sessionId 创建一个挂载了 Redis 的“记忆小本子”
     * maxMessages(10): 维持滑动窗口，只记录最近 10 条对话，兼顾成本与性能
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(RedisChatMemoryStore redisChatMemoryStore) {
        return sessionId -> MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(10)
                .chatMemoryStore(redisChatMemoryStore) // 💡 [v3.0 关键] 挂载持久化存储
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