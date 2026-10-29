package com.example.onlinebooking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long movieId;

    @NotBlank(message = "Movie title is required")
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "aspect_ratio")
    private String aspectRatio;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be greater than 0 minutes")
    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "rating")
    private Double rating = 8.5;

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Show> shows = new ArrayList<>();

    public Movie() {
    }

    public Movie(Long movieId, String title, String aspectRatio, Integer duration, String posterUrl) {
        this(movieId, title, aspectRatio, duration, posterUrl, 8.5);
    }

    public Movie(Long movieId, String title, String aspectRatio, Integer duration, String posterUrl, Double rating) {
        this.movieId = movieId;
        this.title = title;
        this.aspectRatio = aspectRatio;
        this.duration = duration;
        this.posterUrl = posterUrl;
        this.rating = rating;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public List<Show> getShows() {
        return shows;
    }

    public void setShows(List<Show> shows) {
        this.shows = shows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return Objects.equals(movieId, movie.movieId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(movieId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long movieId;
        private String title;
        private String aspectRatio;
        private Integer duration;
        private String posterUrl;
        private Double rating = 8.5;
        private List<Show> shows = new ArrayList<>();

        public Builder movieId(Long movieId) {
            this.movieId = movieId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder aspectRatio(String aspectRatio) {
            this.aspectRatio = aspectRatio;
            return this;
        }

        public Builder duration(Integer duration) {
            this.duration = duration;
            return this;
        }

        public Builder posterUrl(String posterUrl) {
            this.posterUrl = posterUrl;
            return this;
        }

        public Builder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        public Builder shows(List<Show> shows) {
            this.shows = shows;
            return this;
        }

        public Movie build() {
            Movie movie = new Movie(movieId, title, aspectRatio, duration, posterUrl, rating);
            if (shows != null) {
                movie.setShows(shows);
            }
            return movie;
        }
    }
}
