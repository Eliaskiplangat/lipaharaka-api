package com.lipaharaka.api.advance;

import com.lipaharaka.api.advance.dto.AdvanceQuoteResponse;
import com.lipaharaka.api.advance.lender.LenderApiClient;
import com.lipaharaka.api.advance.lender.LenderDecision;
import com.lipaharaka.api.audit.Audited;
import com.lipaharaka.api.business.Business;
import com.lipaharaka.api.common.exception.ConflictException;
import com.lipaharaka.api.common.exception.ForbiddenException;
import com.lipaharaka.api.common.exception.NotFoundException;
import com.lipaharaka.api.common.exception.ValidationException;
import com.lipaharaka.api.invoice.Invoice;
import com.lipaharaka.api.invoice.InvoiceService;
import com.lipaharaka.api.invoice.InvoiceStatus;
import com.lipaharaka.api.payment.Transaction;
import com.lipaharaka.api.payment.TransactionRepository;
import com.lipaharaka.api.payment.TransactionStatus;
import com.lipaharaka.api.payment.TransactionType;
import com.lipaharaka.api.payment.mpesa.B2CResult;
import com.lipaharaka.api.payment.mpesa.MpesaClient;
import com.lipaharaka.api.risk.RiskScore;
import com.lipaharaka.api.risk.RiskScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;


@Service
@EnableConfigurationProperties(AdvanceProperties.class)
public class AdvanceService {

    private static final Logger log = LoggerFactory.getLogger(AdvanceService.class);

    private final AdvanceRepository advanceRepository;
    private final LenderPartnerRepository lenderPartnerRepository;
    private final InvoiceService invoiceService;
    private final RiskScoringService riskScoringService;
    private final LenderApiClient lenderApiClient;
    private final MpesaClient mpesaClient;
    private final TransactionRepository transactionRepository;
    private final AdvanceProperties properties;

    public AdvanceService(AdvanceRepository advanceRepository, LenderPartnerRepository lenderPartnerRepository,
                           InvoiceService invoiceService, RiskScoringService riskScoringService,
                           LenderApiClient lenderApiClient, MpesaClient mpesaClient,
                           TransactionRepository transactionRepository, AdvanceProperties properties) {
        this.advanceRepository = advanceRepository;
        this.lenderPartnerRepository = lenderPartnerRepository;
        this.invoiceService = invoiceService;
        this.riskScoringService = riskScoringService;
        this.lenderApiClient = lenderApiClient;
        this.mpesaClient = mpesaClient;
        this.transactionRepository = transactionRepository;
        this.properties = properties;
    }

    public AdvanceQuoteResponse quote(UUID invoiceId) {
        Invoice invoice = invoiceService.getOwnedInvoiceOrThrow(invoiceId);
        validateInvoiceIsEligible(invoice);

        BigDecimal maxAdvance = invoice.outstandingBalance().multiply(properties.maxAdvancePercentage())
                .setScale(2, RoundingMode.DOWN);
        BigDecimal fee = maxAdvance.multiply(properties.feePercentage()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netPayout = maxAdvance.subtract(fee);

        return new AdvanceQuoteResponse(maxAdvance, fee, netPayout, properties.feePercentage());
    }

    @Transactional
    @Audited(action = "ADVANCE_REQUESTED", entityType = "Advance")
    public Advance requestAdvance(UUID invoiceId) {
        Invoice invoice = invoiceService.getOwnedInvoiceOrThrow(invoiceId);
        validateInvoiceIsEligible(invoice);

        if (advanceRepository.findByInvoiceId(invoiceId).isPresent()) {
            throw new ConflictException("An advance has already been requested against this invoice.");
        }

        Business business = invoice.getBusiness();
        RiskScore riskScore = riskScoringService.getLatestOrCompute(business);

        AdvanceQuoteResponse quote = quote(invoiceId);

        Advance advance = new Advance();
        advance.setInvoice(invoice);
        advance.setBusiness(business);
        advance.setRequestedAmount(quote.requestedAmount());
        advance.setFeeAmount(quote.feeAmount());
        advance.setNetPayout(quote.netPayout());
        advance = advanceRepository.save(advance);

        try {
            LenderDecision decision = lenderApiClient.submitAdvanceRequest(
                    business.getId(), invoice.getId(), quote.requestedAmount(), riskScore.getScore());
            advance.setStatus(decision.approved() ? AdvanceStatus.APPROVED : AdvanceStatus.REJECTED);
            advance.setDecisionedAt(Instant.now());
            advanceRepository.save(advance);

            if (decision.approved()) {
                disburse(advance);
            }
        } catch (Exception ex) {
            log.error("Lender routing failed for advance {}", advance.getId(), ex);
            throw ex;
        }

        return advance;
    }

    private void disburse(Advance advance) {
        Business business = advance.getBusiness();
        String payoutPhone = business.getMpesaShortcode(); // production: dedicated SME payout MSISDN, not the till number

        B2CResult result = mpesaClient.disburseToBusiness(payoutPhone, advance.getNetPayout(),
                "LipaHaraka advance for invoice " + advance.getInvoice().getInvoiceNumber());

        Transaction txn = new Transaction();
        txn.setAdvanceId(advance.getId());
        txn.setType(TransactionType.ADVANCE_DISBURSEMENT);
        txn.setStatus(TransactionStatus.PENDING); // confirmed asynchronously by the B2C result callback
        txn.setAmount(advance.getNetPayout());
        txn.setCounterpartyPhone(payoutPhone);
        transactionRepository.save(txn);

        advance.setStatus(AdvanceStatus.DISBURSED);
        advance.setDisbursedAt(Instant.now());
        advanceRepository.save(advance);

        log.info("Disbursement initiated for advance {} conversationId={}", advance.getId(), result.conversationId());
    }


    @Transactional
    @Audited(action = "ADVANCE_REPAID", entityType = "Advance")
    public void offsetAgainstInvoicePayment(UUID invoiceId) {
        advanceRepository.findDisbursedAdvanceForInvoiceForUpdate(invoiceId).ifPresent(advance -> {
            advance.setStatus(AdvanceStatus.REPAID);
            advance.setRepaidAt(Instant.now());
            advanceRepository.save(advance);

            Transaction repayment = new Transaction();
            repayment.setAdvanceId(advance.getId());
            repayment.setInvoiceId(invoiceId);
            repayment.setType(TransactionType.ADVANCE_REPAYMENT);
            repayment.setStatus(TransactionStatus.SUCCESS);
            repayment.setAmount(advance.getRequestedAmount());
            repayment.setSettledAt(Instant.now());
            transactionRepository.save(repayment);
        });
    }

    private void validateInvoiceIsEligible(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.SENT && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new ValidationException("Only sent or overdue invoices are eligible for an advance.");
        }
        if (!invoice.getBusiness().isApproved()) {
            throw new ForbiddenException("Business KYC must be approved before requesting an advance.");
        }
    }

    public Advance getOwnedOrThrow(UUID advanceId, UUID businessId) {
        Advance advance = advanceRepository.findById(advanceId).orElseThrow(() -> NotFoundException.of("Advance", advanceId));
        if (!advance.getBusiness().getId().equals(businessId)) {
            throw new ForbiddenException("This advance does not belong to your business.");
        }
        return advance;
    }

    public org.springframework.data.domain.Page<Advance> listForBusiness(UUID businessId, org.springframework.data.domain.Pageable pageable) {
        return advanceRepository.findByBusinessId(businessId, pageable);
    }
}
