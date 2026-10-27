package com.lipaharaka.api.invoice;

import com.lipaharaka.api.common.response.ApiResponse;
import com.lipaharaka.api.common.response.PageResponse;
import com.lipaharaka.api.invoice.dto.CreateInvoiceRequest;
import com.lipaharaka.api.invoice.dto.InvoiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Create an invoice", description = "Creates a new invoice for the current SME and returns the invoice payload.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Invoice created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invoice validation failed",
                    content = @Content(schema = @Schema(implementation = com.lipaharaka.api.common.response.ApiError.class))
            )
    })
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(@Valid @RequestBody CreateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(InvoiceResponse.from(invoiceService.create(request))));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasRole('SME_OWNER')")
    @Operation(summary = "Send an invoice", description = "Marks the invoice as sent and delivers it via configured channels to the buyer.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invoice sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<ApiResponse<InvoiceResponse>> send(@Parameter(description = "Invoice UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(InvoiceResponse.from(invoiceService.send(id))));
    }

    @GetMapping
    @PreAuthorize("hasRole('SME_OWNER')")
    @Operation(summary = "List invoices for the current business")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invoices returned successfully")
    })
    public ResponseEntity<ApiResponse<PageResponse<InvoiceResponse>>> list(Pageable pageable) {
        var page = invoiceService.listForCurrentBusiness(pageable).map(InvoiceResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SME_OWNER')")
    @Operation(summary = "Get a business invoice by ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invoice returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<ApiResponse<InvoiceResponse>> get(@Parameter(description = "Invoice UUID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(InvoiceResponse.from(invoiceService.getOwnedInvoiceOrThrow(id))));
    }

    @GetMapping("/public/{token}")
    @Operation(summary = "Get invoice by public share token", description = "Returns invoice data for a buyer who opened a public payment link.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Public invoice returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Public invoice token not found")
    })
    public ResponseEntity<ApiResponse<InvoiceResponse>> getPublic(@Parameter(description = "Public invoice token", required = true) @PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(InvoiceResponse.from(invoiceService.getByShareableTokenOrThrow(token))));
    }
}
