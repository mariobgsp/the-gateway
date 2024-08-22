package com.example.gatewayservice.models.rqrs.request;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;

import java.util.Date;

public class RequestInfo {

    private String operationName;
    private String username;
    private Object request;
    private Date requestAt;
    private HttpServletRequest httpServletRequest;

    public RequestInfo(String operationName, String username, Object request, Date requestAt, HttpServletRequest httpServletRequest) {
        this.operationName = operationName;
        this.username = username;
        this.request = request;
        this.requestAt = requestAt;
        this.httpServletRequest = httpServletRequest;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Object getRequest() {
        return request;
    }

    public void setRequest(Object request) {
        this.request = request;
    }

    public Date getRequestAt() {
        return requestAt;
    }

    public void setRequestAt(Date requestAt) {
        this.requestAt = requestAt;
    }

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public void setHttpServletRequest(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }
}
