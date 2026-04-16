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

class DiscountCodePricingTest {

    private final Seat regular = new Seat(2, 0, SeatTier.REGULAR, SeatStatus.AVAILABLE);

    private Showtime showtime() {
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        return new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 14, 0),
                new BigDecimal("100.00"), hall.generateSeatGrid());
    }

    @Test
    void student20_applies20PercentDiscount() {
        DiscountCodePricing pricing = new DiscountCodePricing(new StandardPricing(), "STUDENT20");
        // 100 - 20% = 80
        assertEquals(0, new BigDecimal("80.00").compareTo(
                pricing.calculate(showtime(), regular)));
    }

    @Test
    void senior15_applies15PercentDiscount() {
        DiscountCodePricing pricing = new DiscountCodePricing(new StandardPricing(), "SENIOR15");
        // 100 - 15% = 85
        assertEquals(0, new BigDecimal("85.00").compareTo(
                pricing.calculate(showtime(), regular)));
    }

    @Test
    void welcome10_applies10PercentDiscount() {
        DiscountCodePricing pricing = new DiscountCodePricing(new StandardPricing(), "WELCOME10");
        // 100 - 10% = 90
        assertEquals(0, new BigDecimal("90.00").compareTo(
                pricing.calculate(showtime(), regular)));
    }

    @Test
    void caseInsensitive() {
        DiscountCodePricing pricing = new DiscountCodePricing(new StandardPricing(), "student20");
        assertEquals(0, new BigDecimal("80.00").compareTo(
                pricing.calculate(showtime(), regular)));
    }

    @Test
    void unknownCode_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new DiscountCodePricing(new StandardPricing(), "FAKE99"));
    }

    @Test
    void vipSeat_discountAppliedToVipPrice() {
        Seat vip = new Seat(0, 0, SeatTier.VIP, SeatStatus.AVAILABLE);
        DiscountCodePricing pricing = new DiscountCodePricing(new StandardPricing(), "STUDENT20");
        // 100 * 1.5 (VIP) = 150, 150 - 20% = 120
        assertEquals(0, new BigDecimal("120.00").compareTo(
                pricing.calculate(showtime(), vip)));
    }

    @Test
    void isValidCode_check() {
        assertTrue(DiscountCodePricing.isValidCode("STUDENT20"));
        assertTrue(DiscountCodePricing.isValidCode("student20"));
        assertTrue(DiscountCodePricing.isValidCode("SENIOR15"));
        assertTrue(DiscountCodePricing.isValidCode("WELCOME10"));
        assertFalse(DiscountCodePricing.isValidCode("FAKE99"));
        assertFalse(DiscountCodePricing.isValidCode(null));
    }
}
