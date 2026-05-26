package com.example.orchestrator.agentconfig;

import com.example.orchestrator.config.AgentProfileProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AgentProfileService}.
 *
 * <p>The most important coverage here is the DB-only agent lifecycle:
 * create → saveOverride → resolve. This used to throw
 * {@code NoSuchElementException("Unknown agent ...")} from
 * {@code saveOverride} because that method required a YAML baseline.
 * That regression cost a production debugging session — these tests
 * pin it down.
 *
 * <p>{@code @DataJpaTest} gives us a real {@link AgentProfileVersionRepository}
 * on H2; we construct the {@link AgentProfileProperties} and
 * {@link ResourceLoader} by hand so the test stays focused on
 * service behaviour rather than Spring wiring.
 */
@DataJpaTest
@Import(AgentProfileServiceTest.YamlPromptFileTestConfig.class) // none — placeholder to keep imports stable
class AgentProfileServiceTest {

    private static final String YAML_AGENT = "test-yaml-agent";
    private static final String DB_AGENT   = "test-db-agent";

    @Autowired
    private AgentProfileVersionRepository repo;

    private AgentProfileService service;

    @BeforeEach
    void setUp() {
        var properties = new AgentProfileProperties();

        var yamlProfile = new AgentProfileProperties.Profile();
        yamlProfile.setPromptFile("classpath:/prompts/test-yaml-agent.md");
        yamlProfile.setAllowedTools(List.of("yaml_tool_a", "yaml_tool_b"));
        yamlProfile.setTargetRepo("yaml-owner/yaml-repo");
        yamlProfile.setBaseBranch("main");
        yamlProfile.setBranchPrefix("yaml/");
        properties.getProfiles().put(YAML_AGENT, yamlProfile);

        ResourceLoader loader = new DefaultResourceLoader();
        service = new AgentProfileService(properties, repo, loader);
    }

    // ── DB-only agent lifecycle (the regression that motivated these tests) ──

    @Test
    @DisplayName("create() then saveOverride() works for a DB-only agent")
    void saveOverride_acceptsDbOnlyAgent() {
        // v1 via create()
        service.create(DB_AGENT, new AgentProfileService.CreateForm(
                "DB-only prompt v1",
                List.of("db_tool_x", "db_tool_y"),
                "db-owner/db-repo", "main", "db/",
                "initial"
        ), "tester");

        // v2 via saveOverride() — this used to throw NoSuchElementException.
        var afterOverride = service.saveOverride(DB_AGENT, new AgentProfileService.OverrideForm(
                "DB-only prompt v2",
                List.of("db_tool_x", "db_tool_y", "db_tool_z"),
                null, null, null,
                "tweak prompt and add a tool"
        ), "tester");

        assertThat(afterOverride.latestVersion().version()).isEqualTo(2);
        assertThat(afterOverride.effective().systemPrompt()).isEqualTo("DB-only prompt v2");
        assertThat(afterOverride.effective().allowedTools())
                .containsExactly("db_tool_x", "db_tool_y", "db_tool_z");
        assertThat(afterOverride.yamlDeclared()).isFalse();
    }

    @Test
    @DisplayName("DB-only saveOverride() with blank fields carries previous values forward")
    void saveOverride_dbOnly_blanksCarryForwardPreviousValues() {
        service.create(DB_AGENT, new AgentProfileService.CreateForm(
                "Prompt v1",
                List.of("tool_a"),
                "owner/repo", "main", "ai/",
                "initial"
        ), "tester");

        // Only change the prompt; leave everything else blank/null.
        var resolved = service.saveOverride(DB_AGENT, new AgentProfileService.OverrideForm(
                "Prompt v2",
                null,   // blank tool list
                null,   // blank repo
                null,   // blank base branch
                null,   // blank branch prefix
                "only changing prompt"
        ), "tester");

        var p = resolved.effective();
        assertThat(p.systemPrompt()).isEqualTo("Prompt v2");
        // For a DB-only agent, blanks MUST NOT erase prior values — there is
        // no YAML to fall back to.
        assertThat(p.allowedTools()).containsExactly("tool_a");
        assertThat(p.targetRepo()).isEqualTo("owner/repo");
        assertThat(p.baseBranch()).isEqualTo("main");
        assertThat(p.branchPrefix()).isEqualTo("ai/");
    }

    @Test
    @DisplayName("saveOverride() on an unknown agent gives a helpful error")
    void saveOverride_unknownAgent_throws() {
        assertThatThrownBy(() -> service.saveOverride("never-existed",
                new AgentProfileService.OverrideForm("x", List.of("t"), "o/r", "main", "p/", "note"),
                "tester"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("never-existed")
            .hasMessageContaining("POST /api/agents"); // points users at the create endpoint
    }

    // ── YAML agent: blank-means-inherit semantics must NOT regress ──

    @Test
    @DisplayName("YAML-backed saveOverride() with blank fields stores null and inherits YAML")
    void saveOverride_yamlBacked_blanksInheritFromYaml() {
        // Override only the prompt; leave tools, repo, branches blank.
        var resolved = service.saveOverride(YAML_AGENT, new AgentProfileService.OverrideForm(
                "Overridden prompt only",
                null, null, null, null,
                "prompt-only override"
        ), "tester");

        var p = resolved.effective();
        assertThat(p.systemPrompt()).isEqualTo("Overridden prompt only");
        // Blanks inherit from YAML defaults.
        assertThat(p.allowedTools()).containsExactly("yaml_tool_a", "yaml_tool_b");
        assertThat(p.targetRepo()).isEqualTo("yaml-owner/yaml-repo");
        assertThat(p.baseBranch()).isEqualTo("main");
        assertThat(p.branchPrefix()).isEqualTo("yaml/");
    }

    @Test
    @DisplayName("resolve() returns YAML defaults when there is no DB row")
    void resolve_yamlOnly_returnsYamlDefaults() {
        var p = service.resolve(YAML_AGENT);
        assertThat(p.systemPrompt()).contains("YAML baseline prompt");
        assertThat(p.allowedTools()).containsExactly("yaml_tool_a", "yaml_tool_b");
    }

    @Test
    @DisplayName("listNames() unions YAML and DB-only agents (YAML first)")
    void listNames_unionsYamlAndDbOnly() {
        service.create(DB_AGENT, new AgentProfileService.CreateForm(
                "p", List.of("t"), "o/r", "main", "ai/", "init"), "tester");

        List<String> names = service.listNames();
        assertThat(names).containsExactly(YAML_AGENT, DB_AGENT);
    }

    /** Placeholder — present so {@code @Import} compiles even with no extra beans needed. */
    static class YamlPromptFileTestConfig {}
}
