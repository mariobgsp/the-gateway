package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class DuplicateAddApiException extends CommonException {
    public DuplicateAddApiException(String errorMessage) {
        super(HttpStatus.CONFLICT, "031", errorMessage, errorMessage);
    }
}
