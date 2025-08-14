package com.github.liuchangming88.ecommerce_backend.service.user;

import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationResponse;
import com.github.liuchangming88.ecommerce_backend.exception.ResourceNotFoundException;
import com.github.liuchangming88.ecommerce_backend.model.user.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.user.Role;
import com.github.liuchangming88.ecommerce_backend.model.user.repository.LocalUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminUserServiceTest {
    @Mock
    private LocalUserRepository localUserRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    public void getProfileById_profileDoesNotExist_throwsResourceNotFoundException() {
        // Act and assert
        assertThrows(ResourceNotFoundException.class,
                () -> adminUserService.getProfileById(1L));
    }

    @Test
    public void getProfileById_profileByIdExists_returnsProfile() {
        // Arrange
        LocalUser localUser = new LocalUser();
        localUser.setUsername("testUser");
        localUser.setPassword("PasswordTest123");
        localUser.setEmail("testEmail@gmail.com");
        localUser.setFirstName("testUser first name");
        localUser.setLastName("testUser last name");
        localUser.setRole(Role.USER);
        localUser.setIsEmailVerified(true);
        localUser.setId(1L);

        // Arrange
        when(localUserRepository.findById(localUser.getId()))
                .thenReturn(Optional.of(localUser));

        // Act
        RegistrationResponse response = adminUserService.getProfileById(localUser.getId());

        // Assert
        assertEquals(localUser.getUsername(), response.getUsername());
        assertEquals(localUser.getEmail(), response.getEmail());
    }
}
