package com.example.orchestrator.prompt;

import java.util.List;

/**
 * Resolved view of one entry under {@code agents.profiles.*}.
 * Built by {@code AgentRouter} after merging YAML config + the loaded
 * prompt-file contents. Immutable.
 */
public record AgentProfile(
    String name,
    String systemPrompt,        // contents of the .md file
    List<String> allowedTools,  // MCP tool names this profile may call
    String targetRepo,          // owner/repo
    String baseBranch,          // never written to directly
    String branchPrefix         // e.g. "ai/fix-"
) {}
