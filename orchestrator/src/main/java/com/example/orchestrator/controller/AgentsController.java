package com.example.orchestrator.controller;

import com.example.orchestrator.agentconfig.AgentProfileService;
import com.example.orchestrator.agentconfig.AgentProfileService.CreateForm;
import com.example.orchestrator.agentconfig.AgentProfileService.OverrideForm;
import com.example.orchestrator.agentconfig.AgentProfileService.Resolved;
import com.example.orchestrator.agentconfig.AgentProfileService.VersionSummary;
import com.example.orchestrator.config.AgentProfileProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Inventory + edit API for deployed agents.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET    /api/agents}                   — list summary cards.</li>
 *   <li>{@code GET    /api/agents/{name}}            — full detail (effective + version meta + system prompt).</li>
 *   <li>{@code POST   /api/agents/{name}/override}   — save a new override (creates v = N+1).</li>
 *   <li>{@code DELETE /api/agents/{name}/override}   — revert to YAML default (creates a "revert" row).</li>
 *   <li>{@code GET    /api/agents/{name}/history}    — version history, newest first.</li>
 *   <li>{@code POST   /api/agents/{name}/revert/{v}} — restore a specific prior version's content.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/agents")
public class AgentsController {

    private final AgentProfileProperties properties;
    private final AgentProfileService    service;

    public AgentsController(AgentProfileProperties properties, AgentProfileService service) {
        this.properties = properties;
        this.service    = service;
    }

    // ── READ ───────────────────────────────────────────────────────────────

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AgentSummary> list() {
        return service.listNames().stream()
                .map(n -> AgentSummary.from(n, service.resolveDetail(n),
                                            properties.getProfiles().get(n)))
                .toList();
    }

    @GetMapping(value = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AgentDetail> detail(@PathVariable String name) {
        try {
            return ResponseEntity.ok(AgentDetail.from(
                    name, service.resolveDetail(name),
                    properties.getProfiles().get(name)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create a brand-new DB-only agent (no YAML baseline). All fields required.
     * Request body wraps the form alongside the desired name:
     * <pre>
     *   POST /api/agents
     *   { "name": "my-new-agent",
     *     "promptText": "…",
     *     "allowedTools": ["create_branch","create_or_update_file"],
     *     "targetRepo": "owner/repo", "baseBranch": "main",
     *     "branchPrefix": "ai/", "changeNote": "initial create" }
     * </pre>
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@RequestBody CreateRequest req,
                                    @RequestHeader(name = "X-User", required = false) String user) {
        try {
            Resolved r = service.create(req.name(), new CreateForm(
                    req.promptText(),
                    req.allowedTools(),
                    req.targetRepo(),
                    req.baseBranch(),
                    req.branchPrefix(),
                    req.changeNote()
            ), user == null ? "ui" : user);
            return ResponseEntity.ok(AgentDetail.from(req.name(), r, properties.getProfiles().get(req.name())));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("error", iae.getMessage()));
        }
    }

    /** Combined name + CreateForm body for the POST /api/agents endpoint. */
    public record CreateRequest(
            String       name,
            String       promptText,
            List<String> allowedTools,
            String       targetRepo,
            String       baseBranch,
            String       branchPrefix,
            String       changeNote
    ) {}

    @GetMapping(value = "/{name}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<VersionSummary> history(@PathVariable String name) {
        return service.history(name);
    }

    // ── WRITE ──────────────────────────────────────────────────────────────

    @PostMapping(value = "/{name}/override",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public AgentDetail saveOverride(@PathVariable String name,
                                    @RequestBody OverrideForm form,
                                    @RequestHeader(name = "X-User", required = false) String user) {
        Resolved r = service.saveOverride(name, form, user == null ? "ui" : user);
        return AgentDetail.from(name, r, properties.getProfiles().get(name));
    }

    @DeleteMapping(value = "/{name}/override", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> revertToDefault(@PathVariable String name,
                                             @RequestHeader(name = "X-User", required = false) String user) {
        if (!service.isYamlDeclared(name)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error",
                "Agent '" + name + "' is DB-only — there is no YAML default to revert to. " +
                "Either save a new version with the desired content, or restore an earlier version from history."
            ));
        }
        Resolved r = service.revertToDefault(name, user == null ? "ui" : user);
        return ResponseEntity.ok(AgentDetail.from(name, r, properties.getProfiles().get(name)));
    }

    @PostMapping(value = "/{name}/revert/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AgentDetail revertToVersion(@PathVariable String name,
                                       @PathVariable int version,
                                       @RequestHeader(name = "X-User", required = false) String user) {
        Resolved r = service.revertToVersion(name, version, user == null ? "ui" : user);
        return AgentDetail.from(name, r, properties.getProfiles().get(name));
    }

    // ── DTOs ───────────────────────────────────────────────────────────────

    /** Lightweight card for the listing page. */
    public record AgentSummary(
            String       name,
            String       source,            // "yaml" — YAML baseline (optionally overridden) ; "db" — created via UI
            String       targetRepo,
            String       baseBranch,
            String       branchPrefix,
            List<String> allowedTools,
            String       promptFile,        // null for DB-only agents
            Integer      currentVersion,    // null = pure YAML (no overrides yet)
            Instant      lastEditedAt,
            String       lastEditedBy,
            boolean      revertedToDefault
    ) {
        static AgentSummary from(String name, Resolved r, AgentProfileProperties.Profile yaml) {
            var p = r.effective();
            var v = r.latestVersion();
            return new AgentSummary(
                name,
                r.yamlDeclared() ? "yaml" : "db",
                p.targetRepo(), p.baseBranch(), p.branchPrefix(),
                p.allowedTools(),
                yaml == null ? null : yaml.getPromptFile(),
                v == null ? null : v.version(),
                v == null ? null : v.changedAt(),
                v == null ? null : v.changedBy(),
                v != null && v.revertToDefault()
            );
        }
    }

    /** Detail view used by the modal — includes the fully-resolved system prompt text. */
    public record AgentDetail(
            String       name,
            String       source,
            String       targetRepo,
            String       baseBranch,
            String       branchPrefix,
            List<String> allowedTools,
            String       promptFile,
            String       systemPrompt,       // the EFFECTIVE prompt text (merged)
            Integer      currentVersion,
            Instant      lastEditedAt,
            String       lastEditedBy,
            boolean      revertedToDefault
    ) {
        static AgentDetail from(String name, Resolved r, AgentProfileProperties.Profile yaml) {
            var p = r.effective();
            var v = r.latestVersion();
            return new AgentDetail(
                name,
                r.yamlDeclared() ? "yaml" : "db",
                p.targetRepo(), p.baseBranch(), p.branchPrefix(),
                p.allowedTools(),
                yaml == null ? null : yaml.getPromptFile(),
                p.systemPrompt(),
                v == null ? null : v.version(),
                v == null ? null : v.changedAt(),
                v == null ? null : v.changedBy(),
                v != null && v.revertToDefault()
            );
        }
    }
}
