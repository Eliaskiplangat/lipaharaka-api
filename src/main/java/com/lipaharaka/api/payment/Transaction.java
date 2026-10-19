package com.lipaharaka.api.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID invoiceId;
    private UUID advanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "counterparty_phone", length = 20)
    private String counterpartyPhone;

    @Column(name = "mpesa_receipt_number", length = 40)
    private String mpesaReceiptNumber;

    @Column(name = "mpesa_checkout_request_id", length = 60)
    private String mpesaCheckoutRequestId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_callback_payload")
    private Map<String, Object> rawCallbackPayload;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant settledAt;
}
