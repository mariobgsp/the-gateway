package com.example.gatewayservice.service;

import com.example.gatewayservice.service.redis.RedisServices;
import com.example.gatewayservice.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SystemPropertiesServices {

    @Autowired
    private RedisServices redisServices;

    public String getCachedRedisProps(String key) {
        String appName = "gateway-services";
        Map props = CommonUtil.gson.fromJson(redisServices.get(appName), Map.class);
        return (String) props.get(key);
    }

    public String getProps(String key) {
        String s = getCachedRedisProps(key);
        if (s != null) {
            return s;
        }
        return "";
    }
}
