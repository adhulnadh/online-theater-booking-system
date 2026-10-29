package com.example.onlinebooking;

import com.example.onlinebooking.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SeatLockTest {

    @Test
    void testSeatLockAndExpiration() {
        User user = User.builder()
                .userId(1L)
                .name("Alice")
                .email("alice@example.com")
                .role(Role.USER)
                .build();

        Seat seat = Seat.builder()
                .seatId(100L)
                .seatNumber("A1")
                .status(SeatStatus.AVAILABLE)
                .build();

        assertTrue(seat.isAvailableForLock(), "New seat should be available for lock");

        // Lock for 10 minutes
        seat.lockForUser(user, 10);
        assertEquals(SeatStatus.LOCKED, seat.getStatus());
        assertEquals(user, seat.getUser());
        assertNotNull(seat.getLockedUntil());
        assertFalse(seat.isLockExpired());
        assertFalse(seat.isAvailableForLock());

        // Simulate expired lock
        seat.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        assertTrue(seat.isLockExpired(), "Seat lock should be recognized as expired");
        assertTrue(seat.isAvailableForLock(), "Seat with expired lock should be available for lock");

        // Release lock
        seat.releaseLock();
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        assertNull(seat.getUser());
        assertNull(seat.getLockedUntil());

        // Book seat
        seat.confirmBooking(user);
        assertEquals(SeatStatus.BOOKED, seat.getStatus());
        assertEquals(user, seat.getUser());
        assertNull(seat.getLockedUntil());
        assertFalse(seat.isAvailableForLock());
    }
}
