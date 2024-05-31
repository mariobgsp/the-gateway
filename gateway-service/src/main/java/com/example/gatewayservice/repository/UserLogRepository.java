package com.example.gatewayservice.repository;

import com.example.gatewayservice.models.entity.UserLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLogRepository extends JpaRepository<UserLog, Long> {

    Optional<UserLog> findByUserToken(String token);

    @Query("select ul from UserLog ul " +
            "left join fetch ul.user u " +
            "where userToken = :token ")
    Optional<UserLog> findUserLogByToken(String token);



}
