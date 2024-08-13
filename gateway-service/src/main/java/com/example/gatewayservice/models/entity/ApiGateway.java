package com.example.gatewayservice.models.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_gateway", schema = "public")
public class ApiGateway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_name", length = 255)
    private String apiName;

    @Column(name = "api_identifier", length = 255)
    private String apiIdentifier;

    @Column(name = "api_host", length = 255)
    private String apiHost;

    @Column(name = "api_path", length = 255)
    private String apiPath;

    @Column(name = "status", length = 255)
    private String status;

    @Column(name = "method", length = 20)
    private String method;

    @Column(name = "header", length = 255)
    private String header;

    @Column(name = "require_request_body")
    private Boolean requireRequestBody;

    @Column(name = "require_request_param")
    private Boolean requireRequestParam;

    @Column(name = "param", length = 255)
    private String param;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public Boolean getRequireRequestBody() {
        return requireRequestBody;
    }

    public void setRequireRequestBody(Boolean requireRequestBody) {
        this.requireRequestBody = requireRequestBody;
    }

    public Boolean getRequireRequestParam() {
        return requireRequestParam;
    }

    public void setRequireRequestParam(Boolean requireRequestParam) {
        this.requireRequestParam = requireRequestParam;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
