package com.example.onlinebooking.service;

import com.example.onlinebooking.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight, standard RFC 7519 JWT implementation using HMAC-SHA256
 * and Jackson ObjectMapper. Eliminates external library dependency conflicts.
 */
@Service
public class JwtService {

    private final String secretKey;
    private final long expirationMillis;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${jwt.secret:defaultSecretKeyForJwtTokenGenerationWithSufficientLength1234567890!}") String secretKey,
            @Value("${jwt.expiration:86400000}") long expirationMillis,
            ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.expirationMillis = expirationMillis;
        this.objectMapper = objectMapper;
    }

    public String generateToken(User user) {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            long now = System.currentTimeMillis();
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", user.getEmail());
            payload.put("userId", user.getUserId());
            payload.put("name", user.getName());
            payload.put("role", user.getRole().name());
            payload.put("iat", now / 1000);
            payload.put("exp", (now + expirationMillis) / 1000);

            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(payload));

            String contentToSign = encodedHeader + "." + encodedPayload;
            String signature = sign(contentToSign, secretKey);

            return contentToSign + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Error generating JWT token", e);
        }
    }

    public Map<String, Object> validateAndGetClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT structure");
            }

            String contentToSign = parts[0] + "." + parts[1];
            String expectedSignature = sign(contentToSign, secretKey);

            if (!expectedSignature.equals(parts[2])) {
                throw new SecurityException("JWT signature validation failed");
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, new TypeReference<Map<String, Object>>() {});

            Number exp = (Number) claims.get("exp");
            if (exp != null && System.currentTimeMillis() / 1000 > exp.longValue()) {
                throw new SecurityException("JWT token has expired");
            }

            return claims;
        } catch (Exception e) {
            throw new SecurityException("Invalid JWT token: " + e.getMessage(), e);
        }
    }

    public String getEmailFromToken(String token) {
        Map<String, Object> claims = validateAndGetClaims(token);
        return (String) claims.get("sub");
    }

    public Long getUserIdFromToken(String token) {
        Map<String, Object> claims = validateAndGetClaims(token);
        Number userId = (Number) claims.get("userId");
        return userId != null ? userId.longValue() : null;
    }

    private String sign(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(hmacBytes);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
