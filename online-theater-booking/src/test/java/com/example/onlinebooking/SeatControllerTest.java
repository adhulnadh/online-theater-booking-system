package com.example.onlinebooking;

import com.example.onlinebooking.controller.SeatController;
import com.example.onlinebooking.dto.SeatLockResponseDto;
import com.example.onlinebooking.model.Role;
import com.example.onlinebooking.model.Seat;
import com.example.onlinebooking.model.SeatStatus;
import com.example.onlinebooking.model.User;
import com.example.onlinebooking.repository.SeatRepository;
import com.example.onlinebooking.repository.UserRepository;
import com.example.onlinebooking.service.JwtService;
import com.example.onlinebooking.service.SeatLockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatControllerTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private UserRepository userRepository;

    private JwtService jwtService;
    private SeatLockService seatLockService;
    private SeatController seatController;

    private User testUser;
    private Seat testSeat;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "superSecretKeyWithSufficientEntropyAndLengthForTesting1234567890!",
                3600000L,
                new ObjectMapper()
        );
        seatLockService = new SeatLockService(seatRepository);
        seatController = new SeatController(seatRepository, userRepository, seatLockService, jwtService);

        testUser = User.builder()
                .userId(1L)
                .name("Alice")
                .email("alice@example.com")
                .role(Role.USER)
                .build();

        testSeat = Seat.builder()
                .seatId(10L)
                .seatNumber("B2")
                .status(SeatStatus.AVAILABLE)
                .build();
    }

    @Test
    @DisplayName("Requirement 4: POST /api/seats/{seatId}/lock with valid userId locks seat and returns 200 OK")
    void testLockSeatById_ValidUserParam_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(seatRepository.findById(10L))
                .thenReturn(Optional.of(testSeat))
                .thenReturn(Optional.of(Seat.builder()
                        .seatId(10L)
                        .seatNumber("B2")
                        .status(SeatStatus.LOCKED)
                        .lockedUntil(LocalDateTime.now().plusMinutes(10))
                        .user(testUser)
                        .build()));

        when(seatRepository.lockSeatAtomic(
                eq(10L),
                eq(SeatStatus.LOCKED),
                any(LocalDateTime.class),
                eq(testUser),
                eq(SeatStatus.AVAILABLE),
                any(LocalDateTime.class)
        )).thenReturn(1);

        ResponseEntity<?> response = seatController.lockSeatById(10L, 1L, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof SeatLockResponseDto);

        SeatLockResponseDto body = (SeatLockResponseDto) response.getBody();
        assertEquals(10L, body.getSeatId());
        assertEquals("B2", body.getSeatNumber());
        assertEquals(SeatStatus.LOCKED, body.getStatus());
        assertEquals(1L, body.getUserId());
        assertNotNull(body.getLockedUntil());
    }

    @Test
    @DisplayName("Requirement 4: POST /api/seats/{seatId}/lock with Bearer JWT token locks seat and returns 200 OK")
    void testLockSeatById_BearerToken_Success() {
        String token = jwtService.generateToken(testUser);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(testUser));
        when(seatRepository.findById(10L))
                .thenReturn(Optional.of(testSeat))
                .thenReturn(Optional.of(Seat.builder()
                        .seatId(10L)
                        .seatNumber("B2")
                        .status(SeatStatus.LOCKED)
                        .lockedUntil(LocalDateTime.now().plusMinutes(10))
                        .user(testUser)
                        .build()));

        when(seatRepository.lockSeatAtomic(
                eq(10L),
                eq(SeatStatus.LOCKED),
                any(LocalDateTime.class),
                eq(testUser),
                eq(SeatStatus.AVAILABLE),
                any(LocalDateTime.class)
        )).thenReturn(1);

        ResponseEntity<?> response = seatController.lockSeatById(10L, null, null, "Bearer " + token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof SeatLockResponseDto);
    }

    @Test
    @DisplayName("POST /api/seats/{seatId}/lock returns 409 Conflict when seat is already locked or booked")
    void testLockSeatById_AlreadyLocked_ReturnsConflict() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(seatRepository.findById(10L)).thenReturn(Optional.of(testSeat));

        // Atomic lock returns 0 rows updated
        when(seatRepository.lockSeatAtomic(
                eq(10L),
                eq(SeatStatus.LOCKED),
                any(LocalDateTime.class),
                eq(testUser),
                eq(SeatStatus.AVAILABLE),
                any(LocalDateTime.class)
        )).thenReturn(0);

        ResponseEntity<?> response = seatController.lockSeatById(10L, 1L, null, null);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.get("message").toString().contains("not available for locking"));
    }

    @Test
    @DisplayName("POST /api/seats/{seatId}/lock returns 400 Bad Request when user identification is missing")
    void testLockSeatById_MissingUser_ReturnsBadRequest() {
        ResponseEntity<?> response = seatController.lockSeatById(10L, null, null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.get("message").toString().contains("User validation failed"));
    }

    @Test
    @DisplayName("POST /api/seats/{seatId}/lock returns 404 Not Found when userId does not exist")
    void testLockSeatById_UserNotFound_ReturnsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = seatController.lockSeatById(10L, 99L, null, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.get("message").toString().contains("User not found with id: 99"));
    }
}
