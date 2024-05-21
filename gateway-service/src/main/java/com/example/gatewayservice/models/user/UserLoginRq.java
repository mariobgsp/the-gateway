package com.example.gatewayservice.models.user;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
public class UserLoginRq {

    private String username;
    private String password;

}
