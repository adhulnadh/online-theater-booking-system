package com.example.onlinebooking.dto;

import com.example.onlinebooking.model.Seat;
import com.example.onlinebooking.model.SeatStatus;

import java.time.LocalDateTime;

public class SeatLockResponseDto {

    private Long seatId;
    private String seatNumber;
    private SeatStatus status;
    private LocalDateTime lockedUntil;
    private Long userId;
    private String userEmail;
    private Long showId;
    private String message;

    public SeatLockResponseDto() {
    }

    public static SeatLockResponseDto from(Seat seat, String message) {
        SeatLockResponseDto dto = new SeatLockResponseDto();
        dto.setSeatId(seat.getSeatId());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setStatus(seat.getStatus());
        dto.setLockedUntil(seat.getLockedUntil());
        if (seat.getUser() != null) {
            dto.setUserId(seat.getUser().getUserId());
            dto.setUserEmail(seat.getUser().getEmail());
        }
        if (seat.getShow() != null) {
            dto.setShowId(seat.getShow().getShowId());
        }
        dto.setMessage(message);
        return dto;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
