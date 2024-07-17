package com.example.gatewayservice.controller;

import com.example.gatewayservice.models.rqrs.Response;
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
@RequestMapping("/api/gateway")
public class GatewayController {

    @Autowired
    private ApiGatewayServices apiGatewayServices;

    @PostMapping("/forward/test1/{path}")
    public ResponseEntity<?> testGateway(
            @RequestHeader HttpHeaders httpHeaders,
            @PathVariable String path, // path should be encoded to get entire request as example `?` as `%3F`
            @RequestBody(required = false) Object requestBody){

        log.info("received request: {}", path);
        Map<String, Object> testResponse = new HashMap<>();
        testResponse.put("httpHeaders", httpHeaders);

        // process path
        String[] arrPath = path.split("\\?");

        String pathName = arrPath[0];
        String queryString = arrPath.length > 1 ? arrPath[1] : "";
        Map<String, Object> queryParams = CommonUtil.parseQueryString(queryString);

        Map<String, Object> pathMap = new HashMap<>();
        pathMap.put("pathName", pathName);
        pathMap.put("requestParam", queryParams);

        testResponse.put("path", pathMap);

        testResponse.put("requestBody", requestBody);

        return new ResponseEntity<>(testResponse, HttpStatus.OK);

    }

    @GetMapping("/forward/test2/{path}")
    public ResponseEntity<?> testGateway2(
            @RequestHeader HttpHeaders httpHeaders,
            @PathVariable String path, // path should be encoded to get entire request as example `?` as `%3F`
            @RequestBody(required = false) Object requestBody){

        Map<String, Object> processedRq = CommonUtil.processRequest(path, httpHeaders, requestBody);
        Response<Object> rs = apiGatewayServices.processForwardApi(processedRq);
        return new ResponseEntity<>(rs, rs.getHttpStatus());


    }
}
