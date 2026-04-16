package com.cinebook.domain;

import com.cinebook.domain.enums.BookingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookingTest {

    private Booking pending() {
        return new Booking("BK-001", "U001", "ST001",
                List.of("D6", "D7"), new BigDecimal("200.00"),
                BookingStatus.PENDING, Instant.now(), 1);
    }

    // --- confirm ---

    @Test
    void confirm_fromPending_succeeds() {
        Booking b = pending();
        b.confirm();
        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
        assertEquals(2, b.getVersion());
    }

    @Test
    void confirm_fromConfirmed_throws() {
        Booking b = pending();
        b.confirm();
        assertThrows(IllegalStateException.class, b::confirm);
    }

    @Test
    void confirm_fromCancelled_throws() {
        Booking b = pending();
        b.cancel();
        assertThrows(IllegalStateException.class, b::confirm);
    }

    // --- cancel ---

    @Test
    void cancel_fromPending_succeeds() {
        Booking b = pending();
        b.cancel();
        assertEquals(BookingStatus.CANCELLED, b.getStatus());
        assertEquals(2, b.getVersion());
    }

    @Test
    void cancel_fromConfirmed_succeeds() {
        Booking b = pending();
        b.confirm();
        b.cancel();
        assertEquals(BookingStatus.CANCELLED, b.getStatus());
        assertEquals(3, b.getVersion());
    }

    @Test
    void cancel_fromCancelled_throws() {
        Booking b = pending();
        b.cancel();
        assertThrows(IllegalStateException.class, b::cancel);
    }

    @Test
    void cancel_fromRefunded_throws() {
        Booking b = pending();
        b.confirm();
        b.refund();
        assertThrows(IllegalStateException.class, b::cancel);
    }

    // --- refund ---

    @Test
    void refund_fromConfirmed_succeeds() {
        Booking b = pending();
        b.confirm();
        b.refund();
        assertEquals(BookingStatus.REFUNDED, b.getStatus());
        assertEquals(3, b.getVersion());
    }

    @Test
    void refund_fromPending_throws() {
        Booking b = pending();
        assertThrows(IllegalStateException.class, b::refund);
    }

    @Test
    void refund_fromCancelled_throws() {
        Booking b = pending();
        b.cancel();
        assertThrows(IllegalStateException.class, b::refund);
    }

    // --- version tracking ---

    @Test
    void versionIncrementsOnEachTransition() {
        Booking b = pending();
        assertEquals(1, b.getVersion());
        b.confirm();
        assertEquals(2, b.getVersion());
        b.refund();
        assertEquals(3, b.getVersion());
    }
}
