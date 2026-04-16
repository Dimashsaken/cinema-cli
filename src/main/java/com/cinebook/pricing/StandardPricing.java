package com.cinebook.pricing;

import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;
import com.cinebook.domain.enums.SeatTier;

import java.math.BigDecimal;

/**
 * Default pricing: base price for regular seats, 1.5x multiplier for VIP seats.
 */
public class StandardPricing implements PricingStrategy {

    private static final BigDecimal VIP_MULTIPLIER = new BigDecimal("1.50");

    @Override
    public BigDecimal calculate(Showtime showtime, Seat seat) {
        BigDecimal base = showtime.getBasePrice();
        if (seat.getTier() == SeatTier.VIP) {
            return base.multiply(VIP_MULTIPLIER);
        }
        return base;
    }
}
