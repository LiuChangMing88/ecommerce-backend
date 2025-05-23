package com.github.liuchangming88.ecommerce_backend.service;


import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationResponse;
import com.github.liuchangming88.ecommerce_backend.configuration.MapperConfig;
import com.github.liuchangming88.ecommerce_backend.exception.*;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.VerificationToken;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.model.repository.VerificationTokenRepository;
import com.github.liuchangming88.ecommerce_backend.util.TestDataUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    // Mocks
    @Mock
    private LocalUserRepository localUserRepository;
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private JwtService jwtService;
    @Mock
    private VerificationTokenService verificationTokenService;
    @Mock
    private EmailService emailService;

    // Service class under test
    @InjectMocks
    private UserService userService;

    private final int emailVerificationTokenExpiryTime = 86400;

    @BeforeEach
    void setUp() {
        // Since this mapper has custom mappings, use the real one to preserve configurations
        ModelMapper modelMapper = new MapperConfig().modelMapper();
        ReflectionTestUtils.setField(userService, "modelMapper", modelMapper);
        ReflectionTestUtils.setField(userService, "emailVerificationTokenExpiryTime", 86400);
    }

    @Test
    public void registerUser_withExistingUsername_throwsUserAlreadyExists() {
        RegistrationRequest request = TestDataUtil.createTestRegisterRequest();

        LocalUser mockUser = mock(LocalUser.class);

        // Stub the repository method to return an Optional containing the mock user
        when(localUserRepository.findByUsernameIgnoreCase(request.getUsername()))
                .thenReturn(Optional.of(mockUser));

        // Proceed with the test logic that expects a UserAlreadyExistsException
        assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.registerUser(request)
        );
    }

    @Test
    public void registerUser_withExistingEmail_throwsUserAlreadyExists() {
        RegistrationRequest request = TestDataUtil.createTestRegisterRequest();

        LocalUser mockUser = mock(LocalUser.class);

        // Stub the repository method to return an Optional containing the mock user
        when(localUserRepository.findByEmailIgnoreCase(request.getEmail()))
                .thenReturn(Optional.of(mockUser));

        // Proceed with the test logic that expects a UserAlreadyExistsException
        Assertions.assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.registerUser(request)
        );
    }

    @Test
    public void registerUser_withNewCredentials_savesAndSendsMail() {
        RegistrationRequest request = TestDataUtil.createTestRegisterRequest();

        // Stub
        when(encryptionService.encryptPassword(request.getPassword()))
                .thenReturn("EncryptedPassword");
        VerificationToken mockToken = mock(VerificationToken.class);
        when(verificationTokenService.createVerificationToken(any(LocalUser.class)))
                .thenReturn(mockToken);

        // Assert the return
        RegistrationResponse actual = userService.registerUser(request);
        Assertions.assertEquals(request.getUsername(), actual.getUsername());
        Assertions.assertEquals(request.getEmail(), actual.getEmail());

        // Verify
        verify(emailService, times(1)).sendVerificationEmail(mockToken);
        verify(verificationTokenRepository, times(1)).save(mockToken);
    }

    @Test
    public void loginUser_withWrongUsername_throwsIncorrectUsername() {
        LoginRequest request = TestDataUtil.createTestLoginRequest();

        // Assert
        Assertions.assertThrows(IncorrectUsernameException.class,
                () -> userService.loginUser(request),
                "Wrong username");
    }

    @Test
    public void loginUser_withWrongPassword_throwsIncorrectPassword() {
        LoginRequest request = TestDataUtil.createTestLoginRequest();

        // Stub
        when(localUserRepository.findByUsernameIgnoreCase(request.getUsername()))
                .thenReturn(Optional.of(new LocalUser()));

        // Assert
        Assertions.assertThrows(IncorrectPasswordException.class,
                () -> userService.loginUser(request),
                "Wrong username");
    }

    @Test
    public void loginUser_correctCredentialsAndUnverifiedEmail_noResendEmail() {
        // Arrange
        LoginRequest request = TestDataUtil.createTestLoginRequest();

        // Create a VerificationToken with a recent creation time
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setCreatedAt(LocalDateTime.now());

        // Create a LocalUser with unverified email and associated verification token
        LocalUser localUser = new LocalUser();
        localUser.setUsername(request.getUsername());
        localUser.setPassword("hashedPassword");
        localUser.setIsEmailVerified(false);
        localUser.setVerificationToken(verificationToken);

        // Stub repository to return the LocalUser
        when(localUserRepository.findByUsernameIgnoreCase(eq(request.getUsername())))
                .thenReturn(Optional.of(localUser));

        // Stub encryptionService to return true for password verification
        when(encryptionService.verifyPassword(eq(request.getPassword()), eq("hashedPassword")))
                .thenReturn(true);

        // Act & Assert
        Assertions.assertThrows(UserNotVerifiedException.class,
                () -> userService.loginUser(request),
                "Expected UserNotVerifiedException for unverified email");

        // Verify that no new verification email is sent
        verify(emailService, never()).sendVerificationEmail(any());
        verify(verificationTokenRepository, never()).save(any());
    }



    @Test
    public void loginUser_correctCredentialsAndUnverifiedEmailExpiredToken_resendsEmail() {
        LoginRequest request = TestDataUtil.createTestLoginRequest();

        // Create a LocalUser with unverified email and expired token
        LocalUser localUser = new LocalUser();
        localUser.setUsername(request.getUsername());
        localUser.setPassword("hashedPassword");
        localUser.setIsEmailVerified(false);

        VerificationToken oldToken = new VerificationToken();
        oldToken.setCreatedAt(LocalDateTime.now().minusSeconds(emailVerificationTokenExpiryTime + 1));
        localUser.setVerificationToken(oldToken);

        // Stub repository to return the LocalUser
        when(localUserRepository.findByUsernameIgnoreCase(eq(request.getUsername())))
                .thenReturn(Optional.of(localUser));

        // Stub encryptionService to return true for password verification
        when(encryptionService.verifyPassword(eq(request.getPassword()), eq("hashedPassword")))
                .thenReturn(true);

        // Stub verificationTokenService to return a new token
        VerificationToken newToken = new VerificationToken();
        when(verificationTokenService.createVerificationToken(eq(localUser)))
                .thenReturn(newToken);

        // Assert that UserNotVerifiedException is thrown with appropriate message
        assertThrows(UserNotVerifiedException.class,
                () -> userService.loginUser(request));

        // Verify that a new verification email is sent and token is saved
        verify(emailService, times(1)).sendVerificationEmail(eq(newToken));
        verify(verificationTokenRepository, times(1)).save(eq(newToken));
    }

    @Test
    public void loginUser_correctCredentialsAndUnverifiedEmailNoToken_resendsEmail() {
        LoginRequest request = TestDataUtil.createTestLoginRequest();

        // Create a LocalUser with unverified email and expired token
        LocalUser localUser = new LocalUser();
        localUser.setUsername(request.getUsername());
        localUser.setPassword("hashedPassword");
        localUser.setIsEmailVerified(false);
        localUser.setVerificationToken(null);

        // Stub repository to return the LocalUser
        when(localUserRepository.findByUsernameIgnoreCase(eq(request.getUsername())))
                .thenReturn(Optional.of(localUser));

        // Stub encryptionService to return true for password verification
        when(encryptionService.verifyPassword(eq(request.getPassword()), eq("hashedPassword")))
                .thenReturn(true);

        // Stub verificationTokenService to return a new token
        VerificationToken newToken = new VerificationToken();
        when(verificationTokenService.createVerificationToken(eq(localUser)))
                .thenReturn(newToken);

        // Assert that UserNotVerifiedException is thrown with appropriate message
        assertThrows(UserNotVerifiedException.class,
                () -> userService.loginUser(request));

        // Verify that a new verification email is sent and token is saved
        verify(emailService, times(1)).sendVerificationEmail(eq(newToken));
        verify(verificationTokenRepository, times(1)).save(eq(newToken));
    }

    @Test
    public void loginUser_correctCredentialsEmailVerified_returnsJwtToken() {
        LoginRequest request = TestDataUtil.createTestLoginRequest();

        // Create a LocalUser with verified email
        LocalUser localUser = new LocalUser();
        localUser.setUsername(request.getUsername());
        localUser.setPassword("hashedPassword");
        localUser.setIsEmailVerified(true);

        // Stub repository to return the LocalUser
        when(localUserRepository.findByUsernameIgnoreCase(eq(request.getUsername())))
                .thenReturn(Optional.of(localUser));

        // Stub encryptionService to return true for password verification
        when(encryptionService.verifyPassword(eq(request.getPassword()), eq("hashedPassword")))
                .thenReturn(true);

        // Stub jwtService to return a token
        when(jwtService.generateJwt(eq(localUser)))
                .thenReturn("mockJwtToken");

        // Assert that the returned token matches the stubbed token
        String token = userService.loginUser(request);
        Assertions.assertEquals("mockJwtToken", token);

        // Verify that no verification email is sent
        verify(emailService, never()).sendVerificationEmail(any());
    }


    @Test
    public void verifyUser_invalidToken_throwsInvalidToken() {
        String token = "invalidToken";

        // Stub repository to return empty
        when(verificationTokenRepository.findByToken(eq(token)))
                .thenReturn(Optional.empty());

        // Assert that InvalidVerificationTokenException is thrown
        assertThrows(
                InvalidVerificationTokenException.class,
                () -> userService.verifyUser(token)
        );
    }


    @Test
    public void verifyUser_userAlreadyVerified_throwsUserAlreadyVerified() {
        String token = "validToken";

        // Create a LocalUser with verified email
        LocalUser localUser = new LocalUser();
        localUser.setIsEmailVerified(true);

        // Create a VerificationToken associated with the LocalUser
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setLocalUser(localUser);

        // Stub repository to return the VerificationToken
        when(verificationTokenRepository.findByToken(eq(token)))
                .thenReturn(Optional.of(verificationToken));

        // Assert that UserAlreadyVerifiedException is thrown
        assertThrows(
                UserAlreadyVerifiedException.class,
                () -> userService.verifyUser(token)
        );

        // Verify that the token is deleted and user's verification token is set to null
        verify(localUserRepository, times(1)).save(eq(localUser));
        verify(verificationTokenRepository, times(1)).delete(eq(verificationToken));
    }


    @Test
    public void verifyUser_expiredToken_throwsExpiredToken() {
        String token = "expiredToken";

        // Create a LocalUser with unverified email
        LocalUser localUser = new LocalUser();
        localUser.setIsEmailVerified(false);

        // Create an expired VerificationToken associated with the LocalUser
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setLocalUser(localUser);
        verificationToken.setCreatedAt(LocalDateTime.now().minusSeconds(emailVerificationTokenExpiryTime + 1));

        // Stub repository to return the VerificationToken
        when(verificationTokenRepository.findByToken(eq(token)))
                .thenReturn(Optional.of(verificationToken));

        // Assert that VerificationTokenExpiredException is thrown
        assertThrows(
                VerificationTokenExpiredException.class,
                () -> userService.verifyUser(token)
        );
    }


    @Test
    public void verifyUser_unverifiedUserAndValidUnexpiredToken_verifiesUserAndDeletesToken() {
        String token = "validToken";

        // Create a LocalUser with unverified email
        LocalUser localUser = new LocalUser();
        localUser.setIsEmailVerified(false);

        // Create a valid VerificationToken associated with the LocalUser
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setLocalUser(localUser);
        verificationToken.setCreatedAt(LocalDateTime.now());

        // Stub repository to return the VerificationToken
        when(verificationTokenRepository.findByToken(eq(token)))
                .thenReturn(Optional.of(verificationToken));

        // Call the method under test
        userService.verifyUser(token);

        // Assert that the user's email is now verified and verification token is null
        Assertions.assertTrue(localUser.getIsEmailVerified());
        Assertions.assertNull(localUser.getVerificationToken());

        // Verify that the user is saved and token is deleted
        verify(localUserRepository, times(1)).save(eq(localUser));
        verify(verificationTokenRepository, times(1)).delete(eq(verificationToken));
    }

}