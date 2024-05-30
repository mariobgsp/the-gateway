package com.example.gatewayservice.service;

import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.user.UserLoginRq;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@RunWith(SpringRunner.class)
class UserServicesTest {

    private final Logger logger = LoggerFactory.getLogger(UserServicesTest.class);

    @Autowired
    private UserServices userServices;

    @Test
    void login() {
        UserLoginRq userLoginRq = new UserLoginRq();
        userLoginRq.setUsername("ario_test");
        userLoginRq.setPassword("password123");
        Response<Object> rs = userServices.login(userLoginRq);
        logger.info("{}", rs);
    }
}