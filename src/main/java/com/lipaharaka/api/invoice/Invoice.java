package com.lipaharaka.api.invoice;

import com.lipaharaka.api.business.Business;
import com.lipaharaka.api.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class Invoice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    @Column(name = "invoice_number", nullable = false, length = 40)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "shareable_token", nullable = false, length = 64)
    private String shareableToken;

    private Instant sentAt;
    private Instant paidAt;

    @Version
    private Long version; // optimistic lock: guards against a webhook and an admin edit racing

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "line_order")
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    public BigDecimal outstandingBalance() {
        return totalAmount.subtract(amountPaid);
    }

    public boolean isFullyPaid() {
        return outstandingBalance().signum() <= 0;
    }
}
