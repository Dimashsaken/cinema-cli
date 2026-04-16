package com.cinebook.service;

import com.cinebook.domain.Showtime;
import com.cinebook.repository.ShowtimeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory showtime repository for concurrency testing.
 *
 * <p>Unlike the JSON-based repo, this stores Showtime objects directly
 * in a ConcurrentHashMap, so all threads share the same Seat instances.
 * This is essential for testing that the ReentrantLock in BookingManager
 * properly serializes access to the seat grid.
 */
class InMemoryShowtimeRepository implements ShowtimeRepository {

    private final ConcurrentHashMap<String, Showtime> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Showtime> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Showtime> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Showtime> findByMovieId(String movieId) {
        return store.values().stream()
                .filter(s -> s.getMovieId().equals(movieId))
                .toList();
    }

    @Override
    public void save(Showtime showtime) {
        store.put(showtime.getShowtimeId(), showtime);
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
