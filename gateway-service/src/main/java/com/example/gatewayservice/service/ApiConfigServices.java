package com.example.gatewayservice.service;

import com.example.gatewayservice.exception.definition.*;
import com.example.gatewayservice.exception.models.CommonException;
import com.example.gatewayservice.models.entity.ApiGateway;
import com.example.gatewayservice.models.entity.StoreAccount;
import com.example.gatewayservice.models.entity.StoreApiAccess;
import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.rqrs.custom.GatewayRs;
import com.example.gatewayservice.models.rqrs.request.*;
import com.example.gatewayservice.models.rqrs.response.Response;
import com.example.gatewayservice.repository.ApiGatewayRepository;
import com.example.gatewayservice.repository.StoreAccountRepository;
import com.example.gatewayservice.repository.StoreApiAccessRepository;
import com.example.gatewayservice.service.redis.RedisServices;
import com.example.gatewayservice.service.security.AuthUserService;
import com.example.gatewayservice.util.SecretKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ApiConfigServices {

    @Autowired
    private ApiGatewayRepository apiGatewayRepository;
    @Autowired
    private StoreAccountRepository storeAccountRepository;
    @Autowired
    private StoreApiAccessRepository storeApiAccessRepository;
    @Autowired
    private RedisServices redisServices;
    @Autowired
    private AuthUserService authUserService;

    public Response<Object> getListGateways(RequestInfo requestInfo) {
        log.info("process request : {} : {}", Thread.currentThread().getStackTrace()[1].getMethodName(), requestInfo);

        List<GatewayRs> newList = new ArrayList<GatewayRs>();
        Response<Object> rs = new Response<Object>();
        try {
            List<ApiGateway> list = apiGatewayRepository.findAll();
            for (ApiGateway api : list) {
                GatewayRs gatewayRs = new GatewayRs();
                gatewayRs.setId(api.getId());
                gatewayRs.setApiName(api.getApiName());
                gatewayRs.setApiIdentifier(api.getApiIdentifier());
                gatewayRs.setMethod(api.getMethod());
                gatewayRs.setStatus(api.getStatus());

                newList.add(gatewayRs);
            }
            rs.setSuccess(newList);
        } catch (Exception e) {
            log.error("error getListGateways", e);
            CommonException co = new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }

        log.info("done process request : {} : {}", Thread.currentThread().getStackTrace()[1].getMethodName(), rs);
        return rs;
    }

    public Response<Object> getApiDetailed(RequestInfo requestInfo, Long apiId) {
        log.info("process request : {} : {}", Thread.currentThread().getStackTrace()[1].getMethodName(), requestInfo);

        Response<Object> rs = new Response<>();

        try {
            Optional<ApiGateway> api = apiGatewayRepository.findById(apiId);
            if (api.isEmpty()) {
                throw new ApiGatewayNotFoundException("api not found!");
            }

            rs.setSuccess(api);
        } catch (Exception e) {
            log.error("error processForwardApi", e);
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }

        log.info("done process request : {} : {}", Thread.currentThread().getStackTrace()[1].getMethodName(), rs);
        return rs;
    }

    public Response<Object> updateApi(RequestInfo requestInfo, PublishRq publishRq) {
        log.info("process request : {} : {}", Thread.currentThread().getStackTrace()[1].getMethodName(), requestInfo);

        Response<Object> rs = new Response<>();
        boolean storeInsert = true;

        try {
            // find by identifier
            Optional<ApiGateway> api = apiGatewayRepository.findById(publishRq.getApiId());
            if (api.isEmpty()) {
                throw new ApiGatewayNotFoundException("api not found!");
            }
            Optional<StoreAccount> sac = storeAccountRepository.findById(publishRq.getStoreId());
            if (sac.isPresent() && sac.get().getStatus().equals("ACTIVE")) {
                throw new StoreAccountNotActiveException("store is not active, activate first!");
            }

            // check if exist on store account to avoid dups
            List<StoreApiAccess> sa = storeApiAccessRepository.findByApiGateway(api.get());
            // store in list string
            List<Long> existingStoreId = new ArrayList<>(0);
            if (!sa.isEmpty()) {
                for (StoreApiAccess s : sa) {
                    Optional<StoreAccount> str = storeAccountRepository.findById(s.getId());
                    str.ifPresent(storeAccount -> existingStoreId.add(storeAccount.getId()));
                }
            }

            if (existingStoreId.contains(publishRq.getStoreId())) {
                if (api.get().getStatus().equals("PUBLISH")) {
                    throw new UpdateApiException(HttpStatus.CONFLICT, "03", "api already published with same store");
                } else {
                    api.get().setStatus("PUBLISHED");
                    storeInsert = false;
                }
            }

            Optional<StoreAccount> stAcc;
            if (publishRq.getStatus().equals("PUBLISH") && storeInsert) {
                if (publishRq.getStoreId() == null) {
                    // set Default Key
                    stAcc = storeAccountRepository.findByStoreCode("D1");
                    if (stAcc.isEmpty()) {
                        throw new ApiGatewayNotFoundException("store account not found!");
                    }

                    StoreApiAccess storeApiAccess = new StoreApiAccess();
                    storeApiAccess.setApiGateway(api.get());
                    storeApiAccess.setStoreAccount(stAcc.get());

                    api.get().setStatus("PUBLISHED");

                    //save
                    storeApiAccessRepository.save(storeApiAccess);
                } else {
                    // find by storeAccount
                    stAcc = storeAccountRepository.findById(publishRq.getStoreId());
                    if (stAcc.isEmpty()) {
                        throw new ApiGatewayNotFoundException("store account not found!");
                    }

                    StoreApiAccess storeApiAccess = new StoreApiAccess();
                    storeApiAccess.setApiGateway(api.get());
                    storeApiAccess.setStoreAccount(stAcc.get());

                    api.get().setStatus("PUBLISHED");

                    //save
                    storeApiAccessRepository.save(storeApiAccess);
                }
            } else if (storeInsert) {
                // DEPRECATE
                api.get().setStatus("DEPRECATED");
            }
            apiGatewayRepository.save(api.get());

            rs.setSuccessMessage("success change status api");
        } catch (Exception e) {
            log.error("error updateApi", e);
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }

        log.info("done process request : {} : {}", Thread.currentThread().getStackTrace()[1].getMethodName(), rs);
        return rs;
    }

    public Response<Object> addNewApi(RequestInfo requestInfo, AddNewApiRq addNewApiRq) {
        Response<Object> rs = new Response<>();
        log.info("process request : {} : {}", Thread.currentThread().getStackTrace()[1].getMethodName(), requestInfo);


        try {
            Optional<ApiGateway> apiGateway = apiGatewayRepository.findByApiIdentifier(addNewApiRq.getApiIdentifier());
            if (apiGateway.isPresent()) {
                throw new DuplicateAddApiException("Duplicate api identifier, make unique!");
            }
            // construct api gateway
            ApiGateway newApi = getApiGateway(addNewApiRq);

            // save api
            apiGatewayRepository.save(newApi);
            rs.setSuccessMessage("success add New Api!");
        } catch (Exception e) {
            log.error("error addNewApi", e);
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }

        log.info("done process request : {} : {}", Thread.currentThread().getStackTrace()[1].getMethodName(), rs);
        return rs;
    }

    public Response<Object> addStore(RequestInfo requestInfo, AddStoreRq addStoreRq) {
        log.info("process request : {} : {}", Thread.currentThread().getStackTrace()[1].getMethodName(), requestInfo);
        Response<Object> rs = new Response<>();

        try {
            Optional<StoreAccount> sa = storeAccountRepository.findByStoreCode(addStoreRq.getStoreCode());
            if (sa.isPresent()) {
                throw new DuplicateStoreAccountException("Duplicate store code, make unique!");
            }
            // construct store model
            StoreAccount storeAccount = new StoreAccount();
            storeAccount.setStoreName(addStoreRq.getStoreName());
            storeAccount.setStoreCode(addStoreRq.getStoreCode());
            storeAccount.setClientId(SecretKeyUtil.generateClientId());
            storeAccount.setSecretKey(SecretKeyUtil.generateClientSecretId());

            // save store account
            storeAccountRepository.save(storeAccount);
            rs.setSuccessMessage("success add Store Account!");
        } catch (Exception e) {
            log.error("error addStore", e);
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }
        log.info("done process request : {} : {}", Thread.currentThread().getStackTrace()[1].getMethodName(), rs);
        return rs;
    }

    public Response<Object> updateStore(RequestInfo requestInfo, UpdateStoreRq updateStoreRq) {
        log.info("process request : {} : {}", Thread.currentThread().getStackTrace()[1].getMethodName(), requestInfo);
        Response<Object> rs = new Response<>();

        try {
            User user = authUserService.findByUsername(requestInfo.getUsername());
            if (user == null) {
                throw new UserNotFoundException("user not found");
            }

            String role = user.getRole().getRole();
            if (role.equals("ADMIN")) {
                Optional<StoreAccount> storeAccount = storeAccountRepository.findById(updateStoreRq.getStoreId());
                if (storeAccount.isEmpty()) {
                    throw new StoreAccountNotFoundException("store account not found");
                }
            } else {
                throw new PrivilegeException("account not allowed to update");
            }

            rs.setSuccess("success update store");
        } catch (Exception e) {
            log.error("error updateStore", e);
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }
        log.info("done process request : {} : {}", Thread.currentThread().getStackTrace()[1].getMethodName(), rs);
        return rs;
    }

    private static ApiGateway getApiGateway(AddNewApiRq addNewApiRq) {
        ApiGateway newApi = new ApiGateway();
        newApi.setApiName(addNewApiRq.getApiName());
        newApi.setApiIdentifier(addNewApiRq.getApiIdentifier());
        newApi.setApiHost(addNewApiRq.getApiHost());
        newApi.setMethod(addNewApiRq.getMethod());
        newApi.setStatus(addNewApiRq.getStatus());
        newApi.setHeader(addNewApiRq.getHeader());
        newApi.setParam(addNewApiRq.getParam());
        newApi.setRequireRequestParam(addNewApiRq.getRequireRequestParam());
        newApi.setRequireRequestBody(addNewApiRq.getRequireRequestBody());
        return newApi;
    }
}
