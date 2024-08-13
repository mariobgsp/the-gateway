package com.example.gatewayservice.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UtilityTest {

    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    @Test
    public void encodePasswordTest() {
        String password = "password123";

        String encoded = bCryptPasswordEncoder.encode(password);
        System.out.println(encoded);

        // $2a$10$Wf03KbQQyWv2Ymb4jS3WN.zB00wyA1sxlMIzj/DYSHreV4HH2AJPO
    }
}
