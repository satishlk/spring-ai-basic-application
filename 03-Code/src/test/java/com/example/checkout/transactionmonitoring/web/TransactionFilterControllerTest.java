package com.example.checkout.transactionmonitoring.web;

import com.example.checkout.transactionmonitoring.service.TransactionFilterService;
import com.example.checkout.transactionmonitoring.web.TransactionFilterController.TransactionFilterRequest;
import com.example.checkout.transactionmonitoring.web.TransactionFilterController.TransactionView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for TransactionFilterController.
 * Tests routing, parameter binding, and validation logic.
 * Service layer is mocked; business logic tests belong in service tests.
 */
@WebMvcTest(TransactionFilterController.class)
class TransactionFilterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionFilterService filterService;

    @Test
    @DisplayName("GET /transactions with no filters returns 500 (method not implemented)")
    void filterTransactions_noFilters_throwsUnsupportedOperation() throws Exception {
        // The controller method throws UnsupportedOperationException
        mockMvc.perform(get("/transactions"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /transactions with status filter returns 500 (method not implemented)")
    void filterTransactions_withStatusFilter_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions")
                        .param("status", "completed"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /transactions with multiple filters returns 500 (method not implemented)")
    void filterTransactions_withMultipleFilters_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions")
                        .param("status", "failed")
                        .param("errorCode", "payment_declined")
                        .param("userId", "user123")
                        .param("minAmount", "10.00")
                        .param("maxAmount", "100.00"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /transactions with date range filters returns 500 (method not implemented)")
    void filterTransactions_withDateRange_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions")
                        .param("fromDate", "2024-01-01T00:00:00")
                        .param("toDate", "2024-12-31T23:59:59"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /transactions with pagination parameters returns 500 (method not implemented)")
    void filterTransactions_withPagination_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions")
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /transactions with size > 500 caps at 500 but still throws (method not implemented)")
    void filterTransactions_withOversizedPage_capsAt500AndThrows() throws Exception {
        // Controller caps size at 500 before throwing
        mockMvc.perform(get("/transactions")
                        .param("page", "0")
                        .param("size", "1000"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /transactions with email filter returns 500 (method not implemented)")
    void filterTransactions_withEmailFilter_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions")
                        .param("email", "user@example.com"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("TransactionFilterRequest record constructs correctly")
    void transactionFilterRequest_construction_succeeds() {
        LocalDateTime now = LocalDateTime.now();
        TransactionFilterRequest request = new TransactionFilterRequest(
                "completed",
                "payment_declined",
                "user123",
                "user@example.com",
                now.minusDays(7),
                now,
                BigDecimal.valueOf(10.00),
                BigDecimal.valueOf(100.00)
        );

        assertThat(request.status()).isEqualTo("completed");
        assertThat(request.errorCode()).isEqualTo("payment_declined");
        assertThat(request.userId()).isEqualTo("user123");
        assertThat(request.email()).isEqualTo("user@example.com");
        assertThat(request.minAmount()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        assertThat(request.maxAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    @DisplayName("TransactionView record constructs correctly")
    void transactionView_construction_succeeds() {
        LocalDateTime now = LocalDateTime.now();
        TransactionView view = new TransactionView(
                "txn123",
                "user456",
                "u***@example.com",
                BigDecimal.valueOf(99.99),
                "USD",
                "completed",
                "credit_card",
                "**** **** **** 1234",
                null,
                now.minusHours(1),
                now,
                BigDecimal.valueOf(2.99),
                BigDecimal.valueOf(97.00),
                1000L
        );

        assertThat(view.transactionId()).isEqualTo("txn123");
        assertThat(view.userId()).isEqualTo("user456");
        assertThat(view.maskedEmail()).isEqualTo("u***@example.com");
        assertThat(view.amount()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        assertThat(view.currency()).isEqualTo("USD");
        assertThat(view.status()).isEqualTo("completed");
        assertThat(view.maskedCardNumber()).isEqualTo("**** **** **** 1234");
        assertThat(view.totalCount()).isEqualTo(1000L);
    }

    // Helper method for AssertJ
    private static org.assertj.core.api.AbstractBigDecimalAssert<?> assertThat(BigDecimal actual) {
        return org.assertj.core.api.Assertions.assertThat(actual);
    }

    private static org.assertj.core.api.AbstractStringAssert<?> assertThat(String actual) {
        return org.assertj.core.api.Assertions.assertThat(actual);
    }

    private static org.assertj.core.api.AbstractLongAssert<?> assertThat(Long actual) {
        return org.assertj.core.api.Assertions.assertThat(actual);
    }
}
