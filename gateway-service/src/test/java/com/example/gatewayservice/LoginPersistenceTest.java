package com.example.gatewayservice;

import com.example.gatewayservice.models.rqrs.Response;
import com.example.gatewayservice.models.user.UserLoginRq;
import com.example.gatewayservice.repository.UserRepository;
import com.example.gatewayservice.service.security.AuthUserService;
import com.example.gatewayservice.service.security.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class LoginPersistenceTest {

    @Autowired
    private AuthUserService authUserService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LoginAttemptService loginAttemptService;

    @Test
    void repeatedLoginsMustNotDuplicateUserRows() {
        loginAttemptService.registerSuccess("ario_test");
        loginAttemptService.registerSuccess("ario_test");

        UserLoginRq request = new UserLoginRq();
        request.setUsername("ario_test");
        request.setPassword("password123");

        Response<Object> first = authUserService.authLogin(request);
        assertEquals("00", first.getCode());
        assertEquals(1, countUsers("ario_test"));

        Response<Object> second = authUserService.authLogin(request);
        assertEquals("00", second.getCode());

        assertEquals(1, countUsers("ario_test"));
    }

    private long countUsers(String username) {
        return userRepository.findAll().stream().filter(u -> username.equals(u.getUsername())).count();
    }
}
