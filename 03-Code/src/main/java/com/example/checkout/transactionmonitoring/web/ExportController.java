package com.example.checkout.transactionmonitoring.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * REST controller for exporting transaction data to CSV format.
 * Supports the same filter parameters as TransactionFilterController.
 * Uses streaming for large result sets to avoid memory limits.
 */
@RestController
@RequestMapping("/transactions")
public class ExportController {

    // TransactionFilterService will be injected by Spring; implementation in follow-up

    /**
     * Export filtered transactions to CSV format.
     * Accepts the same filter parameters as the main transaction query endpoint.
     * Streams CSV output for large result sets (>10k rows).
     *
     * @param status Transaction status filter
     * @param errorCode Error code filter
     * @param userId User ID filter
     * @param email Email filter
     * @param fromDate Start date filter
     * @param toDate End date filter
     * @param minAmount Minimum amount filter
     * @param maxAmount Maximum amount filter
     * @return Streaming CSV file with Content-Disposition header
     */
    @GetMapping("/export.csv")
    public ResponseEntity<StreamingResponseBody> exportToCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {

        String filename = generateFilename(status, fromDate, toDate);

        StreamingResponseBody stream = outputStream -> {
            // Implementation will write CSV header and rows to outputStream
            // using TransactionFilterService to fetch data in batches
            throw new UnsupportedOperationException("not yet implemented");
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(stream);
    }

    /**
     * Generate a descriptive filename for the CSV export.
     * Includes timestamp and filter summary.
     *
     * @param status Status filter (if present)
     * @param fromDate Start date filter (if present)
     * @param toDate End date filter (if present)
     * @return Filename string (e.g., "transactions_2024-01-15_status-completed.csv")
     */
    private String generateFilename(String status, LocalDateTime fromDate, LocalDateTime toDate) {
        StringBuilder filename = new StringBuilder("transactions_");
        filename.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")));

        if (status != null && !status.isEmpty()) {
            filename.append("_status-").append(status);
        }

        if (fromDate != null || toDate != null) {
            filename.append("_daterange");
        }

        filename.append(".csv");
        return filename.toString();
    }
}
