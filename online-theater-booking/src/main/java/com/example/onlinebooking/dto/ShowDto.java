package com.example.onlinebooking.dto;

import com.example.onlinebooking.model.Show;

import java.time.LocalDateTime;

public class ShowDto {

    private Long showId;
    private Long movieId;
    private String movieTitle;
    private Long theaterId;
    private String theaterName;
    private String theaterLocation;
    private Double screenSizeInches;
    private String screenAspectRatio;
    private LocalDateTime startTime;

    public ShowDto() {
    }

    public ShowDto(Long showId, Long movieId, String movieTitle, Long theaterId, String theaterName,
                   String theaterLocation, Double screenSizeInches, String screenAspectRatio, LocalDateTime startTime) {
        this.showId = showId;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.theaterId = theaterId;
        this.theaterName = theaterName;
        this.theaterLocation = theaterLocation;
        this.screenSizeInches = screenSizeInches;
        this.screenAspectRatio = screenAspectRatio;
        this.startTime = startTime;
    }

    public static ShowDto from(Show show) {
        return new ShowDto(
                show.getShowId(),
                show.getMovie() != null ? show.getMovie().getMovieId() : null,
                show.getMovie() != null ? show.getMovie().getTitle() : null,
                show.getTheater() != null ? show.getTheater().getTheaterId() : null,
                show.getTheater() != null ? show.getTheater().getName() : null,
                show.getTheater() != null ? show.getTheater().getLocation() : null,
                show.getTheater() != null ? show.getTheater().getScreenSizeInches() : null,
                show.getTheater() != null ? show.getTheater().getScreenAspectRatio() : null,
                show.getStartTime()
        );
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public Long getTheaterId() {
        return theaterId;
    }

    public void setTheaterId(Long theaterId) {
        this.theaterId = theaterId;
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

    public Double getScreenSizeInches() {
        return screenSizeInches;
    }

    public void setScreenSizeInches(Double screenSizeInches) {
        this.screenSizeInches = screenSizeInches;
    }

    public String getScreenAspectRatio() {
        return screenAspectRatio;
    }

    public void setScreenAspectRatio(String screenAspectRatio) {
        this.screenAspectRatio = screenAspectRatio;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
}
