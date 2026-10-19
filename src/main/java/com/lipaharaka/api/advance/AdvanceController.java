package com.lipaharaka.api.advance;

import com.lipaharaka.api.advance.dto.AdvanceQuoteResponse;
import com.lipaharaka.api.advance.dto.AdvanceResponse;
import com.lipaharaka.api.advance.dto.RequestAdvanceRequest;
import com.lipaharaka.api.business.BusinessService;
import com.lipaharaka.api.common.response.ApiResponse;
import com.lipaharaka.api.common.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/advances")
@Tag(name = "Advances", description = "Invoice-backed cash advances (FR-5.2 to FR-5.4)")
@PreAuthorize("hasRole('SME_OWNER')")
public class AdvanceController {

    private final AdvanceService advanceService;
    private final BusinessService businessService;

    public AdvanceController(AdvanceService advanceService, BusinessService businessService) {
        this.advanceService = advanceService;
        this.businessService = businessService;
    }

    @GetMapping("/quote")
    public ResponseEntity<ApiResponse<AdvanceQuoteResponse>> quote(@RequestParam UUID invoiceId) {
        return ResponseEntity.ok(ApiResponse.ok(advanceService.quote(invoiceId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdvanceResponse>> request(@Valid @RequestBody RequestAdvanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(AdvanceResponse.from(advanceService.requestAdvance(request.invoiceId()))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdvanceResponse>>> list(Pageable pageable) {
        UUID businessId = businessService.getForCurrentUser().getId();
        var page = advanceService.listForBusiness(businessId, pageable).map(AdvanceResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }
}

