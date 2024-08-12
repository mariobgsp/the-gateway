package com.example.gatewayservice.controller;

import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.rqrs.request.PublishRq;
import com.example.gatewayservice.service.ApiConfigService;
import com.example.gatewayservice.service.ApiGatewayServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/gateway/config")
public class GatewayConfigController {

    @Autowired
    private ApiConfigService apiConfigService;

    @RequestMapping(value = "/getApiList", method = RequestMethod.GET)
    public ResponseEntity<?> getApiList(){
        Response<Object> rs = apiConfigService.getListGateways();
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/getDetailedApi", method = RequestMethod.POST)
    public ResponseEntity<?> getDetailedApi(@RequestParam("api_identifier") String apiIdentifier){
        Response<Object> rs = apiConfigService.getApiDetailed(apiIdentifier);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    public ResponseEntity<?> publishApi(@RequestBody PublishRq publishRq){
        Response<Object> rs = apiConfigService.publishApi(publishRq);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }
}
