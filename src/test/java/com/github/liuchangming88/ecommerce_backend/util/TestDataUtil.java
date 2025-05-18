package com.github.liuchangming88.ecommerce_backend.util;

import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationRequest;

public class TestDataUtil {
    static public RegistrationRequest createTestRegisterRequest() {
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setUsername("usernameA");
        registrationRequest.setPassword("PasswordA123");
        registrationRequest.setEmail("emailA@gmail.com");
        registrationRequest.setFirstName("userA first name");
        registrationRequest.setLastName("userA last name");
        return registrationRequest;
    }

    static public LoginRequest createTestLoginRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("usernameA");
        loginRequest.setPassword("PasswordA123");
        return loginRequest;
    }
}
