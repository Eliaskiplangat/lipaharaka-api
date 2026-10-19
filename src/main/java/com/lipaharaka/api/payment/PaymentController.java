package com.lipaharaka.api.payment;

import com.lipaharaka.api.common.response.ApiResponse;
import com.lipaharaka.api.payment.dto.InitiatePaymentRequest;
import com.lipaharaka.api.payment.dto.InitiatePaymentResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "M-Pesa STK Push payment and callback reconciliation (FR-4.1, FR-4.2)")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/mpesa/invoices/{token}/initiate")
    public ApiResponse<InitiatePaymentResponse> initiate(@PathVariable String token,
                                                          @Valid @RequestBody InitiatePaymentRequest request) {
        return ApiResponse.ok(paymentService.initiateInvoicePayment(token, request.payerPhoneNumber()));
    }

    @PostMapping("/mpesa/callback/stk")
    public Map<String, Object> stkCallback(@RequestBody Map<String, Object> payload) {
        handleStkCallbackAsync(payload);
        return Map.of("ResultCode", 0, "ResultDesc", "Accepted");
    }

    @Async("taskExecutor")
    void handleStkCallbackAsync(Map<String, Object> payload) {
        try {
            paymentService.handleStkCallback(payload);
        } catch (Exception ex) {
            log.error("Failed processing STK callback", ex);
        }
    }

    @PostMapping("/mpesa/callback/b2c/result")
    public Map<String, Object> b2cResultCallback(@RequestBody Map<String, Object> payload) {
        log.info("B2C result callback received: {}", payload);
        return Map.of("ResultCode", 0, "ResultDesc", "Accepted");
    }

    @PostMapping("/mpesa/callback/b2c/timeout")
    public Map<String, Object> b2cTimeoutCallback(@RequestBody Map<String, Object> payload) {
        log.warn("B2C timeout callback received: {}", payload);
        return Map.of("ResultCode", 0, "ResultDesc", "Accepted");
    }
}
