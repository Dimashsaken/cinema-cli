package com.cinebook.domain;

import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.domain.enums.SeatTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ShowtimeTest {

    private Showtime showtime;

    @BeforeEach
    void setUp() {
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        List<List<Seat>> grid = hall.generateSeatGrid();
        showtime = new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 18, 0),
                new BigDecimal("80.00"), grid);
    }

    @Test
    void findSeat_validCode() {
        Optional<Seat> seat = showtime.findSeat("A1");
        assertTrue(seat.isPresent());
        assertEquals(0, seat.get().getRow());
        assertEquals(0, seat.get().getCol());
        assertEquals(SeatTier.VIP, seat.get().getTier());
    }

    @Test
    void findSeat_lastSeat() {
        Optional<Seat> seat = showtime.findSeat("C4");
        assertTrue(seat.isPresent());
        assertEquals(2, seat.get().getRow());
        assertEquals(3, seat.get().getCol());
        assertEquals(SeatTier.REGULAR, seat.get().getTier());
    }

    @Test
    void findSeat_outOfBoundsRow() {
        Optional<Seat> seat = showtime.findSeat("Z1");
        assertTrue(seat.isEmpty());
    }

    @Test
    void findSeat_outOfBoundsCol() {
        Optional<Seat> seat = showtime.findSeat("A99");
        assertTrue(seat.isEmpty());
    }

    @Test
    void findSeat_invalidCode_throws() {
        assertThrows(IllegalArgumentException.class, () -> showtime.findSeat("!!"));
    }

    @Test
    void getRowCount() {
        assertEquals(3, showtime.getRowCount());
    }

    @Test
    void getColCount() {
        assertEquals(4, showtime.getColCount());
    }

    @Test
    void getLock_neverNull() {
        assertNotNull(showtime.getLock());
    }

    @Test
    void getLock_sameInstance() {
        assertSame(showtime.getLock(), showtime.getLock());
    }

    @Test
    void allSeatsAvailable() {
        for (List<Seat> row : showtime.getSeats()) {
            for (Seat seat : row) {
                assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
            }
        }
    }

    @Test
    void vipRowsAssignedCorrectly() {
        // Row 0 should be VIP
        for (Seat seat : showtime.getSeats().get(0)) {
            assertEquals(SeatTier.VIP, seat.getTier());
        }
        // Rows 1-2 should be REGULAR
        for (int r = 1; r < 3; r++) {
            for (Seat seat : showtime.getSeats().get(r)) {
                assertEquals(SeatTier.REGULAR, seat.getTier());
            }
        }
    }
}
