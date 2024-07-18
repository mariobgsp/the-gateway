package com.example.gatewayservice.repository;

import com.example.gatewayservice.models.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findAll();

    @Query("select u from User u " +
            "left join fetch u.role " +
            "where username = :username ")
    Optional<User> findDetailedByUsername(String username);

    @Query("select u from User u " +
            "left join fetch u.role " +
            "where username = :username ")
    User findDetailedByUsernameV2(String username);
}
