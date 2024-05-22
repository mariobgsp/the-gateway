package com.example.gatewayservice.util;

import java.security.SecureRandom;
import java.util.Base64;

public class SecretKeyGenerator {
    public String generateSecretKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32]; // 256-bit key
        secureRandom.nextBytes(key);

        return Base64.getEncoder().encodeToString(key);
    }
}
