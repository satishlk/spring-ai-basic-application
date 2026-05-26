package com.example.orchestrator.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private static final Logger log = LoggerFactory.getLogger(AuditController.class);

    private final AuditService audit;

    public AuditController(AuditService audit) { this.audit = audit; }

    /** Most recent N events, newest first. */
    @GetMapping(value = "/recent", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuditEvent> recent() {
        return audit.recent();
    }

    /** Aggregate counters for the dashboard header. */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuditService.Stats stats() { return audit.stats(); }

    /**
     * Server-Sent Events stream. Browser dashboards open this with EventSource
     * and receive each AuditEvent update (insert + state transitions + tool
     * call additions) in real time.
     *
     * <p>Robustness pattern (lessons from "AsyncRequestNotUsableException:
     * Broken pipe" + "Cannot start async: [ERROR]" we saw in production):
     * <ul>
     *   <li>A single {@code dead} flag short-circuits the subscriber once the
     *       socket has closed in any way (client disconnect, IO error,
     *       servlet container teardown). Without it, every subsequent
     *       AuditEvent that arrives at the Reactor sink would re-trigger
     *       emitter.send() → IOException → cascading async errors.</li>
     *   <li>All terminators ({@code onCompletion}, {@code onTimeout},
     *       {@code onError}) route through the same cleanup runnable so the
     *       Reactor subscription is always disposed exactly once.</li>
     *   <li>The subscriber catches {@code Throwable} (not just
     *       {@code IOException}) — Tomcat throws {@code
     *       AsyncRequestNotUsableException} for the same condition.</li>
     * </ul>
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(0L);     // no server-side timeout
        AtomicBoolean dead = new AtomicBoolean(false);

        Disposable subscription = audit.stream().subscribe(
                ev -> {
                    if (dead.get()) return;
                    try {
                        emitter.send(SseEmitter.event().name("audit").data(ev));
                    } catch (Throwable t) {
                        // Most common: browser tab closed → broken pipe.
                        // Don't keep trying — flip the flag and finish.
                        if (dead.compareAndSet(false, true)) {
                            log.debug("SSE subscriber disconnected: {}", t.toString());
                            try { emitter.completeWithError(t); } catch (Throwable ignored) {}
                        }
                    }
                },
                err -> {
                    if (dead.compareAndSet(false, true)) {
                        try { emitter.completeWithError(err); } catch (Throwable ignored) {}
                    }
                },
                () -> {
                    if (dead.compareAndSet(false, true)) {
                        try { emitter.complete(); } catch (Throwable ignored) {}
                    }
                });

        Runnable cleanup = () -> {
            dead.set(true);
            subscription.dispose();
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(t -> cleanup.run());

        return emitter;
    }
}
