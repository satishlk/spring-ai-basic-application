package com.example.checkout.transactionmonitoring.web;

import com.example.checkout.transactionmonitoring.service.TransactionFilterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for ExportController.
 * Tests routing, CSV header generation, and summary statistics endpoint.
 * Export streaming logic is not yet implemented (throws UnsupportedOperationException).
 */
@WebMvcTest(ExportController.class)
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionFilterService filterService;

    @Test
    @DisplayName("GET /transactions/export.csv with no filters returns CSV headers and 500 (not yet implemented)")
    void exportTransactions_noFilters_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions/export.csv"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("transactions_")))
                .andExpect(header().string("Content-Type", "text/csv"));
    }

    @Test
    @DisplayName("GET /transactions/export.csv with status filter includes filter in filename")
    void exportTransactions_withStatusFilter_includesFilterInFilename() throws Exception {
        mockMvc.perform(get("/transactions/export.csv")
                        .param("status", "completed"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("status-completed")));
    }

    @Test
    @DisplayName("GET /transactions/export.csv with error code filter includes filter in filename")
    void exportTransactions_withErrorCodeFilter_includesFilterInFilename() throws Exception {
        mockMvc.perform(get("/transactions/export.csv")
                        .param("errorCode", "payment_declined"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("error-payment_declined")));
    }

    @Test
    @DisplayName("GET /transactions/export.csv with user ID filter includes filter in filename")
    void exportTransactions_withUserIdFilter_includesFilterInFilename() throws Exception {
        mockMvc.perform(get("/transactions/export.csv")
                        .param("userId", "user123"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("user-user123")));
    }

    @Test
    @DisplayName("GET /transactions/export.csv with multiple filters includes all in filename")
    void exportTransactions_withMultipleFilters_includesAllInFilename() throws Exception {
        mockMvc.perform(get("/transactions/export.csv")
                        .param("status", "failed")
                        .param("errorCode", "insufficient_funds")
                        .param("userId", "user456"))
                .andExpect(status().is5xxServerError())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("status-failed")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("error-insufficient_funds")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("user-user456")));
    }

    @Test
    @DisplayName("GET /transactions/export.csv with date range filters returns 500 (not yet implemented)")
    void exportTransactions_withDateRange_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions/export.csv")
                        .param("fromDate", "2024-01-01T00:00:00")
                        .param("toDate", "2024-12-31T23:59:59"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /transactions/export.csv with amount range filters returns 500 (not yet implemented)")
    void exportTransactions_withAmountRange_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions/export.csv")
                        .param("minAmount", "10.00")
                        .param("maxAmount", "100.00"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /transactions/summary with no filters returns 500 (not yet implemented)")
    void getSummaryStatistics_noFilters_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions/summary"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /transactions/summary with status filter returns 500 (not yet implemented)")
    void getSummaryStatistics_withStatusFilter_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions/summary")
                        .param("status", "completed"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /transactions/summary with multiple filters returns 500 (not yet implemented)")
    void getSummaryStatistics_withMultipleFilters_throwsUnsupportedOperation() throws Exception {
        mockMvc.perform(get("/transactions/summary")
                        .param("status", "failed")
                        .param("errorCode", "payment_declined")
                        .param("fromDate", "2024-01-01T00:00:00")
                        .param("toDate", "2024-12-31T23:59:59"))
                .andExpect(status().is5xxServerError());
    }

    // Pinned by Phase 3 — when export streaming and summary statistics are implemented,
    // replace the above tests with real assertions that verify:
    // - Correct CSV structure and content
    // - Correct PII masking in exported data
    // - Correct streaming behavior for large result sets
    // - Correct summary statistics calculations (total count, amounts, rates, breakdown)
}
