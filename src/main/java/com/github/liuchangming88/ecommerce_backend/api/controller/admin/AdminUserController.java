package com.github.liuchangming88.ecommerce_backend.api.controller.admin;

import com.github.liuchangming88.ecommerce_backend.api.model.AddressResponse;
import com.github.liuchangming88.ecommerce_backend.api.model.AddressUpdateRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationResponse;
import com.github.liuchangming88.ecommerce_backend.service.AdminService;
import com.github.liuchangming88.ecommerce_backend.service.UserService;
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
    private final AdminService adminService;

    public AdminUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping(path = "/{userId}/profile")
    public ResponseEntity<RegistrationResponse> getProfile (@PathVariable Long userId) {
        return new ResponseEntity<>(adminService.getProfileById(userId), HttpStatus.OK);
    }

    @GetMapping("/{userId}/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(@PathVariable Long userId) {
        return new ResponseEntity<>(adminService.getAddressesByUserId(userId), HttpStatus.OK);
    }

    @PostMapping("/{userId}/addresses")
    public ResponseEntity<AddressResponse> addAddress(
            @PathVariable Long userId,
            @Valid @RequestBody AddressUpdateRequest addressUpdateRequest) {
        return new ResponseEntity<>(adminService.addAddressToUserById(userId, addressUpdateRequest), HttpStatus.CREATED);
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressUpdateRequest addressUpdateRequest) {
        return new ResponseEntity<>(adminService.updateAddressByUserId(userId, addressId, addressUpdateRequest), HttpStatus.OK);
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        adminService.deleteAddressByUserId(userId, addressId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}