package com.example.gatewayservice.repository;

import com.example.gatewayservice.models.entity.TokenLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TokenLogRepository extends JpaRepository<TokenLog, Long> {

    List<TokenLog> findByStatus(String status);


}
