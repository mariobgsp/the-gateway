package com.example.gatewayservice.models.rqrs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {

    @JsonIgnore
    private HttpStatus httpStatus;
    private String code;
    private String message;
    private Object data;
    private String errorMessage;

    public void setSuccess(){
        this.httpStatus = HttpStatus.OK;
        this.code = "00";
        this.message = "success";
    }

    public void setSuccess(Object data){
        this.httpStatus = HttpStatus.OK;
        this.code = "00";
        this.message = "success";
        this.data = data;
    }

    public void setSuccess(String message, Object data){
        this.httpStatus = HttpStatus.OK;
        this.code = "00";
        this.message = message;
        this.data = data;
    }

    public void setSuccessAccepted(String message, Object data){
        this.httpStatus = HttpStatus.ACCEPTED;
        this.code = "00";
        this.message = message;
        this.data = data;
    }

}
