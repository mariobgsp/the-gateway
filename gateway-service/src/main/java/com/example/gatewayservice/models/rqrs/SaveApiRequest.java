package com.example.gatewayservice.models.rqrs;

import lombok.Data;

@Data
public class SaveApiRequest {
    private String name;
    private String host;
    private String path;
    private String method;
}
