package com.example.gatewayservice.repository;

import com.example.gatewayservice.models.entity.ApiGateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiGatewayRepository extends JpaRepository<ApiGateway, Long> {

    Optional<ApiGateway> findByApiIdentifier(String path);
}
