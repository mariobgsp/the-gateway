package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class StoreAccountInvalidCredentials extends CommonException {

    public StoreAccountInvalidCredentials(String errorMessage) {
        super(HttpStatus.UNAUTHORIZED, "021", errorMessage, errorMessage);
    }
}
