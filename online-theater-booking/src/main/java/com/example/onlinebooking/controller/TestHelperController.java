package com.example.onlinebooking.controller;

import com.example.onlinebooking.model.Seat;
import com.example.onlinebooking.model.SeatStatus;
import com.example.onlinebooking.repository.SeatRepository;
import com.example.onlinebooking.service.SeatLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller supporting automated End-to-End integration testing and simulation.
 * Allows simulating 10-minute lock expiration, triggering the scheduler on demand,
 * and querying raw database seat state.
 */
@RestController
@RequestMapping("/api/test")
public class TestHelperController {

    private static final Logger log = LoggerFactory.getLogger(TestHelperController.class);

    private final SeatRepository seatRepository;
    private final SeatLockService seatLockService;

    public TestHelperController(SeatRepository seatRepository, SeatLockService seatLockService) {
        this.seatRepository = seatRepository;
        this.seatLockService = seatLockService;
    }

    /**
     * Simulates the passage of time by setting lockedUntil into the past (expired).
     */
    @PostMapping("/expire-seat/{seatId}")
    @Transactional
    public ResponseEntity<?> expireSeat(@PathVariable Long seatId) {
        Seat seat = seatRepository.findById(seatId).orElse(null);
        if (seat == null) {
            return ResponseEntity.notFound().build();
        }

        // Set lock expiration to 2 minutes in the past
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(2);
        seat.setLockedUntil(expiredTime);
        seatRepository.save(seat);

        log.info("TestHelper: Simulated lock expiration for seat {} (ID: {}) to {}",
                seat.getSeatNumber(), seatId, expiredTime);

        return ResponseEntity.ok(Map.of(
                "seatId", seat.getSeatId(),
                "seatNumber", seat.getSeatNumber(),
                "status", seat.getStatus().name(),
                "lockedUntil", seat.getLockedUntil().toString(),
                "message", "Seat lock expired successfully for testing"
        ));
    }

    /**
     * Executes the @Scheduled cleanupExpiredLocks logic on demand.
     */
    @PostMapping("/run-scheduler")
    public ResponseEntity<?> runScheduler() {
        log.info("TestHelper: Manually triggering cleanupExpiredLocks scheduler task");
        int releasedCount = seatLockService.cleanupExpiredLocks();
        return ResponseEntity.ok(Map.of(
                "releasedCount", releasedCount,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Queries database seat directly to verify state.
     */
    @GetMapping("/seat/{seatId}")
    public ResponseEntity<?> getSeat(@PathVariable Long seatId) {
        Seat seat = seatRepository.findById(seatId).orElse(null);
        if (seat == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
                "seatId", seat.getSeatId(),
                "seatNumber", seat.getSeatNumber(),
                "status", seat.getStatus().name(),
                "lockedUntil", seat.getLockedUntil() != null ? seat.getLockedUntil().toString() : "",
                "userId", seat.getUser() != null ? seat.getUser().getUserId() : ""
        ));
    }
}
