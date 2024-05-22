package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class InvalidUserException extends CommonException {
    public InvalidUserException(String errorMessage) {
        super(HttpStatus.BAD_REQUEST , "02" , errorMessage , errorMessage);
    }
}
