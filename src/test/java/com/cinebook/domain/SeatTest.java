package com.cinebook.domain;

import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.domain.enums.SeatTier;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class SeatTest {

    private Seat available(int row, int col) {
        return new Seat(row, col, SeatTier.REGULAR, SeatStatus.AVAILABLE);
    }

    // --- getCode ---

    @Test
    void getCode_row0Col0_returnsA1() {
        assertEquals("A1", available(0, 0).getCode());
    }

    @Test
    void getCode_row2Col9_returnsC10() {
        assertEquals("C10", available(2, 9).getCode());
    }

    // --- parseSeatCode ---

    @Test
    void parseSeatCode_validA1() {
        int[] rc = Seat.parseSeatCode("A1");
        assertEquals(0, rc[0]);
        assertEquals(0, rc[1]);
    }

    @Test
    void parseSeatCode_validC10() {
        int[] rc = Seat.parseSeatCode("C10");
        assertEquals(2, rc[0]);
        assertEquals(9, rc[1]);
    }

    @Test
    void parseSeatCode_lowercase() {
        int[] rc = Seat.parseSeatCode("b5");
        assertEquals(1, rc[0]);
        assertEquals(4, rc[1]);
    }

    @Test
    void parseSeatCode_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> Seat.parseSeatCode(null));
    }

    @Test
    void parseSeatCode_empty_throws() {
        assertThrows(IllegalArgumentException.class, () -> Seat.parseSeatCode(""));
    }

    @Test
    void parseSeatCode_singleChar_throws() {
        assertThrows(IllegalArgumentException.class, () -> Seat.parseSeatCode("A"));
    }

    @Test
    void parseSeatCode_noDigit_throws() {
        assertThrows(IllegalArgumentException.class, () -> Seat.parseSeatCode("AB"));
    }

    @Test
    void parseSeatCode_zeroCol_throws() {
        assertThrows(IllegalArgumentException.class, () -> Seat.parseSeatCode("A0"));
    }

    @Test
    void parseSeatCode_digitFirst_throws() {
        assertThrows(IllegalArgumentException.class, () -> Seat.parseSeatCode("1A"));
    }

    // --- hold ---

    @Test
    void hold_fromAvailable_succeeds() {
        Seat seat = available(0, 0);
        Instant expires = Instant.now().plus(5, ChronoUnit.MINUTES);

        seat.hold("U001", expires);

        assertEquals(SeatStatus.HELD, seat.getStatus());
        assertEquals("U001", seat.getHeldBy());
        assertEquals(expires, seat.getHoldExpiresAt());
    }

    @Test
    void hold_fromHeld_throws() {
        Seat seat = available(0, 0);
        seat.hold("U001", Instant.now().plus(5, ChronoUnit.MINUTES));

        assertThrows(IllegalStateException.class,
                () -> seat.hold("U002", Instant.now().plus(5, ChronoUnit.MINUTES)));
    }

    @Test
    void hold_fromBooked_throws() {
        Seat seat = available(0, 0);
        Instant expires = Instant.now().plus(5, ChronoUnit.MINUTES);
        seat.hold("U001", expires);
        seat.book("U001");

        assertThrows(IllegalStateException.class,
                () -> seat.hold("U002", Instant.now().plus(5, ChronoUnit.MINUTES)));
    }

    // --- book ---

    @Test
    void book_fromHeld_sameUser_succeeds() {
        Seat seat = available(0, 0);
        seat.hold("U001", Instant.now().plus(5, ChronoUnit.MINUTES));

        seat.book("U001");

        assertEquals(SeatStatus.BOOKED, seat.getStatus());
        assertNull(seat.getHeldBy());
        assertNull(seat.getHoldExpiresAt());
    }

    @Test
    void book_fromAvailable_throws() {
        Seat seat = available(0, 0);
        assertThrows(IllegalStateException.class, () -> seat.book("U001"));
    }

    @Test
    void book_differentUser_throws() {
        Seat seat = available(0, 0);
        seat.hold("U001", Instant.now().plus(5, ChronoUnit.MINUTES));

        assertThrows(IllegalStateException.class, () -> seat.book("U002"));
    }

    @Test
    void book_expiredHold_throws() {
        Seat seat = available(0, 0);
        seat.hold("U001", Instant.now().minus(1, ChronoUnit.MINUTES));

        assertThrows(IllegalStateException.class, () -> seat.book("U001"));
    }

    // --- release ---

    @Test
    void release_fromHeld_succeeds() {
        Seat seat = available(0, 0);
        seat.hold("U001", Instant.now().plus(5, ChronoUnit.MINUTES));

        seat.release();

        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        assertNull(seat.getHeldBy());
        assertNull(seat.getHoldExpiresAt());
    }

    @Test
    void release_fromBooked_succeeds() {
        Seat seat = available(0, 0);
        seat.hold("U001", Instant.now().plus(5, ChronoUnit.MINUTES));
        seat.book("U001");

        seat.release();

        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
    }

    @Test
    void release_fromAvailable_throws() {
        Seat seat = available(0, 0);
        assertThrows(IllegalStateException.class, seat::release);
    }

    // --- isHoldExpired ---

    @Test
    void isHoldExpired_notHeld_false() {
        Seat seat = available(0, 0);
        assertFalse(seat.isHoldExpired());
    }

    @Test
    void isHoldExpired_heldNotExpired_false() {
        Seat seat = available(0, 0);
        seat.hold("U001", Instant.now().plus(5, ChronoUnit.MINUTES));
        assertFalse(seat.isHoldExpired());
    }

    @Test
    void isHoldExpired_heldExpired_true() {
        Seat seat = available(0, 0);
        seat.hold("U001", Instant.now().minus(1, ChronoUnit.MINUTES));
        assertTrue(seat.isHoldExpired());
    }

    // --- full lifecycle ---

    @Test
    void fullLifecycle_hold_book_release() {
        Seat seat = available(2, 4);
        assertEquals("C5", seat.getCode());

        seat.hold("U001", Instant.now().plus(5, ChronoUnit.MINUTES));
        assertEquals(SeatStatus.HELD, seat.getStatus());

        seat.book("U001");
        assertEquals(SeatStatus.BOOKED, seat.getStatus());

        seat.release();
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
    }
}
