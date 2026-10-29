package com.example.onlinebooking;

import com.example.onlinebooking.model.Role;
import com.example.onlinebooking.model.Seat;
import com.example.onlinebooking.model.SeatStatus;
import com.example.onlinebooking.model.User;
import com.example.onlinebooking.repository.SeatRepository;
import com.example.onlinebooking.service.SeatLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatLockServiceTest {

    @Mock
    private SeatRepository seatRepository;

    private SeatLockService seatLockService;
    private java.util.List<com.example.onlinebooking.dto.SeatReleaseMessage> broadcastMessages;
    private com.example.onlinebooking.websocket.SeatWebSocketHandler seatWebSocketHandler;

    private User testUser;
    private Seat testSeat;

    @BeforeEach
    void setUp() {
        broadcastMessages = new java.util.ArrayList<>();
        seatWebSocketHandler = new com.example.onlinebooking.websocket.SeatWebSocketHandler(null) {
            @Override
            public void broadcastSeatReleased(Long showId, Long seatId, String seatNumber) {
                broadcastMessages.add(com.example.onlinebooking.dto.SeatReleaseMessage.of(showId, seatId, seatNumber));
            }

            @Override
            public void broadcastSeatLocked(Long showId, Long seatId, String seatNumber, String lockedUntil) {
                // No-op for tests
            }
        };

        seatLockService = new SeatLockService(seatRepository, seatWebSocketHandler);

        testUser = User.builder()
                .userId(1L)
                .name("Alice")
                .email("alice@example.com")
                .role(Role.USER)
                .build();

        testSeat = Seat.builder()
                .seatId(10L)
                .seatNumber("C4")
                .status(SeatStatus.AVAILABLE)
                .build();
    }

    @Test
    @DisplayName("Requirement 1 & 2: Atomically lock seat for 10 minutes when available")
    void testLockSeatById_Success() {
        when(seatRepository.findById(10L))
                .thenReturn(Optional.of(testSeat))
                .thenReturn(Optional.of(Seat.builder()
                        .seatId(10L)
                        .seatNumber("C4")
                        .status(SeatStatus.LOCKED)
                        .lockedUntil(LocalDateTime.now().plusMinutes(10))
                        .user(testUser)
                        .build()));

        ArgumentCaptor<LocalDateTime> lockedUntilCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        when(seatRepository.lockSeatAtomic(
                eq(10L),
                eq(SeatStatus.LOCKED),
                lockedUntilCaptor.capture(),
                eq(testUser),
                eq(SeatStatus.AVAILABLE),
                nowCaptor.capture()
        )).thenReturn(1); // 1 row successfully updated

        Seat result = seatLockService.lockSeatById(10L, testUser);

        assertNotNull(result);
        assertEquals(SeatStatus.LOCKED, result.getStatus());
        assertEquals(testUser, result.getUser());
        assertNotNull(result.getLockedUntil());

        // Verify locked_until is set to Current Time + 10 Minutes
        LocalDateTime capturedLockedUntil = lockedUntilCaptor.getValue();
        LocalDateTime capturedNow = nowCaptor.getValue();
        long minutesBetween = ChronoUnit.MINUTES.between(capturedNow, capturedLockedUntil);
        assertEquals(10, minutesBetween, "locked_until must be exactly 10 minutes in the future");

        verify(seatRepository, times(1)).lockSeatAtomic(
                eq(10L),
                eq(SeatStatus.LOCKED),
                any(LocalDateTime.class),
                eq(testUser),
                eq(SeatStatus.AVAILABLE),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("Requirement 2: Atomic update conflict throws IllegalStateException when another user already locked the seat")
    void testLockSeatById_SimultaneousConflict_ThrowsException() {
        when(seatRepository.findById(10L)).thenReturn(Optional.of(testSeat));

        // Atomic query returns 0 (seat was already locked by another concurrent transaction)
        when(seatRepository.lockSeatAtomic(
                eq(10L),
                eq(SeatStatus.LOCKED),
                any(LocalDateTime.class),
                eq(testUser),
                eq(SeatStatus.AVAILABLE),
                any(LocalDateTime.class)
        )).thenReturn(0);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> seatLockService.lockSeatById(10L, testUser)
        );

        assertTrue(ex.getMessage().contains("not available for locking"),
                "Should notify that seat is already locked or booked");
    }

    @Test
    @DisplayName("Locking non-existent seat throws IllegalArgumentException")
    void testLockSeatById_SeatNotFound_ThrowsException() {
        when(seatRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> seatLockService.lockSeatById(999L, testUser)
        );

        assertTrue(ex.getMessage().contains("Seat not found with id: 999"));
        verify(seatRepository, never()).lockSeatAtomic(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Locking with null user throws IllegalArgumentException")
    void testLockSeatById_NullUser_ThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> seatLockService.lockSeatById(10L, null)
        );

        assertTrue(ex.getMessage().contains("A valid user is required"));
        verify(seatRepository, never()).lockSeatAtomic(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Requirement 3: Scheduled task runs and resets seats with status=LOCKED and locked_until < NOW() to AVAILABLE and broadcasts via WebSocket")
    void testCleanupExpiredLocks_ScheduledTask() {
        com.example.onlinebooking.model.Show testShow = com.example.onlinebooking.model.Show.builder()
                .showId(100L)
                .build();

        Seat expired1 = Seat.builder().seatId(101L).seatNumber("A1").show(testShow).status(SeatStatus.LOCKED).build();
        Seat expired2 = Seat.builder().seatId(102L).seatNumber("A2").show(testShow).status(SeatStatus.LOCKED).build();
        Seat expired3 = Seat.builder().seatId(103L).seatNumber("A3").show(testShow).status(SeatStatus.LOCKED).build();

        when(seatRepository.findByStatusAndLockedUntilBefore(eq(SeatStatus.LOCKED), any(LocalDateTime.class)))
                .thenReturn(java.util.List.of(expired1, expired2, expired3));

        when(seatRepository.releaseExpiredLocks(
                eq(SeatStatus.AVAILABLE),
                eq(SeatStatus.LOCKED),
                any(LocalDateTime.class)
        )).thenReturn(3);

        int released = seatLockService.cleanupExpiredLocks();

        assertEquals(3, released, "Scheduler should report 3 released seats");
        verify(seatRepository, times(1)).findByStatusAndLockedUntilBefore(
                eq(SeatStatus.LOCKED),
                any(LocalDateTime.class)
        );
        verify(seatRepository, times(1)).releaseExpiredLocks(
                eq(SeatStatus.AVAILABLE),
                eq(SeatStatus.LOCKED),
                any(LocalDateTime.class)
        );

        // Verify WebSocket release broadcast for all 3 expired seats to topic /topic/show/100
        assertEquals(3, broadcastMessages.size(), "Should broadcast release for each expired seat");
        assertEquals(100L, broadcastMessages.get(0).getShowId());
        assertEquals(101L, broadcastMessages.get(0).getSeatId());
        assertEquals("A1", broadcastMessages.get(0).getSeatNumber());
        assertEquals("AVAILABLE", broadcastMessages.get(0).getStatus());
        assertEquals("SEAT_RELEASED", broadcastMessages.get(0).getAction());
    }

    @Test
    @DisplayName("Scheduled task returns 0 and skips broadcast when no seats have expired locks")
    void testCleanupExpiredLocks_NoExpiredSeats() {
        when(seatRepository.findByStatusAndLockedUntilBefore(eq(SeatStatus.LOCKED), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.emptyList());

        int released = seatLockService.cleanupExpiredLocks();

        assertEquals(0, released, "Should return 0 when no expired seats");
        verify(seatRepository, never()).releaseExpiredLocks(any(), any(), any());
        assertTrue(broadcastMessages.isEmpty(), "No WebSocket messages should be broadcast");
    }
}
