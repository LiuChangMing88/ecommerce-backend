package com.github.liuchangming88.ecommerce_backend.util;

import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationRequest;

public class TestDataUtil {
    static public RegistrationRequest createTestRegisterRequest() {
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setUsername("testUser");
        registrationRequest.setPassword("PasswordTest123");
        registrationRequest.setEmail("testEmail@gmail.com");
        registrationRequest.setFirstName("testUser first name");
        registrationRequest.setLastName("testUser last name");
        return registrationRequest;
    }

    static public LoginRequest createTestLoginRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testUser");
        loginRequest.setPassword("PasswordTest123");
        return loginRequest;
    }

    static public RegistrationRequest createUserARegisterRequest() {
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setUsername("usernameA");
        registrationRequest.setPassword("PasswordA123");
        registrationRequest.setEmail("emailA@gmail.com");
        registrationRequest.setFirstName("userA first name");
        registrationRequest.setLastName("userA last name");
        return registrationRequest;
    }

    static public LoginRequest createUserALoginRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("usernameA");
        loginRequest.setPassword("PasswordA123");
        return loginRequest;
    }

    static public RegistrationRequest createUserBRegisterRequest() {
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setUsername("usernameB");
        registrationRequest.setPassword("PasswordB123");
        registrationRequest.setEmail("emailB@gmail.com");
        registrationRequest.setFirstName("userB first name");
        registrationRequest.setLastName("userB last name");
        return registrationRequest;
    }

    static public LoginRequest createUserBLoginRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("usernameB");
        loginRequest.setPassword("PasswordB123");
        return loginRequest;
    }
}
