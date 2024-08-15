package com.example.gatewayservice.service;

import com.example.gatewayservice.exception.definition.*;
import com.example.gatewayservice.exception.models.CommonException;
import com.example.gatewayservice.models.entity.*;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.repository.ApiGatewayRepository;
import com.example.gatewayservice.repository.StoreAccountRepository;
import com.example.gatewayservice.repository.TokenAccessLogRepository;
import com.example.gatewayservice.util.SecretKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ApiGatewayServices {

    @Autowired
    private ApiGatewayRepository apiGatewayRepository;
    @Autowired
    private HttpServices httpServices;
    @Autowired
    private StoreAccountRepository storeAccountRep;
    @Autowired
    private TokenAccessLogRepository tokenAccessLogRepository;
    @Autowired
    private SystemPropertiesServices systemPropertiesServices;

    public Response<Object> processForwardApi(Map<String, Object> request
    ) {
        Response<Object> rs = new Response<>();
        try {
            Map<String, Object> path = (Map<String, Object>) request.get("path");
            String pathName = (String) path.get("pathName");
            Map<String, Object> httpHeadrs = (Map<String, Object>) request.get("httpHeaders");
            Map<String, Object> requestParam = (Map<String, Object>) request.get("requestParam");
            Object requestBody = request.get("requestBody");

            //get props
            String statusActive = systemPropertiesServices.getProps("TOKEN_ACCESS_ACTIVE_STATUS");

            Optional<ApiGateway> ag = apiGatewayRepository.findByApiIdentifier(pathName);
            if (ag.isEmpty()) {
                throw new ApiGatewayNotFoundException("API Gateway Configuration Not Found");
            }
            ApiGateway apiGateway = ag.get();

            // check if status published
            if(apiGateway.getStatus().equals("PUBLISHED")){
                String token = ((String) httpHeadrs.get("Authorization")).replace("Bearer ", "");
                //check token log
                Optional<TokenAccessLog> accessLog = tokenAccessLogRepository.findByTokenAndStatus(token, statusActive);
                if(accessLog.isEmpty()){
                    throw new TokenInactiveException("inactive token access");
                }

                // valid flow start forwarding
                // construct Url
                StringBuilder url = new StringBuilder(apiGateway.getApiHost() + apiGateway.getApiPath());

                // construct header
                HttpHeaders httpHeaders = new HttpHeaders();
                List<String> httpHeadersConfig = Arrays.asList(apiGateway.getHeader().split(";"));
                for (Map.Entry<String, Object> entry : httpHeadrs.entrySet()) {
                    if (httpHeadersConfig.contains(entry.getKey())) {
                        httpHeaders.put(entry.getKey(), Collections.singletonList(entry.getValue().toString()));
                    }
                }

                // check by configuration
                if (apiGateway.getRequireRequestParam() && requestParam == null) {
                    throw new InvalidRequestException("Bad request: emptyRequestParam");
                } else if (apiGateway.getRequireRequestParam() && requestParam != null) {
                    url.append("?");

                    List<String> paramConfig = Arrays.asList(apiGateway.getParam().split(";"));
                    for (Map.Entry<String, Object> entry : requestParam.entrySet()) {
                        if (paramConfig.contains(entry.getKey())) {
                            url.append(entry.getKey()).append("=").append(entry.getValue());
                        }
                    }
                }

                if (apiGateway.getRequireRequestBody() && requestBody == null) {
                    throw new InvalidRequestException("Bad request: emptyRequestBody");
                }

                ResponseEntity<Object> response = httpServices.invokeUrl(
                        url.toString(),
                        HttpMethod.valueOf(apiGateway.getMethod()),
                        httpHeaders,
                        requestBody);

                rs.setSuccess(response.getBody());
            }else{
                throw new UnpublishedException("API unpublished");
            }
        } catch (Exception e) {
            log.error("error processForwardApi", e);
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }
        return rs;
    }

    public Response<Object> generateToken(String accessToken) {
        Response<Object> rs = new Response<>();

        try {
            // validate token
            Map<String, String> storeCred = SecretKeyUtil.decodeString(accessToken);

            // check through store account repository
            Optional<StoreAccount> storeAccount = storeAccountRep.findByClientIdAndSecretKey(storeCred.get("clientId"), storeCred.get("secretKey"));
            if (storeAccount.isEmpty()) {
                throw new StoreAccountInvalidCredentials("Invalid credentials");
            }

            // generate token
            String key = UUID.randomUUID().toString();

            String status = systemPropertiesServices.getProps("TOKEN_ACCESS_ACTIVE_STATUS");
            int expiringTime = Integer.parseInt(systemPropertiesServices.getProps("API_ACCESS_TIME"));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("accessToken", key);
            result.put("expiringIn", expiringTime);

            TokenAccessLog tokenLog = new TokenAccessLog();
            tokenLog.setToken(key);
            tokenLog.setStatus(status);
            tokenLog.setStoreAccount(storeAccount.get());

            tokenAccessLogRepository.save(tokenLog);
        } catch (Exception e) {
            log.error("error processForwardApi", e);
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }

        return rs;
    }


}
