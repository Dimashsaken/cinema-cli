package com.cinebook.service;

import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;
import com.cinebook.pricing.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Selects and composes pricing strategies based on showtime attributes.
 *
 * <p>Strategy composition order: Standard -> PeakHour (if applicable)
 * -> Weekend (if applicable) -> DiscountCode (if provided).
 */
public class PricingService {

    /**
     * Calculate the total price for a list of seats in a showtime.
     *
     * @param showtime     the target showtime
     * @param seats        the seats being booked
     * @param discountCode optional discount code, may be null
     * @return the total price for all seats
     */
    public BigDecimal calculateTotal(Showtime showtime, List<Seat> seats, String discountCode) {
        PricingStrategy strategy = buildStrategy(showtime, discountCode);
        BigDecimal total = BigDecimal.ZERO;
        for (Seat seat : seats) {
            total = total.add(strategy.calculate(showtime, seat));
        }
        return total;
    }

    /**
     * Calculate the price for a single seat.
     */
    public BigDecimal calculateSingle(Showtime showtime, Seat seat, String discountCode) {
        return buildStrategy(showtime, discountCode).calculate(showtime, seat);
    }

    /**
     * Build a composed strategy chain for the given showtime and discount code.
     */
    PricingStrategy buildStrategy(Showtime showtime, String discountCode) {
        PricingStrategy strategy = new StandardPricing();

        if (PeakHourPricing.isPeakHour(showtime)) {
            strategy = new PeakHourPricing(strategy);
        }

        if (WeekendPricing.isWeekend(showtime)) {
            strategy = new WeekendPricing(strategy);
        }

        if (discountCode != null && DiscountCodePricing.isValidCode(discountCode)) {
            strategy = new DiscountCodePricing(strategy, discountCode);
        }

        return strategy;
    }
}
