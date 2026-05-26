package com.example.orchestrator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Application-level throttle. Java virtual threads are cheap, but the
 * downstream GitHub API is not — a single agent run can fire 10-20
 * REST/GraphQL calls. Two parallel agent runs sit comfortably under
 * the ~80 calls/minute soft limit; beyond that we'd see 429s.
 *
 * <p>Pattern of use:
 * <pre>{@code
 *     if (!gate.tryAcquire()) {
 *         log.warn("Concurrency gate full; dropping event");
 *         return;
 *     }
 *     try {
 *         runAgent(...);
 *     } finally {
 *         gate.release();
 *     }
 * }</pre>
 */
@Component
public class ConcurrencyGate {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyGate.class);

    private final Semaphore semaphore;
    private final long      acquireTimeoutMs;

    public ConcurrencyGate(
            @Value("${orchestrator.concurrency.max-parallel:2}") int maxParallel,
            @Value("${orchestrator.concurrency.acquire-timeout-ms:30000}") long acquireTimeoutMs) {
        this.semaphore        = new Semaphore(maxParallel, /* fair */ true);
        this.acquireTimeoutMs = acquireTimeoutMs;
        log.info("ConcurrencyGate initialised with {} permit(s), {}ms acquire timeout",
                 maxParallel, acquireTimeoutMs);
    }

    /** Returns true if a permit was acquired within {@code acquireTimeoutMs}. */
    public boolean tryAcquire() throws InterruptedException {
        return semaphore.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
    }

    public void release() { semaphore.release(); }

    public int available() { return semaphore.availablePermits(); }
}
