package com.example.onlinebooking.service;

import com.example.onlinebooking.dto.RecommendedTheaterDto;
import com.example.onlinebooking.model.Movie;
import com.example.onlinebooking.model.Show;
import com.example.onlinebooking.model.Theater;
import com.example.onlinebooking.repository.MovieRepository;
import com.example.onlinebooking.repository.ShowRepository;
import com.example.onlinebooking.repository.TheaterRepository;
import com.example.onlinebooking.util.TheaterScoreCalculator;
import com.example.onlinebooking.util.TheaterScoreCalculator.ScoreBreakdown;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TheaterRecommendationService {

    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final ShowRepository showRepository;

    public TheaterRecommendationService(MovieRepository movieRepository,
                                       TheaterRepository theaterRepository,
                                       ShowRepository showRepository) {
        this.movieRepository = movieRepository;
        this.theaterRepository = theaterRepository;
        this.showRepository = showRepository;
    }

    /**
     * Recommends theaters for a given movie sorted by recommendation score descending.
     * Evaluates screen size (larger screens score higher) and aspect ratio match
     * (closer match to the movie's aspect ratio scores higher).
     */
    public List<RecommendedTheaterDto> getRecommendedTheaters(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found with id: " + movieId));

        List<Theater> theaters = theaterRepository.findAll();

        // Get active shows for this movie by theater
        List<Show> movieShows = showRepository.findByMovie_MovieId(movieId);
        Map<Long, Long> showsCountByTheater = movieShows.stream()
                .filter(s -> s.getTheater() != null)
                .collect(Collectors.groupingBy(s -> s.getTheater().getTheaterId(), Collectors.counting()));

        return theaters.stream()
                .map(theater -> {
                    ScoreBreakdown score = TheaterScoreCalculator.calculateScore(
                            theater.getScreenSizeInches(),
                            movie.getAspectRatio(),
                            theater.getScreenAspectRatio()
                    );
                    int activeShowsCount = showsCountByTheater.getOrDefault(theater.getTheaterId(), 0L).intValue();
                    return RecommendedTheaterDto.from(theater, score, activeShowsCount);
                })
                .sorted(Comparator
                        .comparing(RecommendedTheaterDto::getScore, Comparator.reverseOrder())
                        .thenComparing(RecommendedTheaterDto::getScreenSizeInches, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .collect(Collectors.toList());
    }
}
