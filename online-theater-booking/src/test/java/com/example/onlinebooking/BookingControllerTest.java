package com.example.onlinebooking;

import com.example.onlinebooking.controller.BookingController;
import com.example.onlinebooking.dto.BookingDto;
import com.example.onlinebooking.model.*;
import com.example.onlinebooking.repository.BookingRepository;
import com.example.onlinebooking.repository.SeatRepository;
import com.example.onlinebooking.repository.UserRepository;
import com.example.onlinebooking.service.BookingService;
import com.example.onlinebooking.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private UserRepository userRepository;

    private JwtService jwtService;
    private BookingService bookingService;
    private BookingController bookingController;

    private User alice;
    private User bob;
    private String aliceToken;
    private String bobToken;
    private Show upcomingShow;
    private Booking booking;
    private Seat seat;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "superSecretKeyWithSufficientEntropyAndLengthForTesting1234567890!",
                3600000L,
                new ObjectMapper()
        );

        bookingService = new BookingService(bookingRepository, seatRepository, null);
        bookingController = new BookingController(bookingService, seatRepository, userRepository, jwtService);

        alice = User.builder().userId(1L).name("Alice").email("alice@example.com").role(Role.USER).build();
        bob = User.builder().userId(2L).name("Bob").email("bob@example.com").role(Role.USER).build();

        aliceToken = "Bearer " + jwtService.generateToken(alice);
        bobToken = "Bearer " + jwtService.generateToken(bob);

        Movie movie = Movie.builder().movieId(1L).title("Inception").posterUrl("poster.jpg").build();
        Theater theater = Theater.builder().theaterId(1L).name("IMAX 1").location("Downtown").build();
        upcomingShow = Show.builder().showId(10L).movie(movie).theater(theater).startTime(LocalDateTime.now().plusHours(2)).build();

        booking = Booking.builder()
                .bookingId(100L)
                .user(alice)
                .show(upcomingShow)
                .bookingTime(LocalDateTime.now().minusHours(1))
                .status(BookingStatus.CONFIRMED)
                .build();

        seat = Seat.builder()
                .seatId(501L)
                .seatNumber("A1")
                .show(upcomingShow)
                .user(alice)
                .status(SeatStatus.BOOKED)
                .booking(booking)
                .build();

        booking.setSeats(new ArrayList<>(List.of(seat)));
    }

    @Test
    @DisplayName("Requirement 1: DELETE /api/bookings/{bookingId}/cancel with valid JWT cancels booking and returns 200 OK")
    void testCancelBooking_ValidJwt_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = bookingController.cancelBooking(100L, null, aliceToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Booking cancelled successfully", body.get("message"));
        assertEquals("CANCELLED", body.get("status"));
        assertEquals(100L, body.get("bookingId"));
    }

    @Test
    @DisplayName("Requirement 2: DELETE /api/bookings/{bookingId}/cancel without authentication returns 401 Unauthorized")
    void testCancelBooking_Unauthenticated_ReturnsUnauthorized() {
        ResponseEntity<?> response = bookingController.cancelBooking(100L, null, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Requirement 2: DELETE /api/bookings/{bookingId}/cancel by another user returns 403 Forbidden")
    void testCancelBooking_UnauthorizedUser_ReturnsForbidden() {
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(bob));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = bookingController.cancelBooking(100L, null, bobToken);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.get("message").toString().contains("not authorized"));
    }

    @Test
    @DisplayName("Requirement 2: DELETE /api/bookings/{bookingId}/cancel for past showtime returns 400 Bad Request")
    void testCancelBooking_PastShowtime_ReturnsBadRequest() {
        booking.setShow(Show.builder().showId(10L).startTime(LocalDateTime.now().minusHours(1)).build());

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        ResponseEntity<?> response = bookingController.cancelBooking(100L, null, aliceToken);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue(body.get("message").toString().contains("showtime has already passed"));
    }

    @Test
    @DisplayName("GET /api/bookings/my-bookings with JWT returns user's booking list")
    void testGetMyBookings_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(bookingRepository.findByUser_UserIdOrderByBookingTimeDesc(1L)).thenReturn(List.of(booking));

        ResponseEntity<List<BookingDto>> response = bookingController.getMyBookings(null, aliceToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Inception", response.getBody().get(0).getMovieTitle());
        assertEquals(100L, response.getBody().get(0).getBookingId());
        assertTrue(response.getBody().get(0).isCanCancel());
    }
}
