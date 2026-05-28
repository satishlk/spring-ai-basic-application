package com.example.checkout.transactionmonitoring.service;

import com.example.checkout.transactionmonitoring.web.TransactionFilterController.TransactionFilterRequest;
import com.example.checkout.transactionmonitoring.web.TransactionFilterController.TransactionView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Service for filtering and searching transactions based on multiple criteria.
 * Implements the business logic for advanced filtering as defined in PRD section 1.
 * 
 * Responsibilities:
 * - Apply filter criteria with logical AND combination
 * - Handle pagination and result set limits
 * - Apply PII masking to all returned data
 * - Ensure p99 latency < 500ms for result sets up to 10k transactions
 */
@Service
public interface TransactionFilterService {

    /**
     * Filter transactions by the provided criteria and return paginated results.
     * All filter fields are optional and combined with logical AND.
     * Results include PII masking by default (card numbers, emails).
     *
     * @param request Filter criteria (all fields optional)
     * @param pageable Pagination parameters (page number, size, sort)
     * @return Paginated list of transactions matching all criteria, with total count
     * @throws IllegalArgumentException if filter values are invalid
     */
    Page<TransactionView> filter(TransactionFilterRequest request, Pageable pageable);
}
