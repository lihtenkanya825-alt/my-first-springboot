package com.example.demo;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * NexusAI 通用智能控制器
 * 版本演进：v2.4(声明式) -> v2.5(时间补丁) -> v3.0(分布式记忆) -> v3.1(稳定性修复)
 */
@RestController
public class AIController {

    private final ChatAssistant chatAssistant;

    interface ChatAssistant {
        // [v2.5] 时空感知 | [v3.0] 记忆挂载
        @SystemMessage("你是一个 NexusAI 通用助手。当前的真实时间是 2026 年 3 月。请基于此背景与用户交流。")
        TokenStream chat(@MemoryId String chatId, @UserMessage String message);
    }

    public AIController(StreamingChatLanguageModel streamingChatModel, ChatMemoryProvider chatMemoryProvider) {
        this.chatAssistant = AiServices.builder(ChatAssistant.class)
                .streamingChatLanguageModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    /**
     * [v3.1 修复] 补全流式异常监听链路
     */
    @GetMapping(value = "/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String message,
                                 @RequestParam(defaultValue = "default-user") String chatId) {
        SseEmitter emitter = new SseEmitter(60 * 1000L);

        chatAssistant.chat(chatId, message)
                .onNext(token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .onComplete(response -> emitter.complete())
                // 💡 [v3.1-Hotfix] 解决 One of onError or ignoreErrors must be invoked 报错
                .onError(error -> {
                    error.printStackTrace();
                    emitter.completeWithError(error);
                })
                .start();

        return emitter;
    }
}