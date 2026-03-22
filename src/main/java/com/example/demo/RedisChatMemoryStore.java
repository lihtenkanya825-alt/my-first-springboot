package com.example.demo;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * [v3.0 新增] 分布式聊天记忆存储器
 * 💡 设计意图：利用 Redis 持久化对话记录，实现 Java 服务重启后的“记忆恢复”
 * 技术细节：使用 StringRedisTemplate 进行消息的序列化(JSON)与反序列化
 */
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redisTemplate;
    // 规范化 Redis Key，方便后续在图形界面查看
    private static final String REDIS_PREFIX = "nexus:chat-memory:";

    public RedisChatMemoryStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // 从 Redis 提取 JSON 字符串
        String json = redisTemplate.opsForValue().get(REDIS_PREFIX + memoryId);
        // [v3.0] 使用内建反序列化工具还原对话列表
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // [v3.0] 将当前对话上下文转化为 JSON 存入 Redis
        String json = ChatMessageSerializer.messagesToJson(messages);
        redisTemplate.opsForValue().set(REDIS_PREFIX + memoryId, json);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(REDIS_PREFIX + memoryId);
    }
}