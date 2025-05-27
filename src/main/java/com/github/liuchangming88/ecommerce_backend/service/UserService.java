package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.api.model.*;
import com.github.liuchangming88.ecommerce_backend.exception.*;
import com.github.liuchangming88.ecommerce_backend.model.Address;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.PasswordResetToken;
import com.github.liuchangming88.ecommerce_backend.model.VerificationToken;
import com.github.liuchangming88.ecommerce_backend.model.repository.AddressRepository;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalUserRepository;
import com.github.liuchangming88.ecommerce_backend.model.repository.PasswordResetTokenRepository;
import com.github.liuchangming88.ecommerce_backend.model.repository.VerificationTokenRepository;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {
    private final LocalUserRepository localUserRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AddressRepository addressRepository;
    private final EncryptionService encryptionService;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;
    private final TokenService tokenService;
    private final EmailService emailService;

    public UserService(LocalUserRepository localUserRepository, EncryptionService encryptionService, JwtService jwtService,
                       ModelMapper modelMapper, TokenService tokenService, EmailService emailService,
                       VerificationTokenRepository verificationTokenRepository, PasswordResetTokenRepository passwordResetTokenRepository,
                       AddressRepository addressRepository) {
        this.localUserRepository = localUserRepository;
        this.encryptionService = encryptionService;
        this.jwtService = jwtService;
        this.modelMapper = modelMapper;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.addressRepository = addressRepository;
    }

    /**
     * Handles user registration and logging in
     */


    public RegistrationResponse registerUser(RegistrationRequest registrationRequest) {
        // Check if the unique attributes already exist in the database
        if (localUserRepository.findByEmailIgnoreCase(registrationRequest.getEmail()).isPresent())
            throw new DuplicatedUserException("User with that email already exists");
        if (localUserRepository.findByUsernameIgnoreCase(registrationRequest.getUsername()).isPresent())
            throw new DuplicatedUserException("User with that username already exists");


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

    /**
     * Handles user verification
     */

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

    /**
     * Handles password resetting
     */

    public void sendResetPasswordEmail(String email) {
        // Retrieve user
        Optional<LocalUser> retrievedUser = localUserRepository.findByEmailIgnoreCase(email);
        if (retrievedUser.isEmpty())
            return;

        LocalUser extractedUser = retrievedUser.get();
        // Check if the last token is expired (if it has, send a new token. If it hasn't expired, don't send)
        // If it hasn't expired return
        if (extractedUser.getPasswordResetToken() != null
            && extractedUser.getPasswordResetToken().getExpireAt().isAfter(LocalDateTime.now()))
            return;

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

    /**
     * Handles address crud
     */

    // To check authorization (for now, an address can only be modified by the owner of that address)
    private boolean addressCrudPermissionCheck (Long userId, Address address) {
        return Objects.equals(address.getLocalUser().getId(), userId);
    }

    public List<AddressResponse> getAddresses(LocalUser localUser) {
        List<Address> addressList = addressRepository.findByLocalUser_Id(localUser.getId());
        return addressList.stream().map(
                address -> modelMapper.map(address, AddressResponse.class)
        ).toList();
    }

    public AddressResponse addAddressToUser(LocalUser localUser, AddressUpdateRequest addressUpdateRequest) {
        // Check if the address already exists for that user
        if (addressRepository.existsByLocalUserAndAddressLine1AndAddressLine2AndCityAndCountry(localUser,
                addressUpdateRequest.getAddressLine1(),
                addressUpdateRequest.getAddressLine2(),
                addressUpdateRequest.getCity(),
                addressUpdateRequest.getCountry()))
            throw new DuplicatedAddressException("You already have that address registered");

        Address address = modelMapper.map(addressUpdateRequest, Address.class);
        address.setLocalUser(localUser);
        Address savedAddress = addressRepository.save(address);
        return modelMapper.map(savedAddress, AddressResponse.class);
    }

    public AddressResponse updateAddress(LocalUser localUser, Long addressId, AddressUpdateRequest addressUpdateRequest) {
        // Check if address exists
        Optional<Address> retrievedAddress = addressRepository.findById(addressId);
        if (retrievedAddress.isEmpty())
            throw new AddressNotFoundException("Address with ID " + addressId + " not found");

        Address extractedAddress = retrievedAddress.get();
        // Check authorization
        if (!addressCrudPermissionCheck(localUser.getId(), extractedAddress))
            throw new AccessDeniedException("You don't have the permission to change the address with ID" + addressId);

        // Check if the address already exists for that user
        if (addressRepository.existsByLocalUserAndAddressLine1AndAddressLine2AndCityAndCountry(localUser,
                addressUpdateRequest.getAddressLine1(),
                addressUpdateRequest.getAddressLine2(),
                addressUpdateRequest.getCity(),
                addressUpdateRequest.getCountry()))
            throw new DuplicatedAddressException("You already have that address registered");

        // Update the 4 fields: addressLine1, addressLine2, city, country
        extractedAddress.setAddressLine1(addressUpdateRequest.getAddressLine1());
        extractedAddress.setAddressLine2(addressUpdateRequest.getAddressLine2());
        extractedAddress.setCity(addressUpdateRequest.getCity());
        extractedAddress.setCountry(addressUpdateRequest.getCountry());
        return modelMapper.map(addressRepository.save(extractedAddress), AddressResponse.class);
    }

    public void deleteAddress(LocalUser localUser, Long addressId) {
        // Check if address exists
        Optional<Address> retrievedAddress = addressRepository.findById(addressId);
        if (retrievedAddress.isEmpty())
            throw new AddressNotFoundException("Address with ID " + addressId + " not found");

        Address extractedAddress = retrievedAddress.get();
        // Check authorization
        if (!addressCrudPermissionCheck(localUser.getId(), extractedAddress))
            throw new AccessDeniedException("You don't have the permission to change the address with ID " + addressId);

        // Delete
        addressRepository.deleteById(addressId);
    }
}
