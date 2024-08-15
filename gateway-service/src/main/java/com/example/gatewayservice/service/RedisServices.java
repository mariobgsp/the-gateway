package com.example.gatewayservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisServices {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void saveKey(String key, Object value){
        redisTemplate.opsForValue().set(key, value);
    }

    public Object getKey(String key){
        return redisTemplate.opsForValue().get(key);
    }

}
