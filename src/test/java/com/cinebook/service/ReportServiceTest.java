package com.cinebook.service;

import com.cinebook.domain.Booking;
import com.cinebook.domain.Hall;
import com.cinebook.domain.Showtime;
import com.cinebook.domain.enums.BookingStatus;
import com.cinebook.infra.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {

    private ReportService reportService;
    private InMemoryShowtimeRepository showtimeRepo;
    private InMemoryBookingRepository bookingRepo;

    @BeforeEach
    void setUp() {
        showtimeRepo = new InMemoryShowtimeRepository();
        bookingRepo = new InMemoryBookingRepository();
        reportService = new ReportService(showtimeRepo, bookingRepo);
    }

    @Test
    void occupancyReport_noBookings() {
        Hall hall = new Hall("H1", 2, 3, Set.of());
        Showtime st = new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 14, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid());
        showtimeRepo.save(st);

        String report = reportService.occupancyReport("ST001");
        assertTrue(report.contains("Total: 6"));
        assertTrue(report.contains("Available: 6"));
        assertTrue(report.contains("Occupancy: 0.0%"));
    }

    @Test
    void occupancyReport_withBookedSeats() {
        Hall hall = new Hall("H1", 2, 5, Set.of());
        Showtime st = new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 14, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid());
        // Book 3 seats
        st.findSeat("A1").get().hold("U1", Instant.now().plusSeconds(300));
        st.findSeat("A1").get().book("U1");
        st.findSeat("A2").get().hold("U1", Instant.now().plusSeconds(300));
        st.findSeat("A2").get().book("U1");
        st.findSeat("B1").get().hold("U1", Instant.now().plusSeconds(300));
        st.findSeat("B1").get().book("U1");
        showtimeRepo.save(st);

        String report = reportService.occupancyReport("ST001");
        assertTrue(report.contains("Booked: 3"));
        assertTrue(report.contains("Occupancy: 30.0%"));
    }

    @Test
    void occupancyReport_notFound() {
        assertEquals("Showtime not found.", reportService.occupancyReport("NOPE"));
    }

    @Test
    void revenueReport_empty() {
        String report = reportService.revenueReport();
        assertTrue(report.contains("Confirmed: 0"));
        assertTrue(report.contains("$0"));
    }

    @Test
    void revenueReport_withBookings() {
        bookingRepo.save(new Booking("BK-001", "U001", "ST001",
                List.of("A1"), new BigDecimal("80.00"),
                BookingStatus.CONFIRMED, Instant.now(), 1));
        bookingRepo.save(new Booking("BK-002", "U001", "ST001",
                List.of("A2"), new BigDecimal("120.00"),
                BookingStatus.CONFIRMED, Instant.now(), 1));
        bookingRepo.save(new Booking("BK-003", "U001", "ST001",
                List.of("A3"), new BigDecimal("100.00"),
                BookingStatus.REFUNDED, Instant.now(), 1));

        String report = reportService.revenueReport();
        assertTrue(report.contains("Confirmed: 2"));
        assertTrue(report.contains("Refunded: 1"));
        assertTrue(report.contains("$200"));
    }

    @Test
    void exportBookingsCsv_format() {
        bookingRepo.save(new Booking("BK-001", "U001", "ST001",
                List.of("A1", "A2"), new BigDecimal("160.00"),
                BookingStatus.CONFIRMED, Instant.parse("2026-04-20T12:00:00Z"), 2));

        String csv = reportService.exportBookingsCsv();
        assertTrue(csv.contains("bookingId,userId,showtimeId"));
        assertTrue(csv.contains("BK-001"));
        assertTrue(csv.contains("\"A1;A2\""));
        assertTrue(csv.contains("CONFIRMED"));
    }
}
