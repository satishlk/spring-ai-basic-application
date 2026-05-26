package com.example.orchestrator.agentconfig;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AgentProfileVersionRepository extends JpaRepository<AgentProfileVersion, Long> {

    /** Latest saved version for an agent, or empty if no overrides yet. */
    Optional<AgentProfileVersion> findFirstByNameOrderByVersionDesc(String name);

    /** Full history, newest first. */
    List<AgentProfileVersion> findByNameOrderByVersionDesc(String name);

    /** Get a specific version (e.g. for the "revert to v3" feature). */
    Optional<AgentProfileVersion> findByNameAndVersion(String name, int version);

    /** All distinct agent names that have at least one DB row — used to surface DB-only agents. */
    @Query("SELECT DISTINCT v.name FROM AgentProfileVersion v")
    List<String> findDistinctNames();
}
