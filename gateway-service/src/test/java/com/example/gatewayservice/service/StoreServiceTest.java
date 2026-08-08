package com.example.gatewayservice.service;

import com.example.gatewayservice.models.entity.StoreAccount;
import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.entity.UserStoreR;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.rqrs.SaveStoreRequest;
import com.example.gatewayservice.models.rqrs.custom.StoreRs;
import com.example.gatewayservice.repository.StoreAccountRepository;
import com.example.gatewayservice.repository.UserRepository;
import com.example.gatewayservice.repository.UserStoreRRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreAccountRepository storeAccountRepository;
    @Mock
    private UserStoreRRepository userStoreRRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StoreService storeService;

    private User sampleUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("ario_test");
        return user;
    }

    private StoreAccount sampleStore(Long id, String name) {
        StoreAccount store = new StoreAccount();
        store.setId(id);
        store.setStoreName(name);
        store.setClientId("client_" + id);
        store.setSecretKey("gw_secret_" + id);
        return store;
    }

    @Test
    void getListStoresReturnsOnlyUserStores() {
        when(userRepository.findDetailedByUsername("ario_test")).thenReturn(Optional.of(sampleUser()));
        when(userStoreRRepository.findByIdUserId(1L))
                .thenReturn(List.of(new UserStoreR(1L, 10L), new UserStoreR(1L, 11L)));
        when(storeAccountRepository.findAllByIdIn(List.of(10L, 11L)))
                .thenReturn(List.of(sampleStore(10L, "Store A"), sampleStore(11L, "Store B")));

        Response<Object> rs = storeService.getListStores("ario_test");

        assertEquals("00", rs.getCode());
        List<?> data = (List<?>) rs.getData();
        assertEquals(2, data.size());
    }

    @Test
    void getStoreDetailDeniedWhenNotOwned() {
        when(userRepository.findDetailedByUsername("ario_test")).thenReturn(Optional.of(sampleUser()));
        when(userStoreRRepository.existsByIdUserIdAndIdStoreId(1L, 99L)).thenReturn(false);

        Response<Object> rs = storeService.getStoreDetail(99L, "ario_test");

        assertEquals(HttpStatus.NOT_FOUND, rs.getHttpStatus());
    }

    @Test
    void saveStoreCreatesAndLinksToUser() {
        SaveStoreRequest request = new SaveStoreRequest();
        request.setStoreName("New Store");

        when(userRepository.findDetailedByUsername("ario_test")).thenReturn(Optional.of(sampleUser()));
        when(storeAccountRepository.existsByStoreName("New Store")).thenReturn(false);
        when(storeAccountRepository.save(any(StoreAccount.class))).thenAnswer(inv -> {
            StoreAccount saved = inv.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        Response<Object> rs = storeService.saveStore(request, "ario_test");

        assertEquals("00", rs.getCode());
        verify(storeAccountRepository).save(any(StoreAccount.class));
        verify(userStoreRRepository).save(any(UserStoreR.class));
        StoreRs data = (StoreRs) rs.getData();
        assertEquals(42L, data.getId());
        assertNotNull(data.getSecretKey());
        assertTrue(data.getSecretKey().startsWith("gw_"));
    }

    @Test
    void saveStoreRejectsDuplicateName() {
        SaveStoreRequest request = new SaveStoreRequest();
        request.setStoreName("Taken");

        when(userRepository.findDetailedByUsername("ario_test")).thenReturn(Optional.of(sampleUser()));
        when(storeAccountRepository.existsByStoreName("Taken")).thenReturn(true);

        Response<Object> rs = storeService.saveStore(request, "ario_test");

        assertEquals("04", rs.getCode());
        verify(storeAccountRepository, never()).save(any());
    }

    @Test
    void saveStoreUpdatesOwnedStoreName() {
        SaveStoreRequest request = new SaveStoreRequest();
        request.setStoreId(10L);
        request.setStoreName("Renamed");

        StoreAccount store = sampleStore(10L, "Old Name");
        when(userRepository.findDetailedByUsername("ario_test")).thenReturn(Optional.of(sampleUser()));
        when(userStoreRRepository.existsByIdUserIdAndIdStoreId(1L, 10L)).thenReturn(true);
        when(storeAccountRepository.findById(10L)).thenReturn(Optional.of(store));

        Response<Object> rs = storeService.saveStore(request, "ario_test");

        assertEquals("00", rs.getCode());
        assertEquals("Renamed", store.getStoreName());
        verify(storeAccountRepository).save(store);
    }

    @Test
    void deleteStoreUnlinksAndDeletes() {
        when(userRepository.findDetailedByUsername("ario_test")).thenReturn(Optional.of(sampleUser()));
        when(userStoreRRepository.existsByIdUserIdAndIdStoreId(1L, 10L)).thenReturn(true);
        when(storeAccountRepository.findById(10L)).thenReturn(Optional.of(sampleStore(10L, "Store A")));

        Response<Object> rs = storeService.deleteStore(10L, "ario_test");

        assertEquals("00", rs.getCode());
        verify(userStoreRRepository).deleteByIdStoreId(10L);
        verify(storeAccountRepository).delete(any(StoreAccount.class));
    }

    @Test
    void regenerateSecretProducesNewKey() {
        StoreAccount store = sampleStore(10L, "Store A");
        when(userRepository.findDetailedByUsername("ario_test")).thenReturn(Optional.of(sampleUser()));
        when(userStoreRRepository.existsByIdUserIdAndIdStoreId(1L, 10L)).thenReturn(true);
        when(storeAccountRepository.findById(10L)).thenReturn(Optional.of(store));

        Response<Object> rs = storeService.regenerateSecret(10L, "ario_test");

        assertEquals("00", rs.getCode());
        StoreRs data = (StoreRs) rs.getData();
        assertNotNull(data.getSecretKey());
        assertTrue(data.getSecretKey().startsWith("gw_"));
        assertNotEquals("gw_secret_10", data.getSecretKey());
    }
}
