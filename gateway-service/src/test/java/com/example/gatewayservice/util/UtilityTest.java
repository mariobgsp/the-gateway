package com.example.gatewayservice.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityTest {

    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    @Test
    void seededUserHashMatchesPassword123() {
        assertTrue(bCryptPasswordEncoder.matches("password123", "$2a$10$s8CVccrkN0d/eryxRHMz2.2m.YBmYLzRBPedMY34b9l6xyL8I8ru6"));
    }

    @Test
    void encodeProducesBcryptHash() {
        String encoded = bCryptPasswordEncoder.encode("password123");
        assertTrue(encoded.startsWith("$2a$"));
        assertTrue(bCryptPasswordEncoder.matches("password123", encoded));
        assertNotEquals("password123", encoded);
    }
}
