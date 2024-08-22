package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class StoreAccountNotFoundException extends CommonException {
    public StoreAccountNotFoundException(String errorMessage) {
        super(HttpStatus.NOT_FOUND, "013", errorMessage, errorMessage);
    }
}
