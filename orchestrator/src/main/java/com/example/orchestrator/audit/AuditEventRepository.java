package com.example.orchestrator.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {

    /**
     * Newest first. Pageable arg controls how many we pull — we use this to
     * load the most recent {@code MAX_EVENTS} on boot to rehydrate the
     * in-memory cache.
     */
    @Query("SELECT e FROM AuditEvent e ORDER BY e.acceptedAt DESC")
    List<AuditEvent> findRecent(Pageable pageable);

    /** Used at boot to convert stale RUNNING rows (from a previous crashed JVM). */
    List<AuditEvent> findAllByState(AuditEvent.State state);
}
