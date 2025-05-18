package com.github.liuchangming88.ecommerce_backend.model.repository;

import com.github.liuchangming88.ecommerce_backend.model.LocalOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface LocalOrderRepository extends JpaRepository<LocalOrder, Long> {

    List<LocalOrder> findByLocalUser_Id(Long id);
}
