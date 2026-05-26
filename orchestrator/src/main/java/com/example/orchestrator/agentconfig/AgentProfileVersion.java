package com.example.orchestrator.agentconfig;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Append-only override history. One row per save.
 *
 * <p>Conceptually:
 * <pre>
 *   "default" (version 0)  =  YAML + classpath:/prompts/*.md   (implicit, NOT stored)
 *   "v1, v2, …"            =  user edits, each as a new row    (this table)
 *   effective config       =  YAML defaults merged with the LATEST row for this agent
 *                              (any non-null DB field overrides; null DB field = inherit YAML)
 * </pre>
 *
 * <p>Why append-only: every change is auditable + reversible without
 * losing the previous state. Reverting to a prior version = inserting
 * yet another row whose content equals that prior version's content.
 */
@Entity
@Table(
    name = "agent_profile_version",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_agent_name_version",
        columnNames = {"name", "version"}
    ),
    indexes = {
        @Index(name = "ix_agent_name_changed_at", columnList = "name,changedAt DESC"),
    }
)
public class AgentProfileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false)
    private int version;        // 1, 2, 3, … per agent

    @Column(columnDefinition = "CLOB")
    private String promptText;          // null → inherit YAML default

    @Column(columnDefinition = "CLOB")
    private String allowedToolsJson;    // JSON array of strings; null → inherit YAML

    @Column(length = 255)
    private String targetRepo;          // null → inherit YAML

    @Column(length = 64)
    private String baseBranch;

    @Column(length = 64)
    private String branchPrefix;

    @Column(nullable = false)
    private Instant changedAt;

    @Column(length = 64)
    private String changedBy;           // free text — "ui", "api", "satish@host", etc.

    @Column(length = 500)
    private String changeNote;          // optional human-readable summary

    /**
     * If true, this row represents an explicit "revert to YAML default" — all
     * field overrides are cleared. Useful for distinguishing "no overrides set
     * yet" (no rows) from "user explicitly reverted to default" (revert row).
     */
    @Column(nullable = false)
    private boolean revertToDefault;

    protected AgentProfileVersion() {}

    public AgentProfileVersion(String name, int version) {
        this.name = name;
        this.version = version;
        this.changedAt = Instant.now();
    }

    public Long getId()                  { return id; }
    public String getName()              { return name; }
    public int getVersion()              { return version; }
    public String getPromptText()        { return promptText; }
    public String getAllowedToolsJson()  { return allowedToolsJson; }
    public String getTargetRepo()        { return targetRepo; }
    public String getBaseBranch()        { return baseBranch; }
    public String getBranchPrefix()      { return branchPrefix; }
    public Instant getChangedAt()        { return changedAt; }
    public String getChangedBy()         { return changedBy; }
    public String getChangeNote()        { return changeNote; }
    public boolean isRevertToDefault()   { return revertToDefault; }

    public void setPromptText(String v)        { this.promptText = v; }
    public void setAllowedToolsJson(String v)  { this.allowedToolsJson = v; }
    public void setTargetRepo(String v)        { this.targetRepo = v; }
    public void setBaseBranch(String v)        { this.baseBranch = v; }
    public void setBranchPrefix(String v)      { this.branchPrefix = v; }
    public void setChangedBy(String v)         { this.changedBy = v; }
    public void setChangeNote(String v)        { this.changeNote = v; }
    public void setRevertToDefault(boolean v)  { this.revertToDefault = v; }
}
