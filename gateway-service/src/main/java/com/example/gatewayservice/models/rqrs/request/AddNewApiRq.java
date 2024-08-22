package com.example.gatewayservice.models.rqrs.request;

public class AddNewApiRq {

    private String apiName;
    private String apiIdentifier;
    private String apiHost;
    private String method;
    private String status;
    private String header;
    private String param;
    private Boolean requireRequestParam;
    private Boolean requireRequestBody;

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getApiIdentifier() {
        return apiIdentifier;
    }

    public void setApiIdentifier(String apiIdentifier) {
        this.apiIdentifier = apiIdentifier;
    }

    public String getApiHost() {
        return apiHost;
    }

    public void setApiHost(String apiHost) {
        this.apiHost = apiHost;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public Boolean getRequireRequestParam() {
        return requireRequestParam;
    }

    public void setRequireRequestParam(Boolean requireRequestParam) {
        this.requireRequestParam = requireRequestParam;
    }

    public Boolean getRequireRequestBody() {
        return requireRequestBody;
    }

    public void setRequireRequestBody(Boolean requireRequestBody) {
        this.requireRequestBody = requireRequestBody;
    }
}
