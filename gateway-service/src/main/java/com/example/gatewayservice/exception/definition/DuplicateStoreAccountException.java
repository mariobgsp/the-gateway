package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class DuplicateStoreAccountException extends CommonException {
    public DuplicateStoreAccountException(String errorMessage) {
        super(HttpStatus.CONFLICT, "032", errorMessage, errorMessage);
    }
}
