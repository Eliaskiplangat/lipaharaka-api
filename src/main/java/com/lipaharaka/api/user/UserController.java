package com.lipaharaka.api.user;

import com.lipaharaka.api.common.response.ApiResponse;
import com.lipaharaka.api.security.CurrentUserProvider;
import com.lipaharaka.api.user.dto.UpdateProfileRequest;
import com.lipaharaka.api.user.dto.UserProfileResponse;
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
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        var user = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(UserProfileResponse.from(user)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        var user = userService.updateProfile(request.fullName(), request.phoneNumber());
        return ResponseEntity.ok(ApiResponse.ok(UserProfileResponse.from(user)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteCurrentUser() {
        userService.deleteCurrentUser();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.ok(null));
    }
}
