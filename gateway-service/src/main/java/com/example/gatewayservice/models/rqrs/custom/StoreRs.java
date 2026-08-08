package com.example.gatewayservice.models.rqrs.custom;

import lombok.Data;

@Data
public class StoreRs {
    private Long id;
    private String storeName;
    private String clientId;
    private String secretKey;
}
