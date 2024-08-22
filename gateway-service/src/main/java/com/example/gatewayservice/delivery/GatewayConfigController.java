package com.example.gatewayservice.delivery;

import com.example.gatewayservice.models.rqrs.request.*;
import com.example.gatewayservice.models.rqrs.response.Response;
import com.example.gatewayservice.service.ApiConfigServices;
import com.example.gatewayservice.service.security.AuthUserService;
import com.example.gatewayservice.util.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/config")
public class GatewayConfigController {

    @Autowired
    private ApiConfigServices apiConfigServices;
    @Autowired
    private AuthUserService authUserService;

    @RequestMapping(value = "/getApiList", method = RequestMethod.GET)
    public ResponseEntity<?> getApiList(HttpServletRequest httpServletRequest) {
        Response<Object> rs = new Response<>();
        // load username
        String username = authUserService.findUsernameUsingJwt(httpServletRequest.getHeader("Authorization"));
        if (username == null) {
            rs.setError(HttpStatus.UNAUTHORIZED, "failed", "02", "Unauthorized Request");
        }
        RequestInfo requestInfo = CommonUtil.constructRequestInfo(
                "getApiList",
                username,
                null,
                new Date(),
                httpServletRequest
        );

        rs = apiConfigServices.getListGateways(requestInfo);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/getDetailedApi", method = RequestMethod.POST)
    public ResponseEntity<?> getDetailedApi(@RequestBody Map<String, Object> request, HttpServletRequest httpServletRequest) {
        Response<Object> rs = new Response<>();
        // load username
        String username = authUserService.findUsernameUsingJwt(httpServletRequest.getHeader("Authorization"));
        RequestInfo requestInfo = CommonUtil.constructRequestInfo(
                "getDetailedApi",
                username,
                request.get("apiId"),
                new Date(),
                httpServletRequest
        );

        rs = apiConfigServices.getApiDetailed(requestInfo, (Long) request.get("apiId"));
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/api/update", method = RequestMethod.POST)
    public ResponseEntity<?> updateApi(@RequestBody PublishRq publishRq, HttpServletRequest httpServletRequest) {
        Response<Object> rs = new Response<>();
        // load username
        String username = authUserService.findUsernameUsingJwt(httpServletRequest.getHeader("Authorization"));
        RequestInfo requestInfo = CommonUtil.constructRequestInfo(
                "updateApi",
                username,
                publishRq,
                new Date(),
                httpServletRequest
        );

        rs = apiConfigServices.updateApi(requestInfo, publishRq);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/api/add", method = RequestMethod.POST)
    public ResponseEntity<?> addApiGateway(@RequestBody AddNewApiRq req, HttpServletRequest httpServletRequest) {
        Response<Object> rs = new Response<>();
        // load username
        String username = authUserService.findUsernameUsingJwt(httpServletRequest.getHeader("Authorization"));
        RequestInfo requestInfo = CommonUtil.constructRequestInfo(
                "addApiGateway",
                username,
                req,
                new Date(),
                httpServletRequest
        );

        rs = apiConfigServices.addNewApi(requestInfo, req);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/store/add", method = RequestMethod.POST)
    public ResponseEntity<?> addStore(@RequestBody AddStoreRq req, HttpServletRequest httpServletRequest) {
        Response<Object> rs = new Response<>();
        // load username
        String username = authUserService.findUsernameUsingJwt(httpServletRequest.getHeader("Authorization"));
        RequestInfo requestInfo = CommonUtil.constructRequestInfo(
                "addStore",
                username,
                req,
                new Date(),
                httpServletRequest
        );
        rs = apiConfigServices.addStore(requestInfo, req);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @RequestMapping(value = "/store/update", method = RequestMethod.POST)
    public ResponseEntity<?> updateStore(@RequestBody UpdateStoreRq rq, HttpServletRequest httpServletRequest) {
        Response<Object> rs = new Response<>();
        // load username
        String username = authUserService.findUsernameUsingJwt(httpServletRequest.getHeader("Authorization"));
        RequestInfo requestInfo = CommonUtil.constructRequestInfo(
                "updateStore",
                username,
                rq,
                new Date(),
                httpServletRequest
        );

        rs = apiConfigServices.updateStore(requestInfo, rq);
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }
}
