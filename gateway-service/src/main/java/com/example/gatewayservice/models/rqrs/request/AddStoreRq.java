package com.example.gatewayservice.models.rqrs.request;

public class AddStoreRq {

    private String storeName;
    private String storeCode;

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }
}
