package com.example.onlinebooking.dto;

import com.example.onlinebooking.model.Theater;
import com.example.onlinebooking.util.TheaterScoreCalculator.ScoreBreakdown;

public class RecommendedTheaterDto {

    private Long theaterId;
    private String name;
    private String location;
    private Double screenSizeInches;
    private String screenAspectRatio;
    private Double score;
    private Double screenSizeScore;
    private Double aspectRatioScore;
    private Double aspectRatioDifference;
    private String matchQuality;
    private boolean hasActiveShows;
    private int activeShowsCount;

    public RecommendedTheaterDto() {
    }

    public static RecommendedTheaterDto from(Theater theater, ScoreBreakdown breakdown, int activeShowsCount) {
        RecommendedTheaterDto dto = new RecommendedTheaterDto();
        dto.setTheaterId(theater.getTheaterId());
        dto.setName(theater.getName());
        dto.setLocation(theater.getLocation());
        dto.setScreenSizeInches(theater.getScreenSizeInches());
        dto.setScreenAspectRatio(theater.getScreenAspectRatio());
        dto.setScore(breakdown.totalScore());
        dto.setScreenSizeScore(breakdown.screenSizeScore());
        dto.setAspectRatioScore(breakdown.aspectRatioScore());
        dto.setAspectRatioDifference(breakdown.aspectRatioDifference());
        dto.setMatchQuality(getMatchQualityLabel(breakdown.aspectRatioDifference()));
        dto.setActiveShowsCount(activeShowsCount);
        dto.setHasActiveShows(activeShowsCount > 0);
        return dto;
    }

    private static String getMatchQualityLabel(double diff) {
        if (diff < 0.05) return "Perfect Match";
        if (diff < 0.20) return "Excellent Match";
        if (diff < 0.45) return "Good Match";
        return "Standard Match";
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

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getScreenSizeScore() {
        return screenSizeScore;
    }

    public void setScreenSizeScore(Double screenSizeScore) {
        this.screenSizeScore = screenSizeScore;
    }

    public Double getAspectRatioScore() {
        return aspectRatioScore;
    }

    public void setAspectRatioScore(Double aspectRatioScore) {
        this.aspectRatioScore = aspectRatioScore;
    }

    public Double getAspectRatioDifference() {
        return aspectRatioDifference;
    }

    public void setAspectRatioDifference(Double aspectRatioDifference) {
        this.aspectRatioDifference = aspectRatioDifference;
    }

    public String getMatchQuality() {
        return matchQuality;
    }

    public void setMatchQuality(String matchQuality) {
        this.matchQuality = matchQuality;
    }

    public boolean isHasActiveShows() {
        return hasActiveShows;
    }

    public void setHasActiveShows(boolean hasActiveShows) {
        this.hasActiveShows = hasActiveShows;
    }

    public int getActiveShowsCount() {
        return activeShowsCount;
    }

    public void setActiveShowsCount(int activeShowsCount) {
        this.activeShowsCount = activeShowsCount;
    }
}
