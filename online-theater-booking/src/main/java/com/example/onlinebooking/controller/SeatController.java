package com.example.onlinebooking.controller;

import com.example.onlinebooking.dto.SeatLockResponseDto;
import com.example.onlinebooking.model.Seat;
import com.example.onlinebooking.model.User;
import com.example.onlinebooking.repository.SeatRepository;
import com.example.onlinebooking.repository.UserRepository;
import com.example.onlinebooking.service.JwtService;
import com.example.onlinebooking.service.SeatLockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SeatController {

    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final SeatLockService seatLockService;
    private final JwtService jwtService;

    public SeatController(SeatRepository seatRepository,
                          UserRepository userRepository,
                          SeatLockService seatLockService,
                          JwtService jwtService) {
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.seatLockService = seatLockService;
        this.jwtService = jwtService;
    }

    /**
     * Requirement 4: Expose a REST endpoint POST /api/seats/{seatId}/lock
     * that validates the user and performs the lock.
     *
     * Supports user identification via:
     * 1. Query parameter: ?userId=1
     * 2. Request body JSON: {"userId": 1}
     * 3. Authorization Header: Bearer <jwt_token>
     */
    @PostMapping("/seats/{seatId}/lock")
    public ResponseEntity<?> lockSeatById(
            @PathVariable Long seatId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long resolvedUserId = userId;

        // Check request body if userId param is absent
        if (resolvedUserId == null && body != null && body.get("userId") != null) {
            try {
                resolvedUserId = Long.valueOf(body.get("userId").toString());
            } catch (NumberFormatException ignored) {
            }
        }

        // Validate user via userId or Bearer JWT token
        User user = null;
        if (resolvedUserId != null) {
            user = userRepository.findById(resolvedUserId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found with id: " + resolvedUserId));
            }
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String email = jwtService.getEmailFromToken(token);
                user = userRepository.findByEmail(email).orElse(null);
                if (user == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "User not found for provided token"));
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid or expired authorization token"));
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "User validation failed: userId parameter, JSON body, or Authorization Bearer token is required"));
        }

        try {
            Seat lockedSeat = seatLockService.lockSeatById(seatId, user);
            return ResponseEntity.ok(SeatLockResponseDto.from(lockedSeat, "Seat locked successfully for 10 minutes"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ==========================================
    // Show-specific seat endpoints (Compatibility)
    // ==========================================

    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<List<Seat>> getSeatsForShow(@PathVariable Long showId) {
        return ResponseEntity.ok(seatRepository.findByShow_ShowId(showId));
    }

    @PostMapping("/shows/{showId}/seats/{seatNumber}/lock")
    public ResponseEntity<?> lockSeat(
            @PathVariable Long showId,
            @PathVariable String seatNumber,
            @RequestParam Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
            Seat lockedSeat = seatLockService.lockSeat(showId, seatNumber, user);
            return ResponseEntity.ok(lockedSeat);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/shows/{showId}/seats/{seatNumber}/book")
    public ResponseEntity<?> bookSeat(
            @PathVariable Long showId,
            @PathVariable String seatNumber,
            @RequestParam Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
            Seat bookedSeat = seatLockService.bookSeat(showId, seatNumber, user);
            return ResponseEntity.ok(bookedSeat);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
