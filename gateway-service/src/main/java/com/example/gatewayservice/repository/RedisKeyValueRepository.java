package com.example.gatewayservice.repository;

import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface RedisKeyValueRepository extends KeyValueRepository<String, Object> {
}
