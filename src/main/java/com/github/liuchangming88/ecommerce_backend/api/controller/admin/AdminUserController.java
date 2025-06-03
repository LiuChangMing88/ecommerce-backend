package com.github.liuchangming88.ecommerce_backend.api.controller.admin;

import com.github.liuchangming88.ecommerce_backend.api.model.AddressResponse;
import com.github.liuchangming88.ecommerce_backend.api.model.AddressUpdateRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationResponse;
import com.github.liuchangming88.ecommerce_backend.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/admins/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping(path = "/{userId}/profile")
    public ResponseEntity<RegistrationResponse> getProfile (@PathVariable Long userId) {
        return new ResponseEntity<>(adminUserService.getProfileById(userId), HttpStatus.OK);
    }

    @GetMapping("/{userId}/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(@PathVariable Long userId) {
        return new ResponseEntity<>(adminUserService.getAddressesByUserId(userId), HttpStatus.OK);
    }

    @PostMapping("/{userId}/addresses")
    public ResponseEntity<AddressResponse> addAddress(
            @PathVariable Long userId,
            @Valid @RequestBody AddressUpdateRequest addressUpdateRequest) {
        return new ResponseEntity<>(adminUserService.addAddressToUserById(userId, addressUpdateRequest), HttpStatus.CREATED);
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressUpdateRequest addressUpdateRequest) {
        return new ResponseEntity<>(adminUserService.updateAddressByUserId(userId, addressId, addressUpdateRequest), HttpStatus.OK);
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        adminUserService.deleteAddressByUserId(userId, addressId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}