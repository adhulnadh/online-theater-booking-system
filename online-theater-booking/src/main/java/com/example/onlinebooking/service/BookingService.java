package com.example.onlinebooking.service;

import com.example.onlinebooking.model.*;
import com.example.onlinebooking.repository.BookingRepository;
import com.example.onlinebooking.repository.SeatRepository;
import com.example.onlinebooking.websocket.SeatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final SeatWebSocketHandler seatWebSocketHandler;

    public BookingService(BookingRepository bookingRepository,
                          SeatRepository seatRepository,
                          SeatWebSocketHandler seatWebSocketHandler) {
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.seatWebSocketHandler = seatWebSocketHandler;
    }

    /**
     * Requirement 1, 2, 3 & 4: Atomically cancels an existing booking,
     * validates user ownership and showtime, resets seats to AVAILABLE,
     * and broadcasts real-time WebSocket seat release notifications.
     */
    @Transactional
    public Booking cancelBooking(Long bookingId, User user) {
        if (bookingId == null) {
            throw new IllegalArgumentException("Booking ID is required");
        }
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("A valid authenticated user is required");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        // Requirement 2: Security check - Ensure booking belongs to logged-in user
        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            log.warn("Security violation: User {} attempted to cancel booking {} owned by user {}",
                    user.getUserId(), bookingId, booking.getUser().getUserId());
            throw new SecurityException("You are not authorized to cancel this booking");
        }

        // Requirement 2: Showtime check - Do not allow cancellation for past shows
        LocalDateTime now = LocalDateTime.now();
        if (booking.getShow() != null && booking.getShow().getStartTime() != null
                && booking.getShow().getStartTime().isBefore(now)) {
            log.warn("Cancellation rejected: Show {} for booking {} has already passed at {}",
                    booking.getShow().getShowId(), bookingId, booking.getShow().getStartTime());
            throw new IllegalStateException("Cannot cancel booking: the showtime has already passed");
        }

        // Check if already cancelled
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking has already been cancelled");
        }

        // Requirement 3: Update booking status to CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);

        // Requirement 3: Update all associated seats back to AVAILABLE & clear user and lockedUntil
        List<Seat> seatsToRelease = new ArrayList<>();
        if (booking.getSeats() != null && !booking.getSeats().isEmpty()) {
            seatsToRelease.addAll(booking.getSeats());
        }

        Long showId = booking.getShow() != null ? booking.getShow().getShowId() : null;

        for (Seat seat : seatsToRelease) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setUser(null);
            seat.setLockedUntil(null);
            seat.setBooking(null);
            seatRepository.save(seat);

            // Requirement 4: Broadcast WebSocket message to /topic/show/{showId}
            if (seatWebSocketHandler != null && showId != null) {
                seatWebSocketHandler.broadcastSeatReleased(showId, seat.getSeatId(), seat.getSeatNumber());
            }
        }

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking {} successfully cancelled. {} seat(s) released back to AVAILABLE.",
                bookingId, seatsToRelease.size());

        return savedBooking;
    }

    /**
     * Confirms and creates a booking record for a locked seat.
     */
    @Transactional
    public Booking createBookingForSeat(Seat seat, User user) {
        Booking booking = Booking.builder()
                .user(user)
                .show(seat.getShow())
                .bookingTime(LocalDateTime.now())
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        seat.confirmBooking(user, savedBooking);
        seatRepository.save(seat);

        return savedBooking;
    }

    /**
     * Retrieves all bookings for a user.
     */
    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUser_UserIdOrderByBookingTimeDesc(userId);
    }
}
