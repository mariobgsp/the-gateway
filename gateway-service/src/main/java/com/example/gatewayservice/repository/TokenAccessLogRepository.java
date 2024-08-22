package com.example.gatewayservice.repository;

import com.example.gatewayservice.models.entity.TokenAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenAccessLogRepository extends JpaRepository<TokenAccessLog, Long> {

    List<TokenAccessLog> findByStatus(String status);

    Optional<TokenAccessLog> findByTokenAndStatus(String token, String status);


}
