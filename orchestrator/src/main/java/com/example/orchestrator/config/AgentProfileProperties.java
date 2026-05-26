package com.example.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds {@code agents.profiles.<name>.*} — one entry per agent.
 * Adding a new agent = adding a YAML entry + a prompt file. No Java change.
 */
@ConfigurationProperties(prefix = "agents")
public class AgentProfileProperties {

    /** Keyed by profile name (e.g. "ci-failure-fixer", "test-card-curator-remote"). */
    private Map<String, Profile> profiles = new HashMap<>();

    public Map<String, Profile> getProfiles() { return profiles; }
    public void setProfiles(Map<String, Profile> profiles) { this.profiles = profiles; }

    /** One agent's full config. */
    public static class Profile {

        /** classpath:/prompts/<file>.md — the system prompt for this agent. */
        private String promptFile;

        /** MCP tool names this agent is permitted to call. Anything else is filtered out. */
        private List<String> allowedTools = List.of();

        /** GitHub repo the agent operates on, in "owner/repo" form. */
        private String targetRepo;

        /** Branch this agent's PRs target. NEVER pushed to directly. */
        private String baseBranch = "main";

        /** Feature-branch prefix (e.g. "ai/fix-"). The agent picks the suffix. */
        private String branchPrefix = "ai/";

        public String getPromptFile() { return promptFile; }
        public void setPromptFile(String promptFile) { this.promptFile = promptFile; }

        public List<String> getAllowedTools() { return allowedTools; }
        public void setAllowedTools(List<String> allowedTools) { this.allowedTools = allowedTools; }

        public String getTargetRepo() { return targetRepo; }
        public void setTargetRepo(String targetRepo) { this.targetRepo = targetRepo; }

        public String getBaseBranch() { return baseBranch; }
        public void setBaseBranch(String baseBranch) { this.baseBranch = baseBranch; }

        public String getBranchPrefix() { return branchPrefix; }
        public void setBranchPrefix(String branchPrefix) { this.branchPrefix = branchPrefix; }
    }
}
