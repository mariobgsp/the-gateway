package com.example.gatewayservice.models.entity;


import jakarta.persistence.*;

@Entity
@Table(name = "store_account", schema = "public")
public class StoreAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String storeName;
    private String clientId;
    private String secretKey;
}
