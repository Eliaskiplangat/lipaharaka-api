package com.lipaharaka.api.user;

import com.lipaharaka.api.common.exception.ConflictException;
import com.lipaharaka.api.security.JwtService;
import com.lipaharaka.api.user.dto.AuthResponse;
import com.lipaharaka.api.user.dto.LoginRequest;
import com.lipaharaka.api.user.dto.RegisterRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, OtpService otpService,
                        PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void requestRegistrationOtp(String phoneNumber) {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new ConflictException("An account with this phone number already exists.");
        }
        otpService.requestOtp(phoneNumber, OtpPurpose.REGISTRATION);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ConflictException("An account with this phone number already exists.");
        }
        otpService.verifyOtpOrThrow(request.phoneNumber(), OtpPurpose.REGISTRATION, request.otpCode());

        User user = new User();
        user.setPhoneNumber(request.phoneNumber());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.SME_OWNER);
        user.setPhoneVerified(true);
        userRepository.save(user);

        return issueTokenResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new BadCredentialsException("Invalid phone number or password."));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid phone number or password.");
        }
        return issueTokenResponse(user);
    }

    private AuthResponse issueTokenResponse(User user) {
        String token = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getRole().name());
    }
}
