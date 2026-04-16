package com.cinebook.service;

import com.cinebook.domain.Hall;
import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;
import com.cinebook.domain.enums.SeatStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HoldExpirySchedulerTest {

    private InMemoryShowtimeRepository showtimeRepository;
    private SeatStatusNotifier notifier;
    private HoldExpiryScheduler scheduler;

    @BeforeEach
    void setUp() {
        BookingManager.getInstance().clearLocks();
        showtimeRepository = new InMemoryShowtimeRepository();
        notifier = new SeatStatusNotifier();
        scheduler = new HoldExpiryScheduler(showtimeRepository, notifier);
    }

    private Showtime createShowtime() {
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        return new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 14, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid());
    }

    @Test
    void scanAndRelease_expiredHold_released() {
        Showtime st = createShowtime();
        Seat seat = st.findSeat("C1").get();
        seat.hold("U001", Instant.now().minus(1, ChronoUnit.MINUTES)); // already expired
        showtimeRepository.save(st);

        scheduler.scanAndRelease();

        Showtime result = showtimeRepository.findById("ST001").get();
        assertEquals(SeatStatus.AVAILABLE, result.findSeat("C1").get().getStatus());
    }

    @Test
    void scanAndRelease_nonExpiredHold_notReleased() {
        Showtime st = createShowtime();
        Seat seat = st.findSeat("C1").get();
        seat.hold("U001", Instant.now().plus(5, ChronoUnit.MINUTES));
        showtimeRepository.save(st);

        scheduler.scanAndRelease();

        Showtime result = showtimeRepository.findById("ST001").get();
        assertEquals(SeatStatus.HELD, result.findSeat("C1").get().getStatus());
    }

    @Test
    void scanAndRelease_notifiesObservers() {
        Showtime st = createShowtime();
        st.findSeat("C1").get().hold("U001", Instant.now().minus(1, ChronoUnit.MINUTES));
        st.findSeat("C2").get().hold("U001", Instant.now().minus(1, ChronoUnit.MINUTES));
        showtimeRepository.save(st);

        List<String> notified = new ArrayList<>();
        notifier.subscribe((showtimeId, seatCodes) -> notified.addAll(seatCodes));

        scheduler.scanAndRelease();

        assertEquals(2, notified.size());
        assertTrue(notified.contains("C1"));
        assertTrue(notified.contains("C2"));
    }

    @Test
    void scanAndRelease_mixedExpiredAndValid() {
        Showtime st = createShowtime();
        st.findSeat("C1").get().hold("U001", Instant.now().minus(1, ChronoUnit.MINUTES));
        st.findSeat("C2").get().hold("U002", Instant.now().plus(5, ChronoUnit.MINUTES));
        showtimeRepository.save(st);

        scheduler.scanAndRelease();

        Showtime result = showtimeRepository.findById("ST001").get();
        assertEquals(SeatStatus.AVAILABLE, result.findSeat("C1").get().getStatus());
        assertEquals(SeatStatus.HELD, result.findSeat("C2").get().getStatus());
    }

    @Test
    void scanAndRelease_noHolds_noop() {
        Showtime st = createShowtime();
        showtimeRepository.save(st);

        List<String> notified = new ArrayList<>();
        notifier.subscribe((showtimeId, seatCodes) -> notified.addAll(seatCodes));

        scheduler.scanAndRelease();

        assertTrue(notified.isEmpty());
    }
}
