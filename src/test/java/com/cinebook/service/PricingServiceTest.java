package com.cinebook.service;

import com.cinebook.domain.Hall;
import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;
import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.domain.enums.SeatTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    private PricingService service;

    @BeforeEach
    void setUp() {
        service = new PricingService();
    }

    private Showtime showtime(int month, int day, int hour, BigDecimal basePrice) {
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        return new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, month, day, hour, 0),
                basePrice, hall.generateSeatGrid());
    }

    private Seat regular() {
        return new Seat(2, 0, SeatTier.REGULAR, SeatStatus.AVAILABLE);
    }

    private Seat vip() {
        return new Seat(0, 0, SeatTier.VIP, SeatStatus.AVAILABLE);
    }

    // --- weekday off-peak, no discount ---

    @Test
    void weekday_offPeak_regular_basePrice() {
        // Monday 14:00, base 80
        Showtime st = showtime(4, 20, 14, new BigDecimal("80.00"));
        assertEquals(0, new BigDecimal("80.00").compareTo(
                service.calculateSingle(st, regular(), null)));
    }

    @Test
    void weekday_offPeak_vip_150Percent() {
        // Monday 14:00, base 80 -> 120
        Showtime st = showtime(4, 20, 14, new BigDecimal("80.00"));
        assertEquals(0, new BigDecimal("120.00").compareTo(
                service.calculateSingle(st, vip(), null)));
    }

    // --- weekday peak ---

    @Test
    void weekday_peak_regular() {
        // Monday 19:00, base 80, peak 1.25 -> 100
        Showtime st = showtime(4, 20, 19, new BigDecimal("80.00"));
        assertEquals(0, new BigDecimal("100.00").compareTo(
                service.calculateSingle(st, regular(), null)));
    }

    @Test
    void weekday_peak_vip() {
        // Monday 19:00, base 80, VIP 1.5 -> 120, peak 1.25 -> 150
        Showtime st = showtime(4, 20, 19, new BigDecimal("80.00"));
        assertEquals(0, new BigDecimal("150.00").compareTo(
                service.calculateSingle(st, vip(), null)));
    }

    // --- weekend off-peak ---

    @Test
    void weekend_offPeak_regular() {
        // Saturday 14:00, base 80, weekend 1.20 -> 96
        Showtime st = showtime(4, 18, 14, new BigDecimal("80.00"));
        assertEquals(0, new BigDecimal("96.00").compareTo(
                service.calculateSingle(st, regular(), null)));
    }

    // --- weekend peak (both surcharges stack) ---

    @Test
    void weekend_peak_regular() {
        // Saturday 19:00, base 80, peak 1.25 -> 100, weekend 1.20 -> 120
        Showtime st = showtime(4, 18, 19, new BigDecimal("80.00"));
        assertEquals(0, new BigDecimal("120.00").compareTo(
                service.calculateSingle(st, regular(), null)));
    }

    @Test
    void weekend_peak_vip() {
        // Saturday 19:00, base 80, VIP 1.5 -> 120, peak 1.25 -> 150, weekend 1.20 -> 180
        Showtime st = showtime(4, 18, 19, new BigDecimal("80.00"));
        assertEquals(0, new BigDecimal("180.00").compareTo(
                service.calculateSingle(st, vip(), null)));
    }

    // --- with discount codes ---

    @Test
    void weekday_offPeak_withStudent20() {
        // Monday 14:00, base 80, STUDENT20 -> 80 - 20% = 64
        Showtime st = showtime(4, 20, 14, new BigDecimal("80.00"));
        assertEquals(0, new BigDecimal("64.00").compareTo(
                service.calculateSingle(st, regular(), "STUDENT20")));
    }

    @Test
    void weekend_peak_vip_withStudent20() {
        // Saturday 19:00, base 80, VIP -> 120, peak -> 150, weekend -> 180
        // STUDENT20 -> 180 - 20% = 144
        Showtime st = showtime(4, 18, 19, new BigDecimal("80.00"));
        assertEquals(0, new BigDecimal("144.00").compareTo(
                service.calculateSingle(st, vip(), "STUDENT20")));
    }

    @Test
    void invalidDiscountCode_ignored() {
        // Invalid code should be ignored (not throw), price stays normal
        Showtime st = showtime(4, 20, 14, new BigDecimal("80.00"));
        assertEquals(0, new BigDecimal("80.00").compareTo(
                service.calculateSingle(st, regular(), "INVALID")));
    }

    @Test
    void nullDiscountCode_noDiscount() {
        Showtime st = showtime(4, 20, 14, new BigDecimal("80.00"));
        assertEquals(0, new BigDecimal("80.00").compareTo(
                service.calculateSingle(st, regular(), null)));
    }

    // --- calculateTotal ---

    @Test
    void calculateTotal_multipleSeats() {
        // Monday 14:00, base 80
        // 2 regular (80 each) + 1 VIP (120) = 280
        Showtime st = showtime(4, 20, 14, new BigDecimal("80.00"));
        BigDecimal total = service.calculateTotal(st,
                List.of(regular(), regular(), vip()), null);
        assertEquals(0, new BigDecimal("280.00").compareTo(total));
    }

    @Test
    void calculateTotal_emptySeats_zero() {
        Showtime st = showtime(4, 20, 14, new BigDecimal("80.00"));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                service.calculateTotal(st, List.of(), null)));
    }

    // --- peak boundary edge cases ---

    @Test
    void peakBoundary_17_59_noSurcharge() {
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        Showtime st = new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 17, 59),
                new BigDecimal("80.00"), hall.generateSeatGrid());
        assertEquals(0, new BigDecimal("80.00").compareTo(
                service.calculateSingle(st, regular(), null)));
    }

    @Test
    void peakBoundary_18_00_surcharge() {
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        Showtime st = new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 18, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid());
        assertEquals(0, new BigDecimal("100.00").compareTo(
                service.calculateSingle(st, regular(), null)));
    }

    @Test
    void peakBoundary_21_00_noSurcharge() {
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        Showtime st = new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 21, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid());
        assertEquals(0, new BigDecimal("80.00").compareTo(
                service.calculateSingle(st, regular(), null)));
    }
}
