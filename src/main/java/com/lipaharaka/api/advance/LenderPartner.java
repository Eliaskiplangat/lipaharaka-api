package com.lipaharaka.api.advance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;


@Entity
@Table(name = "lender_partners")
@Getter
@Setter
@NoArgsConstructor
public class LenderPartner {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "api_base_url", nullable = false, length = 300)
    private String apiBaseUrl;

    @Column(nullable = false)
    private boolean active = true;
}
