package com.example.onlinebooking.dto;

public class SeatReleaseMessage {

    private Long showId;
    private Long seatId;
    private String seatNumber;
    private String status;
    private String action;
    private String timestamp;

    public SeatReleaseMessage() {
    }

    public SeatReleaseMessage(Long showId, Long seatId, String seatNumber, String status, String action, String timestamp) {
        this.showId = showId;
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.status = status;
        this.action = action;
        this.timestamp = timestamp;
    }

    public static SeatReleaseMessage of(Long showId, Long seatId, String seatNumber) {
        return new SeatReleaseMessage(
                showId,
                seatId,
                seatNumber,
                "AVAILABLE",
                "SEAT_RELEASED",
                java.time.LocalDateTime.now().toString()
        );
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
