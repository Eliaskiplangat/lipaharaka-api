package com.lipaharaka.api.user;

import com.lipaharaka.api.common.response.ApiResponse;
import com.lipaharaka.api.security.CurrentUserProvider;
import com.lipaharaka.api.user.dto.UpdateProfileRequest;
import com.lipaharaka.api.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Current-user profile settings and self-service account management")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserService userService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current user returned successfully")
    })
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        var user = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(UserProfileResponse.from(user)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates the current authenticated user's profile and phone number.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Profile update validation failed",
                    content = @Content(schema = @Schema(implementation = com.lipaharaka.api.common.response.ApiError.class))
            )
    })
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        var user = userService.updateProfile(request.fullName(), request.phoneNumber());
        return ResponseEntity.ok(ApiResponse.ok(UserProfileResponse.from(user)));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete current user account")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "User deleted successfully")
    })
    public ResponseEntity<ApiResponse<Void>> deleteCurrentUser() {
        userService.deleteCurrentUser();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.ok(null));
    }
}
