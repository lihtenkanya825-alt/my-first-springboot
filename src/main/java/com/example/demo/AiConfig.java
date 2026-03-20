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
        // 💡 重点：这里直接写变量名，不需要加 $ 符号
        String apiKey = System.getenv("ZHIPU_API_KEY");

        // 增加一个防御性判断，防止你忘了配环境变量导致程序报错
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("❌ 启动失败：未检测到环境变量 ZHIPU_API_KEY，请检查 IDEA 配置！");
        }

        return ZhipuAiChatModel.builder()
                .apiKey(apiKey)
                .model("glm-4-flash")
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();
    }
}