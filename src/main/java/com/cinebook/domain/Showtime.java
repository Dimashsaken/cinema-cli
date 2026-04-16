package com.cinebook.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/** A screening of a movie in a hall at a specific time, with an inline seat grid. */
public class Showtime {

    private String showtimeId;
    private String movieId;
    private String hallId;
    private LocalDateTime startsAt;
    private BigDecimal basePrice;
    private List<List<Seat>> seats;

    @JsonIgnore
    private final transient ReentrantLock lock = new ReentrantLock();

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
    public ReentrantLock getLock() { return lock; }

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
