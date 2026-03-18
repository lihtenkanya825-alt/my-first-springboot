// 这是包名，Spring Boot 项目的基础规范，照抄即可
package com.example.demo.controller;

// Spring Boot 核心注解，必须导入
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 注解说明：标记这是一个控制器，同时返回JSON/字符串（不是页面）
@RestController
public class HelloWorldController {

    // 注解说明：标记这是一个GET请求，访问路径是 /hello
    @GetMapping("/hello")
    public String sayHello() {
        // 返回内容，访问http://localhost:8080/hello就能看到
        return "Hello Spring Boot! 我的第一次Git推送成功啦～";
    }
}