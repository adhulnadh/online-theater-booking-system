package com.example.onlinebooking;

import com.example.onlinebooking.dto.SeatReleaseMessage;
import com.example.onlinebooking.model.*;
import com.example.onlinebooking.repository.BookingRepository;
import com.example.onlinebooking.repository.SeatRepository;
import com.example.onlinebooking.service.BookingService;
import com.example.onlinebooking.websocket.SeatWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatRepository seatRepository;

    private List<SeatReleaseMessage> broadcastMessages;
    private SeatWebSocketHandler seatWebSocketHandler;
    private BookingService bookingService;

    private User alice;
    private User bob;
    private Show upcomingShow;
    private Show pastShow;
    private Seat bookedSeat1;
    private Seat bookedSeat2;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        broadcastMessages = new ArrayList<>();
        seatWebSocketHandler = new SeatWebSocketHandler(null) {
            @Override
            public void broadcastSeatReleased(Long showId, Long seatId, String seatNumber) {
                broadcastMessages.add(SeatReleaseMessage.of(showId, seatId, seatNumber));
            }
        };

        bookingService = new BookingService(bookingRepository, seatRepository, seatWebSocketHandler);

        alice = User.builder().userId(1L).name("Alice").email("alice@example.com").role(Role.USER).build();
        bob = User.builder().userId(2L).name("Bob").email("bob@example.com").role(Role.USER).build();

        upcomingShow = Show.builder().showId(10L).startTime(LocalDateTime.now().plusHours(3)).build();
        pastShow = Show.builder().showId(20L).startTime(LocalDateTime.now().minusHours(2)).build();

        testBooking = Booking.builder()
                .bookingId(100L)
                .user(alice)
                .show(upcomingShow)
                .bookingTime(LocalDateTime.now().minusMinutes(30))
                .status(BookingStatus.CONFIRMED)
                .build();

        bookedSeat1 = Seat.builder()
                .seatId(501L)
                .seatNumber("C1")
                .show(upcomingShow)
                .user(alice)
                .status(SeatStatus.BOOKED)
                .booking(testBooking)
                .build();

        bookedSeat2 = Seat.builder()
                .seatId(502L)
                .seatNumber("C2")
                .show(upcomingShow)
                .user(alice)
                .status(SeatStatus.BOOKED)
                .booking(testBooking)
                .build();

        testBooking.setSeats(new ArrayList<>(List.of(bookedSeat1, bookedSeat2)));
    }

    @Test
    @DisplayName("Requirement 1, 2, 3 & 4: Successful cancellation updates booking to CANCELLED, releases seats to AVAILABLE, and broadcasts WebSocket release")
    void testCancelBooking_Success() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancelBooking(100L, alice);

        // Verify Booking status updated to CANCELLED
        assertNotNull(result);
        assertEquals(BookingStatus.CANCELLED, result.getStatus());

        // Verify Seats reset to AVAILABLE and user cleared
        assertEquals(SeatStatus.AVAILABLE, bookedSeat1.getStatus());
        assertNull(bookedSeat1.getUser());
        assertNull(bookedSeat1.getLockedUntil());
        assertNull(bookedSeat1.getBooking());

        assertEquals(SeatStatus.AVAILABLE, bookedSeat2.getStatus());
        assertNull(bookedSeat2.getUser());
        assertNull(bookedSeat2.getLockedUntil());
        assertNull(bookedSeat2.getBooking());

        verify(seatRepository, times(2)).save(any(Seat.class));
        verify(bookingRepository, times(1)).save(testBooking);

        // Verify WebSocket release broadcast sent for both seats to /topic/show/10
        assertEquals(2, broadcastMessages.size());
        assertEquals(10L, broadcastMessages.get(0).getShowId());
        assertEquals(501L, broadcastMessages.get(0).getSeatId());
        assertEquals("C1", broadcastMessages.get(0).getSeatNumber());
        assertEquals("AVAILABLE", broadcastMessages.get(0).getStatus());

        assertEquals(10L, broadcastMessages.get(1).getShowId());
        assertEquals(502L, broadcastMessages.get(1).getSeatId());
        assertEquals("C2", broadcastMessages.get(1).getSeatNumber());
        assertEquals("AVAILABLE", broadcastMessages.get(1).getStatus());
    }

    @Test
    @DisplayName("Requirement 2: Unauthorized user cannot cancel another user's booking")
    void testCancelBooking_WrongUser_ThrowsSecurityException() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));

        SecurityException ex = assertThrows(
                SecurityException.class,
                () -> bookingService.cancelBooking(100L, bob)
        );

        assertTrue(ex.getMessage().contains("not authorized to cancel this booking"));
        verify(bookingRepository, never()).save(any());
        verify(seatRepository, never()).save(any());
        assertTrue(broadcastMessages.isEmpty());
    }

    @Test
    @DisplayName("Requirement 2: Cannot cancel booking for a past showtime")
    void testCancelBooking_PastShow_ThrowsIllegalStateException() {
        testBooking.setShow(pastShow);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.cancelBooking(100L, alice)
        );

        assertTrue(ex.getMessage().contains("showtime has already passed"));
        verify(bookingRepository, never()).save(any());
        verify(seatRepository, never()).save(any());
        assertTrue(broadcastMessages.isEmpty());
    }

    @Test
    @DisplayName("Requirement 3: Cannot cancel already cancelled booking")
    void testCancelBooking_AlreadyCancelled_ThrowsIllegalStateException() {
        testBooking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.cancelBooking(100L, alice)
        );

        assertTrue(ex.getMessage().contains("already been cancelled"));
        verify(bookingRepository, never()).save(any());
        verify(seatRepository, never()).save(any());
    }

    @Test
    @DisplayName("Requirement 1: Non-existent booking throws IllegalArgumentException")
    void testCancelBooking_NotFound_ThrowsIllegalArgumentException() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(999L, alice)
        );

        assertTrue(ex.getMessage().contains("Booking not found with id: 999"));
    }
}
