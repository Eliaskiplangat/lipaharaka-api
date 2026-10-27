package com.lipaharaka.api.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceRequest(
        @Schema(description = "Buyer full name", example = "Mary Wambui")
        @NotBlank String buyerName,

        @Schema(description = "Buyer phone number in Kenyan format", example = "+254712345678")
        String buyerPhoneNumber,

        @Schema(description = "Buyer email address", example = "mary@example.com")
        @Email String buyerEmail,

        @Schema(description = "Invoice due date", example = "2026-11-15")
        @NotNull @FutureOrPresent LocalDate dueDate,

        @Schema(description = "Line items for the invoice")
        @NotEmpty @Valid List<InvoiceLineItemRequest> lineItems
) {
}
