package com.example.gatewayservice.models.rqrs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response<T> {

    @JsonIgnore
    private HttpStatus httpStatus;
    private String code;
    private String message;
    private T data;
    private String errorMessage;

    public void setSuccess(){
        this.httpStatus = HttpStatus.OK;
        this.code = "00";
        this.message = "success";
    }

    public void setSuccess(T data){
        this.httpStatus = HttpStatus.OK;
        this.code = "00";
        this.message = "success";
        this.data = data;
    }

    public void setSuccess(String message, T data){
        this.httpStatus = HttpStatus.OK;
        this.code = "00";
        this.message = message;
        this.data = data;
    }

    public void setSuccessAccepted(String message, T data){
        this.httpStatus = HttpStatus.ACCEPTED;
        this.code = "00";
        this.message = message;
        this.data = data;
    }

    public void setError(HttpStatus httpStatus, String code, String message){

        this.httpStatus = httpStatus!=null? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR;
        this.code = code !=null ? code : "99";
        this.message = message !=null ? message : "UnknownError";
    }

}
