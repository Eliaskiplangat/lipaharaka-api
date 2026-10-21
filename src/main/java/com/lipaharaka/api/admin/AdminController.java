package com.lipaharaka.api.admin;

import com.lipaharaka.api.audit.AuditLog;
import com.lipaharaka.api.business.dto.BusinessResponse;
import com.lipaharaka.api.business.dto.KycReviewRequest;
import com.lipaharaka.api.common.response.ApiResponse;
import com.lipaharaka.api.common.response.PageResponse;
import com.lipaharaka.api.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Platform KPIs, KYC review, and audit trail (FR-1.4, FR-6.1, FR-6.2)")
public class AdminController {

    private final AdminService adminService;
    private final CurrentUserProvider currentUserProvider;

    public AdminController(AdminService adminService, CurrentUserProvider currentUserProvider) {
        this.adminService = adminService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/kpis")
    public ApiResponse<PlatformKpiResponse> kpis() {
        return ApiResponse.ok(adminService.getPlatformKpis());
    }

    @GetMapping("/kyc/pending")
    public ApiResponse<PageResponse<BusinessResponse>> pendingKyc(Pageable pageable) {
        var page = adminService.listPendingKyc(pageable).map(BusinessResponse::from);
        return ApiResponse.ok(PageResponse.from(page));
    }

    @PostMapping("/kyc/{businessId}/review")
    public ApiResponse<BusinessResponse> reviewKyc(@PathVariable UUID businessId, @Valid @RequestBody KycReviewRequest request) {
        UUID reviewerId = currentUserProvider.get().userId();
        var business = adminService.reviewKyc(businessId, request.approve(), request.reason(), reviewerId);
        return ApiResponse.ok(BusinessResponse.from(business));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditLog>> auditLogs(@RequestParam String entityType,
                                                          @RequestParam UUID entityId,
                                                          Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(adminService.getAuditTrail(entityType, entityId, pageable)));
    }

    @GetMapping("/businesses")
    public ApiResponse<PageResponse<BusinessResponse>> listBusinesses(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(adminService.listBusinesses(pageable).map(BusinessResponse::from)));
    }

    @DeleteMapping("/businesses/{businessId}")
    public ApiResponse<Void> deactivateBusiness(@PathVariable UUID businessId) {
        adminService.deactivateBusiness(businessId);
        return ApiResponse.ok(null);
    }
}
