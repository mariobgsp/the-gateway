package com.example.gatewayservice.service;

import com.example.gatewayservice.exception.definition.ApiGatewayNotFoundException;
import com.example.gatewayservice.exception.definition.UpdateApiException;
import com.example.gatewayservice.exception.models.CommonException;
import com.example.gatewayservice.models.entity.ApiGateway;
import com.example.gatewayservice.models.entity.StoreAccount;
import com.example.gatewayservice.models.entity.StoreApiAccess;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.rqrs.custom.GatewayRs;
import com.example.gatewayservice.models.rqrs.request.PublishRq;
import com.example.gatewayservice.repository.ApiGatewayRepository;
import com.example.gatewayservice.repository.StoreAccountRepository;
import com.example.gatewayservice.repository.StoreApiAccessRepository;
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


    public Response<Object> getListGateways() {
        List<GatewayRs> newList = new ArrayList<>();
        Response<Object> rs = new Response<>();
        try {
            List<ApiGateway> list = apiGatewayRepository.findAll();
            for (ApiGateway api : list) {
                GatewayRs gatewayRs = new GatewayRs();
                gatewayRs.setId(api.getId());
                gatewayRs.setApiName(api.getApiName());
                gatewayRs.setApiIdentifier(api.getApiIdentifier());
                gatewayRs.setStatus(api.getStatus());

                newList.add(gatewayRs);
            }
            rs.setSuccess(newList);
        } catch (Exception e) {
            log.error("error processForwardApi", e);
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }
        return rs;
    }

    public Response<Object> getApiDetailed(Long apiId) {
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

        return rs;
    }

    public Response<Object> updateApi(PublishRq publishRq) {
        Response<Object> rs = new Response<>();
        Boolean storeInsert = true;

        try {
            // find by identifier
            Optional<ApiGateway> api = apiGatewayRepository.findById(publishRq.getApiId());
            if (api.isEmpty()) {
                throw new ApiGatewayNotFoundException("api not found!");
            }

            // check if exist on store account to avoid dups
            List<StoreApiAccess> sa = storeApiAccessRepository.findByApiGateway(api.get());
            // store in list string
            List<Long> existingStoreId = new ArrayList<>(0);
            if(!sa.isEmpty()){
                for (StoreApiAccess s : sa){
                    Optional<StoreAccount> str = storeAccountRepository.findById(s.getId());
                    str.ifPresent(storeAccount -> existingStoreId.add(storeAccount.getId()));
                }
            }

            if(existingStoreId.contains(publishRq.getStoreId())){
                if(api.get().getStatus().equals("PUBLISH")){
                    throw new UpdateApiException(HttpStatus.CONFLICT, "03", "api already published with same store");
                }else{
                    api.get().setStatus("PUBLISHED");
                    storeInsert = false;
                }
            }

            Optional<StoreAccount> stAcc;
            if(publishRq.getStatus().equals("PUBLISH") && storeInsert){
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
            }else if (storeInsert){
                // DEPRECATE
                api.get().setStatus("DEPRECATED");
            }
            apiGatewayRepository.save(api.get());

            rs.setSuccessMessage("success change status api");
        } catch (Exception e) {
            log.error("error processForwardApi", e);
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }

        return rs;
    }
}
