package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class TokenInactiveException extends CommonException {
    public TokenInactiveException(String errorMessage) {
        super(HttpStatus.UNAUTHORIZED, "022", errorMessage, errorMessage);
    }
}
