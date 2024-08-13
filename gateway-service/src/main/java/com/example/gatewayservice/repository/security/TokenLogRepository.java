package com.example.gatewayservice.repository.security;

import com.example.gatewayservice.models.entity.TokenLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenLogRepository extends JpaRepository<TokenLog, Long> {

    TokenLog findByToken(String token);

}
