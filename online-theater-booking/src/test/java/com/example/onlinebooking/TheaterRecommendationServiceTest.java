package com.example.onlinebooking;

import com.example.onlinebooking.dto.RecommendedTheaterDto;
import com.example.onlinebooking.model.Movie;
import com.example.onlinebooking.model.Show;
import com.example.onlinebooking.model.Theater;
import com.example.onlinebooking.repository.MovieRepository;
import com.example.onlinebooking.repository.ShowRepository;
import com.example.onlinebooking.repository.TheaterRepository;
import com.example.onlinebooking.service.TheaterRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TheaterRecommendationServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private TheaterRepository theaterRepository;

    @Mock
    private ShowRepository showRepository;

    private TheaterRecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new TheaterRecommendationService(
                movieRepository,
                theaterRepository,
                showRepository
        );
    }

    @Test
    @DisplayName("Should return sorted theaters by score descending for a movie")
    void testGetRecommendedTheatersSorted() {
        Movie movie = Movie.builder()
                .movieId(1L)
                .title("Inception")
                .aspectRatio("2.39:1")
                .duration(148)
                .build();

        Theater t1 = Theater.builder()
                .theaterId(1L)
                .name("Grand IMAX Screen 1")
                .location("Downtown Mall")
                .screenSizeInches(840.0)
                .screenAspectRatio("1.90:1")
                .build();

        Theater t2 = Theater.builder()
                .theaterId(2L)
                .name("Cineplex Screen 2")
                .location("City Center")
                .screenSizeInches(600.0)
                .screenAspectRatio("2.39:1")
                .build();

        Theater t3 = Theater.builder()
                .theaterId(3L)
                .name("Starlight Laser Ultra")
                .location("Westside Plaza")
                .screenSizeInches(920.0)
                .screenAspectRatio("2.39:1")
                .build();

        Theater t4 = Theater.builder()
                .theaterId(4L)
                .name("Small Screen 4")
                .location("Suburbs")
                .screenSizeInches(400.0)
                .screenAspectRatio("1.43:1")
                .build();

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(theaterRepository.findAll()).thenReturn(Arrays.asList(t1, t2, t3, t4));

        Show show = Show.builder()
                .showId(101L)
                .movie(movie)
                .theater(t3)
                .startTime(LocalDateTime.now().plusHours(2))
                .build();
        when(showRepository.findByMovie_MovieId(1L)).thenReturn(Collections.singletonList(show));

        List<RecommendedTheaterDto> recommendations = recommendationService.getRecommendedTheaters(1L);

        assertNotNull(recommendations);
        assertEquals(4, recommendations.size());

        // Verify sorted descending by score
        for (int i = 0; i < recommendations.size() - 1; i++) {
            assertTrue(
                    recommendations.get(i).getScore() >= recommendations.get(i + 1).getScore(),
                    "Theaters must be sorted in descending order of recommendation score"
            );
        }

        // #1 should be Starlight Laser Ultra (920" screen with 2.39:1 exact aspect ratio match)
        RecommendedTheaterDto topTheater = recommendations.get(0);
        assertEquals(3L, topTheater.getTheaterId());
        assertEquals("Starlight Laser Ultra", topTheater.getName());
        assertEquals(96.0, topTheater.getScore(), 0.1);
        assertTrue(topTheater.isHasActiveShows());
        assertEquals(1, topTheater.getActiveShowsCount());
        assertEquals("Perfect Match", topTheater.getMatchQuality());

        // Bottom theater should be Small Screen 4 (400", 1.43:1 mismatch)
        RecommendedTheaterDto bottomTheater = recommendations.get(3);
        assertEquals(4L, bottomTheater.getTheaterId());
        assertEquals("Small Screen 4", bottomTheater.getName());
        assertFalse(bottomTheater.isHasActiveShows());
    }

    @Test
    @DisplayName("Throws exception when movie id does not exist")
    void testMovieNotFoundThrowsException() {
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> recommendationService.getRecommendedTheaters(999L)
        );

        assertTrue(ex.getMessage().contains("Movie not found with id: 999"));
    }
}
