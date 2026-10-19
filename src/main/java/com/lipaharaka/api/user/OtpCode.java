package com.lipaharaka.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@NoArgsConstructor
public class OtpCode {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OtpPurpose purpose;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant consumedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public boolean isUsable() {
        return consumedAt == null && Instant.now().isBefore(expiresAt);
    }
}
