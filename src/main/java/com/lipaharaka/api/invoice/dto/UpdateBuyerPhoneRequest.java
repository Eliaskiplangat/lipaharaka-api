package com.lipaharaka.api.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBuyerPhoneRequest(
        @NotBlank(message = "Buyer phone number is required.")
        @Size(min = 7, max = 20, message = "Buyer phone number must be between 7 and 20 characters.")
        String phoneNumber
) {}
