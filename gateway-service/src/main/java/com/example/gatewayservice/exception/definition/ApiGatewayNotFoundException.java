package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class ApiGatewayNotFoundException extends CommonException {
    public ApiGatewayNotFoundException(String errorMessage) {
        super(HttpStatus.NOT_FOUND, "05", errorMessage, errorMessage);
    }
}
