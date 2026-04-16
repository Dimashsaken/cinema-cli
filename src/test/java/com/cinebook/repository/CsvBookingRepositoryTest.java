package com.cinebook.repository;

import com.cinebook.domain.Booking;
import com.cinebook.domain.enums.BookingStatus;
import com.cinebook.infra.CsvAppendAdapter;
import com.cinebook.infra.CsvBookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvBookingRepositoryTest {

    private BookingRepository repo;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        Path file = tempDir.resolve("bookings.csv");
        repo = new CsvBookingRepository(file, new CsvAppendAdapter());
    }

    private Booking booking(String id, String userId, BookingStatus status, int version) {
        return new Booking(id, userId, "ST001",
                List.of("D6", "D7"), new BigDecimal("200.00"),
                status, Instant.parse("2026-04-20T17:35:00Z"), version);
    }

    @Test
    void save_and_findById() {
        Booking b = booking("BK-001", "U001", BookingStatus.CONFIRMED, 1);
        repo.save(b);

        var found = repo.findById("BK-001");
        assertTrue(found.isPresent());
        assertEquals("U001", found.get().getUserId());
        assertEquals(BookingStatus.CONFIRMED, found.get().getStatus());
    }

    @Test
    void findById_nonExistent() {
        assertTrue(repo.findById("NOPE").isEmpty());
    }

    @Test
    void findByUserId() {
        repo.save(booking("BK-001", "U001", BookingStatus.CONFIRMED, 1));
        repo.save(booking("BK-002", "U002", BookingStatus.CONFIRMED, 1));
        repo.save(booking("BK-003", "U001", BookingStatus.PENDING, 1));

        List<Booking> u001 = repo.findByUserId("U001");
        assertEquals(2, u001.size());
    }

    @Test
    void findAll_returnsLatestVersions() {
        repo.save(booking("BK-001", "U001", BookingStatus.PENDING, 1));
        repo.save(booking("BK-001", "U001", BookingStatus.CONFIRMED, 2));

        List<Booking> all = repo.findAll();
        assertEquals(1, all.size());
        assertEquals(BookingStatus.CONFIRMED, all.get(0).getStatus());
        assertEquals(2, all.get(0).getVersion());
    }

    @Test
    void findById_returnsLatestVersion() {
        repo.save(booking("BK-001", "U001", BookingStatus.PENDING, 1));
        repo.save(booking("BK-001", "U001", BookingStatus.CONFIRMED, 2));
        repo.save(booking("BK-001", "U001", BookingStatus.CANCELLED, 3));

        var found = repo.findById("BK-001");
        assertTrue(found.isPresent());
        assertEquals(BookingStatus.CANCELLED, found.get().getStatus());
        assertEquals(3, found.get().getVersion());
    }

    @Test
    void deleteById_throws() {
        assertThrows(UnsupportedOperationException.class,
                () -> repo.deleteById("BK-001"));
    }

    @Test
    void seats_roundTrip() {
        Booking b = new Booking("BK-001", "U001", "ST001",
                List.of("A1", "A2", "B3"), new BigDecimal("300.00"),
                BookingStatus.CONFIRMED, Instant.parse("2026-04-20T17:35:00Z"), 1);
        repo.save(b);

        var found = repo.findById("BK-001").get();
        assertEquals(List.of("A1", "A2", "B3"), found.getSeats());
    }

    @Test
    void persistsAcrossInstances() {
        Path file = tempDir.resolve("persist.csv");
        CsvAppendAdapter adapter = new CsvAppendAdapter();

        BookingRepository repo1 = new CsvBookingRepository(file, adapter);
        repo1.save(booking("BK-001", "U001", BookingStatus.CONFIRMED, 1));

        BookingRepository repo2 = new CsvBookingRepository(file, adapter);
        assertEquals(1, repo2.findAll().size());
    }

    @Test
    void totalPrice_preservesPrecision() {
        Booking b = new Booking("BK-001", "U001", "ST001",
                List.of("A1"), new BigDecimal("99.50"),
                BookingStatus.CONFIRMED, Instant.parse("2026-04-20T17:35:00Z"), 1);
        repo.save(b);

        assertEquals(new BigDecimal("99.50"),
                repo.findById("BK-001").get().getTotalPrice());
    }
}
