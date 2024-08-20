package com.example.gatewayservice.models.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserLoginRs {

    private String loginMessage;
    private String logoutMessage;
    private String accessToken;
    private Long tokenLifetime;

}
