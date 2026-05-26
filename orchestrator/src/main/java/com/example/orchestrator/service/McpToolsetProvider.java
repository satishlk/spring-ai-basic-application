package com.example.orchestrator.service;

import com.example.orchestrator.audit.AuditService;
import com.example.orchestrator.audit.AuditingToolCallback;
import com.example.orchestrator.config.McpClientConfig.McpClientGuard;
import com.example.orchestrator.prompt.AgentProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;

/**
 * Builds an MCP toolset for a single agent run, filtered down to the
 * exact tools the {@link AgentProfile} allows. Anything else discovered
 * on the MCP server is silently dropped so the model literally cannot
 * call it.
 */
@Service
public class McpToolsetProvider {

    private static final Logger log = LoggerFactory.getLogger(McpToolsetProvider.class);

    private final McpClientGuard guard;
    private final AuditService   audit;

    public McpToolsetProvider(McpClientGuard guard, AuditService audit) {
        this.guard = guard;
        this.audit = audit;
    }

    /**
     * Returns the FILTERED tool list for logging/observability only.
     *
     * <p>The actual tool wiring into the chat model is done by Spring AI's
     * {@code McpToolCallbackAutoConfiguration} at boot — every discovered
     * MCP tool is globally available to {@link org.springframework.ai.chat.client.ChatClient}.
     * Trying to ADD a filtered subset on top of that produces a
     * {@code "Multiple tools with the same name"} validation error.
     *
     * <p>Per-agent tool restriction is therefore enforced by the system
     * prompt ({@link com.example.orchestrator.prompt.SystemPrompts}), not
     * by this provider. The {@code allowed-tools} YAML list still drives
     * the prompt's "Tool restrictions" section.
     */
    public ToolCallback[] toolsFor(AgentProfile profile) {
        if (!guard.isAvailable()) {
            throw new IllegalStateException("MCP client is not available — kill switch on or no connection wired");
        }

        // Wrap every connected MCP client as a Spring AI ToolCallbackProvider…
        ToolCallbackProvider provider = new SyncMcpToolCallbackProvider(guard.clients());

        Set<String> allowed = Set.copyOf(profile.allowedTools());

        ToolCallback[] all = provider.getToolCallbacks();

        // Log every discovered tool name once so the operator can compare against the
        // allow-list in application.yml. Spring AI 1.0 prefixes the name with the MCP
        // connection name (e.g. "spring_ai_mcp_client_github_create_branch") so
        // exact-match on "create_branch" alone fails — we match by SUFFIX too.
        if (log.isInfoEnabled()) {
            log.info("MCP tool discovery for profile '{}': {} tool(s) on the wire",
                     profile.name(), all.length);
            for (ToolCallback tc : all) {
                log.info("    • {}", tc.getToolDefinition().name());
            }
        }

        // Filter + dedup by tool-definition name, then wrap each with the
        // auditing decorator so the dashboard sees every invocation.
        ToolCallback[] filtered = Arrays.stream(all)
                .filter(tc -> matchesAllowed(tc.getToolDefinition().name(), allowed))
                .collect(java.util.stream.Collectors.toMap(
                        tc -> tc.getToolDefinition().name(),
                        tc -> (ToolCallback) new AuditingToolCallback(tc, audit),
                        (a, b) -> a,                       // keep first on duplicate name
                        java.util.LinkedHashMap::new))     // preserve discovery order
                .values()
                .toArray(new ToolCallback[0]);

        log.info("Agent profile '{}' — exposing {} of {} MCP tools (allowed: {})",
                 profile.name(), filtered.length, all.length, allowed);

        // Surface allow-list entries that didn't match ANY discovered tool. These
        // are silently dropped by the filter above and were a long-tail mystery to
        // debug (e.g. "list_branches" doesn't exist on the GitHub MCP server;
        // "add_item_to_project_v2" lives on a different MCP server entirely).
        java.util.List<String> unmatched = findUnmatched(allowed, all);
        if (!unmatched.isEmpty()) {
            log.warn("Agent profile '{}' has {} allow-list entr{} that match no discovered tool: {}. "
                   + "These names are silently ignored. Either remove them or check spelling/server. "
                   + "Discovered tool names are listed above this line.",
                   profile.name(), unmatched.size(), unmatched.size() == 1 ? "y" : "ies", unmatched);
        }

        if (filtered.length == 0 && all.length > 0) {
            log.warn("ZERO tools matched. Likely cause: Spring AI prefixed the names " +
                     "and your `allowed-tools:` list uses the bare names. The matcher " +
                     "already accepts suffix matches — if you still see this, copy the " +
                     "exact names from the log above into application.yml.");
        }
        return filtered;
    }

    /**
     * Return allow-list entries that match zero discovered tools.
     * Preserves the allow-list's original order to make the log line stable.
     */
    private static java.util.List<String> findUnmatched(Set<String> allowed, ToolCallback[] discovered) {
        java.util.List<String> miss = new java.util.ArrayList<>();
        for (String a : allowed) {
            boolean matched = false;
            for (ToolCallback tc : discovered) {
                if (matchesAllowed(tc.getToolDefinition().name(), Set.of(a))) {
                    matched = true;
                    break;
                }
            }
            if (!matched) miss.add(a);
        }
        return miss;
    }

    /**
     * Tolerant matcher — true if the tool name equals an allowed name OR ends with
     * "_<allowedName>" (Spring AI MCP prefixed naming) OR ends with "<allowedName>"
     * with no preceding underscore (alternative prefix conventions).
     */
    private static boolean matchesAllowed(String toolName, Set<String> allowed) {
        for (String a : allowed) {
            if (toolName.equals(a) || toolName.endsWith("_" + a) || toolName.endsWith(a)) {
                return true;
            }
        }
        return false;
    }
}
