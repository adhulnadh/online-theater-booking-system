package com.example.onlinebooking.controller;

import com.example.onlinebooking.dto.RecommendedTheaterDto;
import com.example.onlinebooking.model.Movie;
import com.example.onlinebooking.repository.MovieRepository;
import com.example.onlinebooking.service.TheaterRecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieRepository movieRepository;
    private final TheaterRecommendationService theaterRecommendationService;

    public MovieController(MovieRepository movieRepository,
                           TheaterRecommendationService theaterRecommendationService) {
        this.movieRepository = movieRepository;
        this.theaterRecommendationService = theaterRecommendationService;
    }

    @GetMapping
    public ResponseEntity<List<Movie>> getAllMovies(@RequestParam(value = "search", required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(movieRepository.findByTitleContainingIgnoreCase(search.trim()));
        }
        return ResponseEntity.ok(movieRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return movieRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{movieId}/recommended-theaters")
    public ResponseEntity<List<RecommendedTheaterDto>> getRecommendedTheaters(@PathVariable Long movieId) {
        return ResponseEntity.ok(theaterRecommendationService.getRecommendedTheaters(movieId));
    }
}
