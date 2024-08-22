package com.example.gatewayservice;

import com.example.gatewayservice.models.entity.SystemProperties;
import com.example.gatewayservice.repository.SystemPropertiesRepository;
import com.example.gatewayservice.service.SystemPropertiesServices;
import com.example.gatewayservice.service.redis.RedisServices;
import com.example.gatewayservice.util.CommonUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@Slf4j
public class GatewayServiceApplication {

    @Autowired
    private SystemPropertiesRepository systemPropertiesRepository;
    @Autowired
    private RedisServices redisServices;
    @Autowired
    private SystemPropertiesServices systemPropertiesServices;

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    @PostConstruct
    public void initConfiguration() {
        try {
            List<SystemProperties> list = systemPropertiesRepository.findByVgroup("gateway-services");
            String propsName = "gateway-services";

            log.info("load redis configuration : {}", propsName);
            Map<String, Object> spM = new HashMap<String, Object>();
            for (SystemProperties sp : list) {
                spM.put(sp.getKey(), sp.getValue());
            }

            // save at redis
            redisServices.setWithTTL(propsName, CommonUtil.gson.toJson(spM), 60, TimeUnit.MINUTES);
            // get key
            String appName = systemPropertiesServices.getProps("APP_NAME");

            log.info("success get properties - {}", appName);
        } catch (Exception e) {
            log.info("failed load configuration {}", e.getMessage());
        }
    }

}
