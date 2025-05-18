package com.github.liuchangming88.ecommerce_backend.api.controller.auth;

import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.LoginResponse;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationResponse;
import com.github.liuchangming88.ecommerce_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/auth")
public class AuthenticationController {
    private final UserService userService;

    public AuthenticationController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(path = "/register")
    public ResponseEntity<RegistrationResponse> registerUser (@Valid @RequestBody RegistrationRequest registrationRequest){
        // Register the user and save to the database (Logic is done in service layer)
        RegistrationResponse registrationResponse = userService.registerUser(registrationRequest);
        return new ResponseEntity<>(registrationResponse, HttpStatus.OK);
    }

    @PostMapping(path = "/login")
    public ResponseEntity<LoginResponse> loginUser (@Valid @RequestBody LoginRequest loginBody) {
        // Let the client log in and grant them a jwt access token
        String jwtToken = userService.loginUser(loginBody);
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setJwt(jwtToken);
        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }

    @GetMapping(path = "/verify")
    public ResponseEntity<String> verifyUser (@RequestParam String token) {
        userService.verifyUser(token);
        return new ResponseEntity<>("User has been verified!", HttpStatus.OK);
    }
}