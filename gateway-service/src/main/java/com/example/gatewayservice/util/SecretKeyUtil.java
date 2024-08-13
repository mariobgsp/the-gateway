package com.example.gatewayservice.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SecretKeyUtil {

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
}
