package com.example.gatewayservice.models.rqrs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.Expose;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response<T> {

    @JsonIgnore
    @Expose(serialize = false)
    private HttpStatus httpStatus;
    @Expose
    private String status;
    @Expose
    private String code;
    @Expose
    private String message;
    @Expose
    private T data;
    @Expose
    private String errorMessage;

    public void setSuccess() {
        this.httpStatus = HttpStatus.OK;
        this.status = "ok";
        this.code = "00";
        this.message = "success";
    }

    public void setSuccessMessage(String message) {
        this.httpStatus = HttpStatus.OK;
        this.status = "ok";
        this.code = "00";
        this.message = message;
    }

    public void setSuccess(T data) {
        this.httpStatus = HttpStatus.OK;
        this.status = "ok";
        this.code = "00";
        this.message = "success";
        this.data = data;
    }

    public void setSuccess(String message, T data) {
        this.httpStatus = HttpStatus.OK;
        this.status = "ok";
        this.code = "00";
        this.message = message;
        this.data = data;
    }

    public void setSuccessAccepted(String message, T data) {
        this.httpStatus = HttpStatus.ACCEPTED;
        this.status = "accepted";
        this.code = "00";
        this.message = message;
        this.data = data;
    }

    public void setSuccessAccepted() {
        this.httpStatus = HttpStatus.ACCEPTED;
        this.status = "accepted";
        this.code = "00";
        this.message = "request being process";
    }

    public void setError(HttpStatus httpStatus, String status, String code, String message) {

        this.httpStatus = httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR;
        this.status = status;
        this.code = code != null ? code : "99";
        this.message = message != null ? message : "UnknownError";
    }

    public void setError(String message) {

        this.httpStatus = httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR;
        this.status = status != null ? status : "unknownError";
        this.code = code != null ? code : "99";
        this.message = message != null ? message : "UnknownError";
    }

}
