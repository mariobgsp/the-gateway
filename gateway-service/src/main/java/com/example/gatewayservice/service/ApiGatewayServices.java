package com.example.gatewayservice.service;

import com.example.gatewayservice.exception.definition.ApiGatewayNotFoundException;
import com.example.gatewayservice.exception.definition.InvalidRequestException;
import com.example.gatewayservice.models.entity.ApiGateway;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.rqrs.custom.GatewayRs;
import com.example.gatewayservice.repository.ApiGatewayRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ApiGatewayServices {

    @Autowired
    private ApiGatewayRepository apiGatewayRepository;
    @Autowired
    private HttpServices httpServices;

    public Response<Object> processForwardApi(Map<String, Object> request
    ){
        Response<Object> rs = new Response<>();
        try{
            Map path = (Map) request.get("path");
            String pathName = (String) path.get("pathName");
            Map<String, Object> httpHeadrs = (Map<String, Object>) request.get("httpHeaders");
            Map<String, Object> requestParam = (Map<String, Object>) request.get("requestParam");
            Object requestBody = (Object) request.get("requestBody");

            Optional<ApiGateway> ag = apiGatewayRepository.findByApiIdentifier(pathName);
            if(ag.isEmpty()){
                throw new ApiGatewayNotFoundException("API Gateway Configuration Not Found");
            }
            ApiGateway apiGateway = ag.get();

            // construct Url
            StringBuilder url = new StringBuilder(apiGateway.getApiHost() + apiGateway.getApiPath());

            // construct header
            HttpHeaders httpHeaders = new HttpHeaders();
            List<String> httpHeadersConfig = Arrays.asList(apiGateway.getHeader().split(";"));
            for (Map.Entry<String, Object> entry : httpHeadrs.entrySet()) {
                if(httpHeadersConfig.contains(entry.getKey())){
                    httpHeaders.put(entry.getKey(), Collections.singletonList(entry.getValue().toString()));
                }
            }

            // check by configuration
            if(apiGateway.getRequireRequestParam() && requestParam == null){
                throw new InvalidRequestException("Bad request: emptyRequestParam");
            }else if (apiGateway.getRequireRequestParam() && requestParam != null){
                url.append("?");

                List<String> paramConfig = Arrays.asList(apiGateway.getParam().split(";"));
                for (Map.Entry<String, Object> entry : requestParam.entrySet()) {
                    if(paramConfig.contains(entry.getKey())){
                        url.append(entry.getKey()).append("=").append(entry.getValue());
                    }
                }
            }

            if(apiGateway.getRequireRequestBody() && requestBody == null){
                throw new InvalidRequestException("Bad request: emptyRequestBody");
            }

            ResponseEntity<Object> response = httpServices.invokeUrl(
                    url.toString(),
                    HttpMethod.valueOf(apiGateway.getMethod()),
                    httpHeaders,
                    requestBody);

            rs.setSuccess(response.getBody());
        }catch (Exception e){
            log.error("error processForwardApi", e);
            Map error = (Map) e;
            if(error.get("errorCode")!=null){
                rs.setError((HttpStatus) error.get("httpStatus"), (String) error.get("httpStatus"), (String) error.get("errorCode"), (String) error.get("errorMessage"));
            }else{
                rs.setError(e.getMessage());
            }
        }
        return rs;
    }

    public Response<Object> getListGateways(){
        List<GatewayRs> newList = new ArrayList<>();
        Response<Object> rs = new Response<>();
        try{
            List<ApiGateway> list = apiGatewayRepository.findAll();
            for (ApiGateway api : list){
                GatewayRs gatewayRs = new GatewayRs();
                gatewayRs.setId(api.getId());
                gatewayRs.setApiName(api.getApiName());
                gatewayRs.setApiIdentifier(api.getApiIdentifier());
                gatewayRs.setStatus(api.getStatus());

                newList.add(gatewayRs);
            }
            rs.setSuccess(newList);
        }catch (Exception e){
            log.error("error processForwardApi", e);
            Map error = (Map) e;
            if(error.get("errorCode")!=null){
                rs.setError((HttpStatus) error.get("httpStatus"), (String) error.get("httpStatus"), (String) error.get("errorCode"), (String) error.get("errorMessage"));
            }else{
                rs.setError(e.getMessage());
            }
        }
        return rs;
    }

    public Response<Object> getApiDetailed(String apiIdentifier){
        Response<Object> rs = new Response<>();

        try{
            Optional<ApiGateway> api = apiGatewayRepository.findByApiIdentifier(apiIdentifier);
            if(api.isEmpty()){
                throw new ApiGatewayNotFoundException("api not found!");
            }

            rs.setSuccess(api);
        }catch (Exception e){
            log.error("error processForwardApi", e);
            Map error = (Map) e;
            if(error.get("errorCode")!=null){
                rs.setError((HttpStatus) error.get("httpStatus"), (String) error.get("httpStatus"), (String) error.get("errorCode"), (String) error.get("errorMessage"));
            }else{
                rs.setError(e.getMessage());
            }
        }

        return rs;

    }

}
