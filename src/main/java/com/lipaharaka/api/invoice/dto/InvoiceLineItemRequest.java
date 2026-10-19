package com.lipaharaka.api.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InvoiceLineItemRequest(
        @NotBlank String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.00") BigDecimal unitPrice,
        @DecimalMin(value = "0.0") BigDecimal taxRate // fraction, e.g. 0.16 for 16% VAT; defaults to 0
) {
}
