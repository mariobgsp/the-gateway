package com.example.gatewayservice.controller;

import com.example.gatewayservice.models.user.UserLoginRq;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/gateway/api")
public class TestController {

    @GetMapping("/test")
    public ResponseEntity<?> testAPI(){
        return new ResponseEntity<>("API Ready",HttpStatus.OK);
    }

    @PostMapping("user/login")
    public ResponseEntity<?> login(
            @RequestBody UserLoginRq userLoginRq){

        Map<String, Object> rs = new LinkedHashMap<>();
        rs.put("username", userLoginRq.getUsername());
        rs.put("lastLoginSession", "21052024");
        rs.put("sessionStatus","ACTIVE");
        rs.put("userSession","thisIsSessionExample");
        rs.put("userToken","thisIsTokenExample");


        return new ResponseEntity<>(HttpStatus.OK);
    }
}
