package com.cinebook.repository;

import com.cinebook.domain.Movie;
import com.cinebook.infra.JsonFileAdapter;
import com.cinebook.infra.JsonMovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonMovieRepositoryTest {

    private MovieRepository repo;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        Path file = tempDir.resolve("movies.json");
        repo = new JsonMovieRepository(file, new JsonFileAdapter());
    }

    @Test
    void save_and_findById() {
        Movie movie = new Movie("M001", "Dune", 166, "PG-13", "Sci-Fi");
        repo.save(movie);

        Optional<Movie> found = repo.findById("M001");
        assertTrue(found.isPresent());
        assertEquals("Dune", found.get().getTitle());
    }

    @Test
    void findById_nonExistent_empty() {
        assertTrue(repo.findById("NOPE").isEmpty());
    }

    @Test
    void findAll_empty() {
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void findAll_returnsAll() {
        repo.save(new Movie("M001", "Dune", 166, "PG-13", "Sci-Fi"));
        repo.save(new Movie("M002", "Batman", 176, "PG-13", "Action"));

        List<Movie> all = repo.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void save_updatesExisting() {
        repo.save(new Movie("M001", "Dune", 166, "PG-13", "Sci-Fi"));
        repo.save(new Movie("M001", "Dune Part Two", 166, "PG-13", "Sci-Fi"));

        List<Movie> all = repo.findAll();
        assertEquals(1, all.size());
        assertEquals("Dune Part Two", all.get(0).getTitle());
    }

    @Test
    void deleteById() {
        repo.save(new Movie("M001", "Dune", 166, "PG-13", "Sci-Fi"));
        repo.deleteById("M001");

        assertTrue(repo.findById("M001").isEmpty());
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void deleteById_nonExistent_noError() {
        assertDoesNotThrow(() -> repo.deleteById("NOPE"));
    }

    @Test
    void persistsAcrossInstances() {
        Path file = tempDir.resolve("persist.json");
        JsonFileAdapter adapter = new JsonFileAdapter();

        MovieRepository repo1 = new JsonMovieRepository(file, adapter);
        repo1.save(new Movie("M001", "Dune", 166, "PG-13", "Sci-Fi"));

        MovieRepository repo2 = new JsonMovieRepository(file, adapter);
        assertEquals(1, repo2.findAll().size());
        assertEquals("Dune", repo2.findById("M001").get().getTitle());
    }
}
