package com.lipaharaka.api.business;

import com.lipaharaka.api.audit.Audited;
import com.lipaharaka.api.business.dto.CreateBusinessRequest;
import com.lipaharaka.api.business.dto.UploadKycDocumentRequest;
import com.lipaharaka.api.common.exception.ConflictException;
import com.lipaharaka.api.common.exception.NotFoundException;
import com.lipaharaka.api.common.exception.ValidationException;
import com.lipaharaka.api.security.CurrentUserProvider;
import com.lipaharaka.api.user.User;
import com.lipaharaka.api.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public BusinessService(BusinessRepository businessRepository, KycDocumentRepository kycDocumentRepository,
                            UserRepository userRepository, CurrentUserProvider currentUserProvider) {
        this.businessRepository = businessRepository;
        this.kycDocumentRepository = kycDocumentRepository;
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    @Audited(action = "BUSINESS_CREATED", entityType = "Business")
    public Business createForCurrentUser(CreateBusinessRequest request) {
        UUID ownerId = currentUserProvider.get().userId();
        if (businessRepository.findByOwnerId(ownerId).isPresent()) {
            throw new ConflictException("This account already has a registered business.");
        }
        if (businessRepository.existsByKraPin(request.kraPin())) {
            throw new ConflictException("A business with this KRA PIN is already registered.");
        }
        User owner = userRepository.findById(ownerId).orElseThrow(() -> NotFoundException.of("User", ownerId));

        Business business = new Business();
        business.setOwner(owner);
        business.setName(request.name());
        business.setKraPin(request.kraPin());
        business.setSector(request.sector());
        business.setMpesaShortcode(request.mpesaShortcode());
        return businessRepository.save(business);
    }

    public Business getForCurrentUser() {
        UUID ownerId = currentUserProvider.get().userId();
        return businessRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new NotFoundException("No business profile found for this account."));
    }

    public Business getByIdOrThrow(UUID businessId) {
        return businessRepository.findById(businessId).orElseThrow(() -> NotFoundException.of("Business", businessId));
    }

    @Transactional
    @Audited(action = "KYC_DOCUMENT_UPLOADED", entityType = "Business")
    public Business uploadKycDocument(UploadKycDocumentRequest request) {
        Business business = getForCurrentUser();
        KycDocument doc = new KycDocument();
        doc.setBusiness(business);
        doc.setDocumentType(request.documentType());
        doc.setFileUrl(request.fileUrl());
        kycDocumentRepository.save(doc);
        return business;
    }

    public List<KycDocument> listKycDocuments(UUID businessId) {
        return kycDocumentRepository.findByBusinessId(businessId);
    }

    public Page<Business> listPendingKyc(Pageable pageable) {
        return businessRepository.findByKycStatus(KycStatus.PENDING, pageable);
    }

    @Transactional
    @Audited(action = "KYC_REVIEWED", entityType = "Business")
    public Business reviewKyc(UUID businessId, boolean approve, String reason, UUID reviewerId) {
        Business business = getByIdOrThrow(businessId);
        if (business.getKycStatus() != KycStatus.PENDING) {
            throw new ConflictException("This business's KYC has already been reviewed.");
        }
        if (!approve && (reason == null || reason.isBlank())) {
            throw new ValidationException("A reason is required when rejecting a KYC submission.");
        }
        business.setKycStatus(approve ? KycStatus.APPROVED : KycStatus.REJECTED);
        business.setKycReviewReason(reason);
        business.setKycReviewedBy(reviewerId);
        business.setKycReviewedAt(Instant.now());
        return businessRepository.save(business);
    }
}
