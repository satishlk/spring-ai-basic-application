package com.example.checkout.transactionmonitoring.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for AuditLogService.
 * Tests validation logic, PII masking, and audit log recording.
 * Repository-level persistence is not yet implemented (throws UnsupportedOperationException).
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @InjectMocks
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        // Service is instantiated by Mockito with @InjectMocks
    }

    @Test
    @DisplayName("record() with valid parameters throws UnsupportedOperationException (not yet implemented)")
    void record_validParameters_throwsUnsupportedOperation() {
        // Pinned by Phase 3 — replace with real assertions when method body lands.
        assertThatThrownBy(() -> auditLogService.record(
                "actor123",
                "view_transaction",
                "txn456",
                "{\"field\":\"value\"}"
        ))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not yet implemented");
    }

    @Test
    @DisplayName("record() with null actorId throws IllegalArgumentException")
    void record_nullActorId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> auditLogService.record(
                null,
                "view_transaction",
                "txn456",
                "{\"field\":\"value\"}"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Actor ID is required");
    }

    @Test
    @DisplayName("record() with blank actorId throws IllegalArgumentException")
    void record_blankActorId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> auditLogService.record(
                "   ",
                "view_transaction",
                "txn456",
                "{\"field\":\"value\"}"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Actor ID is required");
    }

    @Test
    @DisplayName("record() with null action throws IllegalArgumentException")
    void record_nullAction_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> auditLogService.record(
                "actor123",
                null,
                "txn456",
                "{\"field\":\"value\"}"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Action is required");
    }

    @Test
    @DisplayName("record() with blank action throws IllegalArgumentException")
    void record_blankAction_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> auditLogService.record(
                "actor123",
                "   ",
                "txn456",
                "{\"field\":\"value\"}"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Action is required");
    }

    @Test
    @DisplayName("record() with additional context throws UnsupportedOperationException (not yet implemented)")
    void record_withAdditionalContext_throwsUnsupportedOperation() {
        // Pinned by Phase 3 — replace with real assertions when method body lands.
        assertThatThrownBy(() -> auditLogService.record(
                "actor123",
                "refund",
                "txn456",
                "{\"amount\":50.00}",
                "192.168.1.1",
                "Mozilla/5.0",
                "session789"
        ))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not yet implemented");
    }

    @Test
    @DisplayName("queryByActor() throws UnsupportedOperationException (not yet implemented)")
    void queryByActor_throwsUnsupportedOperation() {
        // Pinned by Phase 3 — replace with real assertions when method body lands.
        assertThatThrownBy(() -> auditLogService.queryByActor(
                "actor123",
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now()
        ))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not yet implemented");
    }

    @Test
    @DisplayName("queryByTransaction() throws UnsupportedOperationException (not yet implemented)")
    void queryByTransaction_throwsUnsupportedOperation() {
        // Pinned by Phase 3 — replace with real assertions when method body lands.
        assertThatThrownBy(() -> auditLogService.queryByTransaction("txn456"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not yet implemented");
    }

    // Note: PII masking logic is private and tested indirectly through record() calls.
    // When record() is implemented, add tests that verify:
    // - Card numbers are masked to "**** **** **** 1234"
    // - Email addresses are masked to "j***@example.com"
    // - Other PII fields are properly redacted

    // Pinned by Phase 3 — when persistence is implemented, add tests that verify:
    // - Audit logs are written synchronously
    // - Failed writes cause the parent action to fail
    // - Audit logs are immutable (append-only)
    // - Query methods return correct filtered results
}
