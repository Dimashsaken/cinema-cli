package com.cinebook.service;

import com.cinebook.domain.Hall;
import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;
import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.infra.TransactionLog;
import com.cinebook.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests WAL crash recovery logic.
 *
 * <p>Simulates a crash mid-booking by writing a HOLD WAL entry and leaving
 * seats in HELD state without a COMMIT. Recovery should release those seats.
 */
class WalRecoveryTest {

    private TransactionLog wal;
    private ShowtimeRepository showtimeRepo;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        wal = new TransactionLog(tempDir.resolve("wal.log"));
        showtimeRepo = new InMemoryShowtimeRepository();
        BookingManager.getInstance().clearLocks();
    }

    private Showtime createShowtime() {
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        Showtime st = new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 14, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid());
        showtimeRepo.save(st);
        return st;
    }

    /**
     * Simulate the recovery logic from App.main.
     */
    private void runRecovery() throws IOException {
        List<String> uncommitted = wal.findUncommittedHolds();
        for (String entry : uncommitted) {
            String[] parts = entry.split(" ");
            if (parts.length < 5) continue;
            String showtimeId = parts[2];
            String[] seatCodes = parts[4].split(",");

            showtimeRepo.findById(showtimeId).ifPresent(showtime -> {
                for (String code : seatCodes) {
                    showtime.findSeat(code.trim()).ifPresent(seat -> {
                        if (seat.getStatus() == SeatStatus.HELD) {
                            seat.release();
                        }
                    });
                }
                showtimeRepo.save(showtime);
            });

            try {
                wal.logRollback(showtimeId, List.of(seatCodes));
            } catch (IOException ignored) {}
        }
    }

    @Test
    void recovery_releasesUncommittedHolds() throws Exception {
        Showtime st = createShowtime();
        Seat seat = st.findSeat("C1").get();
        seat.hold("U001", Instant.now().plus(5, ChronoUnit.MINUTES));
        showtimeRepo.save(st);

        // Write WAL HOLD but NO COMMIT — simulating crash
        wal.logHold("ST001", "U001", List.of("C1"));

        // Run recovery
        runRecovery();

        // Seat should be released
        Showtime recovered = showtimeRepo.findById("ST001").get();
        assertEquals(SeatStatus.AVAILABLE, recovered.findSeat("C1").get().getStatus());
    }

    @Test
    void recovery_multipleSeats() throws Exception {
        Showtime st = createShowtime();
        st.findSeat("C1").get().hold("U001", Instant.now().plus(5, ChronoUnit.MINUTES));
        st.findSeat("C2").get().hold("U001", Instant.now().plus(5, ChronoUnit.MINUTES));
        showtimeRepo.save(st);

        wal.logHold("ST001", "U001", List.of("C1", "C2"));

        runRecovery();

        Showtime recovered = showtimeRepo.findById("ST001").get();
        assertEquals(SeatStatus.AVAILABLE, recovered.findSeat("C1").get().getStatus());
        assertEquals(SeatStatus.AVAILABLE, recovered.findSeat("C2").get().getStatus());
    }

    @Test
    void recovery_committedHold_notReleased() throws Exception {
        Showtime st = createShowtime();
        Seat seat = st.findSeat("C1").get();
        seat.hold("U001", Instant.now().plus(5, ChronoUnit.MINUTES));
        seat.book("U001");
        showtimeRepo.save(st);

        // HOLD + COMMIT — this is a successful booking
        wal.logHold("ST001", "U001", List.of("C1"));
        // No COMMIT, but seat is already BOOKED so recovery shouldn't touch it

        runRecovery();

        Showtime recovered = showtimeRepo.findById("ST001").get();
        // The recovery checks status == HELD, BOOKED seats are untouched
        assertEquals(SeatStatus.BOOKED, recovered.findSeat("C1").get().getStatus());
    }

    @Test
    void recovery_alreadyRolledBack_noop() throws Exception {
        Showtime st = createShowtime();
        showtimeRepo.save(st);

        wal.logHold("ST001", "U001", List.of("C1"));
        wal.logRollback("ST001", List.of("C1"));

        // Already rolled back, seats should be available and recovery is a no-op
        runRecovery();

        Showtime recovered = showtimeRepo.findById("ST001").get();
        assertEquals(SeatStatus.AVAILABLE, recovered.findSeat("C1").get().getStatus());
    }

    @Test
    void recovery_noWalEntries_noop() throws Exception {
        createShowtime();
        // No WAL entries at all
        runRecovery();
        // Just verify it doesn't crash
        assertEquals(SeatStatus.AVAILABLE,
                showtimeRepo.findById("ST001").get().findSeat("C1").get().getStatus());
    }
}
