package com.cinebook.infra;

import com.cinebook.domain.Showtime;
import com.cinebook.repository.ShowtimeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JSON file-backed implementation of {@link ShowtimeRepository}. */
public class JsonShowtimeRepository implements ShowtimeRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonShowtimeRepository.class);
    private static final TypeReference<List<Showtime>> TYPE_REF = new TypeReference<>() {};

    private final Path file;
    private final JsonFileAdapter adapter;
    private List<Showtime> showtimes;

    public JsonShowtimeRepository(Path file, JsonFileAdapter adapter) {
        this.file = file;
        this.adapter = adapter;
        this.showtimes = load();
    }

    @Override
    public Optional<Showtime> findById(String id) {
        return showtimes.stream()
                .filter(s -> s.getShowtimeId().equals(id))
                .findFirst();
    }

    @Override
    public List<Showtime> findAll() {
        return List.copyOf(showtimes);
    }

    @Override
    public List<Showtime> findByMovieId(String movieId) {
        return showtimes.stream()
                .filter(s -> s.getMovieId().equals(movieId))
                .toList();
    }

    @Override
    public void save(Showtime showtime) {
        showtimes.removeIf(s -> s.getShowtimeId().equals(showtime.getShowtimeId()));
        showtimes.add(showtime);
        persist();
    }

    @Override
    public void deleteById(String id) {
        showtimes.removeIf(s -> s.getShowtimeId().equals(id));
        persist();
    }

    private List<Showtime> load() {
        try {
            return new ArrayList<>(adapter.readList(file, TYPE_REF));
        } catch (IOException e) {
            log.error("Failed to load showtimes from {}", file, e);
            throw new UncheckedIOException(e);
        }
    }

    private void persist() {
        try {
            adapter.writeList(file, showtimes);
        } catch (IOException e) {
            log.error("Failed to persist showtimes to {}", file, e);
            throw new UncheckedIOException(e);
        }
    }
}
