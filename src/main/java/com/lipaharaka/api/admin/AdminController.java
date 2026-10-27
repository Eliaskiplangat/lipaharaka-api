package com.lipaharaka.api.admin;

import com.lipaharaka.api.audit.AuditLog;
import com.lipaharaka.api.business.dto.BusinessResponse;
import com.lipaharaka.api.business.dto.KycReviewRequest;
import com.lipaharaka.api.common.response.ApiResponse;
import com.lipaharaka.api.common.response.PageResponse;
import com.lipaharaka.api.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Get platform KPIs", description = "Returns aggregated metrics for platform health, collections, and user activity.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "KPIs returned successfully")
    })
    public ApiResponse<PlatformKpiResponse> kpis() {
        return ApiResponse.ok(adminService.getPlatformKpis());
    }

    @GetMapping("/kyc/pending")
    @Operation(summary = "List pending KYC businesses")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending KYC businesses returned")
    })
    public ApiResponse<PageResponse<BusinessResponse>> pendingKyc(Pageable pageable) {
        var page = adminService.listPendingKyc(pageable).map(BusinessResponse::from);
        return ApiResponse.ok(PageResponse.from(page));
    }

    @PostMapping("/kyc/{businessId}/review")
    @Operation(summary = "Approve or reject a business KYC review")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "KYC review outcome saved")
    })
    public ApiResponse<BusinessResponse> reviewKyc(@Parameter(description = "Business UUID to review", required = true) @PathVariable UUID businessId,
                                                 @Valid @RequestBody KycReviewRequest request) {
        UUID reviewerId = currentUserProvider.get().userId();
        var business = adminService.reviewKyc(businessId, request.approve(), request.reason(), reviewerId);
        return ApiResponse.ok(BusinessResponse.from(business));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get entity audit logs")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit logs returned successfully")
    })
    public ApiResponse<PageResponse<AuditLog>> auditLogs(@Parameter(description = "Entity type name", required = true) @RequestParam String entityType,
                                                          @Parameter(description = "Entity UUID", required = true) @RequestParam UUID entityId,
                                                          Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(adminService.getAuditTrail(entityType, entityId, pageable)));
    }

    @GetMapping("/businesses")
    @Operation(summary = "List all businesses for admin")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Businesses returned successfully")
    })
    public ApiResponse<PageResponse<BusinessResponse>> listBusinesses(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(adminService.listBusinesses(pageable).map(BusinessResponse::from)));
    }

    @DeleteMapping("/businesses/{businessId}")
    @Operation(summary = "Deactivate a business account")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Business deactivated successfully")
    })
    public ApiResponse<Void> deactivateBusiness(@Parameter(description = "Business UUID to deactivate", required = true) @PathVariable UUID businessId) {
        adminService.deactivateBusiness(businessId);
        return ApiResponse.ok(null);
    }
}
