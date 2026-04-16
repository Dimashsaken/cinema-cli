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

class StandardPricingTest {

    private final StandardPricing pricing = new StandardPricing();

    private Showtime showtime(BigDecimal basePrice) {
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        return new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 14, 0),
                basePrice, hall.generateSeatGrid());
    }

    @Test
    void regularSeat_returnsBasePrice() {
        Showtime st = showtime(new BigDecimal("80.00"));
        Seat seat = new Seat(2, 0, SeatTier.REGULAR, SeatStatus.AVAILABLE);

        assertEquals(new BigDecimal("80.00"), pricing.calculate(st, seat));
    }

    @Test
    void vipSeat_returns150Percent() {
        Showtime st = showtime(new BigDecimal("80.00"));
        Seat seat = new Seat(0, 0, SeatTier.VIP, SeatStatus.AVAILABLE);

        assertEquals(0, new BigDecimal("120.00").compareTo(pricing.calculate(st, seat)));
    }

    @Test
    void vipSeat_differentBasePrice() {
        Showtime st = showtime(new BigDecimal("100.00"));
        Seat seat = new Seat(0, 0, SeatTier.VIP, SeatStatus.AVAILABLE);

        assertEquals(0, new BigDecimal("150.00").compareTo(pricing.calculate(st, seat)));
    }
}
