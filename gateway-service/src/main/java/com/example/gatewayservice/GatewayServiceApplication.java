package com.example.gatewayservice;

import com.example.gatewayservice.models.entity.SystemProperties;
import com.example.gatewayservice.repository.SystemPropertiesRepository;
import com.example.gatewayservice.service.RedisServices;
import com.example.gatewayservice.service.SystemPropertiesServices;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@Slf4j
public class GatewayServiceApplication {

    @Autowired
    private SystemPropertiesRepository systemPropertiesRepository;
    @Autowired
    private RedisServices redisServices;

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    @PostConstruct
    public void initConfiguration(){
        List<SystemProperties> list = systemPropertiesRepository.findByVgroup("gateway-services");
        String propsName = "gateway-services";

        log.info("load redis configuration : {}", propsName );
        List<Map<String,Object>> maps = new ArrayList<>();
        for (SystemProperties sp : list){
            Map<String,Object> spM = new HashMap<>();
            spM.put(sp.getKey(), sp.getValue());
            maps.add(spM);
        }

        // save at redis
        redisServices.saveKey(propsName, maps);
        // get key
        Object value = redisServices.getKey(propsName);
        log.info("value : {}", value);
    }

}
