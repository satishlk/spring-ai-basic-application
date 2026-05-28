package com.example.checkout.transactionmonitoring.web;

import com.example.checkout.transactionmonitoring.service.TransactionFilterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for ExportController.
 * Tests routing, parameter binding, CSV headers, and streaming setup.
 * Service layer is mocked; business logic tests belong in service tests.
 */
@WebMvcTest(ExportController.class)
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionFilterService filterService;

    @Test
    @DisplayName("GET /transactions/export.csv with no filters returns CSV headers and 500 (streaming body not implemented)")
    void exportTransactions_noFilters_returnsCSVHeaders() throws Exception {
        mockMvc.perform(get("/transactions/export.csv"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Type", "text/csv"));
    }

    @Test
    @DisplayName("GET /transactions/export.csv with status filter returns CSV headers and 500")
    void exportTransactions_withStatusFilter_returnsCSVHeaders() throws Exception {
        mockMvc.perform(get("/transactions/export.csv")
                        .param("status", "completed"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Type", "text/csv"));
    }

    @Test
    @DisplayName("GET /transactions/export.csv with multiple filters returns CSV headers and 500")
    void exportTransactions_withMultipleFilters_returnsCSVHeaders() throws Exception {
        mockMvc.perform(get("/transactions/export.csv")
                        .param("status", "completed")
                        .param("errorCode", "payment_declined")
                        .param("userId", "user123")
                        .param("minAmount", "10.00")
                        .param("maxAmount", "100.00"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Type", "text/csv"));
    }

    @Test
    @DisplayName("GET /transactions/export.csv Content-Disposition includes timestamp")
    void exportTransactions_contentDispositionIncludesTimestamp() throws Exception {
        mockMvc.perform(get("/transactions/export.csv"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("Content-Disposition", 
                        org.hamcrest.Matchers.containsString("transactions_")))
                .andExpect(header().string("Content-Disposition", 
                        org.hamcrest.Matchers.containsString(".csv")));
    }

    @Test
    @DisplayName("GET /transactions/summary with no filters returns 500 (method not implemented)")
    void getSummaryStatistics_noFilters_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions/summary"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /transactions/summary with filters returns 500 (method not implemented)")
    void getSummaryStatistics_withFilters_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions/summary")
                        .param("status", "completed")
                        .param("fromDate", "2024-01-01T00:00:00")
                        .param("toDate", "2024-12-31T23:59:59"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Controller method exportTransactions streaming body throws UnsupportedOperationException")
    void exportTransactions_streamingBodyNotImplemented() {
        ExportController controller = new ExportController();

        // The method returns ResponseEntity with a StreamingResponseBody that throws
        // when executed. We verify the exception is thrown when the stream is consumed.
        // Pinned by Phase 3 — replace with real assertions when method body lands.
        assertThatThrownBy(() -> {
            var response = controller.exportTransactions(
                    null, null, null, null, null, null, null, null);
            // Attempt to consume the streaming body
            response.getBody().writeTo(new java.io.ByteArrayOutputStream());
        })
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not yet implemented");
    }

    @Test
    @DisplayName("Controller method getSummaryStatistics throws UnsupportedOperationException")
    void getSummaryStatistics_methodNotImplemented() {
        ExportController controller = new ExportController();

        // Pinned by Phase 3 — replace with real assertions when method body lands.
        assertThatThrownBy(() -> controller.getSummaryStatistics(
                null, null, null, null, null, null, null, null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("not yet implemented");
    }

    @Test
    @DisplayName("SummaryStatistics record can be instantiated")
    void summaryStatistics_instantiates() {
        var stats = new ExportController.SummaryStatistics(
                100L,
                java.math.BigDecimal.valueOf(5000.00),
                java.math.BigDecimal.valueOf(50.00),
                95.5,
                2.5,
                java.util.Map.of("completed", 95L, "failed", 5L)
        );

        assertThatThrownBy(() -> {
            if (stats.totalCount() == 0) throw new AssertionError();
        }).doesNotThrowAnyException();
    }
}
