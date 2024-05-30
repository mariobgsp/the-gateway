package com.example.gatewayservice.controller;

import com.example.gatewayservice.exception.definition.InvalidSessionException;
import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.entity.UserLog;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.user.UserLoginRq;
import com.example.gatewayservice.repository.UserRepository;
import com.example.gatewayservice.service.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/gateway/api/user")
public class UserController {

    @Autowired
    private UserServices userServices;

    @GetMapping("/test")
    public ResponseEntity<?> testAPI(
            @RequestHeader(name = "token") String token){


        return new ResponseEntity<>("API Ready",HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody UserLoginRq userLoginRq){
        Response<Object> response = userServices.login(userLoginRq);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> changePassword(
            @RequestHeader(name = "token") String token,
            @RequestBody UserLoginRq userLoginRq){

        CompletableFuture.runAsync(()->userServices.logout(userLoginRq, token));
        Response<Object> rs = new Response<>();
        rs.setSuccess();
        return new ResponseEntity<>(rs, rs.getHttpStatus());

    }
}
