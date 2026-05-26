package com.example.orchestrator.service;

import com.example.orchestrator.agentconfig.AgentProfileService;
import com.example.orchestrator.prompt.AgentProfile;
import org.springframework.stereotype.Service;

/**
 * Thin façade over {@link AgentProfileService}.
 *
 * <p>Historically this class loaded YAML + classpath prompt files directly.
 * Since the hybrid (YAML ⊕ DB) override layer landed, the merge logic now
 * lives in {@code AgentProfileService} — this class exists only to keep
 * the existing {@code resolve(eventType)} call site stable for
 * {@code PipelineEventService} and {@code ClaudeAgentRunner}.
 *
 * <p>The in-memory cache that used to live here is gone: each request now
 * goes through {@code AgentProfileService.resolve(name)}, which performs a
 * single indexed SELECT (< 1 ms on H2 file mode) for the latest DB override
 * and merges with the in-memory YAML+prompt-file cache. That trade-off:
 * we lost a hashmap lookup, gained the ability for the dashboard to edit
 * a prompt and have the NEXT agent run pick it up without a JVM restart.
 */
@Service
public class AgentRouter {

    private final AgentProfileService profileService;

    public AgentRouter(AgentProfileService profileService) {
        this.profileService = profileService;
    }

    public AgentProfile resolve(String eventType) {
        return profileService.resolve(eventType);
    }
}
