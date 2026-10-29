package com.example.onlinebooking.service;

import com.example.onlinebooking.model.*;
import com.example.onlinebooking.repository.BookingRepository;
import com.example.onlinebooking.repository.SeatRepository;
import com.example.onlinebooking.websocket.SeatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeatLockService {

    private static final Logger log = LoggerFactory.getLogger(SeatLockService.class);
    public static final int LOCK_DURATION_MINUTES = 10;

    private final SeatRepository seatRepository;
    private final SeatWebSocketHandler seatWebSocketHandler;
    private final BookingRepository bookingRepository;

    @Autowired
    public SeatLockService(SeatRepository seatRepository,
                           SeatWebSocketHandler seatWebSocketHandler,
                           @Autowired(required = false) BookingRepository bookingRepository) {
        this.seatRepository = seatRepository;
        this.seatWebSocketHandler = seatWebSocketHandler;
        this.bookingRepository = bookingRepository;
    }

    public SeatLockService(SeatRepository seatRepository, SeatWebSocketHandler seatWebSocketHandler) {
        this(seatRepository, seatWebSocketHandler, null);
    }

    public SeatLockService(SeatRepository seatRepository) {
        this(seatRepository, null, null);
    }

    /**
     * Atomically locks a seat for a user by seatId for 10 minutes.
     * Uses an atomic database UPDATE query to ensure two users cannot lock
     * the same seat simultaneously without race conditions.
     *
     * @param seatId the ID of the seat to lock
     * @param user   the user requesting the lock
     * @return the locked Seat entity
     * @throws IllegalArgumentException if seat or user is not found
     * @throws IllegalStateException    if the seat is already locked or booked
     */
    @Transactional
    public Seat lockSeatById(Long seatId, User user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("A valid user is required to lock a seat");
        }

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found with id: " + seatId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockedUntil = now.plusMinutes(LOCK_DURATION_MINUTES);

        // Atomic update query: updates status to LOCKED, lockedUntil, and user
        // only if seat is AVAILABLE or has an expired lock
        int rowsUpdated = seatRepository.lockSeatAtomic(
                seatId,
                SeatStatus.LOCKED,
                lockedUntil,
                user,
                SeatStatus.AVAILABLE,
                now
        );

        if (rowsUpdated == 0) {
            throw new IllegalStateException("Seat " + seat.getSeatNumber() + " (ID: " + seatId +
                    ") is not available for locking (already locked or booked)");
        }

        log.info("Seat {} (ID: {}) atomically locked for user {} until {}",
                seat.getSeatNumber(), seatId, user.getEmail(), lockedUntil);

        Seat lockedSeat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalStateException("Could not reload locked seat: " + seatId));

        // Real-time notification to the show topic that seat was locked
        if (seatWebSocketHandler != null && lockedSeat.getShow() != null) {
            seatWebSocketHandler.broadcastSeatLocked(
                    lockedSeat.getShow().getShowId(),
                    seatId,
                    lockedSeat.getSeatNumber(),
                    lockedUntil.toString()
            );
        }

        return lockedSeat;
    }

    /**
     * Locks a seat by showId and seatNumber for backward compatibility.
     */
    @Transactional
    public Seat lockSeat(Long showId, String seatNumber, User user) {
        Seat seat = seatRepository.findByShow_ShowIdAndSeatNumber(showId, seatNumber)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found for show: " + showId + " and seat: " + seatNumber));

        return lockSeatById(seat.getSeatId(), user);
    }

    /**
     * Confirms the booking for a locked seat by the user before lock expiration.
     */
    @Transactional
    public Seat bookSeat(Long showId, String seatNumber, User user) {
        Seat seat = seatRepository.findSeatForUpdate(showId, seatNumber)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));

        if (seat.getStatus() == SeatStatus.BOOKED) {
            throw new IllegalStateException("Seat is already booked");
        }

        if (seat.getStatus() == SeatStatus.LOCKED) {
            if (seat.isLockExpired()) {
                throw new IllegalStateException("Seat lock has expired. Please lock again.");
            }
            if (seat.getUser() != null && !seat.getUser().getUserId().equals(user.getUserId())) {
                throw new IllegalStateException("Seat is locked by another user");
            }
        }

        Booking booking = null;
        if (bookingRepository != null) {
            booking = bookingRepository.save(Booking.builder()
                    .user(user)
                    .show(seat.getShow())
                    .bookingTime(LocalDateTime.now())
                    .status(BookingStatus.CONFIRMED)
                    .build());
        }

        seat.confirmBooking(user, booking);
        log.info("Seat {} successfully booked for user {} (Booking ID: {})",
                seatNumber, user.getEmail(), booking != null ? booking.getBookingId() : "N/A");
        return seatRepository.save(seat);
    }

    /**
     * Requirement 3: Scheduled task running every 1 minute (60,000 ms) to find seats where
     * status = 'LOCKED' AND locked_until < NOW(), resetting them back to 'AVAILABLE'
     * and triggering a WebSocket message to that show's topic with the seat ID that just became available.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public int cleanupExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> expiredSeats = seatRepository.findByStatusAndLockedUntilBefore(SeatStatus.LOCKED, now);
        if (expiredSeats.isEmpty()) {
            return 0;
        }

        int releasedCount = seatRepository.releaseExpiredLocks(SeatStatus.AVAILABLE, SeatStatus.LOCKED, now);

        // Broadcast to each show's topic (/topic/show/{showId})
        if (seatWebSocketHandler != null) {
            for (Seat seat : expiredSeats) {
                Long showId = seat.getShow() != null ? seat.getShow().getShowId() : null;
                if (showId != null) {
                    seatWebSocketHandler.broadcastSeatReleased(showId, seat.getSeatId(), seat.getSeatNumber());
                }
            }
        }

        if (releasedCount > 0) {
            log.info("Released and broadcasted {} expired seat lock(s) back to AVAILABLE at {}", releasedCount, now);
        }
        return releasedCount;
    }
}
