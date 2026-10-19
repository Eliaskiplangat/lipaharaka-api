package com.lipaharaka.api.user;

import com.lipaharaka.api.business.BusinessRepository;
import com.lipaharaka.api.common.exception.NotFoundException;
import com.lipaharaka.api.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final CurrentUserProvider currentUserProvider;

    public UserService(UserRepository userRepository, BusinessRepository businessRepository,
                      CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public User getCurrentUser() {
        UUID userId = currentUserProvider.get().userId();
        return userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("User", userId));
    }

    @Transactional
    public User updateProfile(String fullName, String phoneNumber) {
        UUID userId = currentUserProvider.get().userId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("User", userId));

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            user.setPhoneNumber(phoneNumber.trim());
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deleteCurrentUser() {
        UUID userId = currentUserProvider.get().userId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("User", userId));
        user.softDelete();
        businessRepository.findByOwnerId(userId).ifPresent(business -> {
            business.setActive(false);
            businessRepository.save(business);
        });
        userRepository.save(user);
    }
}
