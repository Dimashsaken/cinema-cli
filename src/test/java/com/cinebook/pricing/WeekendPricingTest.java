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

class WeekendPricingTest {

    private final WeekendPricing pricing = new WeekendPricing(new StandardPricing());
    private final Seat regular = new Seat(2, 0, SeatTier.REGULAR, SeatStatus.AVAILABLE);

    private Showtime showtimeOnDate(int year, int month, int day) {
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        return new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(year, month, day, 14, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid());
    }

    @Test
    void saturday_appliesSurcharge() {
        // 2026-04-18 is a Saturday
        // 80 * 1.20 = 96
        assertEquals(0, new BigDecimal("96.00").compareTo(
                pricing.calculate(showtimeOnDate(2026, 4, 18), regular)));
    }

    @Test
    void sunday_appliesSurcharge() {
        // 2026-04-19 is a Sunday
        assertEquals(0, new BigDecimal("96.00").compareTo(
                pricing.calculate(showtimeOnDate(2026, 4, 19), regular)));
    }

    @Test
    void monday_noSurcharge() {
        // 2026-04-20 is a Monday
        assertEquals(0, new BigDecimal("80.00").compareTo(
                pricing.calculate(showtimeOnDate(2026, 4, 20), regular)));
    }

    @Test
    void friday_noSurcharge() {
        // 2026-04-17 is a Friday
        assertEquals(0, new BigDecimal("80.00").compareTo(
                pricing.calculate(showtimeOnDate(2026, 4, 17), regular)));
    }

    @Test
    void weekend_vipSeat_stacksMultipliers() {
        Seat vip = new Seat(0, 0, SeatTier.VIP, SeatStatus.AVAILABLE);
        // 80 * 1.5 (VIP) * 1.20 (weekend) = 144
        assertEquals(0, new BigDecimal("144.00").compareTo(
                pricing.calculate(showtimeOnDate(2026, 4, 18), vip)));
    }

    @Test
    void isWeekend_static_check() {
        assertTrue(WeekendPricing.isWeekend(showtimeOnDate(2026, 4, 18)));  // Sat
        assertTrue(WeekendPricing.isWeekend(showtimeOnDate(2026, 4, 19)));  // Sun
        assertFalse(WeekendPricing.isWeekend(showtimeOnDate(2026, 4, 20))); // Mon
        assertFalse(WeekendPricing.isWeekend(showtimeOnDate(2026, 4, 17))); // Fri
    }
}
