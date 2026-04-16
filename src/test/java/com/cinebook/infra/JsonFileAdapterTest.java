package com.cinebook.infra;

import com.cinebook.domain.Movie;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileAdapterTest {

    private JsonFileAdapter adapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        adapter = new JsonFileAdapter();
    }

    @Test
    void writeAndReadList() throws IOException {
        Path file = tempDir.resolve("movies.json");
        List<Movie> movies = List.of(
                new Movie("M001", "Dune", 166, "PG-13", "Sci-Fi"),
                new Movie("M002", "Batman", 176, "PG-13", "Action")
        );

        adapter.writeList(file, movies);
        List<Movie> loaded = adapter.readList(file, new TypeReference<>() {});

        assertEquals(2, loaded.size());
        assertEquals("Dune", loaded.get(0).getTitle());
        assertEquals("Batman", loaded.get(1).getTitle());
    }

    @Test
    void readList_nonExistentFile_returnsEmpty() throws IOException {
        Path file = tempDir.resolve("nope.json");
        List<Movie> result = adapter.readList(file, new TypeReference<>() {});
        assertTrue(result.isEmpty());
    }

    @Test
    void readList_emptyFile_returnsEmpty() throws IOException {
        Path file = tempDir.resolve("empty.json");
        Files.writeString(file, "");
        List<Movie> result = adapter.readList(file, new TypeReference<>() {});
        assertTrue(result.isEmpty());
    }

    @Test
    void writeList_createsParentDirs() throws IOException {
        Path file = tempDir.resolve("sub/dir/movies.json");
        adapter.writeList(file, List.of(new Movie("M001", "Dune", 166, "PG-13", "Sci-Fi")));
        assertTrue(Files.exists(file));
    }

    @Test
    void writeList_overwritesExisting() throws IOException {
        Path file = tempDir.resolve("movies.json");
        adapter.writeList(file, List.of(
                new Movie("M001", "First", 100, "PG", "Drama")
        ));
        adapter.writeList(file, List.of(
                new Movie("M002", "Second", 120, "R", "Thriller")
        ));

        List<Movie> loaded = adapter.readList(file, new TypeReference<>() {});
        assertEquals(1, loaded.size());
        assertEquals("Second", loaded.get(0).getTitle());
    }

    @Test
    void atomicWrite_noTmpFileLeftBehind() throws IOException {
        Path file = tempDir.resolve("movies.json");
        Path tmp = tempDir.resolve("movies.json.tmp");
        adapter.writeList(file, List.of(new Movie("M001", "Dune", 166, "PG-13", "Sci-Fi")));
        assertFalse(Files.exists(tmp), "Temp file should not remain after atomic write");
    }

    @Test
    void writeList_emptyList() throws IOException {
        Path file = tempDir.resolve("movies.json");
        adapter.writeList(file, List.of());
        List<Movie> loaded = adapter.readList(file, new TypeReference<>() {});
        assertTrue(loaded.isEmpty());
    }
}
