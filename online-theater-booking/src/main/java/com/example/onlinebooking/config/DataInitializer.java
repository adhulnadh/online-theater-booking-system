package com.example.onlinebooking.config;

import com.example.onlinebooking.model.*;
import com.example.onlinebooking.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           MovieRepository movieRepository,
                           TheaterRepository theaterRepository,
                           ShowRepository showRepository,
                           SeatRepository seatRepository,
                           BookingRepository bookingRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.theaterRepository = theaterRepository;
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial seeding.");
            return;
        }

        log.info("Seeding initial database data...");

        // 1. Users
        User alice = userRepository.save(User.builder()
                .name("Alice")
                .email("alice@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .build());

        User bob = userRepository.save(User.builder()
                .name("Bob")
                .email("bob@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .build());

        User admin = userRepository.save(User.builder()
                .name("Admin")
                .email("admin@example.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build());

        // 2. Theaters
        Theater imax = theaterRepository.save(Theater.builder()
                .name("Grand IMAX Screen 1")
                .location("Downtown Mall, 4th Floor")
                .screenSizeInches(840.0)
                .screenAspectRatio("1.90:1")
                .build());

        Theater cineplex = theaterRepository.save(Theater.builder()
                .name("Cineplex Screen 2")
                .location("City Center, Main St")
                .screenSizeInches(600.0)
                .screenAspectRatio("2.39:1")
                .build());

        Theater starlight = theaterRepository.save(Theater.builder()
                .name("Starlight Laser Ultra")
                .location("Westside Plaza, Level 2")
                .screenSizeInches(920.0)
                .screenAspectRatio("2.39:1")
                .build());

        // 3. Movies
        Movie inception = movieRepository.save(Movie.builder()
                .title("Inception")
                .duration(148)
                .aspectRatio("2.39:1")
                .rating(8.8)
                .posterUrl("https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=800&q=80")
                .build());

        Movie interstellar = movieRepository.save(Movie.builder()
                .title("Interstellar")
                .duration(169)
                .aspectRatio("1.90:1")
                .rating(8.7)
                .posterUrl("https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80")
                .build());

        Movie oppenheimer = movieRepository.save(Movie.builder()
                .title("Oppenheimer")
                .duration(180)
                .aspectRatio("2.20:1")
                .rating(8.9)
                .posterUrl("https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=800&q=80")
                .build());

        // 4. Shows
        LocalDateTime now = LocalDateTime.now();
        Show show1 = showRepository.save(Show.builder()
                .movie(inception)
                .theater(starlight)
                .startTime(now.plusHours(2))
                .build());

        Show show2 = showRepository.save(Show.builder()
                .movie(inception)
                .theater(imax)
                .startTime(now.plusHours(4))
                .build());

        Show show3 = showRepository.save(Show.builder()
                .movie(inception)
                .theater(cineplex)
                .startTime(now.plusHours(6))
                .build());

        Show show4 = showRepository.save(Show.builder()
                .movie(interstellar)
                .theater(imax)
                .startTime(now.plusHours(3))
                .build());

        // 5. Seats for Show 1 (and Show 2)
        List<Seat> show1Seats = createSeatsForShow(show1);
        createSeatsForShow(show2);

        // 6. Create a sample confirmed booking for Alice on seat D8 (upcoming show)
        Booking aliceBooking = bookingRepository.save(Booking.builder()
                .user(alice)
                .show(show1)
                .bookingTime(now.minusHours(1))
                .status(BookingStatus.CONFIRMED)
                .build());

        Seat seatD8 = show1Seats.stream().filter(s -> s.getSeatNumber().equals("D8")).findFirst().orElse(null);
        if (seatD8 != null) {
            seatD8.setStatus(SeatStatus.BOOKED);
            seatD8.setUser(alice);
            seatD8.setBooking(aliceBooking);
            seatRepository.save(seatD8);
        }

        log.info("Database successfully seeded with users, movies, theaters, shows, seats, and sample bookings!");
    }

    private List<Seat> createSeatsForShow(Show show) {
        List<Seat> seats = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D"};

        for (String row : rows) {
            for (int num = 1; num <= 8; num++) {
                String seatNumber = row + num;
                SeatStatus status = SeatStatus.AVAILABLE;

                // Preset a few booked seats for realism
                if (seatNumber.equals("A4") || seatNumber.equals("B5")) {
                    status = SeatStatus.BOOKED;
                }

                seats.add(Seat.builder()
                        .show(show)
                        .seatNumber(seatNumber)
                        .status(status)
                        .build());
            }
        }
        return seatRepository.saveAll(seats);
    }
}
