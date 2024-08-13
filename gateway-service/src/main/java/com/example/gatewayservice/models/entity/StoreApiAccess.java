package com.example.gatewayservice.models.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "store_api_access", schema = "public")
@NoArgsConstructor
@AllArgsConstructor
public class StoreApiAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_account_id")
    private StoreAccount storeAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gateway_id")
    private ApiGateway apiGateway;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StoreAccount getStoreAccount() {
        return storeAccount;
    }

    public void setStoreAccount(StoreAccount storeAccount) {
        this.storeAccount = storeAccount;
    }

    public ApiGateway getApiGateway() {
        return apiGateway;
    }

    public void setApiGateway(ApiGateway apiGateway) {
        this.apiGateway = apiGateway;
    }
}
