package com.example.demo;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AIController {

    // 这是一个 AI 大模型对象，Spring Boot 会根据刚才的配置自动创建它
    private final ChatLanguageModel chatModel;

    public AIController(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 对话接口
     * 访问地址：http://localhost:8080/ai/chat?message=你好
     */
    @GetMapping("/ai/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "你是谁？") String message) {
        // 让大模型根据你的问题生成答案
        return chatModel.generate(message);
    }
}