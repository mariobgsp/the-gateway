package com.example.gatewayservice.delivery;

import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.rqrs.request.PublishRq;
import com.example.gatewayservice.service.ApiConfigServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/gateway/config")
public class GatewayConfigController {

    @Autowired
    private ApiConfigServices apiConfigServices;

    @RequestMapping(value = "/getApiList", method = RequestMethod.GET)
    public ResponseEntity<?> getApiList() {
        Response<Object> rs = apiConfigServices.getListGateways();
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/getDetailedApi", method = RequestMethod.POST)
    public ResponseEntity<?> getDetailedApi(@RequestParam("api_identifier") String apiIdentifier) {
        Response<Object> rs = apiConfigServices.getApiDetailed(apiIdentifier);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public ResponseEntity<?> updateApi(@RequestBody PublishRq publishRq) {
        Response<Object> rs = apiConfigServices.updateApi(publishRq);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }
}
