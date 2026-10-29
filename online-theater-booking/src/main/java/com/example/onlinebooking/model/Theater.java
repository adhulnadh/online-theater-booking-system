package com.example.onlinebooking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "theaters")
public class Theater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theater_id")
    private Long theaterId;

    @NotBlank(message = "Theater name is required")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Location is required")
    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "screen_size_inches")
    private Double screenSizeInches;

    @Column(name = "screen_aspect_ratio")
    private String screenAspectRatio;

    @OneToMany(mappedBy = "theater", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Show> shows = new ArrayList<>();

    public Theater() {
    }

    public Theater(Long theaterId, String name, String location, Double screenSizeInches, String screenAspectRatio) {
        this.theaterId = theaterId;
        this.name = name;
        this.location = location;
        this.screenSizeInches = screenSizeInches;
        this.screenAspectRatio = screenAspectRatio;
    }

    public Long getTheaterId() {
        return theaterId;
    }

    public void setTheaterId(Long theaterId) {
        this.theaterId = theaterId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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
        Theater theater = (Theater) o;
        return Objects.equals(theaterId, theater.theaterId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(theaterId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long theaterId;
        private String name;
        private String location;
        private Double screenSizeInches;
        private String screenAspectRatio;
        private List<Show> shows = new ArrayList<>();

        public Builder theaterId(Long theaterId) {
            this.theaterId = theaterId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder screenSizeInches(Double screenSizeInches) {
            this.screenSizeInches = screenSizeInches;
            return this;
        }

        public Builder screenAspectRatio(String screenAspectRatio) {
            this.screenAspectRatio = screenAspectRatio;
            return this;
        }

        public Builder shows(List<Show> shows) {
            this.shows = shows;
            return this;
        }

        public Theater build() {
            Theater theater = new Theater(theaterId, name, location, screenSizeInches, screenAspectRatio);
            if (shows != null) {
                theater.setShows(shows);
            }
            return theater;
        }
    }
}
