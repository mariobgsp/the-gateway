package com.example.gatewayservice.service;

import com.example.gatewayservice.exception.definition.ApiGatewayNotFoundException;
import com.example.gatewayservice.exception.definition.InvalidRequestException;
import com.example.gatewayservice.models.entity.ApiGateway;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.rqrs.SaveApiRequest;
import com.example.gatewayservice.models.rqrs.custom.GatewayRs;
import com.example.gatewayservice.repository.ApiGatewayRepository;
import com.example.gatewayservice.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class ApiGatewayServices {

    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH");

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
            Map<String, Object> requestParam = (Map<String, Object>) path.get("requestParam");
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
            List<String> httpHeadersConfig = splitConfig(apiGateway.getHeader());
            if (httpHeadrs != null) {
                for (Map.Entry<String, Object> entry : httpHeadrs.entrySet()) {
                    if (httpHeadersConfig.contains(entry.getKey())) {
                        httpHeaders.put(entry.getKey(), Collections.singletonList(entry.getValue().toString()));
                    }
                }
            }

            // check by configuration
            if(apiGateway.getRequireRequestParam() != null && apiGateway.getRequireRequestParam()){
                if (requestParam == null || requestParam.isEmpty()) {
                    throw new InvalidRequestException("Bad request: emptyRequestParam");
                }
                StringBuilder query = new StringBuilder();
                List<String> paramConfig = splitConfig(apiGateway.getParam());
                for (Map.Entry<String, Object> entry : requestParam.entrySet()) {
                    if (paramConfig.contains(entry.getKey())) {
                        if (query.length() > 0) query.append("&");
                        query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                                .append("=")
                                .append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
                    }
                }
                if (query.length() > 0) {
                    url.append(url.toString().contains("?") ? "&" : "?");
                    url.append(query);
                }
            }

            if(apiGateway.getRequireRequestBody() != null && apiGateway.getRequireRequestBody() && requestBody == null){
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
            CommonUtil.applyError(rs, e);
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
                gatewayRs.setApiPath(api.getApiPath());
                gatewayRs.setMethod(api.getMethod());
                gatewayRs.setStatus(api.getStatus());

                newList.add(gatewayRs);
            }
            rs.setSuccess(newList);
        }catch (Exception e){
            log.error("error getListGateways", e);
            CommonUtil.applyError(rs, e);
        }
        return rs;
    }

    public Response<Object> getApiDetailed(String apiIdentifier){
        Response<Object> rs = new Response<>();

        try{
            if (apiIdentifier == null || apiIdentifier.isBlank()) {
                throw new InvalidRequestException("api_identifier is required");
            }
            Optional<ApiGateway> api = apiGatewayRepository.findByApiIdentifier(apiIdentifier);
            if(api.isEmpty()){
                throw new ApiGatewayNotFoundException("api not found!");
            }

            rs.setSuccess(api.get());
        }catch (Exception e){
            log.error("error getApiDetailed", e);
            CommonUtil.applyError(rs, e);
        }

        return rs;
    }

    @Transactional
    public Response<Object> saveApi(SaveApiRequest request){
        Response<Object> rs = new Response<>();
        try{
            validateSaveApi(request);

            Optional<ApiGateway> existing = apiGatewayRepository.findByApiIdentifier(request.getApiIdentifier());
            ApiGateway apiGateway;
            if (existing.isPresent()) {
                apiGateway = existing.get();
                apiGateway.setApiName(request.getName());
                apiGateway.setApiHost(request.getHost());
                apiGateway.setApiPath(request.getPath());
                apiGateway.setMethod(request.getMethod());
                apiGateway.setStatus(request.getStatus());
                apiGateway.setHeader(request.getHeader());
                apiGateway.setRequireRequestBody(request.getRequireRequestBody());
                apiGateway.setRequireRequestParam(request.getRequireRequestParam());
                apiGateway.setParam(request.getParam());
                apiGateway.setUpdatedAt(LocalDateTime.now());
            } else {
                apiGateway = new ApiGateway();
                apiGateway.setApiIdentifier(request.getApiIdentifier());
                apiGateway.setApiName(request.getName());
                apiGateway.setApiHost(request.getHost());
                apiGateway.setApiPath(request.getPath());
                apiGateway.setMethod(request.getMethod());
                apiGateway.setStatus(request.getStatus() != null ? request.getStatus() : "created");
                apiGateway.setHeader(request.getHeader());
                apiGateway.setRequireRequestBody(request.getRequireRequestBody());
                apiGateway.setRequireRequestParam(request.getRequireRequestParam());
                apiGateway.setParam(request.getParam());
                apiGateway.setCreatedAt(LocalDateTime.now());
                apiGateway.setUpdatedAt(LocalDateTime.now());
            }

            apiGatewayRepository.save(apiGateway);
            rs.setSuccessMessage("api saved successfully!");
        }catch (Exception e){
            log.error("error saveApi", e);
            CommonUtil.applyError(rs, e);
        }
        return rs;
    }

    @Transactional
    public Response<Object> deleteApi(String apiIdentifier){
        Response<Object> rs = new Response<>();
        try{
            if (apiIdentifier == null || apiIdentifier.isBlank()) {
                throw new InvalidRequestException("api_identifier is required");
            }
            if (apiGatewayRepository.findByApiIdentifier(apiIdentifier).isEmpty()) {
                throw new ApiGatewayNotFoundException("api not found!");
            }
            apiGatewayRepository.deleteByApiIdentifier(apiIdentifier);
            rs.setSuccessMessage("api deleted successfully!");
        }catch (Exception e){
            log.error("error deleteApi", e);
            CommonUtil.applyError(rs, e);
        }
        return rs;
    }

    private void validateSaveApi(SaveApiRequest request) throws InvalidRequestException {
        if (request == null) throw new InvalidRequestException("request body is required");
        if (!StringUtils.hasText(request.getApiIdentifier())) throw new InvalidRequestException("api_identifier is required");
        if (!StringUtils.hasText(request.getName())) throw new InvalidRequestException("api name is required");
        if (!StringUtils.hasText(request.getHost())) throw new InvalidRequestException("api host is required");
        if (!StringUtils.hasText(request.getPath())) throw new InvalidRequestException("api path is required");
        if (!StringUtils.hasText(request.getMethod())) throw new InvalidRequestException("api method is required");

        if (request.getApiIdentifier().length() > 255 || request.getName().length() > 255
                || request.getHost().length() > 255 || request.getPath().length() > 255) {
            throw new InvalidRequestException("field length exceeds maximum allowed");
        }

        if (!ALLOWED_METHODS.contains(request.getMethod().toUpperCase())) {
            throw new InvalidRequestException("unsupported http method");
        }
        if (!request.getPath().startsWith("/")) {
            throw new InvalidRequestException("api path must start with /");
        }

        String normalizedHost = request.getHost().toLowerCase();
        boolean validScheme = normalizedHost.startsWith("https://") || normalizedHost.startsWith("http://");
        boolean hasCredentials = normalizedHost.contains("@");
        if (!validScheme || hasCredentials) {
            throw new InvalidRequestException("api host must be a valid http(s) url without embedded credentials");
        }
    }

    private List<String> splitConfig(String config) {
        if (config == null || config.isBlank()) return Collections.emptyList();
        return Arrays.stream(config.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
