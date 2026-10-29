package com.example.onlinebooking.repository;

import com.example.onlinebooking.model.Show;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    @EntityGraph(attributePaths = {"movie", "theater"})
    List<Show> findAll();

    @EntityGraph(attributePaths = {"movie", "theater"})
    Optional<Show> findById(Long id);

    @EntityGraph(attributePaths = {"movie", "theater"})
    List<Show> findByMovie_MovieId(Long movieId);

    @EntityGraph(attributePaths = {"movie", "theater"})
    List<Show> findByTheater_TheaterId(Long theaterId);

    @EntityGraph(attributePaths = {"movie", "theater"})
    List<Show> findByMovie_MovieIdAndStartTimeAfter(Long movieId, LocalDateTime after);

    List<Show> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
