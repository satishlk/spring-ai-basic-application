package com.example.orchestrator.service;

import com.example.orchestrator.prompt.AgentProfile;
import com.example.orchestrator.prompt.SystemPrompts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

/**
 * Executes one ChatClient round-trip with:
 *   • the hardcoded safety preamble (branch protection, workflow steps)
 *   • the agent profile's system prompt (task-specific style + rules)
 *   • the user's task as the user message
 *   • the MCP toolset filtered to the profile's allowed tools
 *
 * <p>Returns Claude's final assistant message — typically a one-paragraph
 * summary including the PR URL.
 */
@Service
public class ClaudeAgentRunner {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAgentRunner.class);

    private final ChatModel chatModel;
    private final McpToolsetProvider toolsetProvider;

    /**
     * We inject {@link ChatModel}, NOT the auto-configured {@code ChatClient.Builder}.
     *
     * <p>Reason: Spring AI 1.0's MCP-client starter auto-injects every discovered
     * MCP tool into the default Builder. If we then call {@code defaultToolCallbacks(tools)}
     * the agent ends up with each tool registered twice → {@code IllegalStateException:
     * Multiple tools with the same name}. Building a fresh ChatClient from the
     * ChatModel sidesteps that — we register exactly the per-profile filtered tools.
     */
    public ClaudeAgentRunner(ChatModel chatModel, McpToolsetProvider toolsetProvider) {
        this.chatModel       = chatModel;
        this.toolsetProvider = toolsetProvider;
    }

    public String run(AgentProfile profile, String userTask) {
        String allowedToolsCsv = String.join(", ", profile.allowedTools());
        String systemMessage =
                SystemPrompts.safetyPreamble(
                        profile.targetRepo(),
                        profile.baseBranch(),
                        profile.branchPrefix(),
                        allowedToolsCsv)
                + "\n\n# AGENT-SPECIFIC PROMPT\n\n"
                + profile.systemPrompt();

        ToolCallback[] tools = toolsetProvider.toolsFor(profile);

        if (tools.length == 0) {
            log.error("Refusing to call Claude with 0 tools — agent would hallucinate tool-call text. " +
                      "Check (1) MCP server is up, (2) agent's allowed-tools matches discovered names, " +
                      "(3) McpToolCallbackAutoConfiguration is excluded so global tools don't conflict.");
            throw new IllegalStateException("No tools available for agent profile " + profile.name());
        }

        log.info("Calling Claude — profile={}, tools={}, prompt={}chars, task={}chars",
                 profile.name(), tools.length, systemMessage.length(), userTask.length());

        // McpToolCallbackAutoConfiguration is excluded in OrchestratorApplication,
        // so we are the sole source of tools. No duplicate-name conflicts.
        ChatClient chat = ChatClient.builder(chatModel)
                .defaultSystem(systemMessage)
                .defaultToolCallbacks(tools)
                .build();

        String response = chat.prompt()
                .user(userTask)
                .call()
                .content();

        log.info("Claude returned — profile={}, response={}chars", profile.name(),
                 response == null ? 0 : response.length());
        return response == null ? "" : response;
    }
}
