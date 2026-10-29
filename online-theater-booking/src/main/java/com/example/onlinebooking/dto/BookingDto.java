package com.example.onlinebooking.dto;

import com.example.onlinebooking.model.Booking;
import com.example.onlinebooking.model.BookingStatus;
import com.example.onlinebooking.model.Seat;
import com.example.onlinebooking.model.SeatStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BookingDto {

    private Long bookingId;
    private Long userId;
    private Long seatId;
    private String seatNumber;
    private List<String> seatNumbers = new ArrayList<>();
    private List<Long> seatIds = new ArrayList<>();
    private String status;
    private LocalDateTime lockedUntil;
    private Long showId;
    private String movieTitle;
    private String posterUrl;
    private String theaterName;
    private String theaterLocation;
    private LocalDateTime showStartTime;
    private LocalDateTime bookingTime;
    private boolean canCancel;

    public BookingDto() {
    }

    public static BookingDto from(Booking booking) {
        BookingDto dto = new BookingDto();
        dto.setBookingId(booking.getBookingId());
        dto.setStatus(booking.getStatus().name());
        dto.setBookingTime(booking.getBookingTime());

        if (booking.getUser() != null) {
            dto.setUserId(booking.getUser().getUserId());
        }

        if (booking.getShow() != null) {
            dto.setShowId(booking.getShow().getShowId());
            dto.setShowStartTime(booking.getShow().getStartTime());

            if (booking.getShow().getMovie() != null) {
                dto.setMovieTitle(booking.getShow().getMovie().getTitle());
                dto.setPosterUrl(booking.getShow().getMovie().getPosterUrl());
            }
            if (booking.getShow().getTheater() != null) {
                dto.setTheaterName(booking.getShow().getTheater().getName());
                dto.setTheaterLocation(booking.getShow().getTheater().getLocation());
            }
        }

        if (booking.getSeats() != null && !booking.getSeats().isEmpty()) {
            List<String> seatNums = booking.getSeats().stream()
                    .map(Seat::getSeatNumber)
                    .collect(Collectors.toList());
            List<Long> sIds = booking.getSeats().stream()
                    .map(Seat::getSeatId)
                    .collect(Collectors.toList());

            dto.setSeatNumbers(seatNums);
            dto.setSeatIds(sIds);
            dto.setSeatNumber(String.join(", ", seatNums));
            dto.setSeatId(sIds.get(0));
        }

        // A booking can be cancelled if it is CONFIRMED and the showtime is in the future
        boolean isUpcoming = dto.getShowStartTime() != null && dto.getShowStartTime().isAfter(LocalDateTime.now());
        dto.setCanCancel(booking.getStatus() == BookingStatus.CONFIRMED && isUpcoming);

        return dto;
    }

    public static BookingDto from(Seat seat) {
        BookingDto dto = new BookingDto();
        dto.setSeatId(seat.getSeatId());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setSeatNumbers(List.of(seat.getSeatNumber()));
        dto.setSeatIds(List.of(seat.getSeatId()));
        dto.setStatus(seat.getStatus().name());
        dto.setLockedUntil(seat.getLockedUntil());

        if (seat.getBooking() != null) {
            dto.setBookingId(seat.getBooking().getBookingId());
            dto.setBookingTime(seat.getBooking().getBookingTime());
            dto.setStatus(seat.getBooking().getStatus().name());
        }

        if (seat.getShow() != null) {
            dto.setShowId(seat.getShow().getShowId());
            dto.setShowStartTime(seat.getShow().getStartTime());
            if (seat.getShow().getMovie() != null) {
                dto.setMovieTitle(seat.getShow().getMovie().getTitle());
                dto.setPosterUrl(seat.getShow().getMovie().getPosterUrl());
            }
            if (seat.getShow().getTheater() != null) {
                dto.setTheaterName(seat.getShow().getTheater().getName());
                dto.setTheaterLocation(seat.getShow().getTheater().getLocation());
            }
        }

        boolean isUpcoming = dto.getShowStartTime() != null && dto.getShowStartTime().isAfter(LocalDateTime.now());
        boolean isConfirmed = "BOOKED".equalsIgnoreCase(dto.getStatus()) || "CONFIRMED".equalsIgnoreCase(dto.getStatus());
        dto.setCanCancel(isConfirmed && isUpcoming);

        return dto;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public List<String> getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(List<String> seatNumbers) {
        this.seatNumbers = seatNumbers;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getTheaterName() {
        return theaterName;
    }

    public void setTheaterName(String theaterName) {
        this.theaterName = theaterName;
    }

    public String getTheaterLocation() {
        return theaterLocation;
    }

    public void setTheaterLocation(String theaterLocation) {
        this.theaterLocation = theaterLocation;
    }

    public LocalDateTime getShowStartTime() {
        return showStartTime;
    }

    public void setShowStartTime(LocalDateTime showStartTime) {
        this.showStartTime = showStartTime;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public boolean isCanCancel() {
        return canCancel;
    }

    public void setCanCancel(boolean canCancel) {
        this.canCancel = canCancel;
    }
}
