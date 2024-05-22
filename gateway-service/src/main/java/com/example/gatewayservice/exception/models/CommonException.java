package com.example.gatewayservice.exception.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class CommonException extends Exception{

    private HttpStatus httpStatus;
    private String errorCode;
    private String errorMessage;
    private String errorDisplayMessage;

    public CommonException(HttpStatus httpStatus, String errorCode, String errorMessage, String errorDisplayMessage) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorDisplayMessage = errorDisplayMessage;
    }

}
