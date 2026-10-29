package com.example.onlinebooking.repository;

import com.example.onlinebooking.model.Seat;
import com.example.onlinebooking.model.SeatStatus;
import com.example.onlinebooking.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    @EntityGraph(attributePaths = {"show", "user"})
    List<Seat> findByShow_ShowId(Long showId);

    List<Seat> findByShow_ShowIdAndStatus(Long showId, SeatStatus status);

    Optional<Seat> findByShow_ShowIdAndSeatNumber(Long showId, String seatNumber);

    List<Seat> findByUser_UserId(Long userId);

    List<Seat> findByStatusAndLockedUntilBefore(SeatStatus status, LocalDateTime dateTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.show.showId = :showId AND s.seatNumber = :seatNumber")
    Optional<Seat> findSeatForUpdate(@Param("showId") Long showId, @Param("seatNumber") String seatNumber);

    /**
     * Atomic UPDATE query ensuring two users cannot lock the same seat simultaneously.
     * Updates status to LOCKED and sets lockedUntil and user only if the seat is AVAILABLE
     * or has an expired lock.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Seat s SET s.status = :lockedStatus, s.lockedUntil = :lockedUntil, s.user = :user " +
           "WHERE s.seatId = :seatId AND (s.status = :availableStatus OR (s.status = :lockedStatus AND s.lockedUntil < :now))")
    int lockSeatAtomic(@Param("seatId") Long seatId,
                       @Param("lockedStatus") SeatStatus lockedStatus,
                       @Param("lockedUntil") LocalDateTime lockedUntil,
                       @Param("user") User user,
                       @Param("availableStatus") SeatStatus availableStatus,
                       @Param("now") LocalDateTime now);

    /**
     * Strict atomic UPDATE query where status must be exactly AVAILABLE.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Seat s SET s.status = :lockedStatus, s.lockedUntil = :lockedUntil, s.user = :user " +
           "WHERE s.seatId = :seatId AND s.status = :availableStatus")
    int lockSeatAtomicStrict(@Param("seatId") Long seatId,
                             @Param("lockedStatus") SeatStatus lockedStatus,
                             @Param("lockedUntil") LocalDateTime lockedUntil,
                             @Param("user") User user,
                             @Param("availableStatus") SeatStatus availableStatus);

    /**
     * Releases expired seat locks by atomically resetting them to AVAILABLE.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Seat s SET s.status = :availableStatus, s.user = null, s.lockedUntil = null " +
           "WHERE s.status = :lockedStatus AND s.lockedUntil < :now")
    int releaseExpiredLocks(@Param("availableStatus") SeatStatus availableStatus,
                            @Param("lockedStatus") SeatStatus lockedStatus,
                            @Param("now") LocalDateTime now);
}
