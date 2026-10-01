package com.lipaharaka.api.invoice.dto;

import com.lipaharaka.api.invoice.InvoiceLineItem;

import java.math.BigDecimal;

public record InvoiceLineItemResponse(String description, BigDecimal quantity, BigDecimal unitPrice,
                                       BigDecimal taxRate, BigDecimal lineTotal) {
    public static InvoiceLineItemResponse from(InvoiceLineItem li) {
        return new InvoiceLineItemResponse(li.getDescription(), li.getQuantity(), li.getUnitPrice(),
                li.getTaxRate(), li.getLineTotal());
    }
}
