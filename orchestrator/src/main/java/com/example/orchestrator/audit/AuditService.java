package com.example.orchestrator.audit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent audit + real-time stream sink.
 *
 * <ul>
 *   <li>Backed by the H2 file-mode database via {@link AuditEventRepository}.
 *       Every state mutation calls {@code repo.save(ev)} so the row stays
 *       in sync with the in-memory cache.</li>
 *   <li>An in-memory ring buffer (capped at {@value #MAX_EVENTS}) serves the
 *       dashboard's "recent" snapshot — keeps it instant without DB hits.</li>
 *   <li>On boot ({@link #rehydrate()}), the most recent {@code MAX_EVENTS}
 *       rows are pulled into the cache, and any rows left in {@code RUNNING}
 *       state from a previous JVM are reconciled to {@code FAILED}.</li>
 *   <li>A Reactor {@link Sinks.Many} broadcasts mutations to SSE subscribers
 *       for the live dashboard.</li>
 *   <li>Counts + durations also reported to Micrometer for Prometheus.</li>
 * </ul>
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    static final int MAX_EVENTS = 200;

    /** Insertion-ordered, capped at {@link #MAX_EVENTS}. Keyed by correlationId. */
    private final Map<String, AuditEvent> events = new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<String, AuditEvent> e) {
            return size() > MAX_EVENTS;
        }
    };

    private final ConcurrentHashMap<String, AuditEvent> active = new ConcurrentHashMap<>();
    private final Sinks.Many<AuditEvent> sink = Sinks.many().multicast().onBackpressureBuffer(64);

    private final AuditEventRepository repo;
    private final Counter accepted;
    private final Counter completed;
    private final Counter failed;
    private final Counter toolCallsCounter;
    private final Timer   runDuration;

    public AuditService(AuditEventRepository repo, MeterRegistry registry) {
        this.repo        = repo;
        this.accepted    = Counter.builder("agent.run").tag("outcome", "accepted").register(registry);
        this.completed   = Counter.builder("agent.run").tag("outcome", "completed").register(registry);
        this.failed      = Counter.builder("agent.run").tag("outcome", "failed").register(registry);
        this.toolCallsCounter = Counter.builder("agent.tool.calls").register(registry);
        this.runDuration = Timer.builder("agent.run.duration")
                .publishPercentiles(0.5, 0.95, 0.99).register(registry);
    }

    /**
     * Boot-time rehydration:
     * <ol>
     *   <li>Pull the last {@link #MAX_EVENTS} rows (newest-first), reverse to
     *       insertion order, and load into the cache.</li>
     *   <li>Find any rows still in {@code RUNNING} — these were live in a
     *       previous JVM that died mid-run. Mark them {@code FAILED} with a
     *       clear reason and save the correction back.</li>
     * </ol>
     */
    @PostConstruct
    @Transactional
    public void rehydrate() {
        // 1) Reconcile stale RUNNING rows from a crashed prior JVM
        var stale = repo.findAllByState(AuditEvent.State.RUNNING);
        for (var ev : stale) {
            ev.markFailed("JVM restart before run completed (final state unknown)");
            repo.save(ev);
            log.warn("Reconciled stale RUNNING run {} → FAILED", ev.getCorrelationId());
        }

        // 2) Load most recent N into the cache
        var recent = repo.findRecent(PageRequest.of(0, MAX_EVENTS));
        Collections.reverse(recent);                    // oldest first → correct LRU order
        synchronized (events) {
            recent.forEach(ev -> events.put(ev.getCorrelationId(), ev));
        }
        log.info("Audit rehydrated {} events from DB ({} stale RUNNING reconciled)",
                 recent.size(), stale.size());
    }

    @Transactional
    public AuditEvent accept(String correlationId, String eventType, String task) {
        var ev = new AuditEvent(correlationId, eventType, task);
        repo.save(ev);
        synchronized (events) { events.put(correlationId, ev); }
        active.put(correlationId, ev);
        accepted.increment();
        publish(ev);
        return ev;
    }

    @Transactional
    public void markRunning(String correlationId) {
        update(correlationId, ev -> { ev.markRunning(); repo.save(ev); });
    }

    @Transactional
    public void markCompleted(String correlationId, String response) {
        var ev = active.remove(correlationId);
        if (ev == null) return;
        ev.markCompleted(response);
        repo.save(ev);
        completed.increment();
        if (ev.durationMs() != null) {
            runDuration.record(Duration.ofMillis(ev.durationMs()));
        }
        publish(ev);
        log.info("Audit COMPLETED {} dur={}ms toolCalls={}",
                 correlationId, ev.durationMs(), ev.getToolCalls().size());
    }

    @Transactional
    public void markFailed(String correlationId, String error) {
        var ev = active.remove(correlationId);
        if (ev == null) return;
        ev.markFailed(error);
        repo.save(ev);
        failed.increment();
        if (ev.durationMs() != null) {
            runDuration.record(Duration.ofMillis(ev.durationMs()));
        }
        publish(ev);
        log.info("Audit FAILED {} err={}", correlationId, error);
    }

    @Transactional
    public void markRejected(String correlationId, String reason) {
        var ev = active.remove(correlationId);
        if (ev == null) return;
        ev.markRejected(reason);
        repo.save(ev);
        failed.increment();
        publish(ev);
    }

    @Transactional
    public void recordToolCall(String correlationId, String tool, long durationMs, String error) {
        var ev = active.get(correlationId);
        if (ev == null) return;
        ev.recordToolCall(tool, durationMs, error);
        repo.save(ev);
        toolCallsCounter.increment();
        publish(ev);
    }

    /** Snapshot — newest first. Read from the in-memory cache for speed. */
    public List<AuditEvent> recent() {
        synchronized (events) {
            var list = new java.util.ArrayList<>(events.values());
            java.util.Collections.reverse(list);
            return list;
        }
    }

    public Flux<AuditEvent> stream() { return sink.asFlux(); }

    public Stats stats() {
        long acc = 0, run = 0, ok = 0, fail = 0, totalTools = 0;
        long minDur = Long.MAX_VALUE, maxDur = 0, sumDur = 0, doneCount = 0;
        synchronized (events) {
            for (var ev : events.values()) {
                acc++;
                totalTools += ev.getToolCalls().size();
                switch (ev.getState()) {
                    case RUNNING -> run++;
                    case COMPLETED -> {
                        ok++;
                        var d = ev.durationMs();
                        if (d != null) {
                            sumDur += d; doneCount++;
                            if (d < minDur) minDur = d;
                            if (d > maxDur) maxDur = d;
                        }
                    }
                    case FAILED, REJECTED -> fail++;
                    default -> {}
                }
            }
        }
        long avg = doneCount == 0 ? 0 : sumDur / doneCount;
        if (minDur == Long.MAX_VALUE) minDur = 0;
        return new Stats(acc, run, ok, fail, totalTools, avg, minDur, maxDur);
    }

    public record Stats(long total, long running, long completed, long failed,
                        long toolCalls, long avgDurMs, long minDurMs, long maxDurMs) {}

    // ── internals ─────────────────────────────────────────────────────────

    private void update(String correlationId, java.util.function.Consumer<AuditEvent> mut) {
        var ev = active.get(correlationId);
        if (ev != null) { mut.accept(ev); publish(ev); }
    }

    private void publish(AuditEvent ev) {
        var result = sink.tryEmitNext(ev);
        if (result.isFailure()) {
            log.debug("Audit sink emit failed: {}", result);
        }
    }
}
