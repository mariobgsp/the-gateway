package com.example.gatewayservice.models.entity;


import jakarta.persistence.*;

@Entity
@Table(name = "user_store_r", schema = "public")
public class UserStoreR {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long storeId;
}
