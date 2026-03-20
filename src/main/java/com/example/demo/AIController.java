package com.example.demo;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AIController {

    private final ChatLanguageModel chatModel;

    // Spring 会自动从 AiConfig 里把创建好的大脑传进来
    public AIController(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/ai/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "你好") String message) {
        return chatModel.generate(message);
    }
}

//http://localhost:8080/ai/chat?message=你的问题