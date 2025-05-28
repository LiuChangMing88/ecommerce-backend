package com.github.liuchangming88.ecommerce_backend.util;

import com.github.liuchangming88.ecommerce_backend.api.model.LoginRequest;
import com.github.liuchangming88.ecommerce_backend.api.model.RegistrationRequest;
import com.github.liuchangming88.ecommerce_backend.model.Address;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;

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

    static public LoginRequest createUserDLoginRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("usernameD");
        loginRequest.setPassword("PasswordD123");
        return loginRequest;
    }

    static public Address createTestAddressA(LocalUser localUser) {
        Address address = new Address();
        address.setId(101L);
        address.setAddressLine1("123 Tester Hill");
        address.setAddressLine2("Apt 4B");
        address.setCity("Testerton");
        address.setCountry("England");
        address.setLocalUser(localUser);
        return address;
    }

    static public Address createTestAddressB(LocalUser localUser) {
        Address address = new Address();
        address.setId(102L);
        address.setAddressLine1("312 Spring Boot");
        address.setAddressLine2("Suite 5C");
        address.setCity("Hibernate");
        address.setCountry("USA");
        address.setLocalUser(localUser);
        return address;
    }
}
