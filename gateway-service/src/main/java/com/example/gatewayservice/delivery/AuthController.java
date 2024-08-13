package com.example.gatewayservice.delivery;

import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.user.UserLoginRq;
import com.example.gatewayservice.service.security.AuthUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/gateway")
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
}
