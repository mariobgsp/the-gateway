package com.example.gatewayservice.util;

import com.example.gatewayservice.exception.models.CommonException;
import com.example.gatewayservice.models.rqrs.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import com.example.gatewayservice.models.rqrs.ForwardRequest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CommonUtil {

    public static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public static void applyError(Response<Object> rs, Exception e) {
        if (e instanceof CommonException ce) {
            rs.setError(ce.getHttpStatus() != null ? ce.getHttpStatus() : HttpStatus.INTERNAL_SERVER_ERROR,
                    ce.getHttpStatus() != null ? ce.getHttpStatus().name() : HttpStatus.INTERNAL_SERVER_ERROR.name(),
                    ce.getErrorCode(), ce.getErrorMessage());
        } else {
            rs.setError(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.name(), "99",
                    e.getMessage() != null ? e.getMessage() : "UnknownError");
        }
    }

    public static Map<String, Object> processRequest(String path, HttpHeaders httpHeaders, Object requestBody){

        log.info("received request: {}", path);
        Map<String, Object> request = new HashMap<>();
        request.put("httpHeaders", httpHeaders);

        // process path — limit 2 so "?" in query value doesn't split again (legacy, now aligned with toForwardRequest)
        String[] arrPath = path.split("\\?", 2);

        String pathName = arrPath[0];
        String queryString = arrPath.length > 1 ? arrPath[1] : "";
        Map<String, Object> queryParams = parseQueryString(queryString);

        Map<String, Object> pathMap = new HashMap<>();
        pathMap.put("pathName", pathName);
        pathMap.put("requestParam", queryParams);

        request.put("path", pathMap);

        request.put("requestBody", requestBody);

        return request;
    }

    public static Map<String, Object> parseQueryString(String queryString) {
        Map<String, Object> queryParams = new HashMap<>();
        if (!queryString.isEmpty()) {
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                String key = keyValue[0];
                String value = keyValue.length > 1 ? keyValue[1] : "";
                queryParams.put(key, value);
            }
        }
        return queryParams;
    }

    /** Deep Module: GatewayForward — typed parser. Tolerant (?foo → "") — container (Tomcat) already percent-decodes @PathVariable, so no second URLDecoder here (avoids "+"→space double-decode). Canonical encoding lives only in GatewayForward. */
    public static ForwardRequest toForwardRequest(String path, HttpHeaders headers, Object body) {
        log.info("received request: {}", path);
        String[] arr = path.split("\\?", 2);
        String pathName = arr[0];
        String queryString = arr.length > 1 ? arr[1] : "";
        Map<String, String> queryParams = parseQueryStringTyped(queryString);
        return new ForwardRequest(pathName, queryParams, headers, body);
    }

    private static Map<String, String> parseQueryStringTyped(String queryString) {
        Map<String, String> out = new LinkedHashMap<>();
        if (queryString == null || queryString.isEmpty()) return out;
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            // ponytail: tolerant — keep last value for duplicate keys, no second decode (container decoded %xx and + already)
            out.put(key, value);
        }
        return out;
    }

    public static List<String> splitConfig(String config) {
        if (config == null || config.isBlank()) return java.util.Collections.emptyList();
        return java.util.Arrays.stream(config.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
