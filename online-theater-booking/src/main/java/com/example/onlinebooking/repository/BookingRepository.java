package com.example.onlinebooking.repository;

import com.example.onlinebooking.model.Booking;
import com.example.onlinebooking.model.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"user", "show", "show.movie", "show.theater", "seats"})
    List<Booking> findByUser_UserIdOrderByBookingTimeDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "show", "show.movie", "show.theater", "seats"})
    List<Booking> findByUser_UserIdAndStatusOrderByBookingTimeDesc(Long userId, BookingStatus status);

    @EntityGraph(attributePaths = {"user", "show", "show.movie", "show.theater", "seats"})
    Optional<Booking> findById(Long bookingId);

    @EntityGraph(attributePaths = {"user", "show", "show.movie", "show.theater", "seats"})
    Optional<Booking> findByBookingIdAndUser_UserId(Long bookingId, Long userId);
}
