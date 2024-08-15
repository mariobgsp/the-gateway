package com.example.gatewayservice.models.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_access_log", schema = "public")
@Data
public class TokenAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String token;
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_account_id")
    private StoreAccount storeAccount;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
