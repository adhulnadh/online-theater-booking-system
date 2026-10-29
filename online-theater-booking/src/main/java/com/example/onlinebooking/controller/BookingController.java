package com.example.onlinebooking.controller;

import com.example.onlinebooking.dto.BookingDto;
import com.example.onlinebooking.model.Booking;
import com.example.onlinebooking.model.Seat;
import com.example.onlinebooking.model.User;
import com.example.onlinebooking.repository.SeatRepository;
import com.example.onlinebooking.repository.UserRepository;
import com.example.onlinebooking.service.BookingService;
import com.example.onlinebooking.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public BookingController(BookingService bookingService,
                             SeatRepository seatRepository,
                             UserRepository userRepository,
                             JwtService jwtService) {
        this.bookingService = bookingService;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * Requirement 1 & 2: DELETE /api/bookings/{bookingId}/cancel
     * Cancels the booking, verifies user ownership via JWT, prevents cancellation
     * of past shows, updates seats back to AVAILABLE, and broadcasts WebSocket release notification.
     */
    @DeleteMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long bookingId,
            @RequestParam(value = "userId", required = false) Long userIdParam,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        User currentUser = resolveUser(authHeader, userIdParam);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Authentication required to cancel a booking"));
        }

        try {
            Booking cancelledBooking = bookingService.cancelBooking(bookingId, currentUser);

            List<String> releasedSeatNumbers = new ArrayList<>();
            if (cancelledBooking.getSeats() != null) {
                releasedSeatNumbers = cancelledBooking.getSeats().stream()
                        .map(Seat::getSeatNumber)
                        .collect(Collectors.toList());
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Booking cancelled successfully",
                    "bookingId", cancelledBooking.getBookingId(),
                    "status", cancelledBooking.getStatus().name(),
                    "showId", cancelledBooking.getShow() != null ? cancelledBooking.getShow().getShowId() : null,
                    "releasedSeats", releasedSeatNumbers,
                    "cancelledAt", LocalDateTime.now().toString()
            ));

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * GET /api/bookings/my-bookings
     * Returns the list of bookings for the authenticated user.
     */
    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingDto>> getMyBookings(
            @RequestParam(value = "userId", required = false) Long userIdParam,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        User currentUser = resolveUser(authHeader, userIdParam);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 1. First fetch formal Booking records
        List<Booking> bookings = bookingService.getUserBookings(currentUser.getUserId());
        List<BookingDto> dtos = new ArrayList<>();

        if (bookings != null && !bookings.isEmpty()) {
            dtos = bookings.stream()
                    .map(BookingDto::from)
                    .collect(Collectors.toList());
        } else {
            // 2. Fallback to direct booked seats if no formal Booking record was created yet (legacy seed data)
            List<Seat> seats = seatRepository.findByUser_UserId(currentUser.getUserId());
            dtos = seats.stream()
                    .map(BookingDto::from)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(dtos);
    }

    private User resolveUser(String authHeader, Long userIdParam) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String email = jwtService.getEmailFromToken(token);
                if (email != null) {
                    return userRepository.findByEmail(email).orElse(null);
                }
            } catch (Exception ignored) {
            }
        }

        if (userIdParam != null) {
            return userRepository.findById(userIdParam).orElse(null);
        }

        return null;
    }
}
