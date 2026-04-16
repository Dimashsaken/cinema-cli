package com.cinebook.pricing;

import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Peak-hour pricing: applies a 1.25x surcharge for showtimes starting
 * between 18:00 and 21:00 (inclusive of 18:00, exclusive of 21:00).
 *
 * <p>Delegates to an inner strategy for the base calculation,
 * then applies the peak multiplier on top.
 */
public class PeakHourPricing implements PricingStrategy {

    private static final LocalTime PEAK_START = LocalTime.of(18, 0);
    private static final LocalTime PEAK_END = LocalTime.of(21, 0);
    private static final BigDecimal PEAK_MULTIPLIER = new BigDecimal("1.25");

    private final PricingStrategy base;

    public PeakHourPricing(PricingStrategy base) {
        this.base = base;
    }

    @Override
    public BigDecimal calculate(Showtime showtime, Seat seat) {
        BigDecimal price = base.calculate(showtime, seat);
        LocalTime time = showtime.getStartsAt().toLocalTime();
        if (!time.isBefore(PEAK_START) && time.isBefore(PEAK_END)) {
            return price.multiply(PEAK_MULTIPLIER);
        }
        return price;
    }

    /** Check if a given showtime falls within peak hours. */
    public static boolean isPeakHour(Showtime showtime) {
        LocalTime time = showtime.getStartsAt().toLocalTime();
        return !time.isBefore(PEAK_START) && time.isBefore(PEAK_END);
    }
}
