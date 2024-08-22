package com.example.gatewayservice.repository;

import com.example.gatewayservice.models.entity.SystemProperties;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemPropertiesRepository extends JpaRepository<SystemProperties, Long> {

    Optional<SystemProperties> findByKey(String key);

    Optional<SystemProperties> findByKeyAndVgroup(String key, String vgroup);

    List<SystemProperties> findByVgroup(String vgroup);

}
