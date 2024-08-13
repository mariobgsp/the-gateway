package com.example.gatewayservice.models.user;

import lombok.Data;

@Data
public class UserLoginRq {

    private String username;
    private String password;

}
