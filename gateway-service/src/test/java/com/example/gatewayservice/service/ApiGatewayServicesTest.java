package com.example.gatewayservice.service;

import com.example.gatewayservice.exception.definition.InvalidRequestException;
import com.example.gatewayservice.models.entity.ApiGateway;
import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.rqrs.SaveApiRequest;
import com.example.gatewayservice.repository.ApiGatewayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiGatewayServicesTest {

    @Mock
    private ApiGatewayRepository apiGatewayRepository;
    @Mock
    private HttpServices httpServices;

    @InjectMocks
    private ApiGatewayServices apiGatewayServices;

    private ApiGateway sampleGateway(String identifier) {
        ApiGateway api = new ApiGateway();
        api.setId(1L);
        api.setApiName("Test API");
        api.setApiIdentifier(identifier);
        api.setApiHost("https://api.thecatapi.com");
        api.setApiPath("/v1/images/search");
        api.setMethod("GET");
        api.setStatus("created");
        api.setHeader("x-api-key;Content-Type");
        api.setRequireRequestBody(false);
        api.setRequireRequestParam(false);
        return api;
    }

    @Test
    void getListGatewaysMapsFields() {
        ApiGateway api = sampleGateway("gateway-catapi");
        when(apiGatewayRepository.findAll()).thenReturn(List.of(api));

        Response<Object> rs = apiGatewayServices.getListGateways();

        assertEquals("00", rs.getCode());
        List<?> data = (List<?>) rs.getData();
        assertEquals(1, data.size());
    }

    @Test
    void getApiDetailedReturnsDataWhenFound() {
        ApiGateway api = sampleGateway("gateway-catapi");
        when(apiGatewayRepository.findByApiIdentifier("gateway-catapi")).thenReturn(Optional.of(api));

        Response<Object> rs = apiGatewayServices.getApiDetailed("gateway-catapi");

        assertEquals("00", rs.getCode());
        ApiGateway data = (ApiGateway) rs.getData();
        assertEquals("gateway-catapi", data.getApiIdentifier());
    }

    @Test
    void getApiDetailedFailsWhenNotFound() {
        when(apiGatewayRepository.findByApiIdentifier("nope")).thenReturn(Optional.empty());

        Response<Object> rs = apiGatewayServices.getApiDetailed("nope");

        assertEquals("02", rs.getCode());
        assertEquals(HttpStatus.NOT_FOUND, rs.getHttpStatus());
    }

    @Test
    void saveApiValidatesHostAndPath() {
        SaveApiRequest request = new SaveApiRequest();
        request.setApiIdentifier("new-api");
        request.setName("New API");
        request.setHost("ftp://internal.server");
        request.setPath("/v1/data");
        request.setMethod("GET");

        Response<Object> rs = apiGatewayServices.saveApi(request);

        assertEquals("04", rs.getCode());
        verify(apiGatewayRepository, never()).save(any());

        request.setHost("https://api.thecatapi.com");
        request.setPath("missing-slash");
        rs = apiGatewayServices.saveApi(request);
        assertEquals("04", rs.getCode());
    }

    @Test
    void saveApiCreatesNewGateway() {
        SaveApiRequest request = new SaveApiRequest();
        request.setApiIdentifier("new-api");
        request.setName("New API");
        request.setHost("https://api.thecatapi.com");
        request.setPath("/v1/data");
        request.setMethod("GET");

        when(apiGatewayRepository.findByApiIdentifier("new-api")).thenReturn(Optional.empty());

        Response<Object> rs = apiGatewayServices.saveApi(request);

        assertEquals("00", rs.getCode());
        verify(apiGatewayRepository).save(any(ApiGateway.class));
    }

    @Test
    void saveApiUpdatesExistingGateway() {
        ApiGateway existing = sampleGateway("gateway-catapi");
        SaveApiRequest request = new SaveApiRequest();
        request.setApiIdentifier("gateway-catapi");
        request.setName("Updated");
        request.setHost("https://api.thecatapi.com");
        request.setPath("/v1/images/search");
        request.setMethod("GET");
        request.setStatus("published");

        when(apiGatewayRepository.findByApiIdentifier("gateway-catapi")).thenReturn(Optional.of(existing));

        Response<Object> rs = apiGatewayServices.saveApi(request);

        assertEquals("00", rs.getCode());
        assertEquals("Updated", existing.getApiName());
        assertEquals("published", existing.getStatus());
        verify(apiGatewayRepository).save(existing);
    }

    @Test
    void deleteApiFailsWhenNotFound() {
        when(apiGatewayRepository.findByApiIdentifier("nope")).thenReturn(Optional.empty());

        Response<Object> rs = apiGatewayServices.deleteApi("nope");

        assertEquals("02", rs.getCode());
        verify(apiGatewayRepository, never()).deleteByApiIdentifier(any());
    }

    @Test
    void deleteApiDeletesWhenFound() {
        when(apiGatewayRepository.findByApiIdentifier("gateway-catapi")).thenReturn(Optional.of(sampleGateway("gateway-catapi")));

        Response<Object> rs = apiGatewayServices.deleteApi("gateway-catapi");

        assertEquals("00", rs.getCode());
        verify(apiGatewayRepository).deleteByApiIdentifier("gateway-catapi");
    }

    @Test
    void forwardApiRejectsMissingRequiredBody() {
        ApiGateway api = sampleGateway("gateway-catapi");
        api.setRequireRequestBody(true);
        when(apiGatewayRepository.findByApiIdentifier("gateway-catapi")).thenReturn(Optional.of(api));

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("path", Map.of("pathName", "gateway-catapi", "requestParam", new java.util.HashMap<>()));
        request.put("httpHeaders", new java.util.HashMap<>());
        request.put("requestBody", null);

        Response<Object> rs = apiGatewayServices.processForwardApi(request);

        assertEquals("04", rs.getCode());
        verify(httpServices, never()).invokeUrl(any(), any(), any(), any());
    }

    @Test
    void forwardApiRejectsUnknownIdentifier() {
        when(apiGatewayRepository.findByApiIdentifier("unknown")).thenReturn(Optional.empty());

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("path", Map.of("pathName", "unknown", "requestParam", new java.util.HashMap<>()));
        request.put("httpHeaders", new java.util.HashMap<>());
        request.put("requestBody", null);

        Response<Object> rs = apiGatewayServices.processForwardApi(request);

        assertEquals("02", rs.getCode());
    }

    @Test
    void forwardApiForwardsWithAllowedParamsAndHeaders() throws Exception {
        ApiGateway api = sampleGateway("gateway-catapi");
        api.setRequireRequestParam(true);
        api.setParam("limit;size");
        api.setHeader("x-api-key;Content-Type");
        when(apiGatewayRepository.findByApiIdentifier("gateway-catapi")).thenReturn(Optional.of(api));

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("limit", "2");
        params.put("evil", "injected&x=1");
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("path", Map.of("pathName", "gateway-catapi", "requestParam", params));
        Map<String, Object> headers = new java.util.HashMap<>();
        headers.put("x-api-key", "secret");
        headers.put("Authorization", "Bearer jwt");
        request.put("httpHeaders", headers);
        request.put("requestBody", null);

        when(httpServices.invokeUrl(anyString(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(Map.of("id", 1), org.springframework.http.HttpStatus.OK));

        Response<Object> rs = apiGatewayServices.processForwardApi(request);

        assertEquals("00", rs.getCode());
        org.mockito.ArgumentCaptor<String> urlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(httpServices).invokeUrl(urlCaptor.capture(), eq(org.springframework.http.HttpMethod.GET), any(), isNull());
        assertTrue(urlCaptor.getValue().startsWith("https://api.thecatapi.com/v1/images/search?limit=2"));
        assertFalse(urlCaptor.getValue().contains("evil"));
        assertFalse(urlCaptor.getValue().contains("injected"));
    }

    @Test
    void forwardApiPassesOnlyConfiguredHeaders() throws Exception {
        ApiGateway api = sampleGateway("gateway-catapi");
        api.setHeader("x-api-key;Content-Type");
        when(apiGatewayRepository.findByApiIdentifier("gateway-catapi")).thenReturn(Optional.of(api));

        Map<String, Object> headers = new java.util.HashMap<>();
        headers.put("x-api-key", "secret");
        headers.put("Authorization", "Bearer jwt");
        headers.put("Cookie", "session=abc");
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("path", Map.of("pathName", "gateway-catapi", "requestParam", new java.util.HashMap<>()));
        request.put("httpHeaders", headers);
        request.put("requestBody", null);

        when(httpServices.invokeUrl(anyString(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(Map.of("id", 1), org.springframework.http.HttpStatus.OK));

        Response<Object> rs = apiGatewayServices.processForwardApi(request);

        assertEquals("00", rs.getCode());
        org.mockito.ArgumentCaptor<org.springframework.http.HttpHeaders> headerCaptor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.http.HttpHeaders.class);
        verify(httpServices).invokeUrl(anyString(), any(), headerCaptor.capture(), any());
        assertTrue(headerCaptor.getValue().containsKey("x-api-key"));
        assertFalse(headerCaptor.getValue().containsKey("Authorization"));
        assertFalse(headerCaptor.getValue().containsKey("Cookie"));
    }
}
