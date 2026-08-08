package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class StoreNotFoundException extends CommonException {
    public StoreNotFoundException(String errorMessage) {
        super(HttpStatus.NOT_FOUND, "03", "03:StoreNotFound:" + errorMessage, "03:StoreNotFound:" + errorMessage);
    }
}
