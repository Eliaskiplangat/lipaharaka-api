package com.lipaharaka.api.user;

import com.lipaharaka.api.business.BusinessRepository;
import com.lipaharaka.api.common.exception.NotFoundException;
import com.lipaharaka.api.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private UserService userService;

    @Test
    void updateProfile_shouldPersistNameAndPhoneNumber() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setFullName("Old Name");
        user.setPhoneNumber("0712345678");
        user.setActive(true);

        when(currentUserProvider.get()).thenReturn(new com.lipaharaka.api.security.AuthenticatedUser(userId, Role.SME_OWNER.name()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateProfile("New Name", "0712345679");

        assertEquals("New Name", updated.getFullName());
        assertEquals("0712345679", updated.getPhoneNumber());
        verify(userRepository).save(user);
    }

    @Test
    void deleteCurrentUser_shouldDeactivateUserAndKeepPrimaryKeyStable() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setFullName("Jane Doe");
        user.setPhoneNumber("0712345678");
        user.setActive(true);

        when(currentUserProvider.get()).thenReturn(new com.lipaharaka.api.security.AuthenticatedUser(userId, Role.SME_OWNER.name()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.deleteCurrentUser();

        assertFalse(user.isActive());
        assertTrue(user.getPhoneNumber().startsWith("deleted-"));
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_shouldRejectMissingUser() {
        UUID userId = UUID.randomUUID();
        when(currentUserProvider.get()).thenReturn(new com.lipaharaka.api.security.AuthenticatedUser(userId, Role.SME_OWNER.name()));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.updateProfile("New Name", "0712345679"));
    }
}
