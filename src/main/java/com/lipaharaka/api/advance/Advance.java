package com.lipaharaka.api.advance;

import com.lipaharaka.api.business.Business;
import com.lipaharaka.api.invoice.Invoice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "advances")
@Getter
@Setter
@NoArgsConstructor
public class Advance {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lender_partner_id")
    private LenderPartner lenderPartner;

    @Column(name = "requested_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "fee_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "net_payout", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPayout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdvanceStatus status = AdvanceStatus.REQUESTED;

    @Column(nullable = false)
    private Instant requestedAt = Instant.now();

    private Instant decisionedAt;
    private Instant disbursedAt;
    private Instant repaidAt;

    @Version
    private Long version; // guards against the repayment offset (FR-5.4) racing an admin decision
}
