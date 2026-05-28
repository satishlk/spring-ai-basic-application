package com.example.checkout.transactionmonitoring.web;

import com.example.checkout.transactionmonitoring.web.AdminActionController.RefundRequest;
import com.example.checkout.transactionmonitoring.web.AdminActionController.RefundType;
import com.example.checkout.transactionmonitoring.web.AdminActionController.DisputeRequest;
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
 * Tests routing, parameter binding, validation, and error handling.
 * Service layer is mocked; business logic tests belong in service tests.
 */
@WebMvcTest(AdminActionController.class)
class AdminActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /transactions/{id}/refund with valid FULL refund returns 500 (method not implemented)")
    void refund_fullRefund_throwsUnsupportedOperation() throws Exception {
        RefundRequest request = new RefundRequest(RefundType.FULL, null);

        mockMvc.perform(post("/transactions/txn123/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("POST /transactions/{id}/refund with valid PARTIAL refund returns 500 (method not implemented)")
    void refund_partialRefund_throwsUnsupportedOperation() throws Exception {
        RefundRequest request = new RefundRequest(RefundType.PARTIAL, BigDecimal.valueOf(25.00));

        mockMvc.perform(post("/transactions/txn123/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("POST /transactions/{id}/refund with null type returns 400 (validation works)")
    void refund_nullType_returnsBadRequest() throws Exception {
        RefundRequest request = new RefundRequest(null, null);

        mockMvc.perform(post("/transactions/txn123/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transactions/{id}/refund with PARTIAL type but null amount returns 400 (validation works)")
    void refund_partialWithoutAmount_returnsBadRequest() throws Exception {
        RefundRequest request = new RefundRequest(RefundType.PARTIAL, null);

        mockMvc.perform(post("/transactions/txn123/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transactions/{id}/void returns 500 (method not implemented)")
    void voidTransaction_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(post("/transactions/txn123/void"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("POST /transactions/{id}/dispute with valid reason returns 500 (method not implemented)")
    void dispute_validReason_throwsUnsupportedOperation() throws Exception {
        DisputeRequest request = new DisputeRequest("Customer claims unauthorized charge");

        mockMvc.perform(post("/transactions/txn123/dispute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("POST /transactions/{id}/dispute with null reason returns 400 (validation works)")
    void dispute_nullReason_returnsBadRequest() throws Exception {
        DisputeRequest request = new DisputeRequest(null);

        mockMvc.perform(post("/transactions/txn123/dispute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transactions/{id}/dispute with blank reason returns 400 (validation works)")
    void dispute_blankReason_returnsBadRequest() throws Exception {
        DisputeRequest request = new DisputeRequest("   ");

        mockMvc.perform(post("/transactions/txn123/dispute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Controller method refund throws UnsupportedOperationException")
    void refund_methodNotImplemented() {
        AdminActionController controller = new AdminActionController();
        RefundRequest request = new RefundRequest(RefundType.FULL, null);

        // Pinned by Phase 3 — replace with real assertions when method body lands.
        assertThatThrownBy(() -> controller.refund("txn123", request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not yet implemented");
    }

    @Test
    @DisplayName("Controller method voidTransaction throws UnsupportedOperationException")
    void voidTransaction_methodNotImplemented() {
        AdminActionController controller = new AdminActionController();

        // Pinned by Phase 3 — replace with real assertions when method body lands.
        assertThatThrownBy(() -> controller.voidTransaction("txn123"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not yet implemented");
    }

    @Test
    @DisplayName("Controller method dispute throws UnsupportedOperationException")
    void dispute_methodNotImplemented() {
        AdminActionController controller = new AdminActionController();
        DisputeRequest request = new DisputeRequest("Dispute reason");

        // Pinned by Phase 3 — replace with real assertions when method body lands.
        assertThatThrownBy(() -> controller.dispute("txn123", request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not yet implemented");
    }

    @Test
    @DisplayName("RefundRequest record can be instantiated")
    void refundRequest_instantiates() {
        RefundRequest request = new RefundRequest(RefundType.PARTIAL, BigDecimal.valueOf(10.00));
        
        assertThatThrownBy(() -> {
            if (request.type() == null) throw new AssertionError();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DisputeRequest record can be instantiated")
    void disputeRequest_instantiates() {
        DisputeRequest request = new DisputeRequest("Reason text");
        
        assertThatThrownBy(() -> {
            if (request.reason() == null) throw new AssertionError();
        }).doesNotThrowAnyException();
    }
}
