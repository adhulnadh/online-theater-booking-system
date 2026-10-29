package com.example.onlinebooking.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_bookings_user", columnList = "user_id"),
        @Index(name = "idx_bookings_show", columnList = "show_id"),
        @Index(name = "idx_bookings_status", columnList = "status")
    }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bookings_user"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "passwordHash"})
    private User user;

    @NotNull(message = "Show is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bookings_show"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Show show;

    @NotNull(message = "Booking time is required")
    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime = LocalDateTime.now();

    @NotNull(message = "Booking status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @OneToMany(mappedBy = "booking", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "booking"})
    private List<Seat> seats = new ArrayList<>();

    public Booking() {
    }

    public Booking(Long bookingId, User user, Show show, LocalDateTime bookingTime, BookingStatus status) {
        this.bookingId = bookingId;
        this.user = user;
        this.show = show;
        this.bookingTime = bookingTime != null ? bookingTime : LocalDateTime.now();
        this.status = status != null ? status : BookingStatus.CONFIRMED;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    public void addSeat(Seat seat) {
        if (seat != null) {
            this.seats.add(seat);
            seat.setBooking(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return Objects.equals(bookingId, booking.bookingId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bookingId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long bookingId;
        private User user;
        private Show show;
        private LocalDateTime bookingTime = LocalDateTime.now();
        private BookingStatus status = BookingStatus.CONFIRMED;
        private List<Seat> seats = new ArrayList<>();

        public Builder bookingId(Long bookingId) {
            this.bookingId = bookingId;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder show(Show show) {
            this.show = show;
            return this;
        }

        public Builder bookingTime(LocalDateTime bookingTime) {
            this.bookingTime = bookingTime;
            return this;
        }

        public Builder status(BookingStatus status) {
            this.status = status;
            return this;
        }

        public Builder seats(List<Seat> seats) {
            this.seats = seats;
            return this;
        }

        public Booking build() {
            Booking booking = new Booking(bookingId, user, show, bookingTime, status);
            if (seats != null) {
                booking.setSeats(seats);
            }
            return booking;
        }
    }
}
