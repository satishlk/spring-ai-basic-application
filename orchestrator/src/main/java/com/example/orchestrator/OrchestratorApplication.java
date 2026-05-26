package com.example.orchestrator;

import org.springframework.ai.mcp.client.autoconfigure.McpToolCallbackAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * We exclude {@link McpToolCallbackAutoConfiguration} so the MCP tools
 * are NOT auto-registered globally on the ChatModel. Without that
 * exclusion, adding the same tools again via {@code defaultToolCallbacks}
 * in {@code ClaudeAgentRunner} produces {@code IllegalStateException:
 * "Multiple tools with the same name"}.
 *
 * <p>The exclusion keeps the rest of the MCP client wiring intact:
 * {@link org.springframework.ai.mcp.client.autoconfigure.McpClientAutoConfiguration}
 * still creates the {@code McpSyncClient} beans we need, just without
 * registering them as global ChatClient tools.
 */
@SpringBootApplication(exclude = { McpToolCallbackAutoConfiguration.class })
@ConfigurationPropertiesScan("com.example.orchestrator.config")
public class OrchestratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApplication.class, args);
    }
}
