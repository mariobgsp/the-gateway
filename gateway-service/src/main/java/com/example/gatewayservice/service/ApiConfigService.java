package com.example.gatewayservice.service;

import com.example.gatewayservice.exception.definition.ApiGatewayNotFoundException;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ApiConfigService {

    @Autowired
    private ApiGatewayRepository apiGatewayRepository;
    @Autowired
    private StoreAccountRepository storeAccountRepository;
    @Autowired
    private StoreApiAccessRepository storeApiAccessRepository;


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
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
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
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }

        return rs;
    }

    public Response<Object> publishApi(PublishRq publishRq){
        Response<Object> rs = new Response<>();

        try{
            // find by identifier
            Optional<ApiGateway> api = apiGatewayRepository.findByApiIdentifier(publishRq.getApiIdentifier());
            if(api.isEmpty()){
                throw new ApiGatewayNotFoundException("api not found!");
            }

            if(publishRq.getStoreName()==null){
                // set Default Key
                Optional<StoreAccount> stAcc = storeAccountRepository.findByStoreCode("default");
                if(stAcc.isEmpty()){
                    throw new ApiGatewayNotFoundException("store account not found!");
                }

                StoreApiAccess storeApiAccess = new StoreApiAccess();
                storeApiAccess.setApiGateway(api.get());
                storeApiAccess.setStoreAccount(stAcc.get());

                //save
                storeApiAccessRepository.save(storeApiAccess);
            }else {
                // find by storeAccount
                Optional<StoreAccount> stAcc = storeAccountRepository.findByStoreCode(publishRq.getStoreName());
                if(stAcc.isEmpty()){
                    throw new ApiGatewayNotFoundException("store account not found!");
                }

                StoreApiAccess storeApiAccess = new StoreApiAccess();
                storeApiAccess.setApiGateway(api.get());
                storeApiAccess.setStoreAccount(stAcc.get());

                //save
                storeApiAccessRepository.save(storeApiAccess);
            }

            rs.setSuccessMessage("success publish api");
        }catch (Exception e){
            log.error("error processForwardApi", e);
            CommonException co = e instanceof CommonException ? (CommonException) e : new CommonException(e);
            rs.setError(co.getHttpStatus(), co.getHttpStatus().name(), co.getErrorCode(), co.getErrorMessage());
        }

        return rs;
    }
}
