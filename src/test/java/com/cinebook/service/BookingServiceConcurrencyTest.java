package com.cinebook.service;

import com.cinebook.domain.*;
import com.cinebook.domain.enums.BookingStatus;
import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.exception.SeatUnavailableException;
import com.cinebook.infra.TransactionLog;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests for BookingService.
 *
 * <p>Uses in-memory repositories so all threads share the same object graph.
 * The BookingManager singleton provides per-showtime ReentrantLock instances.
 */
class BookingServiceConcurrencyTest {

    private BookingService bookingService;
    private ShowtimeRepository showtimeRepository;
    private BookingRepository bookingRepository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Clear singleton lock state between tests
        BookingManager.getInstance().clearLocks();

        showtimeRepository = new InMemoryShowtimeRepository();
        bookingRepository = new InMemoryBookingRepository();
        TransactionLog wal = new TransactionLog(tempDir.resolve("wal.log"));
        PricingService pricingService = new PricingService();

        bookingService = new BookingService(
                showtimeRepository, bookingRepository, pricingService, wal);
    }

    /**
     * TEST 1: 100 threads racing for seat A1.
     *
     * <p>Exactly 1 thread must succeed, 99 must receive SeatUnavailableException.
     * Final seat status must be HELD (since we're only calling holdSeats).
     */
    @Test
    void hundredThreads_singleSeat_exactlyOneWins() throws Exception {
        // Create showtime with a 10-seat row
        Hall hall = new Hall("H1", 1, 10, Set.of());
        Showtime st = new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 14, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid());
        showtimeRepository.save(st);

        int threadCount = 100;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            String userId = "U" + String.format("%03d", i);
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await(); // All threads start at the same time
                    bookingService.holdSeats("ST001", userId, List.of("A1"));
                    successCount.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    // Unexpected exception — still count as failure
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(); // Wait for all threads to be ready
        go.countDown(); // Release all threads at once
        assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete within 30s");

        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly 1 thread should succeed");
        assertEquals(99, failCount.get(), "Exactly 99 threads should fail");

        // Verify final seat state
        Showtime result = showtimeRepository.findById("ST001").get();
        Seat seatA1 = result.findSeat("A1").get();
        assertEquals(SeatStatus.HELD, seatA1.getStatus(), "Seat A1 should be HELD");
        assertNotNull(seatA1.getHeldBy(), "Seat A1 should be held by a user");
    }

    /**
     * TEST 2: 50 threads each booking a different seat.
     *
     * <p>All 50 must succeed since there's no contention.
     */
    @Test
    void fiftyThreads_differentSeats_allSucceed() throws Exception {
        // Create showtime with 5 rows x 10 cols = 50 seats
        Hall hall = new Hall("H1", 5, 10, Set.of());
        Showtime st = new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 14, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid());
        showtimeRepository.save(st);

        int threadCount = 50;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Generate 50 unique seat codes: A1..A10, B1..B10, ..., E1..E10
        for (int i = 0; i < threadCount; i++) {
            int row = i / 10;
            int col = i % 10;
            String seatCode = String.valueOf((char) ('A' + row)) + (col + 1);
            String userId = "U" + String.format("%03d", i);

            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    bookingService.holdSeats("ST001", userId, List.of(seatCode));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        go.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete within 30s");

        executor.shutdown();

        assertEquals(50, successCount.get(), "All 50 threads should succeed");
        assertEquals(0, failCount.get(), "No threads should fail");

        // Verify all 50 seats are HELD
        Showtime result = showtimeRepository.findById("ST001").get();
        int heldCount = 0;
        for (List<Seat> row : result.getSeats()) {
            for (Seat seat : row) {
                if (seat.getStatus() == SeatStatus.HELD) {
                    heldCount++;
                }
            }
        }
        assertEquals(50, heldCount, "All 50 seats should be HELD");
    }

    /**
     * TEST 3: 100 threads racing to hold, then the winner confirms.
     *
     * <p>End-to-end: hold + confirm. Seat must end up BOOKED.
     */
    @Test
    void hundredThreads_holdAndConfirm_seatEndsBooked() throws Exception {
        Hall hall = new Hall("H1", 1, 10, Set.of());
        Showtime st = new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 14, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid());
        showtimeRepository.save(st);

        int threadCount = 100;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger holdSuccess = new AtomicInteger(0);
        AtomicInteger confirmSuccess = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            String userId = "U" + String.format("%03d", i);
            executor.submit(() -> {
                try {
                    ready.countDown();
                    go.await();
                    HoldToken token = bookingService.holdSeats("ST001", userId, List.of("A1"));
                    holdSuccess.incrementAndGet();
                    // The one thread that got the hold also confirms
                    bookingService.confirmBooking(token, null);
                    confirmSuccess.incrementAndGet();
                } catch (Exception e) {
                    // Expected for 99 threads
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        go.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));

        executor.shutdown();

        assertEquals(1, holdSuccess.get(), "Exactly 1 thread should hold");
        assertEquals(1, confirmSuccess.get(), "Exactly 1 thread should confirm");

        // Seat must be BOOKED
        Showtime result = showtimeRepository.findById("ST001").get();
        assertEquals(SeatStatus.BOOKED, result.findSeat("A1").get().getStatus());

        // Exactly 1 booking in the repository
        assertEquals(1, bookingRepository.findAll().size());
        assertEquals(BookingStatus.CONFIRMED, bookingRepository.findAll().get(0).getStatus());
    }
}
