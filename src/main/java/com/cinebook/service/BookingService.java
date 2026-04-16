package com.cinebook.service;

import com.cinebook.domain.*;
import com.cinebook.domain.enums.BookingStatus;
import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.exception.HoldExpiredException;
import com.cinebook.exception.InvalidInputException;
import com.cinebook.exception.SeatUnavailableException;
import com.cinebook.infra.TransactionLog;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.ShowtimeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the booking workflow: hold seats, confirm booking, cancel, refund.
 *
 * <p>This is the single-threaded version. Concurrency (locking) is added
 * in Phase 8 via BookingManager.
 */
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final long HOLD_TTL_MINUTES = 5;

    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    private final PricingService pricingService;
    private final TransactionLog transactionLog;

    public BookingService(ShowtimeRepository showtimeRepository,
                          BookingRepository bookingRepository,
                          PricingService pricingService,
                          TransactionLog transactionLog) {
        this.showtimeRepository = showtimeRepository;
        this.bookingRepository = bookingRepository;
        this.pricingService = pricingService;
        this.transactionLog = transactionLog;
    }

    /**
     * Place a hold on one or more seats for a showtime.
     *
     * @param showtimeId the target showtime
     * @param userId     the user placing the hold
     * @param seatCodes  seat codes like "A1", "D6"
     * @return a HoldToken representing the held seats
     * @throws SeatUnavailableException if any seat is not AVAILABLE
     * @throws InvalidInputException    if seat codes are invalid or showtime not found
     */
    public HoldToken holdSeats(String showtimeId, String userId, List<String> seatCodes)
            throws SeatUnavailableException, InvalidInputException {

        if (seatCodes == null || seatCodes.isEmpty()) {
            throw new InvalidInputException("Must select at least one seat");
        }

        Showtime showtime = findShowtime(showtimeId);

        // Resolve all seats first, validating codes
        List<Seat> seats = resolveSeats(showtime, seatCodes);

        // Check all seats are available before modifying any
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatUnavailableException(
                        "Seat " + seat.getCode() + " is " + seat.getStatus());
            }
        }

        Instant expiresAt = Instant.now().plus(HOLD_TTL_MINUTES, ChronoUnit.MINUTES);

        // WAL: log the hold before mutating state
        logWal(() -> transactionLog.logHold(showtimeId, userId, seatCodes));

        // Transition all seats to HELD
        for (Seat seat : seats) {
            seat.hold(userId, expiresAt);
        }

        // Persist the showtime with updated seat states
        showtimeRepository.save(showtime);

        String tokenId = UUID.randomUUID().toString().substring(0, 8);
        HoldToken token = new HoldToken(tokenId, showtimeId, userId, seatCodes, expiresAt);
        log.info("Held {} seats for user {} on showtime {}", seatCodes.size(), userId, showtimeId);
        return token;
    }

    /**
     * Confirm a booking from a hold token.
     *
     * @param holdToken    the hold to confirm
     * @param discountCode optional discount code, may be null
     * @return the confirmed Booking
     * @throws HoldExpiredException  if the hold has expired
     * @throws SeatUnavailableException if seats are no longer held by this user
     */
    public Booking confirmBooking(HoldToken holdToken, String discountCode)
            throws HoldExpiredException, SeatUnavailableException {

        if (Instant.now().isAfter(holdToken.getExpiresAt())) {
            throw new HoldExpiredException("Hold has expired. Please select seats again.");
        }

        Showtime showtime = findShowtimeUnchecked(holdToken.getShowtimeId());
        List<Seat> seats = resolveSeatsUnchecked(showtime, holdToken.getSeats());

        // Verify all seats are still held by this user
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.HELD) {
                throw new SeatUnavailableException(
                        "Seat " + seat.getCode() + " is no longer held (status: " + seat.getStatus() + ")");
            }
            if (!holdToken.getUserId().equals(seat.getHeldBy())) {
                throw new SeatUnavailableException(
                        "Seat " + seat.getCode() + " is held by a different user");
            }
        }

        // Calculate total price
        BigDecimal totalPrice = pricingService.calculateTotal(showtime, seats, discountCode);

        // Transition HELD -> BOOKED
        for (Seat seat : seats) {
            seat.book(holdToken.getUserId());
        }

        // Create booking
        String bookingId = generateBookingId();
        Booking booking = new Booking(
                bookingId,
                holdToken.getUserId(),
                holdToken.getShowtimeId(),
                holdToken.getSeats(),
                totalPrice,
                BookingStatus.PENDING,
                Instant.now(),
                1
        );
        booking.confirm();

        // Persist
        showtimeRepository.save(showtime);
        bookingRepository.save(booking);

        // WAL: log commit
        logWal(() -> transactionLog.logCommit(bookingId));

        log.info("Confirmed booking {} for user {}", bookingId, holdToken.getUserId());
        return booking;
    }

    /**
     * Cancel a booking and release its seats.
     *
     * @param bookingId the booking to cancel
     * @param userId    the user requesting cancellation (must own the booking)
     * @return the cancelled Booking
     * @throws InvalidInputException if booking not found or not owned by user
     */
    public Booking cancelBooking(String bookingId, String userId) throws InvalidInputException {
        Booking booking = findBooking(bookingId);
        if (!booking.getUserId().equals(userId)) {
            throw new InvalidInputException("You can only cancel your own bookings");
        }

        booking.cancel();
        releaseSeats(booking);
        bookingRepository.save(booking);

        log.info("Cancelled booking {}", bookingId);
        return booking;
    }

    /**
     * Refund a confirmed booking and release its seats.
     *
     * @param bookingId the booking to refund
     * @param userId    the user requesting refund (must own the booking)
     * @return the refunded Booking
     * @throws InvalidInputException if booking not found or not owned by user
     */
    public Booking refundBooking(String bookingId, String userId) throws InvalidInputException {
        Booking booking = findBooking(bookingId);
        if (!booking.getUserId().equals(userId)) {
            throw new InvalidInputException("You can only refund your own bookings");
        }

        booking.refund();
        releaseSeats(booking);
        bookingRepository.save(booking);

        log.info("Refunded booking {}", bookingId);
        return booking;
    }

    /** Get all bookings for a user. */
    public List<Booking> getUserBookings(String userId) {
        return bookingRepository.findByUserId(userId);
    }

    /** Get a specific booking by ID. */
    public Optional<Booking> getBooking(String bookingId) {
        return bookingRepository.findById(bookingId);
    }

    // --- internal helpers ---

    private Showtime findShowtime(String showtimeId) throws InvalidInputException {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new InvalidInputException("Showtime not found: " + showtimeId));
    }

    private Showtime findShowtimeUnchecked(String showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new IllegalStateException("Showtime disappeared: " + showtimeId));
    }

    private Booking findBooking(String bookingId) throws InvalidInputException {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InvalidInputException("Booking not found: " + bookingId));
    }

    private List<Seat> resolveSeats(Showtime showtime, List<String> seatCodes)
            throws InvalidInputException {
        List<Seat> seats = new ArrayList<>();
        for (String code : seatCodes) {
            try {
                Seat seat = showtime.findSeat(code)
                        .orElseThrow(() -> new InvalidInputException(
                                "Seat " + code + " does not exist in this hall"));
                seats.add(seat);
            } catch (IllegalArgumentException e) {
                throw new InvalidInputException("Invalid seat code: " + code);
            }
        }
        return seats;
    }

    private List<Seat> resolveSeatsUnchecked(Showtime showtime, List<String> seatCodes) {
        List<Seat> seats = new ArrayList<>();
        for (String code : seatCodes) {
            seats.add(showtime.findSeat(code)
                    .orElseThrow(() -> new IllegalStateException("Seat disappeared: " + code)));
        }
        return seats;
    }

    private void releaseSeats(Booking booking) {
        Optional<Showtime> opt = showtimeRepository.findById(booking.getShowtimeId());
        if (opt.isEmpty()) return;

        Showtime showtime = opt.get();
        for (String code : booking.getSeats()) {
            showtime.findSeat(code).ifPresent(seat -> {
                if (seat.getStatus() == SeatStatus.BOOKED || seat.getStatus() == SeatStatus.HELD) {
                    seat.release();
                }
            });
        }
        showtimeRepository.save(showtime);
    }

    private String generateBookingId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "BK-" + date + "-" + suffix;
    }

    private void logWal(WalAction action) {
        try {
            action.run();
        } catch (IOException e) {
            log.error("Failed to write WAL entry", e);
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    private interface WalAction {
        void run() throws IOException;
    }
}
