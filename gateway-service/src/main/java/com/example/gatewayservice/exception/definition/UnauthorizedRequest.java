package com.example.gatewayservice.exception.definition;

import com.example.gatewayservice.exception.models.CommonException;
import org.springframework.http.HttpStatus;

public class UnauthorizedRequest extends CommonException {
    public UnauthorizedRequest() {
        super(HttpStatus.UNAUTHORIZED, "02", "Unauthorized Request", "Unauthorized Request");
    }
}
