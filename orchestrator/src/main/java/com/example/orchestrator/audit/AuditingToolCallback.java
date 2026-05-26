package com.example.orchestrator.audit;

import org.slf4j.MDC;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Decorates a {@link ToolCallback} so every invocation is recorded in the
 * {@link AuditService}. The correlationId is pulled from SLF4J MDC, which
 * {@code PipelineEventService} populates on the agent-task virtual thread.
 */
public class AuditingToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final AuditService audit;

    public AuditingToolCallback(ToolCallback delegate, AuditService audit) {
        this.delegate = delegate;
        this.audit    = audit;
    }

    @Override public ToolDefinition getToolDefinition() { return delegate.getToolDefinition(); }
    @Override public ToolMetadata   getToolMetadata()   { return delegate.getToolMetadata(); }

    @Override
    public String call(String args) {
        return timed(() -> delegate.call(args));
    }

    @Override
    public String call(String args, ToolContext context) {
        return timed(() -> delegate.call(args, context));
    }

    private String timed(java.util.function.Supplier<String> body) {
        String correlationId = MDC.get("correlationId");
        String toolName      = getToolDefinition().name();
        long   start         = System.currentTimeMillis();
        try {
            String out = body.get();
            audit.recordToolCall(correlationId, toolName,
                                 System.currentTimeMillis() - start, null);
            return out;
        } catch (RuntimeException e) {
            audit.recordToolCall(correlationId, toolName,
                                 System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }
}
