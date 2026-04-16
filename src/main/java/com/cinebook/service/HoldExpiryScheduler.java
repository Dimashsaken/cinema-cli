package com.cinebook.service;

import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;
import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.repository.ShowtimeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Scans all showtimes every 30 seconds and releases expired holds.
 *
 * <p>Publishes {@code SeatReleasedEvent} via {@link SeatStatusNotifier}
 * so active CLI sessions can refresh their seat map.
 *
 * <p>This is Layer 2 of the 3-layer concurrency defense (Hold TTL).
 */
public class HoldExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(HoldExpiryScheduler.class);
    private static final long SCAN_INTERVAL_SECONDS = 30;

    private final ShowtimeRepository showtimeRepository;
    private final SeatStatusNotifier notifier;
    private final BookingManager bookingManager;
    private ScheduledExecutorService executor;

    public HoldExpiryScheduler(ShowtimeRepository showtimeRepository,
                               SeatStatusNotifier notifier) {
        this.showtimeRepository = showtimeRepository;
        this.notifier = notifier;
        this.bookingManager = BookingManager.getInstance();
    }

    /** Start the background scanner. */
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hold-expiry-scanner");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::scanAndRelease,
                SCAN_INTERVAL_SECONDS, SCAN_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("HoldExpiryScheduler started (interval: {}s)", SCAN_INTERVAL_SECONDS);
    }

    /** Stop the background scanner. */
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
            log.info("HoldExpiryScheduler stopped");
        }
    }

    /** Scan all showtimes for expired holds and release them. */
    void scanAndRelease() {
        try {
            for (Showtime showtime : showtimeRepository.findAll()) {
                List<String> released = releaseExpiredHolds(showtime);
                if (!released.isEmpty()) {
                    showtimeRepository.save(showtime);
                    notifier.notifySeatsReleased(showtime.getShowtimeId(), released);
                    log.info("Released {} expired holds on showtime {}",
                            released.size(), showtime.getShowtimeId());
                }
            }
        } catch (Exception e) {
            log.error("Error during hold expiry scan", e);
        }
    }

    private List<String> releaseExpiredHolds(Showtime showtime) {
        List<String> released = new ArrayList<>();
        ReentrantLock lock = bookingManager.getLock(showtime.getShowtimeId());
        lock.lock();
        try {
            for (List<Seat> row : showtime.getSeats()) {
                for (Seat seat : row) {
                    if (seat.getStatus() == SeatStatus.HELD && seat.isHoldExpired()) {
                        seat.release();
                        released.add(seat.getCode());
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        return released;
    }
}
