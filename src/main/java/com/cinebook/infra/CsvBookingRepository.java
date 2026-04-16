package com.cinebook.infra;

import com.cinebook.domain.Booking;
import com.cinebook.domain.enums.BookingStatus;
import com.cinebook.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * CSV file-backed implementation of {@link BookingRepository}.
 *
 * <p>Bookings are append-only: each save appends a new row. The latest row
 * for a given bookingId (by version) is the authoritative state.
 */
public class CsvBookingRepository implements BookingRepository {

    private static final Logger log = LoggerFactory.getLogger(CsvBookingRepository.class);
    private static final String HEADER =
            "bookingId,userId,showtimeId,seats,totalPrice,status,createdAt,version";

    private final Path file;
    private final CsvAppendAdapter adapter;

    public CsvBookingRepository(Path file, CsvAppendAdapter adapter) {
        this.file = file;
        this.adapter = adapter;
    }

    @Override
    public Optional<Booking> findById(String id) {
        return loadAll().stream()
                .filter(b -> b.getBookingId().equals(id))
                .reduce((a, b) -> b.getVersion() >= a.getVersion() ? b : a);
    }

    @Override
    public List<Booking> findAll() {
        return latestVersions(loadAll());
    }

    @Override
    public List<Booking> findByUserId(String userId) {
        return latestVersions(loadAll()).stream()
                .filter(b -> b.getUserId().equals(userId))
                .toList();
    }

    @Override
    public void save(Booking booking) {
        String seats = "\"" + String.join(";", booking.getSeats()) + "\"";
        String row = String.join(",",
                booking.getBookingId(),
                booking.getUserId(),
                booking.getShowtimeId(),
                seats,
                booking.getTotalPrice().toPlainString(),
                booking.getStatus().name(),
                booking.getCreatedAt().toString(),
                String.valueOf(booking.getVersion()));
        try {
            adapter.append(file, HEADER, row);
        } catch (IOException e) {
            log.error("Failed to append booking to {}", file, e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void deleteById(String id) {
        // Append-only: deletion is not supported.
        // A booking is cancelled/refunded via status change, not deletion.
        throw new UnsupportedOperationException(
                "Append-only CSV does not support deletion. Cancel the booking instead.");
    }

    private List<Booking> loadAll() {
        try {
            List<String[]> rows = adapter.readAll(file);
            List<Booking> bookings = new ArrayList<>();
            for (String[] fields : rows) {
                if (fields.length < 8) continue;
                bookings.add(fromCsvFields(fields));
            }
            return bookings;
        } catch (IOException e) {
            log.error("Failed to load bookings from {}", file, e);
            throw new UncheckedIOException(e);
        }
    }

    /** Keep only the highest-version row for each bookingId. */
    private List<Booking> latestVersions(List<Booking> all) {
        java.util.Map<String, Booking> latest = new java.util.LinkedHashMap<>();
        for (Booking b : all) {
            latest.merge(b.getBookingId(), b,
                    (existing, incoming) ->
                            incoming.getVersion() >= existing.getVersion() ? incoming : existing);
        }
        return new ArrayList<>(latest.values());
    }

    private static Booking fromCsvFields(String[] f) {
        String bookingId = f[0].trim();
        String userId = f[1].trim();
        String showtimeId = f[2].trim();
        List<String> seats = Arrays.asList(f[3].trim().split(";"));
        BigDecimal totalPrice = new BigDecimal(f[4].trim());
        BookingStatus status = BookingStatus.valueOf(f[5].trim());
        Instant createdAt = Instant.parse(f[6].trim());
        int version = Integer.parseInt(f[7].trim());

        return new Booking(bookingId, userId, showtimeId, seats,
                totalPrice, status, createdAt, version);
    }
}
