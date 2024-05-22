package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends CommonException {
    public UserNotFoundException(String errorMessage) {
        super(HttpStatus.NOT_FOUND , "01" , errorMessage , errorMessage);
    }
}
