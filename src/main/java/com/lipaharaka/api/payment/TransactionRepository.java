package com.lipaharaka.api.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByMpesaReceiptNumber(String receiptNumber);
    boolean existsByMpesaReceiptNumber(String receiptNumber);
    Optional<Transaction> findByMpesaCheckoutRequestIdAndStatus(String checkoutRequestId, TransactionStatus status);
}
