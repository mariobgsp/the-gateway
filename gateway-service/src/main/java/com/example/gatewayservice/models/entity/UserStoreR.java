package com.example.gatewayservice.models.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "user_store_r", schema = "public")
@Data
public class UserStoreR {

    @EmbeddedId
    private UserStoreKey id;

    public UserStoreR() {}

    public UserStoreR(Long userId, Long storeId) {
        this.id = new UserStoreKey(userId, storeId);
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStoreKey implements Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "store_id")
        private Long storeId;
    }
}
