package com.example.demo;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.zhipu.ZhipuAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class AiConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // 💡 针对 0.34.0 版本，必须显式设置所有超时参数，否则会报 null 错误
        return ZhipuAiChatModel.builder()
                .apiKey("6e39ae6d74324048ba945916fcd4e2ab.qIXyhns5lSGYJnty") // 你的智谱 Key
                .model("glm-4-flash")
                .callTimeout(Duration.ofSeconds(60))    // 总调用超时
                .connectTimeout(Duration.ofSeconds(60)) // 连接建立超时
                .readTimeout(Duration.ofSeconds(60))    // 读取响应超时
                .writeTimeout(Duration.ofSeconds(60))   // 写入请求超时
                .build();
    }
}