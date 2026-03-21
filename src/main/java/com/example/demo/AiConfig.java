package com.example.demo;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel; // [v2.1新增]
import dev.langchain4j.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.zhipu.ZhipuAiStreamingChatModel; // [v2.1新增]
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

/**
 * NexusAI 大模型核心配置类
 * 负责管理阻塞式与流式大模型实例的生命周期
 */
@Configuration
public class AiConfig {

    /**
     * [v1.0优化] 封装 API Key 获取逻辑
     * 遵循 12-Factor App 规范，从环境变量读取敏感信息
     */
    private String getApiKey() {
        String apiKey = System.getenv("ZHIPU_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("❌ 启动失败：系统环境变量 ZHIPU_API_KEY 未配置！");
        }
        return apiKey;
    }

    /**
     * [v1.0保留] 基础阻塞式对话模型
     * 适用于标准 API 请求及常规 RAG 检索场景
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return ZhipuAiChatModel.builder()
                .apiKey(getApiKey())
                .model("glm-4-flash")
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * [v2.1修复] 高性能流式对话模型
     * 💡 修复点：补全了 callTimeout, connectTimeout 等参数，解决启动报 null 的问题
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return ZhipuAiStreamingChatModel.builder()
                .apiKey(getApiKey())
                .model("glm-4-flash")
                // [v2.1修复] 显式设置流式传输的超时，确保连接稳定性
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();
    }
}