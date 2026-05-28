package com.example.checkout.transactionmonitoring.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.checkout.transactionmonitoring.service.TransactionFilterService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * REST controller for advanced transaction filtering and search.
 * Supports filtering by status, error code, user ID, date range, amount range, and pagination.
 */
@RestController
@RequestMapping("/transactions")
public class TransactionFilterController {

    private final TransactionFilterService filterService;

    public TransactionFilterController(TransactionFilterService filterService) {
        this.filterService = filterService;
    }

    /**
     * Query transactions with multiple filter dimensions.
     * All filters are combined with logical AND.
     *
     * @param status Transaction status (pending, completed, failed, refunded, partially_refunded, voided, disputed)
     * @param errorCode Error code for failed transactions
     * @param userId User identifier
     * @param email User email address (exact or partial match)
     * @param fromDate Start of date range (ISO 8601)
     * @param toDate End of date range (ISO 8601)
     * @param minAmount Minimum transaction amount
     * @param maxAmount Maximum transaction amount
     * @param page Page number (zero-based)
     * @param size Page size (default 50, max 500)
     * @return Paginated list of matching transactions
     */
    @GetMapping
    public ResponseEntity<Page<TransactionView>> filterTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // Validate page size
        if (size > 500) {
            size = 500;
        }

        TransactionFilterRequest request = new TransactionFilterRequest(
                status, errorCode, userId, email, fromDate, toDate, minAmount, maxAmount
        );

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionView> result = filterService.filter(request, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * Request object encapsulating all filter criteria.
     */
    public record TransactionFilterRequest(
            String status,
            String errorCode,
            String userId,
            String email,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount
    ) {}

    /**
     * View object representing a transaction with PII masked by default.
     */
    public record TransactionView(
            String transactionId,
            String userId,
            String maskedEmail,
            BigDecimal amount,
            String currency,
            String status,
            String paymentMethod,
            String maskedCardNumber,
            String errorCode,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            BigDecimal fees,
            BigDecimal netAmount
    ) {}
}
