package com.lipaharaka.api.user;

import com.lipaharaka.api.common.exception.ValidationException;
import com.lipaharaka.api.notification.SmsGateway;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@EnableConfigurationProperties(OtpProperties.class)
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository otpCodeRepository;
    private final SmsGateway smsGateway;
    private final OtpProperties properties;
    private final BCryptPasswordEncoder codeEncoder = new BCryptPasswordEncoder();

    public OtpService(OtpCodeRepository otpCodeRepository, SmsGateway smsGateway, OtpProperties properties) {
        this.otpCodeRepository = otpCodeRepository;
        this.smsGateway = smsGateway;
        this.properties = properties;
    }

    @Transactional
    public void requestOtp(String phoneNumber, OtpPurpose purpose) {
        String code = generateNumericCode(properties.length());

        OtpCode otp = new OtpCode();
        otp.setPhoneNumber(phoneNumber);
        otp.setCodeHash(codeEncoder.encode(code));
        otp.setPurpose(purpose);
        otp.setExpiresAt(Instant.now().plus(properties.ttlMinutes(), ChronoUnit.MINUTES));
        otpCodeRepository.save(otp);

        smsGateway.send(phoneNumber, "Your LipaHaraka verification code is " + code
                + ". It expires in " + properties.ttlMinutes() + " minutes.");
    }

    @Transactional
    public void verifyOtpOrThrow(String phoneNumber, OtpPurpose purpose, String suppliedCode) {
        OtpCode otp = otpCodeRepository
                .findTopByPhoneNumberAndPurposeOrderByCreatedAtDesc(phoneNumber, purpose)
                .orElseThrow(() -> new ValidationException("No verification code was requested for this number."));

        if (!otp.isUsable()) {
            throw new ValidationException("Verification code has expired or was already used. Please request a new one.");
        }
        if (!codeEncoder.matches(suppliedCode, otp.getCodeHash())) {
            throw new ValidationException("Incorrect verification code.");
        }
        otp.setConsumedAt(Instant.now());
        otpCodeRepository.save(otp);
    }

    private String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
