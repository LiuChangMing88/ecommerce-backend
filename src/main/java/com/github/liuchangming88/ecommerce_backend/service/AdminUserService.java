package com.github.liuchangming88.ecommerce_backend.service;

import com.github.liuchangming88.ecommerce_backend.api.model.AddressResponse;
import com.github.liuchangming88.ecommerce_backend.api.model.AddressUpdateRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationResponse;
import com.github.liuchangming88.ecommerce_backend.exception.UserNotFoundException;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.model.repository.LocalUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminUserService {
    private final LocalUserRepository localUserRepository;
    private final UserService userService;

    public AdminUserService(LocalUserRepository localUserRepository, UserService userService) {
        this.localUserRepository = localUserRepository;
        this.userService = userService;
    }

    /**
     * Handles user query
     */

    private LocalUser findLocalUserById (Long userId) {
        Optional<LocalUser> optionalLocalUser = localUserRepository.findById(userId);
        if (optionalLocalUser.isEmpty())
            throw new UserNotFoundException("Can't find user with ID " + userId);
        return optionalLocalUser.get();
    }

    public RegistrationResponse getProfileById(Long userId) {
        LocalUser localUser = findLocalUserById(userId);
        return new RegistrationResponse(
                localUser.getUsername(),
                localUser.getEmail(),
                localUser.getFirstName(),
                localUser.getLastName(),
                ""
        );
    }

    public List<AddressResponse> getAddressesByUserId (Long userId) {
        LocalUser localUser = findLocalUserById(userId);
        return userService.getAddresses(localUser);
    }

    public AddressResponse addAddressToUserById (Long userId, AddressUpdateRequest addressUpdateRequest) {
        LocalUser localUser = findLocalUserById(userId);
        return userService.addAddressToUser(localUser, addressUpdateRequest);
    }

    public AddressResponse updateAddressByUserId (Long userId, Long addressId, AddressUpdateRequest addressUpdateRequest) {
        LocalUser localUser = findLocalUserById(userId);
        return userService.updateAddress(localUser, addressId, addressUpdateRequest);
    }

    public void deleteAddressByUserId (Long userId, Long addressId) {
        LocalUser localUser = findLocalUserById(userId);
        userService.deleteAddress(localUser, addressId);
    }
}
