package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.TimeUnit;

@RestController  // 告诉 Spring Boot，这是一个处理网页请求的类
public class UserController {

    // 自动注入 Redis 操作工具（Spring Boot 帮你写好了底层逻辑）
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 定义一个接口路径，比如前端访问 http://localhost:8080/user/1001
    @GetMapping("/user/{id}")
    public String getUserInfo(@PathVariable("id") String id) {

        // 1. 先去 Redis 缓存里查
        String cacheKey = "userInfo:" + id;
        String userFromRedis = redisTemplate.opsForValue().get(cacheKey);

        // 如果 Redis 里有数据，直接返回，速度极快！
        if (userFromRedis != null) {
            System.out.println("走缓存啦！速度飞快！");
            return "从 Redis 缓存中获取用户信息：" + userFromRedis;
        }

        // 2. 如果 Redis 没有，则去“数据库”查（这里我们用线程睡眠模拟数据库查询的耗时）
        System.out.println("缓存里没有，去 MySQL 数据库里查...");
        try {
            Thread.sleep(1000); // 模拟慢吞吞的数据库查询耗时 1秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 假设这是从数据库查出来的数据
        String userFromDb = "张三 (ID: " + id + ", 年龄: 21, 一本大学)";

        // 3. 查到之后，把它存入 Redis，并设置 60 秒后过期
        redisTemplate.opsForValue().set(cacheKey, userFromDb, 60, TimeUnit.SECONDS);

        return "从 MySQL 数据库中获取用户信息：" + userFromDb;
    }
}