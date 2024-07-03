package com.example.gatewayservice.util;

import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.entity.UserLog;

import java.util.HashMap;
import java.util.Map;

public class CommonUtil {

    public static UserLog constructUserLog(
            User user,
            String userActivityName,
            String userSessionStatus,
            String userSession,
            String userToken,
            String errorMessage){
        return new UserLog(
                user,
                userActivityName,
                userSessionStatus,
                userSession,
                userToken,
                errorMessage);
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
