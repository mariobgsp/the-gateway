package com.example.gatewayservice.controller;

import com.example.gatewayservice.models.rqrs.ForwardRequest;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.rqrs.SaveApiRequest;
import com.example.gatewayservice.service.ApiGatewayServices;
import com.example.gatewayservice.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    @Autowired
    private ApiGatewayServices apiGatewayServices;

    @RequestMapping(value = "/{path}", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> forwardApi(
            @RequestHeader HttpHeaders httpHeaders,
            @PathVariable String path, // path should be encoded to get entire request as example `?` as `%3F`
            @RequestBody(required = false) Object requestBody){

        ForwardRequest req = CommonUtil.toForwardRequest(path, httpHeaders, requestBody);
        Response<Object> rs = apiGatewayServices.processForwardApi(req);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/getApiList", method = RequestMethod.GET)
    public ResponseEntity<?> getApiList(){
        Response<Object> rs = apiGatewayServices.getListGateways();
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/getDetailedApi", method = RequestMethod.POST)
    public ResponseEntity<?> getDetailedApi(@RequestParam("api_identifier") String apiIdentifier){
        Response<Object> rs = apiGatewayServices.getApiDetailed(apiIdentifier);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/saveApi", method = RequestMethod.POST)
    public ResponseEntity<?> saveApi(@RequestBody SaveApiRequest request){
        Response<Object> rs = apiGatewayServices.saveApi(request);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/deleteApi", method = RequestMethod.POST)
    public ResponseEntity<?> deleteApi(@RequestParam("api_identifier") String apiIdentifier){
        Response<Object> rs = apiGatewayServices.deleteApi(apiIdentifier);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }
}
