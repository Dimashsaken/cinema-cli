package com.cinebook;

import com.cinebook.cli.*;
import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;
import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.infra.*;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.MovieRepository;
import com.cinebook.repository.ShowtimeRepository;
import com.cinebook.repository.UserRepository;
import com.cinebook.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * CineBook CLI entry point.
 *
 * <p>Wires all layers together, runs WAL crash recovery, starts the
 * hold-expiry scheduler, and launches the main menu loop.
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        log.info("CineBook v1.0 starting...");

        // Resolve data directory
        Path dataDir = resolveDataDir(args);

        // Infrastructure
        JsonFileAdapter jsonAdapter = new JsonFileAdapter();
        CsvAppendAdapter csvAdapter = new CsvAppendAdapter();
        PasswordHasher passwordHasher = new PasswordHasher();
        TransactionLog transactionLog = new TransactionLog(dataDir.resolve("wal.log"));

        // Repositories
        MovieRepository movieRepo = new JsonMovieRepository(
                dataDir.resolve("movies.json"), jsonAdapter);
        ShowtimeRepository showtimeRepo = new JsonShowtimeRepository(
                dataDir.resolve("showtimes.json"), jsonAdapter);
        UserRepository userRepo = new JsonUserRepository(
                dataDir.resolve("users.json"), jsonAdapter);
        BookingRepository bookingRepo = new CsvBookingRepository(
                dataDir.resolve("bookings.csv"), csvAdapter);

        // WAL crash recovery (Phase 12)
        recoverFromWal(transactionLog, showtimeRepo);

        // Services
        AuthService authService = new AuthService(userRepo, passwordHasher);
        PricingService pricingService = new PricingService();
        BookingService bookingService = new BookingService(
                showtimeRepo, bookingRepo, pricingService, transactionLog);
        ReportService reportService = new ReportService(showtimeRepo, bookingRepo);

        // Hold expiry scheduler (Phase 9)
        SeatStatusNotifier notifier = new SeatStatusNotifier();
        HoldExpiryScheduler holdScheduler = new HoldExpiryScheduler(showtimeRepo, notifier);
        holdScheduler.start();

        // CLI controllers
        Scanner scanner = new Scanner(System.in);
        InputReader input = new InputReader(scanner);
        AuthController authController = new AuthController(authService, input);
        BookingController bookingController = new BookingController(
                bookingService, authService, movieRepo, showtimeRepo, input);
        AdminController adminController = new AdminController(
                movieRepo, showtimeRepo, reportService, input);
        MainMenu mainMenu = new MainMenu(
                authService, authController, bookingController, adminController, input);

        // Run
        mainMenu.run();

        // Cleanup
        holdScheduler.stop();
        scanner.close();
        log.info("CineBook v1.0 shut down.");
    }

    /**
     * WAL crash recovery: find uncommitted HOLDs and release the seats.
     * If the app was killed mid-booking, held seats from incomplete
     * transactions are freed so they don't remain stuck.
     */
    private static void recoverFromWal(TransactionLog wal, ShowtimeRepository showtimeRepo) {
        try {
            List<String> uncommitted = wal.findUncommittedHolds();
            if (uncommitted.isEmpty()) {
                log.info("WAL recovery: no uncommitted holds found.");
                return;
            }
            log.warn("WAL recovery: found {} uncommitted holds, releasing seats...",
                    uncommitted.size());
            for (String entry : uncommitted) {
                // Format: <timestamp> HOLD <showtimeId> <userId> <seats>
                String[] parts = entry.split(" ");
                if (parts.length < 5) continue;
                String showtimeId = parts[2];
                String[] seatCodes = parts[4].split(",");

                showtimeRepo.findById(showtimeId).ifPresent(showtime -> {
                    for (String code : seatCodes) {
                        showtime.findSeat(code.trim()).ifPresent(seat -> {
                            if (seat.getStatus() == SeatStatus.HELD) {
                                seat.release();
                                log.info("WAL recovery: released seat {} on {}",
                                        code, showtimeId);
                            }
                        });
                    }
                    showtimeRepo.save(showtime);
                });

                wal.logRollback(showtimeId, List.of(seatCodes));
            }
            log.info("WAL recovery complete.");
        } catch (IOException e) {
            log.error("WAL recovery failed", e);
        }
    }

    private static Path resolveDataDir(String[] args) {
        if (args.length > 0) {
            return Paths.get(args[0]);
        }
        // Try relative data/ directory first, fallback to working directory
        Path dataDir = Paths.get("data");
        if (dataDir.toFile().isDirectory()) {
            return dataDir;
        }
        return Paths.get(".");
    }
}
