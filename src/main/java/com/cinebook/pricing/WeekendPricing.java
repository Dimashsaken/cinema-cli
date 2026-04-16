package com.cinebook.pricing;

import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/**
 * Weekend pricing: applies a 1.20x surcharge for Saturday and Sunday showtimes.
 *
 * <p>Delegates to an inner strategy for the base calculation,
 * then applies the weekend multiplier on top.
 */
public class WeekendPricing implements PricingStrategy {

    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.20");

    private final PricingStrategy base;

    public WeekendPricing(PricingStrategy base) {
        this.base = base;
    }

    @Override
    public BigDecimal calculate(Showtime showtime, Seat seat) {
        BigDecimal price = base.calculate(showtime, seat);
        DayOfWeek day = showtime.getStartsAt().getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return price.multiply(WEEKEND_MULTIPLIER);
        }
        return price;
    }

    /** Check if a given showtime falls on a weekend. */
    public static boolean isWeekend(Showtime showtime) {
        DayOfWeek day = showtime.getStartsAt().getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
