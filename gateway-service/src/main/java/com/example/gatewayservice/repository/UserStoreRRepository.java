package com.example.gatewayservice.repository;

import com.example.gatewayservice.models.entity.UserStoreR;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStoreRRepository extends JpaRepository<UserStoreR, UserStoreR.UserStoreKey> {

    List<UserStoreR> findByIdUserId(Long userId);

    boolean existsByIdUserIdAndIdStoreId(Long userId, Long storeId);

    Optional<UserStoreR> findByIdUserIdAndIdStoreId(Long userId, Long storeId);

    void deleteByIdStoreId(Long storeId);
}
