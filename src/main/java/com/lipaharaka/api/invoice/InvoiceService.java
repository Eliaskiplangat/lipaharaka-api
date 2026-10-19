package com.lipaharaka.api.invoice;

import com.lipaharaka.api.audit.Audited;
import com.lipaharaka.api.business.Business;
import com.lipaharaka.api.business.BusinessService;
import com.lipaharaka.api.common.exception.ConflictException;
import com.lipaharaka.api.common.exception.ForbiddenException;
import com.lipaharaka.api.common.exception.NotFoundException;
import com.lipaharaka.api.common.exception.ValidationException;
import com.lipaharaka.api.invoice.dto.CreateInvoiceRequest;
import com.lipaharaka.api.invoice.dto.InvoiceLineItemRequest;
import com.lipaharaka.api.notification.EmailGateway;
import com.lipaharaka.api.notification.SmsGateway;
import com.lipaharaka.api.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

@Service
public class InvoiceService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter INVOICE_PREFIX_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final InvoiceRepository invoiceRepository;
    private final BuyerRepository buyerRepository;
    private final BusinessService businessService;
    private final CurrentUserProvider currentUserProvider;
    private final SmsGateway smsGateway;
    private final EmailGateway emailGateway;

    public InvoiceService(InvoiceRepository invoiceRepository, BuyerRepository buyerRepository,
                           BusinessService businessService, CurrentUserProvider currentUserProvider,
                           SmsGateway smsGateway, EmailGateway emailGateway) {
        this.invoiceRepository = invoiceRepository;
        this.buyerRepository = buyerRepository;
        this.businessService = businessService;
        this.currentUserProvider = currentUserProvider;
        this.smsGateway = smsGateway;
        this.emailGateway = emailGateway;
    }

    @Transactional
    @Audited(action = "INVOICE_CREATED", entityType = "Invoice")
    public Invoice create(CreateInvoiceRequest request) {
        Business business = businessService.getForCurrentUser();
        if (!business.isApproved()) {
            throw new ForbiddenException("Business KYC must be approved before invoices can be issued.");
        }

        Buyer buyer = new Buyer();
        buyer.setBusiness(business);
        buyer.setName(request.buyerName());
        buyer.setPhoneNumber(request.buyerPhoneNumber());
        buyer.setEmail(request.buyerEmail());
        buyerRepository.save(buyer);

        Invoice invoice = new Invoice();
        invoice.setBusiness(business);
        invoice.setBuyer(buyer);
        invoice.setInvoiceNumber(generateInvoiceNumber(business.getId()));
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(request.dueDate());
        invoice.setShareableToken(generateShareableToken());

        applyLineItems(invoice, request.lineItems());

        return invoiceRepository.save(invoice);
    }

    private void applyLineItems(Invoice invoice, java.util.List<InvoiceLineItemRequest> requests) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        for (InvoiceLineItemRequest lr : requests) {
            BigDecimal taxRate = lr.taxRate() == null ? BigDecimal.ZERO : lr.taxRate();
            BigDecimal lineSubtotal = lr.quantity().multiply(lr.unitPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTax = lineSubtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineSubtotal.add(lineTax);

            InvoiceLineItem item = new InvoiceLineItem();
            item.setInvoice(invoice);
            item.setDescription(lr.description());
            item.setQuantity(lr.quantity());
            item.setUnitPrice(lr.unitPrice());
            item.setTaxRate(taxRate);
            item.setLineTotal(lineTotal);
            invoice.getLineItems().add(item);

            subtotal = subtotal.add(lineSubtotal);
            taxTotal = taxTotal.add(lineTax);
        }

        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxTotal);
        invoice.setTotalAmount(subtotal.add(taxTotal));
    }

    @Transactional
    @Audited(action = "INVOICE_SENT", entityType = "Invoice")
    public Invoice send(UUID invoiceId) {
        Invoice invoice = getOwnedInvoiceOrThrow(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ConflictException("Only draft invoices can be sent.");
        }
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setSentAt(Instant.now());
        invoiceRepository.save(invoice);

        String link = shareableLinkFor(invoice);
        String message = "Invoice " + invoice.getInvoiceNumber() + " for KES " + invoice.getTotalAmount()
                + " from " + invoice.getBusiness().getName() + " is due " + invoice.getDueDate() + ". Pay here: " + link;

        if (invoice.getBuyer().getPhoneNumber() != null) {
            smsGateway.send(invoice.getBuyer().getPhoneNumber(), message);
        }
        if (invoice.getBuyer().getEmail() != null) {
            emailGateway.send(invoice.getBuyer().getEmail(), "Invoice from " + invoice.getBusiness().getName(), message);
        }
        return invoice;
    }

    public Page<Invoice> listForCurrentBusiness(Pageable pageable) {
        Business business = businessService.getForCurrentUser();
        return invoiceRepository.findByBusinessId(business.getId(), pageable);
    }

    public Invoice getOwnedInvoiceOrThrow(UUID invoiceId) {
        Business business = businessService.getForCurrentUser();
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> NotFoundException.of("Invoice", invoiceId));
        if (!invoice.getBusiness().getId().equals(business.getId())) {
            throw new ForbiddenException("This invoice does not belong to your business.");
        }
        return invoice;
    }

    public Invoice getByShareableTokenOrThrow(String token) {
        return invoiceRepository.findByShareableToken(token)
                .orElseThrow(() -> new NotFoundException("Invoice link is invalid or has expired."));
    }

    @Transactional
    public Invoice applyPayment(UUID invoiceId, BigDecimal amount) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> NotFoundException.of("Invoice", invoiceId));

        if (amount.signum() <= 0) {
            throw new ValidationException("Payment amount must be positive.");
        }
        invoice.setAmountPaid(invoice.getAmountPaid().add(amount));
        if (invoice.isFullyPaid()) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(Instant.now());
        }
        return invoiceRepository.save(invoice);
    }

    public String shareableLinkFor(Invoice invoice) {
        return "https://pay.lipaharaka.co.ke/i/" + invoice.getShareableToken();
    }

    private String generateInvoiceNumber(UUID businessId) {
        String prefix = "INV-" + LocalDate.now().format(INVOICE_PREFIX_FORMAT) + "-";
        // Sequential-looking but collision-checked rather than relying on a shared counter,
        // which keeps invoice creation lock-free under concurrent load from the same business.
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = prefix + String.format("%04d", RANDOM.nextInt(10_000));
            if (!invoiceRepository.existsByBusinessIdAndInvoiceNumber(businessId, candidate)) {
                return candidate;
            }
        }
        throw new ConflictException("Could not allocate a unique invoice number, please retry.");
    }

    private String generateShareableToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
