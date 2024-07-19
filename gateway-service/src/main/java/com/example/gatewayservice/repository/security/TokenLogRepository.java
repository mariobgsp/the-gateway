package com.example.gatewayservice.repository.security;

import com.example.gatewayservice.models.entity.TokenLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenLogRepository extends JpaRepository<TokenLog, Long> {

    TokenLog findByToken(String token);

}
