package com.example.demo;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class AIController {

    private final ChatAssistant chatAssistant;

    // [v2.4] 接口级时间感知：解决模型 2023 年知识截止日期的幻觉问题
    interface ChatAssistant {
        @SystemMessage("你是一个 NexusAI 通用助手。当前的真实时间是 2026 年 3 月。")
        TokenStream chat(String message);
    }

    public AIController(StreamingChatLanguageModel streamingChatModel) {
        this.chatAssistant = AiServices.builder(ChatAssistant.class)
                .streamingChatLanguageModel(streamingChatModel)
                .build();
    }

    @GetMapping(value = "/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String message) {
        SseEmitter emitter = new SseEmitter(60 * 1000L);
        chatAssistant.chat(message)
                .onNext(token -> {
                    try {
                        // [v2.2] 修复：标准协议包装防止乱码
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) { emitter.completeWithError(e); }
                })
                .onComplete(response -> emitter.complete())
                .start();
        return emitter;
    }
}