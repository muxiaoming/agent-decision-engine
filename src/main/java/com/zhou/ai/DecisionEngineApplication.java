package com.zhou.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class DecisionEngineApplication {

    public static void main(String[] args) {
        // Reactor 自动上下文传播：subscribeOn 切换线程时，将 Reactor Context 中的
        // Observation (含 OTel span) 恢复到目标线程，确保 Langfuse 链路不分裂。
        Hooks.enableAutomaticContextPropagation();
        SpringApplication.run(DecisionEngineApplication.class, args);
    }
}
