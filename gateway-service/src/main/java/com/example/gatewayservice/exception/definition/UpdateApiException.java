package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class UpdateApiException extends CommonException {
    public UpdateApiException(HttpStatus httpStatus, String errorCode, String errorMessage) {
        super(httpStatus, errorCode, errorMessage, errorMessage);
    }
}
