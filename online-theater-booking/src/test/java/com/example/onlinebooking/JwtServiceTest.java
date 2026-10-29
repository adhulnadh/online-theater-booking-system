package com.example.onlinebooking;

import com.example.onlinebooking.model.Role;
import com.example.onlinebooking.model.User;
import com.example.onlinebooking.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "superSecretKeyWithSufficientEntropyAndLengthForTesting1234567890!",
                3600000L,
                new ObjectMapper()
        );
    }

    @Test
    void testGenerateAndValidateToken() {
        User user = User.builder()
                .userId(42L)
                .name("Jane Doe")
                .email("jane@example.com")
                .role(Role.USER)
                .build();

        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertTrue(token.contains("."));

        Map<String, Object> claims = jwtService.validateAndGetClaims(token);
        assertEquals("jane@example.com", claims.get("sub"));
        assertEquals(42, ((Number) claims.get("userId")).intValue());
        assertEquals("Jane Doe", claims.get("name"));
        assertEquals("USER", claims.get("role"));

        assertEquals("jane@example.com", jwtService.getEmailFromToken(token));
        assertEquals(42L, jwtService.getUserIdFromToken(token));
    }

    @Test
    void testInvalidTokenThrowsException() {
        assertThrows(SecurityException.class, () -> jwtService.validateAndGetClaims("invalid.token.structure"));
    }
}
