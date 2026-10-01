package com.lipaharaka.api.invoice.dto;

import com.lipaharaka.api.invoice.Invoice;
import com.lipaharaka.api.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id, String invoiceNumber, InvoiceStatus status, LocalDate issueDate, LocalDate dueDate,
        BigDecimal subtotal, BigDecimal taxAmount, BigDecimal totalAmount, BigDecimal amountPaid,
        BigDecimal outstandingBalance, String buyerName, String shareableToken,
        List<InvoiceLineItemResponse> lineItems
) {
    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(), invoice.getInvoiceNumber(), invoice.getStatus(),
                invoice.getIssueDate(), invoice.getDueDate(), invoice.getSubtotal(),
                invoice.getTaxAmount(), invoice.getTotalAmount(), invoice.getAmountPaid(),
                invoice.outstandingBalance(), invoice.getBuyer().getName(), invoice.getShareableToken(),
                invoice.getLineItems().stream().map(InvoiceLineItemResponse::from).toList()
        );
    }
}
