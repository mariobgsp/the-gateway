package com.example.gatewayservice.service;

import com.example.gatewayservice.exception.definition.InvalidRequestException;
import com.example.gatewayservice.exception.definition.StoreNotFoundException;
import com.example.gatewayservice.exception.definition.UserNotFoundException;
import com.example.gatewayservice.models.entity.StoreAccount;
import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.entity.UserStoreR;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.rqrs.custom.StoreRs;
import com.example.gatewayservice.models.rqrs.SaveStoreRequest;
import com.example.gatewayservice.repository.StoreAccountRepository;
import com.example.gatewayservice.repository.UserRepository;
import com.example.gatewayservice.repository.UserStoreRRepository;
import com.example.gatewayservice.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class StoreService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired
    private StoreAccountRepository storeAccountRepository;
    @Autowired
    private UserStoreRRepository userStoreRRepository;
    @Autowired
    private UserRepository userRepository;

    public Response<Object> getListStores(String username) {
        Response<Object> rs = new Response<>();
        try {
            List<StoreRs> stores = new ArrayList<>();
            for (StoreAccount store : loadUserStores(username)) {
                stores.add(toRs(store));
            }
            rs.setSuccess(stores);
        } catch (Exception e) {
            log.error("error getListStores", e);
            CommonUtil.applyError(rs, e);
        }
        return rs;
    }

    public Response<Object> getStoreDetail(Long storeId, String username) {
        Response<Object> rs = new Response<>();
        try {
            if (storeId == null) throw new InvalidRequestException("store_id is required");
            StoreAccount store = loadOwnedStore(storeId, username);
            rs.setSuccess(toRs(store));
        } catch (Exception e) {
            log.error("error getStoreDetail", e);
            CommonUtil.applyError(rs, e);
        }
        return rs;
    }

    @Transactional
    public Response<Object> saveStore(SaveStoreRequest request, String username) {
        Response<Object> rs = new Response<>();
        try {
            validateSaveStore(request);

            StoreAccount store;
            if (request.getStoreId() != null) {
                store = loadOwnedStore(request.getStoreId(), username);
                store.setStoreName(request.getStoreName());
                if (StringUtils.hasText(request.getClientId())) {
                    store.setClientId(request.getClientId());
                }
                storeAccountRepository.save(store);
            } else {
                User user = loadUser(username);
                if (storeAccountRepository.existsByStoreName(request.getStoreName())) {
                    throw new InvalidRequestException("store name already exists");
                }
                store = new StoreAccount();
                store.setStoreName(request.getStoreName());
                store.setClientId(StringUtils.hasText(request.getClientId())
                        ? request.getClientId()
                        : "client_" + randomToken(6));
                store.setSecretKey(generateSecretKey());
                store = storeAccountRepository.save(store);

                userStoreRRepository.save(new UserStoreR(user.getId(), store.getId()));
            }

            rs.setSuccess(toRs(store));
        } catch (Exception e) {
            log.error("error saveStore", e);
            CommonUtil.applyError(rs, e);
        }
        return rs;
    }

    @Transactional
    public Response<Object> deleteStore(Long storeId, String username) {
        Response<Object> rs = new Response<>();
        try {
            if (storeId == null) throw new InvalidRequestException("store_id is required");
            StoreAccount store = loadOwnedStore(storeId, username);
            userStoreRRepository.deleteByIdStoreId(store.getId());
            storeAccountRepository.delete(store);
            rs.setSuccessMessage("store deleted successfully!");
        } catch (Exception e) {
            log.error("error deleteStore", e);
            CommonUtil.applyError(rs, e);
        }
        return rs;
    }

    public Response<Object> regenerateSecret(Long storeId, String username) {
        Response<Object> rs = new Response<>();
        try {
            if (storeId == null) throw new InvalidRequestException("store_id is required");
            StoreAccount store = loadOwnedStore(storeId, username);
            store.setSecretKey(generateSecretKey());
            storeAccountRepository.save(store);
            rs.setSuccess(toRs(store));
        } catch (Exception e) {
            log.error("error regenerateSecret", e);
            CommonUtil.applyError(rs, e);
        }
        return rs;
    }

    private List<StoreAccount> loadUserStores(String username) throws UserNotFoundException {
        User user = loadUser(username);
        List<UserStoreR> links = userStoreRRepository.findByIdUserId(user.getId());
        if (links.isEmpty()) return new ArrayList<>();
        List<Long> storeIds = links.stream().map(l -> l.getId().getStoreId()).toList();
        return storeAccountRepository.findAllByIdIn(storeIds);
    }

    private StoreAccount loadOwnedStore(Long storeId, String username) throws UserNotFoundException, StoreNotFoundException {
        User user = loadUser(username);
        boolean owned = userStoreRRepository.existsByIdUserIdAndIdStoreId(user.getId(), storeId);
        if (!owned) throw new StoreNotFoundException("store not found!");
        Optional<StoreAccount> store = storeAccountRepository.findById(storeId);
        if (store.isEmpty()) throw new StoreNotFoundException("store not found!");
        return store.get();
    }

    private User loadUser(String username) throws UserNotFoundException {
        Optional<User> user = userRepository.findDetailedByUsername(username);
        if (user.isEmpty()) throw new UserNotFoundException("user not found!");
        return user.get();
    }

    private void validateSaveStore(SaveStoreRequest request) throws InvalidRequestException {
        if (request == null) throw new InvalidRequestException("request body is required");
        if (!StringUtils.hasText(request.getStoreName())) throw new InvalidRequestException("store name is required");
        if (request.getStoreName().length() > 255) throw new InvalidRequestException("store name too long");
    }

    private StoreRs toRs(StoreAccount store) {
        StoreRs rs = new StoreRs();
        rs.setId(store.getId());
        rs.setStoreName(store.getStoreName());
        rs.setClientId(store.getClientId());
        rs.setSecretKey(store.getSecretKey());
        return rs;
    }

    private String generateSecretKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "gw_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String randomToken(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
