package com.example.gatewayservice.models.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "gateway", schema = "public")
public class Gateway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


}
