package com.cinebook.infra;

import com.cinebook.domain.Movie;
import com.cinebook.repository.MovieRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JSON file-backed implementation of {@link MovieRepository}. */
public class JsonMovieRepository implements MovieRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonMovieRepository.class);
    private static final TypeReference<List<Movie>> TYPE_REF = new TypeReference<>() {};

    private final Path file;
    private final JsonFileAdapter adapter;
    private List<Movie> movies;

    public JsonMovieRepository(Path file, JsonFileAdapter adapter) {
        this.file = file;
        this.adapter = adapter;
        this.movies = load();
    }

    @Override
    public Optional<Movie> findById(String id) {
        return movies.stream()
                .filter(m -> m.getMovieId().equals(id))
                .findFirst();
    }

    @Override
    public List<Movie> findAll() {
        return List.copyOf(movies);
    }

    @Override
    public void save(Movie movie) {
        movies.removeIf(m -> m.getMovieId().equals(movie.getMovieId()));
        movies.add(movie);
        persist();
    }

    @Override
    public void deleteById(String id) {
        movies.removeIf(m -> m.getMovieId().equals(id));
        persist();
    }

    private List<Movie> load() {
        try {
            return new ArrayList<>(adapter.readList(file, TYPE_REF));
        } catch (IOException e) {
            log.error("Failed to load movies from {}", file, e);
            throw new UncheckedIOException(e);
        }
    }

    private void persist() {
        try {
            adapter.writeList(file, movies);
        } catch (IOException e) {
            log.error("Failed to persist movies to {}", file, e);
            throw new UncheckedIOException(e);
        }
    }
}
