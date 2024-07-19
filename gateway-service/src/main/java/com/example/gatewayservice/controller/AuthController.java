package com.example.gatewayservice.controller;

import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.user.UserLoginRq;
import com.example.gatewayservice.service.security.AuthUserService;
import com.example.gatewayservice.service.security.CustomUserDetailsService;
import com.example.gatewayservice.service.security.TokenBlacklistService;
import com.example.gatewayservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class AuthController {

    @Autowired
    private AuthUserService authUserService;

    @PostMapping("/user/login")
    public ResponseEntity<?> authenticateUser(@RequestBody UserLoginRq request) {
        Response<Object> rs = authUserService.authLogin(request);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @PostMapping("/user/logout")
    public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String authorizationHeader) {
        Response<Object> rs = authUserService.authLogout(authorizationHeader);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @GetMapping("/user/testAuth")
    public ResponseEntity<?>  testAuth() {
        Response<Object> rs = new Response<>();
        rs.setSuccessMessage("success authentication!");
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }
}
