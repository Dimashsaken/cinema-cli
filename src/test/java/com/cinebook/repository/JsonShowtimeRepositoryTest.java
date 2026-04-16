package com.cinebook.repository;

import com.cinebook.domain.Hall;
import com.cinebook.domain.Showtime;
import com.cinebook.infra.JsonFileAdapter;
import com.cinebook.infra.JsonShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JsonShowtimeRepositoryTest {

    private ShowtimeRepository repo;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        Path file = tempDir.resolve("showtimes.json");
        repo = new JsonShowtimeRepository(file, new JsonFileAdapter());
    }

    private Showtime makeShowtime(String id, String movieId) {
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        return new Showtime(id, movieId, "H1",
                LocalDateTime.of(2026, 4, 20, 18, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid());
    }

    @Test
    void save_and_findById() {
        Showtime st = makeShowtime("ST001", "M001");
        repo.save(st);

        assertTrue(repo.findById("ST001").isPresent());
        assertEquals("M001", repo.findById("ST001").get().getMovieId());
    }

    @Test
    void findByMovieId() {
        repo.save(makeShowtime("ST001", "M001"));
        repo.save(makeShowtime("ST002", "M001"));
        repo.save(makeShowtime("ST003", "M002"));

        List<Showtime> m001 = repo.findByMovieId("M001");
        assertEquals(2, m001.size());

        List<Showtime> m002 = repo.findByMovieId("M002");
        assertEquals(1, m002.size());
    }

    @Test
    void findByMovieId_none() {
        assertTrue(repo.findByMovieId("NOPE").isEmpty());
    }

    @Test
    void save_updatesExisting() {
        repo.save(makeShowtime("ST001", "M001"));
        Showtime updated = makeShowtime("ST001", "M002");
        repo.save(updated);

        assertEquals(1, repo.findAll().size());
        assertEquals("M002", repo.findById("ST001").get().getMovieId());
    }

    @Test
    void deleteById() {
        repo.save(makeShowtime("ST001", "M001"));
        repo.deleteById("ST001");
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void seatGrid_survivesRoundTrip() {
        Showtime st = makeShowtime("ST001", "M001");
        repo.save(st);

        Showtime loaded = repo.findById("ST001").get();
        assertEquals(3, loaded.getRowCount());
        assertEquals(4, loaded.getColCount());
        assertTrue(loaded.findSeat("A1").isPresent());
    }

    @Test
    void persistsAcrossInstances() {
        Path file = tempDir.resolve("persist.json");
        JsonFileAdapter adapter = new JsonFileAdapter();

        ShowtimeRepository repo1 = new JsonShowtimeRepository(file, adapter);
        repo1.save(makeShowtime("ST001", "M001"));

        ShowtimeRepository repo2 = new JsonShowtimeRepository(file, adapter);
        assertEquals(1, repo2.findAll().size());
    }
}
