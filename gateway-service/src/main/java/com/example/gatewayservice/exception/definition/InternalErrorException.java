package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class InternalErrorException extends CommonException {
    public InternalErrorException(String errorMessage) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "99", "99:internalServerError:" + errorMessage, "99:internalServerError:" + errorMessage);
    }
}
