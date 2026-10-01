package com.lipaharaka.api.invoice;

import com.lipaharaka.api.common.response.ApiResponse;
import com.lipaharaka.api.common.response.PageResponse;
import com.lipaharaka.api.invoice.dto.CreateInvoiceRequest;
import com.lipaharaka.api.invoice.dto.InvoiceResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices", description = "Invoice creation, sending, and status tracking (FR-2.x)")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SME_OWNER')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(@Valid @RequestBody CreateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(InvoiceResponse.from(invoiceService.create(request))));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasRole('SME_OWNER')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> send(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(InvoiceResponse.from(invoiceService.send(id))));
    }

    @GetMapping
    @PreAuthorize("hasRole('SME_OWNER')")
    public ResponseEntity<ApiResponse<PageResponse<InvoiceResponse>>> list(Pageable pageable) {
        var page = invoiceService.listForCurrentBusiness(pageable).map(InvoiceResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SME_OWNER')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(InvoiceResponse.from(invoiceService.getOwnedInvoiceOrThrow(id))));
    }

    @GetMapping("/public/{token}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getPublic(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(InvoiceResponse.from(invoiceService.getByShareableTokenOrThrow(token))));
    }
}
