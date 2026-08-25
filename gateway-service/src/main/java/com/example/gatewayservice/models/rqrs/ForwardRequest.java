package com.example.gatewayservice.models.rqrs;

import org.springframework.http.HttpHeaders;

import java.util.Collections;
import java.util.Map;

/**
 * Deep Module: GatewayForward — typed forward contract.
 * Replaces Map<String,Object> with 5 string-keyed entries.
 * Seam: GatewayController parses HTTP into ForwardRequest, ApiGatewayServices handles forwarding.
 * UpstreamPort (HttpServices) stays internal to GatewayForward — not exposed at this interface.
 */
public class ForwardRequest {

    private final String pathName;
    private final Map<String, String> queryParams;
    private final HttpHeaders headers;
    private final Object body;

    public ForwardRequest(String pathName, Map<String, String> queryParams, HttpHeaders headers, Object body) {
        this.pathName = pathName;
        this.queryParams = queryParams != null ? Collections.unmodifiableMap(queryParams) : Collections.emptyMap();
        this.headers = headers != null ? headers : new HttpHeaders();
        this.body = body;
    }

    public String getPathName() {
        return pathName;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public Object getBody() {
        return body;
    }
}
