package com.example.orchestrator.agentconfig;

import com.example.orchestrator.config.AgentProfileProperties;
import com.example.orchestrator.prompt.AgentProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * The brain of the hybrid (YAML + DB) agent-profile system.
 *
 * <h2>Resolution rules</h2>
 * For each field of an {@link AgentProfile}, the value comes from:
 * <ol>
 *   <li>The DB override (latest non-null value of that field), <em>if</em>
 *       any version row exists for this agent AND that field is non-null
 *       AND the row is not a {@code revertToDefault} marker;</li>
 *   <li>otherwise from {@code application.yml} ({@link AgentProfileProperties}).</li>
 * </ol>
 *
 * <h2>Audit</h2>
 * Every save inserts a new {@link AgentProfileVersion} row. The history
 * is append-only — you can always replay "what did this agent look like
 * at version N?" Reverting equals inserting a special row whose content
 * is "all overrides cleared", which is preserved in the audit log too.
 *
 * <h2>Caching</h2>
 * YAML defaults + classpath {@code .md} files are cached in process (they
 * never change at runtime). The DB latest-version lookup is one indexed
 * SELECT per resolve() call — well under a millisecond on H2 file mode.
 */
@Service
public class AgentProfileService {

    private static final Logger log = LoggerFactory.getLogger(AgentProfileService.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final AgentProfileProperties properties;
    private final AgentProfileVersionRepository repo;
    private final ResourceLoader resourceLoader;

    /** Cached classpath:/prompts/foo.md content — immutable per deployment. */
    private final ConcurrentHashMap<String, String> promptFileCache = new ConcurrentHashMap<>();

    public AgentProfileService(AgentProfileProperties properties,
                               AgentProfileVersionRepository repo,
                               ResourceLoader resourceLoader) {
        this.properties     = properties;
        this.repo           = repo;
        this.resourceLoader = resourceLoader;
    }

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$");

    // ── READ paths ─────────────────────────────────────────────────────────

    /** Effective {@link AgentProfile} for a given agent name. */
    public AgentProfile resolve(String name) {
        var yaml = properties.getProfiles().get(name);
        var latest = repo.findFirstByNameOrderByVersionDesc(name);

        if (yaml == null && latest.isEmpty()) {
            throw new NoSuchElementException(
                "Unknown agent profile '" + name + "'. Known YAML: " +
                properties.getProfiles().keySet() + " · Known DB: " + repo.findDistinctNames());
        }

        if (yaml == null) {
            // DB-only agent — the latest row must carry all fields itself.
            return fromDbOnly(name, latest.get());
        }
        return merge(name, yaml, latest);
    }

    /** All agent names (YAML-declared ∪ DB-only). YAML names come first. */
    public List<String> listNames() {
        Set<String> all = new LinkedHashSet<>(properties.getProfiles().keySet());
        all.addAll(repo.findDistinctNames());
        return new ArrayList<>(all);
    }

    /** True if the name has a YAML baseline (vs being DB-only). */
    public boolean isYamlDeclared(String name) {
        return properties.getProfiles().containsKey(name);
    }

    /** Full version history for an agent, newest first. */
    public List<VersionSummary> history(String name) {
        return repo.findByNameOrderByVersionDesc(name).stream()
                .map(VersionSummary::from)
                .toList();
    }

    /** Detailed effective profile + version-row metadata for the UI. */
    public Resolved resolveDetail(String name) {
        var effective = resolve(name);
        var latest = repo.findFirstByNameOrderByVersionDesc(name);
        var meta = latest.map(VersionSummary::from).orElse(null);
        return new Resolved(effective, meta, isYamlDeclared(name));
    }

    // ── CREATE (DB-only agent) ─────────────────────────────────────────────

    /**
     * Create a brand-new agent that lives ONLY in the DB (no YAML baseline).
     * All fields are required — there is no YAML to inherit from.
     */
    @Transactional
    public Resolved create(String name, CreateForm form, String createdBy) {
        validateNewName(name);

        // Required field validation — no YAML fallback for DB-only agents.
        if (isBlank(form.promptText()))
            throw new IllegalArgumentException("promptText is required for a new agent");
        if (form.allowedTools() == null || form.allowedTools().isEmpty())
            throw new IllegalArgumentException("allowedTools is required (at least one)");
        if (isBlank(form.targetRepo()))
            throw new IllegalArgumentException("targetRepo is required");
        if (isBlank(form.baseBranch()))
            throw new IllegalArgumentException("baseBranch is required");
        if (isBlank(form.branchPrefix()))
            throw new IllegalArgumentException("branchPrefix is required");

        var row = new AgentProfileVersion(name, 1);
        row.setPromptText(form.promptText().trim());
        row.setAllowedToolsJson(serializeToolList(form.allowedTools()));
        row.setTargetRepo(form.targetRepo().trim());
        row.setBaseBranch(form.baseBranch().trim());
        row.setBranchPrefix(form.branchPrefix().trim());
        row.setChangedBy(blankToNull(createdBy));
        row.setChangeNote(blankToNull(
                form.changeNote() != null ? form.changeNote() : "Agent created via UI"));
        row.setRevertToDefault(false);

        repo.save(row);
        log.info("New DB-only agent '{}' created (v1) by {}", name, createdBy);
        return resolveDetail(name);
    }

    private void validateNewName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                "name must be lowercase kebab-case (letters, digits, dashes); got '" + name + "'");
        }
        if (properties.getProfiles().containsKey(name)) {
            throw new IllegalArgumentException(
                "Cannot create '" + name + "': a YAML agent with that name already exists. " +
                "Use the override endpoints to modify it instead.");
        }
        if (repo.findFirstByNameOrderByVersionDesc(name).isPresent()) {
            throw new IllegalArgumentException(
                "Cannot create '" + name + "': a DB agent with that name already exists.");
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private AgentProfile fromDbOnly(String name, AgentProfileVersion v) {
        if (v.isRevertToDefault()) {
            // Defensive — DB-only agents shouldn't end up with a revert row
            // (we block reverts for them in the controller). If they do, the
            // agent effectively has no config — refuse to resolve.
            throw new IllegalStateException(
                "Agent '" + name + "' has no effective config (DB-only with revert row).");
        }
        return new AgentProfile(
            name,
            v.getPromptText(),
            parseToolList(v.getAllowedToolsJson()),
            v.getTargetRepo(),
            v.getBaseBranch(),
            v.getBranchPrefix()
        );
    }

    // ── WRITE paths ────────────────────────────────────────────────────────

    /**
     * Persist a new override version for an agent.
     *
     * <p>For YAML-declared agents, blank/null form fields mean "inherit
     * YAML default for this field" — only non-null fields are stored.
     *
     * <p>For DB-only agents (no YAML baseline), there is nothing to inherit
     * from, so blank/null fields would erase content on the next resolve.
     * We protect against that by carrying forward the previous version's
     * value for any field the form left blank.
     */
    @Transactional
    public Resolved saveOverride(String name, OverrideForm form, String changedBy) {
        var yaml   = properties.getProfiles().get(name);
        var latest = repo.findFirstByNameOrderByVersionDesc(name);

        if (yaml == null && latest.isEmpty()) {
            throw new NoSuchElementException(
                "Unknown agent '" + name + "'. Use POST /api/agents to create a new DB-only agent first.");
        }

        int next = latest.map(v -> v.getVersion() + 1).orElse(1);

        // DB-only agents must carry every field forward; blanks would
        // resolve to null because there's no YAML to fall back to.
        boolean dbOnly = (yaml == null);
        AgentProfileVersion prev = latest.orElse(null);

        var row = new AgentProfileVersion(name, next);
        row.setPromptText(pickField(form.promptText(), dbOnly, prev == null ? null : prev.getPromptText()));
        row.setAllowedToolsJson(pickTools(form.allowedTools(), dbOnly, prev == null ? null : prev.getAllowedToolsJson()));
        row.setTargetRepo(pickField(form.targetRepo(),   dbOnly, prev == null ? null : prev.getTargetRepo()));
        row.setBaseBranch(pickField(form.baseBranch(),   dbOnly, prev == null ? null : prev.getBaseBranch()));
        row.setBranchPrefix(pickField(form.branchPrefix(), dbOnly, prev == null ? null : prev.getBranchPrefix()));
        row.setChangedBy(blankToNull(changedBy));
        row.setChangeNote(blankToNull(form.changeNote()));
        row.setRevertToDefault(false);

        repo.save(row);
        log.info("Agent '{}' override saved as v{} by {} (source={})",
                 name, next, changedBy, dbOnly ? "db-only" : "yaml+override");
        return resolveDetail(name);
    }

    /**
     * Resolve one field for saveOverride:
     * - YAML-backed: blank form → null (inherit YAML default).
     * - DB-only:     blank form → carry previous version's value forward.
     */
    private static String pickField(String formValue, boolean dbOnly, String previousValue) {
        String trimmed = blankToNull(formValue);
        if (trimmed != null) return trimmed;
        return dbOnly ? previousValue : null;
    }

    /** Same shape as {@link #pickField} but for the tool list (stored as JSON). */
    private String pickTools(List<String> formTools, boolean dbOnly, String previousJson) {
        if (formTools != null && !formTools.isEmpty()) {
            return serializeToolList(formTools);
        }
        return dbOnly ? previousJson : null;
    }

    /** Insert a "revert to YAML default" marker row. */
    @Transactional
    public Resolved revertToDefault(String name, String changedBy) {
        var yaml = properties.getProfiles().get(name);
        if (yaml == null) {
            throw new NoSuchElementException("Unknown agent '" + name + "'");
        }

        int next = repo.findFirstByNameOrderByVersionDesc(name)
                .map(v -> v.getVersion() + 1).orElse(1);
        var row = new AgentProfileVersion(name, next);
        row.setRevertToDefault(true);
        row.setChangedBy(blankToNull(changedBy));
        row.setChangeNote("Reverted to YAML default");
        repo.save(row);
        log.info("Agent '{}' reverted to YAML default as v{} by {}", name, next, changedBy);
        return resolveDetail(name);
    }

    /** Restore a previous version by appending a new row that copies its content. */
    @Transactional
    public Resolved revertToVersion(String name, int targetVersion, String changedBy) {
        var src = repo.findByNameAndVersion(name, targetVersion)
                .orElseThrow(() -> new NoSuchElementException(
                    "No version " + targetVersion + " for agent '" + name + "'"));

        int next = repo.findFirstByNameOrderByVersionDesc(name)
                .map(v -> v.getVersion() + 1).orElse(1);

        var row = new AgentProfileVersion(name, next);
        row.setPromptText(src.getPromptText());
        row.setAllowedToolsJson(src.getAllowedToolsJson());
        row.setTargetRepo(src.getTargetRepo());
        row.setBaseBranch(src.getBaseBranch());
        row.setBranchPrefix(src.getBranchPrefix());
        row.setRevertToDefault(src.isRevertToDefault());
        row.setChangedBy(blankToNull(changedBy));
        row.setChangeNote("Restored content from v" + targetVersion);
        repo.save(row);
        log.info("Agent '{}' restored from v{} to v{} by {}", name, targetVersion, next, changedBy);
        return resolveDetail(name);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private AgentProfile merge(String name,
                               AgentProfileProperties.Profile yaml,
                               Optional<AgentProfileVersion> latestOpt) {
        // Always start from YAML baseline:
        String       promptText   = readPromptFile(yaml.getPromptFile());
        List<String> allowedTools = new ArrayList<>(yaml.getAllowedTools());
        String       targetRepo   = yaml.getTargetRepo();
        String       baseBranch   = yaml.getBaseBranch();
        String       branchPrefix = yaml.getBranchPrefix();

        if (latestOpt.isPresent() && !latestOpt.get().isRevertToDefault()) {
            var v = latestOpt.get();
            if (v.getPromptText()      != null) promptText   = v.getPromptText();
            if (v.getAllowedToolsJson()!= null) allowedTools = parseToolList(v.getAllowedToolsJson());
            if (v.getTargetRepo()      != null) targetRepo   = v.getTargetRepo();
            if (v.getBaseBranch()      != null) baseBranch   = v.getBaseBranch();
            if (v.getBranchPrefix()    != null) branchPrefix = v.getBranchPrefix();
        }

        return new AgentProfile(name, promptText, allowedTools,
                                targetRepo, baseBranch, branchPrefix);
    }

    private String readPromptFile(String location) {
        return promptFileCache.computeIfAbsent(location, loc -> {
            Resource r = resourceLoader.getResource(loc);
            try (var in = r.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read prompt file: " + loc, e);
            }
        });
    }

    private static String serializeToolList(List<String> tools) {
        if (tools == null) return null;
        try { return JSON.writeValueAsString(tools); }
        catch (Exception e) { return null; }
    }

    private static List<String> parseToolList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try { return JSON.readValue(json, STRING_LIST); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // ── DTOs ───────────────────────────────────────────────────────────────

    /** Form used by the UI when saving an override. Any null/blank field → inherit YAML. */
    public record OverrideForm(
            String       promptText,
            List<String> allowedTools,
            String       targetRepo,
            String       baseBranch,
            String       branchPrefix,
            String       changeNote
    ) {}

    /** Form used when creating a brand-new DB-only agent. All fields required. */
    public record CreateForm(
            String       promptText,
            List<String> allowedTools,
            String       targetRepo,
            String       baseBranch,
            String       branchPrefix,
            String       changeNote
    ) {}

    /** What the UI gets after a save / resolveDetail call. */
    public record Resolved(AgentProfile effective, VersionSummary latestVersion, boolean yamlDeclared) {}

    /** History row summary. */
    public record VersionSummary(
            int     version,
            Instant changedAt,
            String  changedBy,
            String  changeNote,
            boolean revertToDefault,
            String  promptTextPreview,    // first 200 chars for the history list
            List<String> allowedTools,
            String  targetRepo,
            String  baseBranch,
            String  branchPrefix
    ) {
        static VersionSummary from(AgentProfileVersion v) {
            String preview = null;
            if (v.getPromptText() != null) {
                preview = v.getPromptText().length() > 200
                        ? v.getPromptText().substring(0, 200) + "…"
                        : v.getPromptText();
            }
            return new VersionSummary(
                v.getVersion(),
                v.getChangedAt(),
                v.getChangedBy(),
                v.getChangeNote(),
                v.isRevertToDefault(),
                preview,
                parseToolList(v.getAllowedToolsJson()),
                v.getTargetRepo(),
                v.getBaseBranch(),
                v.getBranchPrefix()
            );
        }
    }
}
