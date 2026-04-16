package com.cinebook.cli;

import com.cinebook.domain.Hall;
import com.cinebook.domain.Showtime;
import com.cinebook.domain.Movie;
import com.cinebook.domain.User;
import com.cinebook.domain.enums.Role;
import com.cinebook.infra.*;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.MovieRepository;
import com.cinebook.repository.ShowtimeRepository;
import com.cinebook.repository.UserRepository;
import com.cinebook.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MainMenuSmokeTest {

    @TempDir
    Path tempDir;

    /** Feed scripted input, capture output, assert key strings. */
    private String runWithInput(String scriptedInput) {
        JsonFileAdapter jsonAdapter = new JsonFileAdapter();
        CsvAppendAdapter csvAdapter = new CsvAppendAdapter();

        MovieRepository movieRepo = new JsonMovieRepository(
                tempDir.resolve("movies.json"), jsonAdapter);
        ShowtimeRepository showtimeRepo = new JsonShowtimeRepository(
                tempDir.resolve("showtimes.json"), jsonAdapter);
        UserRepository userRepo = new JsonUserRepository(
                tempDir.resolve("users.json"), jsonAdapter);
        BookingRepository bookingRepo = new CsvBookingRepository(
                tempDir.resolve("bookings.csv"), csvAdapter);

        // Seed data
        movieRepo.save(new Movie("M001", "Dune Part Two", 166, "PG-13", "Sci-Fi"));
        Hall hall = new Hall("H1", 3, 4, Set.of(0));
        showtimeRepo.save(new Showtime("ST001", "M001", "H1",
                LocalDateTime.of(2026, 4, 20, 14, 0),
                new BigDecimal("80.00"), hall.generateSeatGrid()));

        PasswordHasher hasher = new PasswordHasher();
        String salt = hasher.generateSalt();
        String hash = hasher.hash("pass1234", salt);
        userRepo.save(new User("U001", "testuser", hash, salt, Role.CUSTOMER));

        TransactionLog wal = new TransactionLog(tempDir.resolve("wal.log"));
        AuthService authService = new AuthService(userRepo, hasher);
        PricingService pricingService = new PricingService();
        BookingService bookingService = new BookingService(
                showtimeRepo, bookingRepo, pricingService, wal);
        ReportService reportService = new ReportService(showtimeRepo, bookingRepo);

        ByteArrayInputStream in = new ByteArrayInputStream(scriptedInput.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        PrintStream oldOut = System.out;
        System.setOut(printStream);

        Scanner scanner = new Scanner(in);
        InputReader input = new InputReader(scanner);
        AuthController authController = new AuthController(authService, input);
        BookingController bookingController = new BookingController(
                bookingService, authService, movieRepo, showtimeRepo, input);
        AdminController adminController = new AdminController(
                movieRepo, showtimeRepo, reportService, input);
        MainMenu mainMenu = new MainMenu(
                authService, authController, bookingController, adminController, input);

        mainMenu.run();
        System.setOut(oldOut);
        return out.toString();
    }

    @Test
    void mainMenu_showsAndQuits() {
        String output = runWithInput("Q\n");
        assertTrue(output.contains("CineBook  v1.0"));
        assertTrue(output.contains("Browse Showtimes"));
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    void browseShowtimes_listsMovies() {
        String output = runWithInput("1\nB\nQ\n");
        assertTrue(output.contains("Dune Part Two"));
        assertTrue(output.contains("Showtimes"));
    }

    @Test
    void loginAndBrowse() {
        // Login, browse, select showtime 1, go back, quit
        String output = runWithInput("3\n1\ntestuser\npass1234\n1\n1\nB\nQ\n");
        assertTrue(output.contains("Welcome back, testuser"));
        assertTrue(output.contains("SCREEN THIS WAY"));
    }

    @Test
    void loginAndBook() {
        // Login -> Browse -> Select showtime 1 -> Select seat C1 -> No discount -> Confirm -> Quit
        String output = runWithInput(
                "3\n1\ntestuser\npass1234\n1\n1\nC1\n\nY\nQ\n");
        assertTrue(output.contains("Booking confirmed"));
    }

    @Test
    void notLoggedIn_cannotBook() {
        // Browse -> Select showtime -> Try to select seat
        String output = runWithInput("1\n1\nC1\nQ\n");
        assertTrue(output.contains("must be logged in"));
    }

    @Test
    void viewBookings_whenEmpty() {
        String output = runWithInput(
                "3\n1\ntestuser\npass1234\n2\nQ\n");
        assertTrue(output.contains("No bookings found"));
    }
}
