package com.example.gatewayservice.models.rqrs;

import lombok.Data;

@Data
public class SaveStoreRequest {
    private Long storeId;
    private String storeName;
    private String clientId;
}
