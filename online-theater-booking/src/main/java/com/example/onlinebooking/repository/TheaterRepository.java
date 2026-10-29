package com.example.onlinebooking.repository;

import com.example.onlinebooking.model.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, Long> {

    List<Theater> findByLocationIgnoreCase(String location);

    List<Theater> findByNameContainingIgnoreCase(String name);
}
