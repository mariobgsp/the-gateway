package com.example.gatewayservice.models.rqrs;

import lombok.Data;

@Data
public class SaveApiRequest {
    private String apiIdentifier;
    private String name;
    private String host;
    private String path;
    private String method;
    private String status;
    private String header;
    private Boolean requireRequestBody;
    private Boolean requireRequestParam;
    private String param;
}
