package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class PrivilegeException extends CommonException {
    public PrivilegeException(String errorMessage) {
        super(HttpStatus.UNAUTHORIZED, "023", errorMessage, errorMessage);
    }
}
