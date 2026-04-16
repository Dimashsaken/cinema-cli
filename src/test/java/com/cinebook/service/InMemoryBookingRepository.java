package com.cinebook.service;

import com.cinebook.domain.Booking;
import com.cinebook.repository.BookingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory booking repository for concurrency testing.
 */
class InMemoryBookingRepository implements BookingRepository {

    private final ConcurrentHashMap<String, Booking> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Booking> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Booking> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Booking> findByUserId(String userId) {
        return store.values().stream()
                .filter(b -> b.getUserId().equals(userId))
                .toList();
    }

    @Override
    public void save(Booking booking) {
        store.put(booking.getBookingId(), booking);
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }
}
