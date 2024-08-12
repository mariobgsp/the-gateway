package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class InvalidRequestException extends CommonException {
    public InvalidRequestException(String errorMessage) {
        super(HttpStatus.BAD_REQUEST, "04", "04:BadRequest:"+errorMessage, "04:BadRequest:"+errorMessage);
    }
}
