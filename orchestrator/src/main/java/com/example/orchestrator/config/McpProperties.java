package com.example.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code mcp.github.*}. Backed by env vars via the standard
 * Spring relaxed binding (e.g. MCP_GITHUB_ENABLED).
 *
 * <p>The {@code enabled} flag is the kill switch: if false, the
 * conditional bean in {@link McpClientConfig} is never created,
 * the SSE connection is never opened, and {@code PipelineEventService}
 * short-circuits to a log line.
 */
@ConfigurationProperties(prefix = "mcp.github")
public class McpProperties {
    private boolean enabled = true;
    private String  connectionName     = "github";
    private long    handshakeTimeoutMs = 10_000L;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getConnectionName() { return connectionName; }
    public void setConnectionName(String connectionName) { this.connectionName = connectionName; }

    public long getHandshakeTimeoutMs() { return handshakeTimeoutMs; }
    public void setHandshakeTimeoutMs(long ms) { this.handshakeTimeoutMs = ms; }
}
