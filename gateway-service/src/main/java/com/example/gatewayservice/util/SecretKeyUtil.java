package com.example.gatewayservice.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SecretKeyUtil {


    private static final int SECRET_LENGTH_CLIENT = 32;
    private static final int SECRET_LENGTH_ID = 16;

    public static String encodeString(String clientId, String secretKey) {
        return Base64.getEncoder().encodeToString((clientId + ":" + secretKey).getBytes(StandardCharsets.UTF_8));
    }

    public static Map<String, String> decodeString(String encodedString) {

        // Decode the Base64 encoded string
        String decodedString = new String(Base64.getDecoder().decode(encodedString), StandardCharsets.UTF_8);

        // Split the decoded string to get clientId and secretKey
        String[] parts = decodedString.split(":", 2);
        String clientId = parts[0];
        String secretKey = parts[1];

        Map<String, String> storeAccountCred = new HashMap<>();
        storeAccountCred.put("clientId", clientId);
        storeAccountCred.put("secretKey", secretKey);

        return storeAccountCred;
    }

    public static void main(String[] args) {
        System.out.println("Client ID: " + generateClientId());
        System.out.println("Client Secret: " + generateClientSecretId());
    }

    public static String generateClientId() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] secretBytes = new byte[SECRET_LENGTH_ID];
        secureRandom.nextBytes(secretBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
    }

    public static String generateClientSecretId() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] secretBytes = new byte[SECRET_LENGTH_CLIENT];
        secureRandom.nextBytes(secretBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
    }
}
