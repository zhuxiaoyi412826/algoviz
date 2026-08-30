package com.algoviz.know.qrdant;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 独立向量检索服务：算法题目知识库（Qdrant + bge-large-zh-v1.5 + Dubbo 3）。
 * 主服务通过 Dubbo 直连（dubbo://127.0.0.1:20880）调用，服务未启动不影响主服务核心功能。
 */
@EnableDubbo
@SpringBootApplication
public class KnowQrdantApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowQrdantApplication.class, args);
    }
}
