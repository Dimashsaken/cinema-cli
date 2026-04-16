package com.cinebook.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Singleton registry of per-showtime locks.
 *
 * <p>Guarantees that all threads share the same {@link ReentrantLock} instance
 * for a given showtime, preventing double-booking through in-process
 * mutual exclusion.
 *
 * <p>This is Layer 1 of the 3-layer concurrency defense.
 */
public class BookingManager {

    private static final BookingManager INSTANCE = new BookingManager();

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private BookingManager() {}

    /** Get the singleton instance. */
    public static BookingManager getInstance() {
        return INSTANCE;
    }

    /**
     * Get or create a lock for the given showtime.
     * All threads calling this with the same showtimeId get the same lock.
     */
    public ReentrantLock getLock(String showtimeId) {
        return locks.computeIfAbsent(showtimeId, k -> new ReentrantLock());
    }

    /** Remove a lock (used in testing to reset state). */
    void removeLock(String showtimeId) {
        locks.remove(showtimeId);
    }

    /** Clear all locks (used in testing). */
    void clearLocks() {
        locks.clear();
    }
}
