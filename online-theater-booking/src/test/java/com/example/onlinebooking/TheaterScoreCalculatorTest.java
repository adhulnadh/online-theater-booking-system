package com.example.onlinebooking;

import com.example.onlinebooking.util.TheaterScoreCalculator;
import com.example.onlinebooking.util.TheaterScoreCalculator.ScoreBreakdown;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TheaterScoreCalculatorTest {

    @Test
    @DisplayName("Test parsing of various aspect ratio string formats")
    void testParseAspectRatio() {
        assertEquals(2.39, TheaterScoreCalculator.parseAspectRatio("2.39:1"), 0.001);
        assertEquals(1.85, TheaterScoreCalculator.parseAspectRatio("1.85:1"), 0.001);
        assertEquals(1.90, TheaterScoreCalculator.parseAspectRatio("1.90:1"), 0.001);
        assertEquals(16.0 / 9.0, TheaterScoreCalculator.parseAspectRatio("16:9"), 0.001);
        assertEquals(16.0 / 9.0, TheaterScoreCalculator.parseAspectRatio("16/9"), 0.001);
        assertEquals(2.35, TheaterScoreCalculator.parseAspectRatio("2.35"), 0.001);

        // Fallbacks for null or invalid inputs
        assertEquals(1.7778, TheaterScoreCalculator.parseAspectRatio(null), 0.001);
        assertEquals(1.7778, TheaterScoreCalculator.parseAspectRatio(""), 0.001);
        assertEquals(1.7778, TheaterScoreCalculator.parseAspectRatio("invalid-ratio"), 0.001);
    }

    @Test
    @DisplayName("Larger screen size must score strictly higher than smaller screen size")
    void testScreenSizeScoresHigher() {
        double scoreSmall = TheaterScoreCalculator.calculateScreenSizeScore(500.0);
        double scoreMedium = TheaterScoreCalculator.calculateScreenSizeScore(750.0);
        double scoreLarge = TheaterScoreCalculator.calculateScreenSizeScore(1000.0);

        assertTrue(scoreLarge > scoreMedium, "1000\" screen should score higher than 750\" screen");
        assertTrue(scoreMedium > scoreSmall, "750\" screen should score higher than 500\" screen");

        assertEquals(100.0, scoreLarge, 0.01);
        assertEquals(75.0, scoreMedium, 0.01);
        assertEquals(50.0, scoreSmall, 0.01);

        assertEquals(0.0, TheaterScoreCalculator.calculateScreenSizeScore(null));
        assertEquals(0.0, TheaterScoreCalculator.calculateScreenSizeScore(-50.0));
    }

    @Test
    @DisplayName("Closer aspect ratio match must score strictly higher than distant match")
    void testCloserAspectRatioMatchScoresHigher() {
        String movieRatio = "2.39:1";
        String perfectMatch = "2.39:1";
        String closeMatch = "2.20:1";
        String moderateMatch = "1.90:1";
        String distantMatch = "1.43:1";

        double scorePerfect = TheaterScoreCalculator.calculateAspectRatioScore(movieRatio, perfectMatch);
        double scoreClose = TheaterScoreCalculator.calculateAspectRatioScore(movieRatio, closeMatch);
        double scoreModerate = TheaterScoreCalculator.calculateAspectRatioScore(movieRatio, moderateMatch);
        double scoreDistant = TheaterScoreCalculator.calculateAspectRatioScore(movieRatio, distantMatch);

        assertEquals(100.0, scorePerfect, 0.01, "Perfect match should score 100.0");
        assertTrue(scorePerfect > scoreClose, "Perfect match should score higher than 2.20:1");
        assertTrue(scoreClose > scoreModerate, "Close match (2.20:1) should score higher than 1.90:1");
        assertTrue(scoreModerate > scoreDistant, "Moderate match (1.90:1) should score higher than 1.43:1");
    }

    @Test
    @DisplayName("Total score balances screen size and aspect ratio match")
    void testTotalScoreCalculation() {
        // Inception (2.39:1)
        String movieRatio = "2.39:1";

        // Theater A: 920" screen, 2.39:1 (Huge screen, perfect aspect ratio)
        ScoreBreakdown scoreA = TheaterScoreCalculator.calculateScore(920.0, movieRatio, "2.39:1");

        // Theater B: 600" screen, 2.39:1 (Smaller screen, perfect aspect ratio)
        ScoreBreakdown scoreB = TheaterScoreCalculator.calculateScore(600.0, movieRatio, "2.39:1");

        // Theater C: 840" screen, 1.90:1 (Large screen, mismatched aspect ratio)
        ScoreBreakdown scoreC = TheaterScoreCalculator.calculateScore(840.0, movieRatio, "1.90:1");

        // Verify Theater A beats Theater B due to larger screen size
        assertTrue(scoreA.totalScore() > scoreB.totalScore(), "Large screen with perfect match should beat smaller screen with perfect match");

        // Verify Theater A beats Theater C
        assertTrue(scoreA.totalScore() > scoreC.totalScore(), "Theater A has larger size and better aspect ratio match than Theater C");

        assertEquals(0.0, scoreA.aspectRatioDifference(), 0.001);
        assertEquals(100.0, scoreA.aspectRatioScore(), 0.01);
        assertEquals(92.0, scoreA.screenSizeScore(), 0.01);
        assertEquals(96.0, scoreA.totalScore(), 0.01); // (0.5 * 92) + (0.5 * 100) = 96.0
    }
}
