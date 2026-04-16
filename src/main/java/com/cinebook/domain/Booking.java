package com.cinebook.domain;

import com.cinebook.domain.enums.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A booking of one or more seats for a showtime.
 *
 * <p>The {@code version} field supports optimistic concurrency control —
 * it increments on every state change.
 */
public class Booking {

    private String bookingId;
    private String userId;
    private String showtimeId;
    private List<String> seats;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private Instant createdAt;
    private int version;

    public Booking() {}

    public Booking(String bookingId, String userId, String showtimeId,
                   List<String> seats, BigDecimal totalPrice, BookingStatus status,
                   Instant createdAt, int version) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.showtimeId = showtimeId;
        this.seats = seats;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
        this.version = version;
    }

    /**
     * Transition PENDING -> CONFIRMED.
     *
     * @throws IllegalStateException if not currently PENDING
     */
    public void confirm() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm booking " + bookingId + ": status is " + status);
        }
        this.status = BookingStatus.CONFIRMED;
        this.version++;
    }

    /**
     * Transition PENDING or CONFIRMED -> CANCELLED.
     *
     * @throws IllegalStateException if already CANCELLED or REFUNDED
     */
    public void cancel() {
        if (status == BookingStatus.CANCELLED || status == BookingStatus.REFUNDED) {
            throw new IllegalStateException(
                    "Cannot cancel booking " + bookingId + ": status is " + status);
        }
        this.status = BookingStatus.CANCELLED;
        this.version++;
    }

    /**
     * Transition CONFIRMED -> REFUNDED (inverse of confirm).
     *
     * @throws IllegalStateException if not currently CONFIRMED
     */
    public void refund() {
        if (status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Cannot refund booking " + bookingId + ": status is " + status);
        }
        this.status = BookingStatus.REFUNDED;
        this.version++;
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getShowtimeId() { return showtimeId; }
    public void setShowtimeId(String showtimeId) { this.showtimeId = showtimeId; }

    public List<String> getSeats() { return seats; }
    public void setSeats(List<String> seats) { this.seats = seats; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
