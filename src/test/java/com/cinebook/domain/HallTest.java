package com.cinebook.domain;

import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.domain.enums.SeatTier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HallTest {

    @Test
    void generateSeatGrid_dimensions() {
        Hall hall = new Hall("H1", 5, 8, Set.of(0, 1));
        List<List<Seat>> grid = hall.generateSeatGrid();

        assertEquals(5, grid.size());
        for (List<Seat> row : grid) {
            assertEquals(8, row.size());
        }
    }

    @Test
    void generateSeatGrid_vipRows() {
        Hall hall = new Hall("H1", 4, 3, Set.of(0, 1));
        List<List<Seat>> grid = hall.generateSeatGrid();

        for (Seat seat : grid.get(0)) {
            assertEquals(SeatTier.VIP, seat.getTier());
        }
        for (Seat seat : grid.get(1)) {
            assertEquals(SeatTier.VIP, seat.getTier());
        }
        for (Seat seat : grid.get(2)) {
            assertEquals(SeatTier.REGULAR, seat.getTier());
        }
        for (Seat seat : grid.get(3)) {
            assertEquals(SeatTier.REGULAR, seat.getTier());
        }
    }

    @Test
    void generateSeatGrid_allAvailable() {
        Hall hall = new Hall("H1", 3, 3, Set.of());
        List<List<Seat>> grid = hall.generateSeatGrid();

        for (List<Seat> row : grid) {
            for (Seat seat : row) {
                assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
                assertNull(seat.getHeldBy());
                assertNull(seat.getHoldExpiresAt());
            }
        }
    }

    @Test
    void generateSeatGrid_seatCoordinates() {
        Hall hall = new Hall("H1", 2, 2, Set.of());
        List<List<Seat>> grid = hall.generateSeatGrid();

        assertEquals(0, grid.get(0).get(0).getRow());
        assertEquals(0, grid.get(0).get(0).getCol());
        assertEquals("A1", grid.get(0).get(0).getCode());

        assertEquals(1, grid.get(1).get(1).getRow());
        assertEquals(1, grid.get(1).get(1).getCol());
        assertEquals("B2", grid.get(1).get(1).getCode());
    }

    @Test
    void generateSeatGrid_noVipRows() {
        Hall hall = new Hall("H2", 3, 4, Set.of());
        List<List<Seat>> grid = hall.generateSeatGrid();

        for (List<Seat> row : grid) {
            for (Seat seat : row) {
                assertEquals(SeatTier.REGULAR, seat.getTier());
            }
        }
    }

    @Test
    void generateSeatGrid_nullVipRows() {
        Hall hall = new Hall("H2", 2, 2, null);
        List<List<Seat>> grid = hall.generateSeatGrid();

        for (List<Seat> row : grid) {
            for (Seat seat : row) {
                assertEquals(SeatTier.REGULAR, seat.getTier());
            }
        }
    }
}
