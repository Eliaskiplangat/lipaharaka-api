package com.lipaharaka.api.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceRequest(
        @NotBlank String buyerName,
        String buyerPhoneNumber,
        @Email String buyerEmail,
        @NotNull @FutureOrPresent LocalDate dueDate,
        @NotEmpty @Valid List<InvoiceLineItemRequest> lineItems
) {
}
