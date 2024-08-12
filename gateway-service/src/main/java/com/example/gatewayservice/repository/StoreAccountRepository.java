package com.example.gatewayservice.repository;

import com.example.gatewayservice.models.entity.StoreAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreAccountRepository extends JpaRepository<StoreAccount, Long> {

    Optional<StoreAccount> findByStoreCode(String storeCode);

}
