package com.cinebook.service;

import com.cinebook.domain.*;
import com.cinebook.domain.enums.BookingStatus;
import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.exception.HoldExpiredException;
import com.cinebook.exception.InvalidInputException;
import com.cinebook.exception.SeatUnavailableException;
import com.cinebook.infra.*;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BookingServiceTest {

    private BookingService bookingService;
    private ShowtimeRepository showtimeRepository;
    private BookingRepository bookingRepository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        JsonFileAdapter jsonAdapter = new JsonFileAdapter();
        showtimeRepository = new JsonShowtimeRepository(
                tempDir.resolve("showtimes.json"), jsonAdapter);
        bookingRepository = new CsvBookingRepository(
                tempDir.resolve("bookings.csv"), new CsvAppendAdapter());
        TransactionLog wal = new TransactionLog(tempDir.resolve("wal.log"));
        PricingService pricingService = new PricingService();

        bookingService = new BookingService(
                showtimeRepository, bookingRepository, pricingService, wal);

        // Seed a showtime: Monday 14:00 (off-peak weekday), base 80, 3 rows x 4 cols
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        Showtime st = new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 14, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid());
        showtimeRepository.save(st);
    }

    // --- holdSeats ---

    @Test
    void holdSeats_success() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("C1", "C2"));

        assertNotNull(token.getTokenId());
        assertEquals("ST001", token.getShowtimeId());
        assertEquals("U001", token.getUserId());
        assertEquals(List.of("C1", "C2"), token.getSeats());
        assertTrue(token.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void holdSeats_seatsBecomeHeld() throws Exception {
        bookingService.holdSeats("ST001", "U001", List.of("C1"));

        Showtime st = showtimeRepository.findById("ST001").get();
        Seat seat = st.findSeat("C1").get();
        assertEquals(SeatStatus.HELD, seat.getStatus());
        assertEquals("U001", seat.getHeldBy());
    }

    @Test
    void holdSeats_emptyList_throws() {
        assertThrows(InvalidInputException.class,
                () -> bookingService.holdSeats("ST001", "U001", List.of()));
    }

    @Test
    void holdSeats_nullList_throws() {
        assertThrows(InvalidInputException.class,
                () -> bookingService.holdSeats("ST001", "U001", null));
    }

    @Test
    void holdSeats_invalidSeatCode_throws() {
        assertThrows(InvalidInputException.class,
                () -> bookingService.holdSeats("ST001", "U001", List.of("Z99")));
    }

    @Test
    void holdSeats_invalidShowtime_throws() {
        assertThrows(InvalidInputException.class,
                () -> bookingService.holdSeats("NOPE", "U001", List.of("A1")));
    }

    @Test
    void holdSeats_alreadyHeld_throws() throws Exception {
        bookingService.holdSeats("ST001", "U001", List.of("C1"));

        assertThrows(SeatUnavailableException.class,
                () -> bookingService.holdSeats("ST001", "U002", List.of("C1")));
    }

    @Test
    void holdSeats_alreadyBooked_throws() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("C1"));
        bookingService.confirmBooking(token, null);

        assertThrows(SeatUnavailableException.class,
                () -> bookingService.holdSeats("ST001", "U002", List.of("C1")));
    }

    @Test
    void holdSeats_multipleSeats_oneUnavailable_noneHeld() throws Exception {
        bookingService.holdSeats("ST001", "U001", List.of("C1"));

        // Try to hold C1 (held) + C2 (available) — should fail atomically
        assertThrows(SeatUnavailableException.class,
                () -> bookingService.holdSeats("ST001", "U002", List.of("C1", "C2")));

        // C2 should still be available (not partially held)
        Showtime st = showtimeRepository.findById("ST001").get();
        assertEquals(SeatStatus.AVAILABLE, st.findSeat("C2").get().getStatus());
    }

    // --- confirmBooking ---

    @Test
    void confirmBooking_success() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("C1", "C2"));

        Booking booking = bookingService.confirmBooking(token, null);

        assertNotNull(booking.getBookingId());
        assertTrue(booking.getBookingId().startsWith("BK-"));
        assertEquals("U001", booking.getUserId());
        assertEquals("ST001", booking.getShowtimeId());
        assertEquals(List.of("C1", "C2"), booking.getSeats());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        // 2 regular seats at 80 each = 160
        assertEquals(0, new BigDecimal("160.00").compareTo(booking.getTotalPrice()));
    }

    @Test
    void confirmBooking_seatsBecomeBOOKED() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("C1"));
        bookingService.confirmBooking(token, null);

        Showtime st = showtimeRepository.findById("ST001").get();
        assertEquals(SeatStatus.BOOKED, st.findSeat("C1").get().getStatus());
    }

    @Test
    void confirmBooking_vipPricing() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("A1"));
        Booking booking = bookingService.confirmBooking(token, null);

        // VIP seat: 80 * 1.5 = 120
        assertEquals(0, new BigDecimal("120.00").compareTo(booking.getTotalPrice()));
    }

    @Test
    void confirmBooking_withDiscountCode() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("C1"));
        Booking booking = bookingService.confirmBooking(token, "STUDENT20");

        // Regular 80, minus 20% = 64
        assertEquals(0, new BigDecimal("64.00").compareTo(booking.getTotalPrice()));
    }

    @Test
    void confirmBooking_persistedToRepository() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("C1"));
        Booking booking = bookingService.confirmBooking(token, null);

        Optional<Booking> found = bookingRepository.findById(booking.getBookingId());
        assertTrue(found.isPresent());
        assertEquals(BookingStatus.CONFIRMED, found.get().getStatus());
    }

    @Test
    void confirmBooking_expiredHold_throws() throws Exception {
        // Create a hold token that's already expired
        HoldToken expired = new HoldToken("tk1", "ST001", "U001",
                List.of("C1"), Instant.now().minus(1, ChronoUnit.MINUTES));

        assertThrows(HoldExpiredException.class,
                () -> bookingService.confirmBooking(expired, null));
    }

    // --- cancelBooking ---

    @Test
    void cancelBooking_success() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("C1"));
        Booking booking = bookingService.confirmBooking(token, null);

        Booking cancelled = bookingService.cancelBooking(booking.getBookingId(), "U001");

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void cancelBooking_releasesSeats() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("C1"));
        Booking booking = bookingService.confirmBooking(token, null);
        bookingService.cancelBooking(booking.getBookingId(), "U001");

        Showtime st = showtimeRepository.findById("ST001").get();
        assertEquals(SeatStatus.AVAILABLE, st.findSeat("C1").get().getStatus());
    }

    @Test
    void cancelBooking_wrongUser_throws() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("C1"));
        Booking booking = bookingService.confirmBooking(token, null);

        assertThrows(InvalidInputException.class,
                () -> bookingService.cancelBooking(booking.getBookingId(), "U002"));
    }

    @Test
    void cancelBooking_notFound_throws() {
        assertThrows(InvalidInputException.class,
                () -> bookingService.cancelBooking("NOPE", "U001"));
    }

    // --- refundBooking ---

    @Test
    void refundBooking_success() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("C1"));
        Booking booking = bookingService.confirmBooking(token, null);

        Booking refunded = bookingService.refundBooking(booking.getBookingId(), "U001");

        assertEquals(BookingStatus.REFUNDED, refunded.getStatus());
    }

    @Test
    void refundBooking_releasesSeats() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("C1"));
        Booking booking = bookingService.confirmBooking(token, null);
        bookingService.refundBooking(booking.getBookingId(), "U001");

        Showtime st = showtimeRepository.findById("ST001").get();
        assertEquals(SeatStatus.AVAILABLE, st.findSeat("C1").get().getStatus());
    }

    @Test
    void refundBooking_wrongUser_throws() throws Exception {
        HoldToken token = bookingService.holdSeats("ST001", "U001", List.of("C1"));
        Booking booking = bookingService.confirmBooking(token, null);

        assertThrows(InvalidInputException.class,
                () -> bookingService.refundBooking(booking.getBookingId(), "U002"));
    }

    // --- getUserBookings ---

    @Test
    void getUserBookings_returnsUserBookings() throws Exception {
        HoldToken t1 = bookingService.holdSeats("ST001", "U001", List.of("C1"));
        bookingService.confirmBooking(t1, null);

        HoldToken t2 = bookingService.holdSeats("ST001", "U001", List.of("C2"));
        bookingService.confirmBooking(t2, null);

        List<Booking> bookings = bookingService.getUserBookings("U001");
        assertEquals(2, bookings.size());
    }

    @Test
    void getUserBookings_empty() {
        assertTrue(bookingService.getUserBookings("U999").isEmpty());
    }

    // --- full lifecycle ---

    @Test
    void fullLifecycle_hold_confirm_refund_rebook() throws Exception {
        // Hold and confirm C1
        HoldToken t1 = bookingService.holdSeats("ST001", "U001", List.of("C1"));
        Booking b1 = bookingService.confirmBooking(t1, null);
        assertEquals(BookingStatus.CONFIRMED, b1.getStatus());

        // Refund — seat becomes available
        bookingService.refundBooking(b1.getBookingId(), "U001");
        Showtime st = showtimeRepository.findById("ST001").get();
        assertEquals(SeatStatus.AVAILABLE, st.findSeat("C1").get().getStatus());

        // Another user can now book C1
        HoldToken t2 = bookingService.holdSeats("ST001", "U002", List.of("C1"));
        Booking b2 = bookingService.confirmBooking(t2, null);
        assertEquals(BookingStatus.CONFIRMED, b2.getStatus());
        assertEquals("U002", b2.getUserId());
    }
}
