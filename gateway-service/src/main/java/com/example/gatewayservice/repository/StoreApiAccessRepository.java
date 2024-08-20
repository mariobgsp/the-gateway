package com.example.gatewayservice.repository;

import com.example.gatewayservice.models.entity.ApiGateway;
import com.example.gatewayservice.models.entity.StoreApiAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreApiAccessRepository extends JpaRepository<StoreApiAccess, Long> {

    List<StoreApiAccess> findByApiGateway(ApiGateway apiGateway);

}
