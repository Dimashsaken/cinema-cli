package com.cinebook.cli;

import com.cinebook.domain.*;
import com.cinebook.domain.enums.BookingStatus;
import com.cinebook.exception.CineBookException;
import com.cinebook.repository.MovieRepository;
import com.cinebook.repository.ShowtimeRepository;
import com.cinebook.service.AuthService;
import com.cinebook.service.BookingService;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/** CLI controller for browsing showtimes, booking seats, and viewing booking history. */
public class BookingController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookingService bookingService;
    private final AuthService authService;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final AsciiSeatRenderer seatRenderer;
    private final InputReader input;

    public BookingController(BookingService bookingService, AuthService authService,
                             MovieRepository movieRepository,
                             ShowtimeRepository showtimeRepository,
                             InputReader input) {
        this.bookingService = bookingService;
        this.authService = authService;
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRenderer = new AsciiSeatRenderer();
        this.input = input;
    }

    /** Browse showtimes and optionally book. */
    public void browseShowtimes() {
        List<Showtime> showtimes = showtimeRepository.findAll();
        if (showtimes.isEmpty()) {
            System.out.println(" No showtimes available.");
            return;
        }

        System.out.println("================================");
        System.out.println("       Showtimes");
        System.out.println("================================");
        for (int i = 0; i < showtimes.size(); i++) {
            Showtime st = showtimes.get(i);
            String movieTitle = movieRepository.findById(st.getMovieId())
                    .map(Movie::getTitle).orElse("Unknown");
            System.out.printf(" [%d] %-20s | %s | Hall %s | $%s%n",
                    i + 1, movieTitle,
                    st.getStartsAt().format(DT_FMT),
                    st.getHallId(),
                    st.getBasePrice().toPlainString());
        }
        System.out.println(" [B] Back");
        System.out.println("--------------------------------");

        String choice = input.readLine(" > ");
        if ("B".equalsIgnoreCase(choice)) return;

        try {
            int idx = Integer.parseInt(choice) - 1;
            if (idx >= 0 && idx < showtimes.size()) {
                showSeatMap(showtimes.get(idx));
            } else {
                System.out.println(" Invalid choice.");
            }
        } catch (NumberFormatException e) {
            System.out.println(" Invalid choice.");
        }
    }

    /** Show seat map and allow booking. */
    private void showSeatMap(Showtime showtime) {
        // Re-fetch for latest state
        showtime = showtimeRepository.findById(showtime.getShowtimeId()).orElse(showtime);

        String movieTitle = movieRepository.findById(showtime.getMovieId())
                .map(Movie::getTitle).orElse("Unknown");

        System.out.println();
        System.out.println(" " + movieTitle + " | " + showtime.getStartsAt().format(DT_FMT));
        System.out.println();
        System.out.println(seatRenderer.render(showtime));
        System.out.println(" Select seats (e.g. D6 D7) or [B] back:");

        String seatInput = input.readLine(" > ");
        if ("B".equalsIgnoreCase(seatInput)) return;

        if (!authService.isLoggedIn()) {
            System.out.println(" You must be logged in to book seats.");
            return;
        }

        List<String> seatCodes = Arrays.asList(seatInput.toUpperCase().split("\\s+"));
        try {
            User user = authService.getCurrentUser();
            HoldToken token = bookingService.holdSeats(
                    showtime.getShowtimeId(), user.getUserId(), seatCodes);

            System.out.println(" Seats held for 5 minutes.");

            // Ask for discount code
            String discountCode = input.readLine(" Discount code (or Enter to skip): ");
            if (discountCode.isBlank()) discountCode = null;

            // Show summary
            Showtime st = showtimeRepository.findById(showtime.getShowtimeId()).orElse(showtime);
            showBookingSummary(st, token, movieTitle, discountCode);

            if (input.confirm(" Confirm?")) {
                Booking booking = bookingService.confirmBooking(token, discountCode);
                System.out.println(" Booking confirmed! Code: " + booking.getBookingId());
            } else {
                System.out.println(" Booking cancelled.");
                // Release the holds — the expiry scheduler will also clean up
            }
        } catch (CineBookException e) {
            System.out.println(" " + e.getMessage());
        }
    }

    private void showBookingSummary(Showtime showtime, HoldToken token,
                                    String movieTitle, String discountCode) {
        System.out.println("================================");
        System.out.println("       BOOKING SUMMARY");
        System.out.println("================================");
        System.out.println(" Movie   : " + movieTitle);
        System.out.println(" Hall    : " + showtime.getHallId());
        System.out.println(" Time    : " + showtime.getStartsAt().format(DT_FMT));
        System.out.println(" Seats   : " + String.join(", ", token.getSeats()));
        if (discountCode != null) {
            System.out.println(" Discount: " + discountCode);
        }
        System.out.println("--------------------------------");
    }

    /** Show booking history for current user. */
    public void myBookings() {
        if (!authService.isLoggedIn()) {
            System.out.println(" You must be logged in.");
            return;
        }

        List<Booking> bookings = bookingService.getUserBookings(
                authService.getCurrentUser().getUserId());

        if (bookings.isEmpty()) {
            System.out.println(" No bookings found.");
            return;
        }

        System.out.println("================================");
        System.out.println("       My Bookings");
        System.out.println("================================");
        for (int i = 0; i < bookings.size(); i++) {
            Booking b = bookings.get(i);
            String movieTitle = showtimeRepository.findById(b.getShowtimeId())
                    .flatMap(st -> movieRepository.findById(st.getMovieId()))
                    .map(Movie::getTitle).orElse("Unknown");
            System.out.printf(" [%d] %s | %s | Seats: %s | %s%n",
                    i + 1, b.getBookingId(), movieTitle,
                    String.join(",", b.getSeats()), b.getStatus());
        }
        System.out.println("--------------------------------");
        System.out.println(" [C] Cancel a booking");
        System.out.println(" [R] Refund a booking");
        System.out.println(" [B] Back");

        String choice = input.readLine(" > ");
        if ("C".equalsIgnoreCase(choice)) {
            cancelBookingPrompt(bookings);
        } else if ("R".equalsIgnoreCase(choice)) {
            refundBookingPrompt(bookings);
        }
    }

    private void cancelBookingPrompt(List<Booking> bookings) {
        String numStr = input.readLine(" Enter booking number to cancel: ");
        try {
            int idx = Integer.parseInt(numStr) - 1;
            if (idx >= 0 && idx < bookings.size()) {
                Booking b = bookings.get(idx);
                bookingService.cancelBooking(b.getBookingId(),
                        authService.getCurrentUser().getUserId());
                System.out.println(" Booking " + b.getBookingId() + " cancelled.");
            }
        } catch (Exception e) {
            System.out.println(" " + e.getMessage());
        }
    }

    private void refundBookingPrompt(List<Booking> bookings) {
        String numStr = input.readLine(" Enter booking number to refund: ");
        try {
            int idx = Integer.parseInt(numStr) - 1;
            if (idx >= 0 && idx < bookings.size()) {
                Booking b = bookings.get(idx);
                bookingService.refundBooking(b.getBookingId(),
                        authService.getCurrentUser().getUserId());
                System.out.println(" Booking " + b.getBookingId() + " refunded.");
            }
        } catch (Exception e) {
            System.out.println(" " + e.getMessage());
        }
    }
}
