package com.cinebook.pricing;

import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;
import java.math.BigDecimal;

/** Strategy interface for computing ticket prices. Implementations can be swapped at runtime. */
public interface PricingStrategy {

    /** Calculate the price for a single seat in the given showtime. */
    BigDecimal calculate(Showtime showtime, Seat seat);
}
