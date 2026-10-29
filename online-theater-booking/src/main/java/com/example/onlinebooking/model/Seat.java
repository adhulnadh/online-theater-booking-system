package com.example.onlinebooking.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "seats",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_show_seat_number", columnNames = {"show_id", "seat_number"})
    },
    indexes = {
        @Index(name = "idx_seat_show_status", columnList = "show_id, status"),
        @Index(name = "idx_seat_locked_until", columnList = "locked_until")
    }
)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    @NotNull(message = "Show is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seats_show"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "seats"})
    private Show show;

    @NotBlank(message = "Seat number is required")
    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @NotNull(message = "Seat status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_seats_user"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", foreignKey = @ForeignKey(name = "fk_seats_booking"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "seats"})
    private Booking booking;

    public Seat() {
    }

    public Seat(Long seatId, Show show, String seatNumber, SeatStatus status, LocalDateTime lockedUntil, User user) {
        this(seatId, show, seatNumber, status, lockedUntil, user, null);
    }

    public Seat(Long seatId, Show show, String seatNumber, SeatStatus status, LocalDateTime lockedUntil, User user, Booking booking) {
        this.seatId = seatId;
        this.show = show;
        this.seatNumber = seatNumber;
        this.status = status != null ? status : SeatStatus.AVAILABLE;
        this.lockedUntil = lockedUntil;
        this.user = user;
        this.booking = booking;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    /**
     * Check if the seat's lock has expired compared to the current time.
     */
    public boolean isLockExpired() {
        return this.status == SeatStatus.LOCKED && this.lockedUntil != null && LocalDateTime.now().isAfter(this.lockedUntil);
    }

    /**
     * Determine if seat is available for locking or booking.
     * A seat is available if marked AVAILABLE or if its previous lock has expired.
     */
    public boolean isAvailableForLock() {
        return this.status == SeatStatus.AVAILABLE || isLockExpired();
    }

    /**
     * Lock the seat for a user for a specific duration (e.g. 10 minutes).
     */
    public void lockForUser(User user, int durationMinutes) {
        this.status = SeatStatus.LOCKED;
        this.user = user;
        this.lockedUntil = LocalDateTime.now().plusMinutes(durationMinutes);
    }

    /**
     * Release seat lock back to available state.
     */
    public void releaseLock() {
        this.status = SeatStatus.AVAILABLE;
        this.user = null;
        this.lockedUntil = null;
        this.booking = null;
    }

    /**
     * Confirm booking for this seat.
     */
    public void confirmBooking(User user) {
        this.status = SeatStatus.BOOKED;
        this.user = user;
        this.lockedUntil = null;
    }

    public void confirmBooking(User user, Booking booking) {
        confirmBooking(user);
        this.booking = booking;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seat seat = (Seat) o;
        return Objects.equals(seatId, seat.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(seatId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long seatId;
        private Show show;
        private String seatNumber;
        private SeatStatus status = SeatStatus.AVAILABLE;
        private LocalDateTime lockedUntil;
        private User user;
        private Booking booking;

        public Builder seatId(Long seatId) {
            this.seatId = seatId;
            return this;
        }

        public Builder show(Show show) {
            this.show = show;
            return this;
        }

        public Builder seatNumber(String seatNumber) {
            this.seatNumber = seatNumber;
            return this;
        }

        public Builder status(SeatStatus status) {
            this.status = status;
            return this;
        }

        public Builder lockedUntil(LocalDateTime lockedUntil) {
            this.lockedUntil = lockedUntil;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder booking(Booking booking) {
            this.booking = booking;
            return this;
        }

        public Seat build() {
            return new Seat(seatId, show, seatNumber, status, lockedUntil, user, booking);
        }
    }
}
