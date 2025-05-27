package com.github.liuchangming88.ecommerce_backend.api.controller.user;

import com.github.liuchangming88.ecommerce_backend.api.model.AddressResponse;
import com.github.liuchangming88.ecommerce_backend.api.model.AddressUpdateRequest;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import com.github.liuchangming88.ecommerce_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal LocalUser localUser) {
        return new ResponseEntity<>(userService.getAddresses(localUser), HttpStatus.OK);
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> addAddress(
            @AuthenticationPrincipal LocalUser localUser,
            @Valid @RequestBody AddressUpdateRequest addressUpdateRequest) {
        return new ResponseEntity<>(userService.addAddressToUser(localUser, addressUpdateRequest), HttpStatus.CREATED);
    }

    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @AuthenticationPrincipal LocalUser localUser,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressUpdateRequest addressUpdateRequest) {
        return new ResponseEntity<>(userService.updateAddress(localUser, addressId, addressUpdateRequest), HttpStatus.OK);
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal LocalUser localUser,
            @PathVariable Long addressId) {
        userService.deleteAddress(localUser, addressId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
