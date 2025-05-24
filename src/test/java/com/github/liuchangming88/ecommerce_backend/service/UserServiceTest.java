package com.github.liuchangming88.ecommerce_backend.service;


import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationResponse;
import com.github.liuchangming88.ecommerce_backend.configuration.MapperConfig;
import com.github.liuchangming88.ecommerce_backend.exception.*;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.PasswordResetToken;
import com.github.liuchangming88.ecommerce_backend.model.VerificationToken;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.model.repository.PasswordResetTokenRepository;
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

import static org.junit.jupiter.api.Assertions.*;
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
    private TokenService tokenService;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    // Service class under test
    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        // Since this mapper has custom mappings, use the real one to preserve configurations
        ModelMapper modelMapper = new MapperConfig().modelMapper();
        ReflectionTestUtils.setField(userService, "modelMapper", modelMapper);
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
        when(tokenService.createVerificationToken(any(LocalUser.class)))
                .thenReturn(mockToken);

        // Assert the return
        RegistrationResponse actual = userService.registerUser(request);
        assertEquals(request.getUsername(), actual.getUsername());
        assertEquals(request.getEmail(), actual.getEmail());

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
        verificationToken.setExpireAt(LocalDateTime.now().plusSeconds(86400));

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
        oldToken.setExpireAt(LocalDateTime.now().minusSeconds(1));
        localUser.setVerificationToken(oldToken);

        // Stub repository to return the LocalUser
        when(localUserRepository.findByUsernameIgnoreCase(eq(request.getUsername())))
                .thenReturn(Optional.of(localUser));

        // Stub encryptionService to return true for password verification
        when(encryptionService.verifyPassword(eq(request.getPassword()), eq("hashedPassword")))
                .thenReturn(true);

        // Stub verificationTokenService to return a new token
        VerificationToken newToken = new VerificationToken();
        when(tokenService.createVerificationToken(eq(localUser)))
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
        when(tokenService.createVerificationToken(eq(localUser)))
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
        assertEquals("mockJwtToken", token);

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
                InvalidTokenException.class,
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
        verificationToken.setExpireAt(LocalDateTime.now().minusSeconds(1));

        // Stub repository to return the VerificationToken
        when(verificationTokenRepository.findByToken(eq(token)))
                .thenReturn(Optional.of(verificationToken));

        // Assert that VerificationTokenExpiredException is thrown
        assertThrows(
                TokenExpiredException.class,
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
        verificationToken.setExpireAt(LocalDateTime.now().plusSeconds(86400));

        // Stub repository to return the VerificationToken
        when(verificationTokenRepository.findByToken(eq(token)))
                .thenReturn(Optional.of(verificationToken));

        // Call the method under test
        userService.verifyUser(token);

        // Assert that the user's email is now verified and verification token is null
        Assertions.assertTrue(localUser.getIsEmailVerified());
        assertNull(localUser.getVerificationToken());

        // Verify that the user is saved and token is deleted
        verify(localUserRepository, times(1)).save(eq(localUser));
        verify(verificationTokenRepository, times(1)).delete(eq(verificationToken));
    }

    @Test
    public void sendResetPasswordEmail_wrongEmail_noEmailSent() {
        // Create a LocalUser
        LocalUser localUser = new LocalUser();
        localUser.setUsername("testUsername");
        localUser.setPassword("hashedPassword");
        localUser.setEmail("testEmail@gmail.com");

        // Stub
        when(localUserRepository.findByEmailIgnoreCase("GibberishEmail@gmail.com"))
                .thenReturn(Optional.empty());

        // Act
        userService.sendResetPasswordEmail("GibberishEmail@gmail.com");

        // Verify
        verify(emailService, times(0)).sendPasswordResetEmail(any(PasswordResetToken.class));
        verify(passwordResetTokenRepository, times(0)).save(any(PasswordResetToken.class));
    }

    @Test
    public void sendResetPasswordEmail_correctEmail_sendEmail() {
        // Create a LocalUser
        LocalUser localUser = new LocalUser();
        localUser.setUsername("testUsername");
        localUser.setPassword("hashedPassword");
        localUser.setEmail("testEmail@gmail.com");

        // Create a password reset token
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken("AStringOfToken");
        passwordResetToken.setExpireAt(LocalDateTime.now().plusSeconds(900));
        passwordResetToken.setLocalUser(localUser);

        // Stub
        when(localUserRepository.findByEmailIgnoreCase(localUser.getEmail()))
                .thenReturn(Optional.of(localUser));
        when(tokenService.createPasswordResetToken(localUser))
                .thenReturn(passwordResetToken);

        // Act
        userService.sendResetPasswordEmail(localUser.getEmail());

        // Verify
        verify(emailService, times(1)).sendPasswordResetEmail(passwordResetToken);
        verify(passwordResetTokenRepository, times(1)).save(passwordResetToken);
    }

    @Test
    public void sendResetPasswordEmail_correctEmailAndTokenHasExpired_resendEmail() {
        // Create a LocalUser
        LocalUser localUser = new LocalUser();
        localUser.setUsername("testUsername");
        localUser.setPassword("hashedPassword");
        localUser.setEmail("testEmail@gmail.com");

        // Create a password reset token
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken("AStringOfToken");
        passwordResetToken.setExpireAt(LocalDateTime.now().minusSeconds(1));
        passwordResetToken.setLocalUser(localUser);

        localUser.setPasswordResetToken(passwordResetToken);

        // Create a new password reset token
        PasswordResetToken newPasswordResetToken = new PasswordResetToken();
        newPasswordResetToken.setToken("newToken");
        newPasswordResetToken.setExpireAt(LocalDateTime.now().plusSeconds(900));
        newPasswordResetToken.setLocalUser(localUser);

        // Stub
        when(localUserRepository.findByEmailIgnoreCase(localUser.getEmail()))
                .thenReturn(Optional.of(localUser));
        when(tokenService.createPasswordResetToken(localUser))
                .thenReturn(newPasswordResetToken);

        // Act
        userService.sendResetPasswordEmail(localUser.getEmail());

        // Verify
        verify(emailService, times(1)).sendPasswordResetEmail(newPasswordResetToken);
        verify(passwordResetTokenRepository, times(1)).save(newPasswordResetToken);
    }

    @Test
    public void sendResetPasswordEmail_correctEmailButTokenHasNotExpired_noResendEmail() {
        // Create a LocalUser
        LocalUser localUser = new LocalUser();
        localUser.setUsername("testUsername");
        localUser.setPassword("hashedPassword");
        localUser.setEmail("testEmail@gmail.com");

        // Create a password reset token
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken("AStringOfToken");
        passwordResetToken.setExpireAt(LocalDateTime.now().plusSeconds(900));
        passwordResetToken.setLocalUser(localUser);

        localUser.setPasswordResetToken(passwordResetToken);

        // Act
        userService.sendResetPasswordEmail(localUser.getEmail());

        // Verify
        verify(emailService, times(0)).sendPasswordResetEmail(any(PasswordResetToken.class));
        verify(passwordResetTokenRepository, times(0)).save(any(PasswordResetToken.class));
    }

    @Test
    public void validateResetToken_invalidToken_throwsInvalidToken() {
        // Arrange
        String invalidToken = "invalidToken";
        when(passwordResetTokenRepository.findByToken(invalidToken))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidTokenException.class,
                () -> userService.validateResetToken(invalidToken)
        );
    }

    @Test
    public void validateResetToken_expiredToken_throwsExpiredToken() {
        // Arrange
        String expiredTokenStr = "expiredToken";
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setToken(expiredTokenStr);
        expiredToken.setExpireAt(LocalDateTime.now().minusSeconds(1));
        when(passwordResetTokenRepository.findByToken(expiredTokenStr))
                .thenReturn(Optional.of(expiredToken));

        // Act & Assert
        assertThrows(TokenExpiredException.class,
                () -> userService.validateResetToken(expiredTokenStr)
        );
    }

    @Test
    public void validateResetToken_validToken_returnsToken() {
        // Arrange
        String validTokenStr = "validToken";
        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setToken(validTokenStr);
        validToken.setExpireAt(LocalDateTime.now().plusSeconds(900));
        when(passwordResetTokenRepository.findByToken(validTokenStr))
                .thenReturn(Optional.of(validToken));

        // Act
        PasswordResetToken result = userService.validateResetToken(validTokenStr);

        // Assert
        assertEquals(validToken, result);
    }

    @Test
    public void resetPassword_passwordsDoNotMatch_throwsPasswordsDoNotMatch() {
        // Arrange
        String token = "someToken";
        String password1 = "Password1";
        String password2 = "Password2";

        // Act & Assert
        assertThrows(PasswordsDoNotMatchException.class,
                () -> userService.resetPassword(token, password1, password2)
        );
    }

    @Test
    public void resetPassword_invalidToken_throwsInvalidToken() {
        // Arrange
        String invalidToken = "invalidToken";
        when(passwordResetTokenRepository.findByToken(invalidToken))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidTokenException.class,
                () -> userService.resetPassword(invalidToken, "Password1", "Password1")
        );
    }

    @Test
    public void resetPassword_expiredToken_throwsExpiredToken() {
        // Arrange
        String expiredTokenStr = "expiredToken";
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setToken(expiredTokenStr);
        expiredToken.setExpireAt(LocalDateTime.now().minusSeconds(1));
        LocalUser user = new LocalUser();
        expiredToken.setLocalUser(user);
        when(passwordResetTokenRepository.findByToken(expiredTokenStr))
                .thenReturn(Optional.of(expiredToken));

        // Act & Assert
        assertThrows(TokenExpiredException.class,
                () -> userService.resetPassword(expiredTokenStr, "Password1", "Password1")
        );
    }

    @Test
    public void resetPassword_validTokenAndMatchingPasswords_updatesPasswordAndDeletesToken() {
        // Arrange
        String validTokenStr = "validToken";
        LocalUser user = new LocalUser();
        user.setUsername("testUser");
        user.setEmail("test@example.com");
        user.setPassword("oldPassword");
        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setToken(validTokenStr);
        validToken.setExpireAt(LocalDateTime.now().plusSeconds(900));
        validToken.setLocalUser(user);
        when(passwordResetTokenRepository.findByToken(validTokenStr))
                .thenReturn(Optional.of(validToken));

        String newPassword = "newPassword";
        String hashedPassword = "hashedNewPassword";
        when(encryptionService.encryptPassword(newPassword))
                .thenReturn(hashedPassword);

        // Act
        userService.resetPassword(validTokenStr, newPassword, newPassword);

        // Assert
        assertEquals(hashedPassword, user.getPassword());
        assertNull(user.getPasswordResetToken());
        verify(localUserRepository).save(user);
        verify(passwordResetTokenRepository).delete(validToken);
    }
}