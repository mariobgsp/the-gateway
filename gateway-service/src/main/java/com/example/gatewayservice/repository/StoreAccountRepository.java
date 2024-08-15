package com.example.gatewayservice.repository;

import com.example.gatewayservice.models.entity.StoreAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreAccountRepository extends JpaRepository<StoreAccount, Long> {

    Optional<StoreAccount> findByStoreCode(String storeCode);

    Optional<StoreAccount> findByClientIdAndSecretKey(String clientId, String secretKey);
}
