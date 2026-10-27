package com.lipaharaka.api.user;

import com.lipaharaka.api.common.response.ApiResponse;
import com.lipaharaka.api.user.dto.AuthResponse;
import com.lipaharaka.api.user.dto.LoginRequest;
import com.lipaharaka.api.user.dto.RegisterRequest;
import com.lipaharaka.api.user.dto.RequestOtpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Phone-number registration/login with OTP verification (FR-1.1)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/otp/request")
    @Operation(
            summary = "Request an OTP",
            description = "Sends a one-time password to the supplied phone number for account registration or login."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP request accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = com.lipaharaka.api.common.response.ApiError.class))
            )
    })
    public ResponseEntity<ApiResponse<Void>> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        authService.requestRegistrationOtp(request.phoneNumber());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new SME owner account after OTP verification.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation or OTP failure",
                    content = @Content(schema = @Schema(implementation = com.lipaharaka.api.common.response.ApiError.class))
            )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with phone number and password", description = "Authenticates the user and returns a JWT token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid phone number or password",
                    content = @Content(schema = @Schema(implementation = com.lipaharaka.api.common.response.ApiError.class))
            )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }
}
