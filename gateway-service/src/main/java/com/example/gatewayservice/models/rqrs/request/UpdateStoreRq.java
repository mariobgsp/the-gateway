package com.example.gatewayservice.models.rqrs.request;

public class UpdateStoreRq {

    private Long storeId;
    private String status;

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
