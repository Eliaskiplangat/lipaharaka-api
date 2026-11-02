package com.lipaharaka.api.user;

import com.lipaharaka.api.common.exception.ConflictException;
import com.lipaharaka.api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void requestRegistrationOtp_shouldRejectExistingPhoneNumber() {
        when(userRepository.existsByPhoneNumber("+254712345678")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> authService.requestRegistrationOtp("+254712345678"));

        verify(otpService, never()).requestOtp(any(), any());
    }

    @Test
    void requestPasswordResetOtp_shouldAllowExistingUser() {
        when(userRepository.existsByPhoneNumber("+254712345678")).thenReturn(true);

        assertDoesNotThrow(() -> authService.requestPasswordResetOtp("+254712345678"));

        verify(otpService).requestOtp("+254712345678", OtpPurpose.PASSWORD_RESET);
    }
}
