package com.lipaharaka.api.business;

import com.lipaharaka.api.business.dto.BusinessResponse;
import com.lipaharaka.api.business.dto.CreateBusinessRequest;
import com.lipaharaka.api.business.dto.UploadKycDocumentRequest;
import com.lipaharaka.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/businesses")
@Tag(name = "Businesses", description = "SME business profile and KYC onboarding (FR-1.2, FR-1.3)")
@PreAuthorize("hasRole('SME_OWNER')")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusinessResponse>> create(@Valid @RequestBody CreateBusinessRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(BusinessResponse.from(businessService.createForCurrentUser(request))));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<BusinessResponse>> getMine() {
        return ResponseEntity.ok(ApiResponse.ok(BusinessResponse.from(businessService.getForCurrentUser())));
    }

    @PostMapping("/me/kyc-documents")
    public ResponseEntity<ApiResponse<BusinessResponse>> uploadKycDocument(@Valid @RequestBody UploadKycDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(BusinessResponse.from(businessService.uploadKycDocument(request))));
    }
}
