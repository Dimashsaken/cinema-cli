package com.cinebook.cli;

import com.cinebook.domain.Hall;
import com.cinebook.domain.Movie;
import com.cinebook.domain.Showtime;
import com.cinebook.repository.MovieRepository;
import com.cinebook.repository.ShowtimeRepository;
import com.cinebook.service.ReportService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** CLI controller for admin operations. */
public class AdminController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ReportService reportService;
    private final InputReader input;

    public AdminController(MovieRepository movieRepository,
                           ShowtimeRepository showtimeRepository,
                           ReportService reportService,
                           InputReader input) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
        this.reportService = reportService;
        this.input = input;
    }

    /** Admin menu loop. */
    public void adminMenu() {
        while (true) {
            System.out.println();
            System.out.println("================================");
            System.out.println("       Admin Panel");
            System.out.println("================================");
            System.out.println(" [1] Add Movie");
            System.out.println(" [2] Add Showtime");
            System.out.println(" [3] Occupancy Report");
            System.out.println(" [4] Revenue Report");
            System.out.println(" [5] Export Bookings CSV");
            System.out.println(" [B] Back");
            System.out.println("--------------------------------");

            String choice = input.readLine(" > ");
            switch (choice.toUpperCase()) {
                case "1" -> addMovie();
                case "2" -> addShowtime();
                case "3" -> occupancyReport();
                case "4" -> revenueReport();
                case "5" -> exportCsv();
                case "B" -> { return; }
                default -> System.out.println(" Invalid choice.");
            }
        }
    }

    private void addMovie() {
        String title = input.readNonBlank(" Title: ");
        String durationStr = input.readNonBlank(" Duration (min): ");
        String rating = input.readNonBlank(" Rating (e.g. PG-13): ");
        String genre = input.readNonBlank(" Genre: ");

        try {
            int duration = Integer.parseInt(durationStr);
            String movieId = "M" + UUID.randomUUID().toString().substring(0, 3).toUpperCase();
            Movie movie = new Movie(movieId, title, duration, rating, genre);
            movieRepository.save(movie);
            System.out.println(" Movie added: " + movieId);
        } catch (NumberFormatException e) {
            System.out.println(" Invalid duration.");
        }
    }

    private void addShowtime() {
        // List movies
        List<Movie> movies = movieRepository.findAll();
        if (movies.isEmpty()) {
            System.out.println(" No movies. Add a movie first.");
            return;
        }
        System.out.println(" Movies:");
        for (int i = 0; i < movies.size(); i++) {
            System.out.printf("  [%d] %s%n", i + 1, movies.get(i).getTitle());
        }
        String movieChoice = input.readLine(" Select movie: ");
        int movieIdx;
        try {
            movieIdx = Integer.parseInt(movieChoice) - 1;
            if (movieIdx < 0 || movieIdx >= movies.size()) {
                System.out.println(" Invalid choice.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println(" Invalid choice.");
            return;
        }

        String hallId = input.readNonBlank(" Hall ID (e.g. H1): ");
        String rowsStr = input.readNonBlank(" Rows: ");
        String colsStr = input.readNonBlank(" Cols: ");
        String vipStr = input.readLine(" VIP rows (comma-separated, e.g. 0,1): ");
        String dateStr = input.readNonBlank(" Start time (yyyy-MM-dd HH:mm): ");
        String priceStr = input.readNonBlank(" Base price: ");

        try {
            int rows = Integer.parseInt(rowsStr);
            int cols = Integer.parseInt(colsStr);
            Set<Integer> vipRows = vipStr.isBlank() ? Set.of() :
                    Stream.of(vipStr.split(","))
                            .map(String::trim)
                            .map(Integer::parseInt)
                            .collect(Collectors.toSet());
            LocalDateTime startsAt = LocalDateTime.parse(dateStr, DT_FMT);
            BigDecimal price = new BigDecimal(priceStr);

            Hall hall = new Hall(hallId, rows, cols, vipRows);
            String stId = "ST" + UUID.randomUUID().toString().substring(0, 3).toUpperCase();
            Showtime st = new Showtime(stId, movies.get(movieIdx).getMovieId(),
                    hallId, startsAt, price, hall.generateSeatGrid());
            showtimeRepository.save(st);
            System.out.println(" Showtime added: " + stId);
        } catch (NumberFormatException | DateTimeParseException e) {
            System.out.println(" Invalid input: " + e.getMessage());
        }
    }

    private void occupancyReport() {
        List<Showtime> showtimes = showtimeRepository.findAll();
        if (showtimes.isEmpty()) {
            System.out.println(" No showtimes.");
            return;
        }
        for (int i = 0; i < showtimes.size(); i++) {
            System.out.printf(" [%d] %s%n", i + 1, showtimes.get(i).getShowtimeId());
        }
        String choice = input.readLine(" Select showtime: ");
        try {
            int idx = Integer.parseInt(choice) - 1;
            if (idx >= 0 && idx < showtimes.size()) {
                System.out.println();
                System.out.println(reportService.occupancyReport(
                        showtimes.get(idx).getShowtimeId()));
            }
        } catch (NumberFormatException e) {
            System.out.println(" Invalid choice.");
        }
    }

    private void revenueReport() {
        System.out.println();
        System.out.println(reportService.revenueReport());
    }

    private void exportCsv() {
        System.out.println();
        System.out.println(reportService.exportBookingsCsv());
    }
}
