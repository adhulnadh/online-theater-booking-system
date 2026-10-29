package com.example.onlinebooking.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for calculating recommendation scores for theaters
 * based on screen size and aspect ratio match against a specific movie.
 */
public final class TheaterScoreCalculator {

    public static final double DEFAULT_BENCHMARK_SCREEN_INCHES = 1000.0;
    public static final double DEFAULT_SIZE_WEIGHT = 0.5;
    public static final double DEFAULT_ASPECT_WEIGHT = 0.5;

    private TheaterScoreCalculator() {
        // Utility class
    }

    /**
     * Parses an aspect ratio string into its numeric decimal ratio.
     * Supported formats: "2.39:1", "16:9", "1.85:1", "16/9", "2.39".
     */
    public static double parseAspectRatio(String ratioStr) {
        if (ratioStr == null || ratioStr.trim().isEmpty()) {
            return 1.7778; // Default 16:9
        }

        String cleaned = ratioStr.trim().replaceAll("\\s+", "");

        try {
            if (cleaned.contains(":")) {
                String[] parts = cleaned.split(":");
                double width = Double.parseDouble(parts[0]);
                double height = parts.length > 1 ? Double.parseDouble(parts[1]) : 1.0;
                return height != 0 ? width / height : width;
            } else if (cleaned.contains("/")) {
                String[] parts = cleaned.split("/");
                double width = Double.parseDouble(parts[0]);
                double height = parts.length > 1 ? Double.parseDouble(parts[1]) : 1.0;
                return height != 0 ? width / height : width;
            } else {
                return Double.parseDouble(cleaned);
            }
        } catch (NumberFormatException e) {
            return 1.7778; // Fallback to 16:9 if unparseable
        }
    }

    /**
     * Calculates the absolute difference between movie aspect ratio and theater aspect ratio.
     */
    public static double calculateAspectRatioDifference(String movieAspectRatio, String theaterAspectRatio) {
        double movieRatio = parseAspectRatio(movieAspectRatio);
        double theaterRatio = parseAspectRatio(theaterAspectRatio);
        return Math.abs(movieRatio - theaterRatio);
    }

    /**
     * Calculates the aspect ratio match score (0 - 100).
     * Closer matches yield scores closer to 100.
     * A difference of 0.0 gives 100.0.
     */
    public static double calculateAspectRatioScore(String movieAspectRatio, String theaterAspectRatio) {
        double diff = calculateAspectRatioDifference(movieAspectRatio, theaterAspectRatio);
        // Penalize 100 points per 1.0 difference, bounded between 0 and 100
        double score = 100.0 - (diff * 100.0);
        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * Calculates the screen size score (0 - 100) relative to a benchmark screen size (1000").
     * Larger screens yield higher scores.
     */
    public static double calculateScreenSizeScore(Double screenSizeInches) {
        if (screenSizeInches == null || screenSizeInches <= 0) {
            return 0.0;
        }
        double score = (screenSizeInches / DEFAULT_BENCHMARK_SCREEN_INCHES) * 100.0;
        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * Calculates composite score with default weights (50% size, 50% aspect ratio).
     */
    public static ScoreBreakdown calculateScore(Double screenSizeInches, String movieAspectRatio, String theaterAspectRatio) {
        return calculateScore(screenSizeInches, movieAspectRatio, theaterAspectRatio, DEFAULT_SIZE_WEIGHT, DEFAULT_ASPECT_WEIGHT);
    }

    /**
     * Calculates composite score with custom weights.
     */
    public static ScoreBreakdown calculateScore(Double screenSizeInches,
                                                String movieAspectRatio,
                                                String theaterAspectRatio,
                                                double sizeWeight,
                                                double aspectWeight) {
        double movieRatio = parseAspectRatio(movieAspectRatio);
        double theaterRatio = parseAspectRatio(theaterAspectRatio);
        double diff = Math.abs(movieRatio - theaterRatio);

        double sizeScore = calculateScreenSizeScore(screenSizeInches);
        double aspectScore = calculateAspectRatioScore(movieAspectRatio, theaterAspectRatio);

        double totalWeight = sizeWeight + aspectWeight;
        double normalizedSizeWeight = totalWeight > 0 ? sizeWeight / totalWeight : 0.5;
        double normalizedAspectWeight = totalWeight > 0 ? aspectWeight / totalWeight : 0.5;

        double composite = (normalizedSizeWeight * sizeScore) + (normalizedAspectWeight * aspectScore);
        double roundedTotal = round(composite, 1);

        return new ScoreBreakdown(
                roundedTotal,
                round(sizeScore, 1),
                round(aspectScore, 1),
                round(diff, 3),
                round(movieRatio, 3),
                round(theaterRatio, 3)
        );
    }

    private static double round(double value, int places) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    public record ScoreBreakdown(
            double totalScore,
            double screenSizeScore,
            double aspectRatioScore,
            double aspectRatioDifference,
            double movieAspectRatioValue,
            double theaterAspectRatioValue
    ) {}
}
