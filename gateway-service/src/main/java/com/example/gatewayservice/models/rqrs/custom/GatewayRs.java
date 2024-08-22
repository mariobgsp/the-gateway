package com.example.gatewayservice.models.rqrs.custom;

import lombok.Data;

@Data
public class GatewayRs {
    private Long id;
    private String apiName;
    private String apiIdentifier;
    private String method;
    private String status;

}
