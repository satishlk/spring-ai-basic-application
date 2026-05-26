package com.example.orchestrator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Virtual-thread executor used to hand off webhook processing off the
 * Servlet container thread so the controller can reply 202 immediately.
 *
 * <p>This is unbounded by design — the actual concurrency limit lives in
 * {@link ConcurrencyGate} (a Semaphore) so that we can rate-limit GitHub
 * API calls regardless of how many virtual threads we cheaply spawn.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "agentTaskExecutor", destroyMethod = "shutdown")
    public ExecutorService agentTaskExecutor() {
        ThreadFactory factory = Thread.ofVirtual()
                .name("agent-task-", 0L)
                .factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    /** Optional debug helper — exposes a global virtual-thread id sequence. */
    @Bean
    public AtomicLong virtualThreadSeq() {
        return new AtomicLong();
    }
}
