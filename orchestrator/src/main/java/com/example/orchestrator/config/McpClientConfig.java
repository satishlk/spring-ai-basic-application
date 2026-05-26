package com.example.orchestrator.config;

import io.modelcontextprotocol.client.McpSyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Holds the MCP client wiring guards.
 *
 * <p>Spring AI's MCP-client starter auto-creates a {@code List<McpSyncClient>}
 * from the connections declared in {@code spring.ai.mcp.client.sse.connections.*}.
 * We provide a thin wrapper that is <b>only created when the kill switch is on</b>:
 *
 * <ul>
 *   <li>{@code mcp.github.enabled=true}  → the @Bean is registered, the
 *       starter's auto-config wires up the SSE connection at boot. If
 *       the SSE server is unreachable at boot, the underlying client
 *       lazy-connects on first use rather than failing the app.</li>
 *   <li>{@code mcp.github.enabled=false} → this bean does NOT exist;
 *       {@link com.example.orchestrator.service.PipelineEventService}
 *       short-circuits with a warning before ever touching MCP.</li>
 * </ul>
 *
 * <p>The wrapper type ({@link McpClientGuard}) is what services inject —
 * so a missing bean fails fast at autowire time rather than producing
 * mysterious NullPointerExceptions.
 */
@Configuration
public class McpClientConfig {

    private static final Logger log = LoggerFactory.getLogger(McpClientConfig.class);

    @Bean
    @Primary
    @ConditionalOnProperty(name = "mcp.github.enabled", havingValue = "true", matchIfMissing = true)
    public McpClientGuard mcpClientGuard(List<McpSyncClient> autowiredClients, McpProperties props) {
        if (autowiredClients == null || autowiredClients.isEmpty()) {
            log.warn("mcp.github.enabled=true but no McpSyncClient beans found — " +
                     "check spring.ai.mcp.client.sse.connections.* in application.yml");
            return new McpClientGuard(List.of(), props);
        }
        log.info("McpClientGuard wired with {} MCP client(s); SSE connection '{}' will lazy-connect on first call",
                 autowiredClients.size(), props.getConnectionName());
        return new McpClientGuard(autowiredClients, props);
    }

    /** Thin wrapper so consumers can fail fast on absent kill-switch instead of NPE. */
    public record McpClientGuard(List<McpSyncClient> clients, McpProperties props) {
        public boolean isAvailable() { return !clients.isEmpty(); }
    }
}
