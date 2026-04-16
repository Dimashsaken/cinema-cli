package com.cinebook.pricing;

import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Discount code pricing: applies a percentage discount from a known code table.
 *
 * <p>Delegates to an inner strategy for the base calculation,
 * then subtracts the discount percentage.
 */
public class DiscountCodePricing implements PricingStrategy {

    private static final Map<String, BigDecimal> CODES = Map.of(
            "STUDENT20", new BigDecimal("0.20"),
            "SENIOR15", new BigDecimal("0.15"),
            "WELCOME10", new BigDecimal("0.10")
    );

    private final PricingStrategy base;
    private final BigDecimal discountRate;

    /**
     * @param base     the underlying pricing strategy
     * @param code     the discount code (case-insensitive)
     * @throws IllegalArgumentException if the code is not recognized
     */
    public DiscountCodePricing(PricingStrategy base, String code) {
        this.base = base;
        this.discountRate = CODES.get(code.toUpperCase());
        if (this.discountRate == null) {
            throw new IllegalArgumentException("Unknown discount code: " + code);
        }
    }

    @Override
    public BigDecimal calculate(Showtime showtime, Seat seat) {
        BigDecimal price = base.calculate(showtime, seat);
        BigDecimal discount = price.multiply(discountRate);
        return price.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    /** Check whether a discount code is valid. */
    public static boolean isValidCode(String code) {
        return code != null && CODES.containsKey(code.toUpperCase());
    }
}
