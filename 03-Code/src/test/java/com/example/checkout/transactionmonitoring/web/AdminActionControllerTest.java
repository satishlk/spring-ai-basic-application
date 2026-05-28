package com.example.checkout.transactionmonitoring.web;

import com.example.checkout.transactionmonitoring.web.AdminActionController.DisputeRequest;
import com.example.checkout.transactionmonitoring.web.AdminActionController.RefundRequest;
import com.example.checkout.transactionmonitoring.web.AdminActionController.RefundType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for AdminActionController.
 * Tests routing, request validation, and error handling.
 * Service-level logic is not yet implemented (throws UnsupportedOperationException).
 */
@WebMvcTest(AdminActionController.class)
class AdminActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /transactions/{id}/refund with FULL refund returns 500 (not yet implemented)")
    void refund_fullRefund_throwsUnsupportedOperation() throws Exception {
        RefundRequest request = new RefundRequest(RefundType.FULL, null);

        mockMvc.perform(post("/transactions/txn123/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("POST /transactions/{id}/refund with PARTIAL refund returns 500 (not yet implemented)")
    void refund_partialRefund_throwsUnsupportedOperation() throws Exception {
        RefundRequest request = new RefundRequest(RefundType.PARTIAL, BigDecimal.valueOf(25.00));

        mockMvc.perform(post("/transactions/txn123/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("POST /transactions/{id}/refund with null type returns 400 (validation)")
    void refund_nullType_returnsBadRequest() throws Exception {
        RefundRequest request = new RefundRequest(null, null);

        mockMvc.perform(post("/transactions/txn123/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transactions/{id}/refund with PARTIAL type but null amount returns 400 (validation)")
    void refund_partialWithoutAmount_returnsBadRequest() throws Exception {
        RefundRequest request = new RefundRequest(RefundType.PARTIAL, null);

        mockMvc.perform(post("/transactions/txn123/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transactions/{id}/void returns 500 (not yet implemented)")
    void voidTransaction_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(post("/transactions/txn123/void")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("POST /transactions/{id}/dispute with valid reason returns 500 (not yet implemented)")
    void dispute_withValidReason_throwsUnsupportedOperation() throws Exception {
        DisputeRequest request = new DisputeRequest("Customer claims unauthorized charge");

        mockMvc.perform(post("/transactions/txn123/dispute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("POST /transactions/{id}/dispute with null reason returns 400 (validation)")
    void dispute_nullReason_returnsBadRequest() throws Exception {
        DisputeRequest request = new DisputeRequest(null);

        mockMvc.perform(post("/transactions/txn123/dispute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transactions/{id}/dispute with blank reason returns 400 (validation)")
    void dispute_blankReason_returnsBadRequest() throws Exception {
        DisputeRequest request = new DisputeRequest("   ");

        mockMvc.perform(post("/transactions/txn123/dispute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RefundRequest record can be instantiated")
    void refundRequest_canBeCreated() {
        RefundRequest request = new RefundRequest(RefundType.PARTIAL, BigDecimal.valueOf(10.00));

        assertThatThrownBy(() -> {
            if (request == null) throw new IllegalStateException();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DisputeRequest record can be instantiated")
    void disputeRequest_canBeCreated() {
        DisputeRequest request = new DisputeRequest("Fraud suspected");

        assertThatThrownBy(() -> {
            if (request == null) throw new IllegalStateException();
        }).doesNotThrowAnyException();
    }

    // Pinned by Phase 3 — when refund/void/dispute services are implemented, replace
    // the above tests with real assertions that verify:
    // - Correct HTTP 200 response for successful actions
    // - Correct response body structure (RefundResponse, VoidResponse, DisputeResponse)
    // - Correct audit log entries created
    // - Correct idempotency behavior
}
