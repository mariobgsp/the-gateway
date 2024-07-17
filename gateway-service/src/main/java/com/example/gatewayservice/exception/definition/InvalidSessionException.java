package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class InvalidSessionException extends CommonException {
    public InvalidSessionException(String errorMessage) {
        super(HttpStatus.UNAUTHORIZED, "03" , errorMessage , errorMessage);
    }
}
