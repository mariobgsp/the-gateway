package com.example.gatewayservice.controller;

import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.user.UserLoginRq;
import com.example.gatewayservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/gateway/api/user")
public class GatewayController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/test")
    public ResponseEntity<?> testAPI(){
        return new ResponseEntity<>("API Ready",HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody UserLoginRq userLoginRq){
        Response response = new Response();
        Map<String, Object> serviceRs = new LinkedHashMap<>();

        serviceRs.put("loginMessage", "login successful!");
        serviceRs.put("sessionStatus","ACTIVE");
        serviceRs.put("userSession","thisIsSessionExample");
        serviceRs.put("userToken","thisIsTokenExample");
        serviceRs.put("loginDate", "21052024");
        serviceRs.put("lastLoginSession", "21052024");
        serviceRs.put("loginSessionTime", 86400);

        response.setSuccess(serviceRs);
        return new ResponseEntity<>(response, response.getHttpStatus());
    }

    @GetMapping("/getAll")
    public ResponseEntity<?> login(){
        Response response = new Response();

        response.setSuccess(userRepository.findAll());
        return new ResponseEntity<>(response, response.getHttpStatus());

    }
}
