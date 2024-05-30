package com.example.gatewayservice.util;

import com.example.gatewayservice.models.entity.User;
import com.example.gatewayservice.models.entity.UserLog;

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
}
