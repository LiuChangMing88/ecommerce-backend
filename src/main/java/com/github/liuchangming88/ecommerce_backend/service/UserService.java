package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationRequest;
import com.github.liuchangming88.ecommerce_backend.exception.*;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationResponse;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.PasswordResetToken;
import com.github.liuchangming88.ecommerce_backend.model.VerificationToken;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.model.repository.PasswordResetTokenRepository;
import com.github.liuchangming88.ecommerce_backend.model.repository.VerificationTokenRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    private final LocalUserRepository localUserRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EncryptionService encryptionService;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;
    private final TokenService tokenService;
    private final EmailService emailService;

    public UserService(LocalUserRepository localUserRepository, EncryptionService encryptionService, JwtService jwtService,
                       ModelMapper modelMapper, TokenService tokenService, EmailService emailService,
                       VerificationTokenRepository verificationTokenRepository, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.localUserRepository = localUserRepository;
        this.encryptionService = encryptionService;
        this.jwtService = jwtService;
        this.modelMapper = modelMapper;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }


    public RegistrationResponse registerUser(RegistrationRequest registrationRequest) {
        // Check if the unique attributes already exist in the database
        if (localUserRepository.findByEmailIgnoreCase(registrationRequest.getEmail()).isPresent())
            throw new UserAlreadyExistsException("User with that email already exists");
        if (localUserRepository.findByUsernameIgnoreCase(registrationRequest.getUsername()).isPresent())
            throw new UserAlreadyExistsException("User with that username already exists");


        // Create a new local user to then save that local user
        LocalUser localUser = new LocalUser();
        localUser.setUsername(registrationRequest.getUsername());
        localUser.setEmail(registrationRequest.getEmail());
        localUser.setFirstName(registrationRequest.getFirstName());
        localUser.setLastName(registrationRequest.getLastName());

        // Encrypt password using Bcrypt
        localUser.setPassword(encryptionService.encryptPassword(registrationRequest.getPassword()));

        // Create the email verification token
        VerificationToken verificationToken = tokenService.createVerificationToken(localUser);

        // Send the email to the user to prompt verification
        emailService.sendVerificationEmail(verificationToken);

        // Save the token (the user is cascaded)
        localUser.setVerificationToken(verificationToken);
        verificationTokenRepository.save(verificationToken);

        RegistrationResponse registrationResponse = modelMapper.map(localUser, RegistrationResponse.class);
        registrationResponse.setMessage("We have sent you a verification message to your email! Please verify for future authentications.");
        return registrationResponse;
    }

    public String loginUser (LoginRequest loginBody) {
        // First check if the user is present in the database
        Optional<LocalUser> optionalLocalUser = localUserRepository.findByUsernameIgnoreCase(loginBody.getUsername());
        if (optionalLocalUser.isEmpty())
            throw new IncorrectUsernameException("The username you entered doesn't exist");

        // Extract user
        LocalUser localUser = optionalLocalUser.get();
        // Check if password is correct
        if (!encryptionService.verifyPassword(loginBody.getPassword(), localUser.getPassword()))
            throw new IncorrectPasswordException("The password you entered is incorrect");

        // If user exists and password is correct check if the user is email verified or not
        if (!localUser.getIsEmailVerified()) {
            // If not email verified, check the most recent verification email sent, resend if it's more than 24 hours (or if the user has no verification token)
            VerificationToken verificationToken = localUser.getVerificationToken();
            boolean resend = (verificationToken == null)
                    || verificationToken.getExpireAt().isBefore(LocalDateTime.now());
            if (resend) {
                // Create new verification token
                VerificationToken newVerificationToken = tokenService.createVerificationToken(localUser);
                emailService.sendVerificationEmail(newVerificationToken);
                localUser.setVerificationToken(newVerificationToken);
                verificationTokenRepository.save(newVerificationToken);
                throw new UserNotVerifiedException("You are not verified. We have sent you a new verification link, please check your email and verify your account!");
            }
            throw new UserNotVerifiedException("You are not verified, please check your email and verify your account!");
        }
        return jwtService.generateJwt(localUser);
    }

    public void verifyUser(String token) {
        // Retrieve verification token
        Optional<VerificationToken> opVerificationToken = verificationTokenRepository.findByToken(token);
        // If invalid token
        if (opVerificationToken.isEmpty())
            throw new InvalidTokenException("The token is invalid");

        VerificationToken verificationToken = opVerificationToken.get();

        // Check if already verified (for safety)
        if (verificationToken.getLocalUser().getIsEmailVerified()) {
            LocalUser localUser = verificationToken.getLocalUser();
            localUser.setVerificationToken(null);
            localUserRepository.save(localUser);
            verificationTokenRepository.delete(verificationToken);
            throw new UserAlreadyVerifiedException("You are already verified!");
        }

        // Check expiry
        if (verificationToken.getExpireAt().isBefore(LocalDateTime.now()))
            throw new TokenExpiredException("The token has expired");

        // Verify user
        LocalUser localUser = verificationToken.getLocalUser();
        localUser.setIsEmailVerified(true);
        localUser.setVerificationToken(null);
        localUserRepository.save(localUser);
        verificationTokenRepository.delete(verificationToken);
    }

    public void sendResetPasswordEmail(String email) {
        // Retrieve user
        Optional<LocalUser> retrievedUser = localUserRepository.findByEmailIgnoreCase(email);
        if (retrievedUser.isEmpty())
            return;

        LocalUser extractedUser = retrievedUser.get();
        // Create token
        PasswordResetToken passwordResetToken = tokenService.createPasswordResetToken(extractedUser);

        // Send email and save token
        emailService.sendPasswordResetEmail(passwordResetToken);
        passwordResetTokenRepository.save(passwordResetToken);
    }

    public PasswordResetToken validateResetToken(String token) {
        // Retrieve token
        Optional<PasswordResetToken> retrievedToken = passwordResetTokenRepository.findByToken(token);

        // Check validity
        if (retrievedToken.isEmpty())
            throw new InvalidTokenException("The token is invalid");
        PasswordResetToken extractedToken = retrievedToken.get();

        // Check expiry
        if (extractedToken.getExpireAt().isBefore(LocalDateTime.now()))
            throw new TokenExpiredException("The token has expired");
        return extractedToken;
    }

    public void resetPassword(String token, String newPassword, String confirmNewPassword) {
        // Validate
        if (!newPassword.equals(confirmNewPassword))
            throw new PasswordsDoNotMatchException("The passwords do not match");
        PasswordResetToken extractedToken = validateResetToken(token);

        // Reset password
        LocalUser extractedUser = extractedToken.getLocalUser();
        extractedUser.setPassword(encryptionService.encryptPassword(newPassword));

        // Delete token

        extractedUser.setPasswordResetToken(null);
        localUserRepository.save(extractedUser);
        passwordResetTokenRepository.delete(extractedToken);
    }
}
