package com.example.gatewayservice.controller;

import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.rqrs.request.PublishRq;
import com.example.gatewayservice.service.ApiGatewayServices;
import com.example.gatewayservice.util.CommonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/gateway/f")
public class GatewayController {

    @Autowired
    private ApiGatewayServices apiGatewayServices;

    @RequestMapping(value = "/{path}", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> forwardApi(
            @RequestHeader HttpHeaders httpHeaders,
            @PathVariable String path, // path should be encoded to get entire request as example `?` as `%3F`
            @RequestBody(required = false) Object requestBody){

        Map<String, Object> processedRq = CommonUtil.processRequest(path, httpHeaders, requestBody);
        Response<Object> rs = apiGatewayServices.processForwardApi(processedRq);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/token", method = RequestMethod.POST)
    public ResponseEntity<?> generateToken(
            @RequestHeader(name="Authorization") String accessToken){

        // it will receive Bearer secretKey:ClientId SHA-256


        Response<Object> rs = apiGatewayServices.generateToken(accessToken);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

}
