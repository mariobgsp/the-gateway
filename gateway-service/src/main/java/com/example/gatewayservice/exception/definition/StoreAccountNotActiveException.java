package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class StoreAccountNotActiveException extends CommonException {

    public StoreAccountNotActiveException(String errorMessage) {
        super(HttpStatus.CONFLICT, "033", errorMessage, errorMessage);
    }
}
