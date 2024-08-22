package com.example.gatewayservice.util;

import com.example.gatewayservice.models.rqrs.request.RequestInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CommonUtil {

    public static Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public static RequestInfo constructRequestInfo(String operationName, String username, Object request, Date requestAt, HttpServletRequest httpServletRequest){
        return new RequestInfo(operationName, username, request, requestAt, httpServletRequest);
    }

    public static Map<String, Object> processRequest(String path, HttpHeaders httpHeaders, Object requestBody) {

        log.info("received request: {}", path);
        Map<String, Object> request = new HashMap<>();
        request.put("httpHeaders", httpHeaders);

        // process path
        String[] arrPath = path.split("\\?");

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
}
