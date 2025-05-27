package com.github.liuchangming88.ecommerce_backend.model.repository;

import com.github.liuchangming88.ecommerce_backend.model.Address;
import com.github.liuchangming88.ecommerce_backend.model.LocalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByLocalUser_Id(Long id);

    boolean existsByLocalUserAndAddressLine1AndAddressLine2AndCityAndCountry(LocalUser localUser, String addressLine1, String addressLine2, String city, String country);
}
