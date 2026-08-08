package com.example.gatewayservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class HttpServices {

    private final RestTemplate restTemplate;

    public HttpServices() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    public ResponseEntity<Object> invokeUrl(String url,
                            HttpMethod httpMethod,
                            HttpHeaders httpHeaders,
                            Object requestBody){

        log.info("process forwarding API!");
        log.info("construct url result: {}", url);
        log.info("httpMethod {}", httpMethod);
        log.info("httpHeaderNames {}", httpHeaders.keySet());

        ResponseEntity<Object> response = restTemplate.exchange(
                url,
                httpMethod,
                new HttpEntity<>(requestBody, httpHeaders)
                , Object.class);
        log.info("success process forwarding API!");
        log.info("responseStatus {}", response.getStatusCode());

        return response;
    }
}
