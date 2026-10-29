package com.example.onlinebooking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "show_id")
    private Long showId;

    @NotNull(message = "Movie is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false, foreignKey = @ForeignKey(name = "fk_shows_movie"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "shows"})
    private Movie movie;

    @NotNull(message = "Theater is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "theater_id", nullable = false, foreignKey = @ForeignKey(name = "fk_shows_theater"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "shows"})
    private Theater theater;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Seat> seats = new ArrayList<>();

    public Show() {
    }

    public Show(Long showId, Movie movie, Theater theater, LocalDateTime startTime) {
        this.showId = showId;
        this.movie = movie;
        this.theater = theater;
        this.startTime = startTime;
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public Theater getTheater() {
        return theater;
    }

    public void setTheater(Theater theater) {
        this.theater = theater;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Show show = (Show) o;
        return Objects.equals(showId, show.showId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(showId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long showId;
        private Movie movie;
        private Theater theater;
        private LocalDateTime startTime;
        private List<Seat> seats = new ArrayList<>();

        public Builder showId(Long showId) {
            this.showId = showId;
            return this;
        }

        public Builder movie(Movie movie) {
            this.movie = movie;
            return this;
        }

        public Builder theater(Theater theater) {
            this.theater = theater;
            return this;
        }

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder seats(List<Seat> seats) {
            this.seats = seats;
            return this;
        }

        public Show build() {
            Show show = new Show(showId, movie, theater, startTime);
            if (seats != null) {
                show.setSeats(seats);
            }
            return show;
        }
    }
}
