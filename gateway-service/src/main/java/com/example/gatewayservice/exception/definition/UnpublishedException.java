package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class UnpublishedException extends CommonException {

    public UnpublishedException(String errorMessage) {
        super(HttpStatus.BAD_REQUEST, "041", errorMessage, errorMessage);
    }
}