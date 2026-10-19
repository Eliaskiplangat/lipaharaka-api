package com.lipaharaka.api.business.dto;

import com.lipaharaka.api.business.KycDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UploadKycDocumentRequest(
        @NotNull KycDocumentType documentType,
        @NotBlank String fileUrl
) {
}
