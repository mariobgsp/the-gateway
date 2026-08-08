package com.example.gatewayservice.controller;

import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.rqrs.SaveStoreRequest;
import com.example.gatewayservice.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store")
public class StoreController {

    @Autowired
    private StoreService storeService;

    @GetMapping("/getList")
    public ResponseEntity<?> getListStores() {
        Response<Object> rs = storeService.getListStores(currentUsername());
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @PostMapping("/getDetail")
    public ResponseEntity<?> getDetailStore(@RequestParam("store_id") Long storeId) {
        Response<Object> rs = storeService.getStoreDetail(storeId, currentUsername());
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveStore(@RequestBody SaveStoreRequest request) {
        Response<Object> rs = storeService.saveStore(request, currentUsername());
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteStore(@RequestParam("store_id") Long storeId) {
        Response<Object> rs = storeService.deleteStore(storeId, currentUsername());
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    @PostMapping("/regenerateSecret")
    public ResponseEntity<?> regenerateSecret(@RequestParam("store_id") Long storeId) {
        Response<Object> rs = storeService.regenerateSecret(storeId, currentUsername());
        return new ResponseEntity<>(rs, rs.getHttpStatus());
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return "";
        }
        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getPrincipal().toString();
    }
}
