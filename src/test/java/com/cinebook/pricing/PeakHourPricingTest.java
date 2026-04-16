package com.cinebook.pricing;

import com.cinebook.domain.Hall;
import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;
import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.domain.enums.SeatTier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PeakHourPricingTest {

    private final PeakHourPricing pricing = new PeakHourPricing(new StandardPricing());
    private final Seat regular = new Seat(2, 0, SeatTier.REGULAR, SeatStatus.AVAILABLE);

    private Showtime showtimeAt(int hour, int minute) {
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        return new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, hour, minute),
                new BigDecimal("80.00"), hall.generateSeatGrid());
    }

    @Test
    void peakHour_18_00_appliesSurcharge() {
        // 80 * 1.25 = 100
        assertEquals(0, new BigDecimal("100.00").compareTo(
                pricing.calculate(showtimeAt(18, 0), regular)));
    }

    @Test
    void peakHour_20_59_appliesSurcharge() {
        assertEquals(0, new BigDecimal("100.00").compareTo(
                pricing.calculate(showtimeAt(20, 59), regular)));
    }

    @Test
    void exactlyAtPeakEnd_21_00_noSurcharge() {
        assertEquals(0, new BigDecimal("80.00").compareTo(
                pricing.calculate(showtimeAt(21, 0), regular)));
    }

    @Test
    void beforePeak_17_59_noSurcharge() {
        assertEquals(0, new BigDecimal("80.00").compareTo(
                pricing.calculate(showtimeAt(17, 59), regular)));
    }

    @Test
    void offPeak_14_00_noSurcharge() {
        assertEquals(0, new BigDecimal("80.00").compareTo(
                pricing.calculate(showtimeAt(14, 0), regular)));
    }

    @Test
    void peakHour_vipSeat_stacksMultipliers() {
        Seat vip = new Seat(0, 0, SeatTier.VIP, SeatStatus.AVAILABLE);
        // 80 * 1.5 (VIP) * 1.25 (peak) = 150
        assertEquals(0, new BigDecimal("150.00").compareTo(
                pricing.calculate(showtimeAt(19, 0), vip)));
    }

    @Test
    void isPeakHour_static_check() {
        assertTrue(PeakHourPricing.isPeakHour(showtimeAt(18, 0)));
        assertTrue(PeakHourPricing.isPeakHour(showtimeAt(20, 30)));
        assertFalse(PeakHourPricing.isPeakHour(showtimeAt(17, 59)));
        assertFalse(PeakHourPricing.isPeakHour(showtimeAt(21, 0)));
    }
}
