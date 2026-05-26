package com.example.orchestrator.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One row per pipeline event. JPA-managed since persistence was added.
 *
 * <p>Mutation pattern: domain methods (markRunning, markCompleted, …)
 * mutate fields in place; {@link AuditService} explicitly calls
 * {@code repository.save(this)} after every mutation so the row stays
 * in sync with the in-memory cache.
 *
 * <p>{@link #toolCalls} is stored as JSON text via {@link ToolCallsJsonConverter}
 * rather than a child table — keeps the schema tiny and matches how the
 * dashboard consumes the list anyway.
 */
@Entity
@Table(name = "audit_event",
       indexes = @Index(name = "idx_audit_accepted_at", columnList = "acceptedAt"))
public class AuditEvent {

    public enum State { ACCEPTED, RUNNING, COMPLETED, FAILED, REJECTED }

    @Id
    @Column(length = 64)
    private String correlationId;

    @Column(nullable = false, length = 128)
    private String eventType;

    @Column(columnDefinition = "VARCHAR(256)")
    private String task;            // truncated to 200 chars at construction

    @Column(nullable = false)
    private Instant acceptedAt;

    private Instant startedAt;
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private State state;

    @Column(columnDefinition = "VARCHAR(1024)")
    private String finalResponse;   // truncated to 1000 chars

    @Column(length = 512)
    private String prUrl;

    @Column(columnDefinition = "VARCHAR(512)")
    private String error;           // truncated to 500 chars

    @Convert(converter = ToolCallsJsonConverter.class)
    @Column(name = "tool_calls_json", columnDefinition = "CLOB")
    private List<ToolCallRecord> toolCalls = new CopyOnWriteArrayList<>();

    /** Required by JPA. Do not use directly. */
    protected AuditEvent() {}

    public AuditEvent(String correlationId, String eventType, String task) {
        this.correlationId = correlationId;
        this.eventType     = eventType;
        this.task          = truncate(task, 200);
        this.acceptedAt    = Instant.now();
        this.state         = State.ACCEPTED;
    }

    public void markRunning()   { this.state = State.RUNNING;   this.startedAt = Instant.now(); }
    public void markCompleted(String response) {
        this.state = State.COMPLETED;
        this.completedAt = Instant.now();
        this.finalResponse = truncate(response, 1000);
        this.prUrl = extractPrUrl(response);
    }
    public void markFailed(String err) {
        this.state = State.FAILED;
        this.completedAt = Instant.now();
        this.error = truncate(err, 500);
    }
    public void markRejected(String reason) {
        this.state = State.REJECTED;
        this.completedAt = Instant.now();
        this.error = truncate(reason, 500);
    }

    public void recordToolCall(String name, long durationMs, String error) {
        // Replace the list with a new CopyOnWriteArrayList so JPA's converter
        // sees a fresh object on next save() (dirty-tracking by reference).
        var updated = new CopyOnWriteArrayList<>(toolCalls);
        updated.add(new ToolCallRecord(name, Instant.now(), durationMs, error));
        this.toolCalls = updated;
    }

    public Long durationMs() {
        if (completedAt == null) return null;
        return java.time.Duration.between(acceptedAt, completedAt).toMillis();
    }

    public String getCorrelationId() { return correlationId; }
    public String getEventType()     { return eventType; }
    public String getTask()          { return task; }
    public Instant getAcceptedAt()   { return acceptedAt; }
    public Instant getStartedAt()    { return startedAt; }
    public Instant getCompletedAt()  { return completedAt; }
    public State getState()          { return state; }
    public String getFinalResponse() { return finalResponse; }
    public String getPrUrl()         { return prUrl; }
    public String getError()         { return error; }
    public List<ToolCallRecord> getToolCalls() { return new ArrayList<>(toolCalls); }

    /** A single tool invocation inside an agent run. */
    public record ToolCallRecord(String name, Instant at, long durationMs, String error) {}

    // ── helpers ───────────────────────────────────────────────────────────

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /** Best-effort PR URL extraction from Claude's final assistant text. */
    private static String extractPrUrl(String response) {
        if (response == null) return null;
        var m = java.util.regex.Pattern
                .compile("https?://github\\.com/[\\w.-]+/[\\w.-]+/pull/\\d+")
                .matcher(response);
        return m.find() ? m.group() : null;
    }
}
