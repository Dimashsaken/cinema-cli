package com.cinebook.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A screening of a movie in a hall at a specific time, with an inline seat grid.
 *
 * <p>The transient ReentrantLock is not serialized — it is re-created on
 * deserialization and shared via BookingManager's ConcurrentHashMap.
 */
public class Showtime {

    private String showtimeId;
    private String movieId;
    private String hallId;
    private LocalDateTime startsAt;
    private BigDecimal basePrice;
    private List<List<Seat>> seats;

    @JsonIgnore
    private transient ReentrantLock lock = new ReentrantLock();

    public Showtime() {}

    public Showtime(String showtimeId, String movieId, String hallId,
                    LocalDateTime startsAt, BigDecimal basePrice, List<List<Seat>> seats) {
        this.showtimeId = showtimeId;
        this.movieId = movieId;
        this.hallId = hallId;
        this.startsAt = startsAt;
        this.basePrice = basePrice;
        this.seats = seats;
    }

    @JsonIgnore
    public ReentrantLock getLock() {
        if (lock == null) {
            lock = new ReentrantLock();
        }
        return lock;
    }

    /**
     * Find a seat by its code (e.g. "A1", "C5").
     *
     * @return the seat, or empty if the code is out of bounds
     */
    public Optional<Seat> findSeat(String seatCode) {
        int[] parsed = Seat.parseSeatCode(seatCode);
        int row = parsed[0];
        int col = parsed[1];
        if (row < 0 || row >= seats.size()) {
            return Optional.empty();
        }
        List<Seat> rowSeats = seats.get(row);
        if (col < 0 || col >= rowSeats.size()) {
            return Optional.empty();
        }
        return Optional.of(rowSeats.get(col));
    }

    /** Returns the total number of rows in this showtime's seat grid. */
    @JsonIgnore
    public int getRowCount() {
        return seats == null ? 0 : seats.size();
    }

    /** Returns the number of columns (seats per row). Assumes uniform rows. */
    @JsonIgnore
    public int getColCount() {
        if (seats == null || seats.isEmpty()) return 0;
        return seats.get(0).size();
    }

    public String getShowtimeId() { return showtimeId; }
    public void setShowtimeId(String showtimeId) { this.showtimeId = showtimeId; }

    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public String getHallId() { return hallId; }
    public void setHallId(String hallId) { this.hallId = hallId; }

    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public List<List<Seat>> getSeats() { return seats; }
    public void setSeats(List<List<Seat>> seats) { this.seats = seats; }
}
