package com.cinebook.domain;

import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.domain.enums.SeatTier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * A single seat within a showtime's seat grid.
 *
 * <p>Seat state transitions follow a strict state machine:
 * AVAILABLE -> HELD -> BOOKED, or HELD -> AVAILABLE (release/expiry).
 * All transitions are enforced by domain methods — callers must not
 * set status directly.
 */
public class Seat {

    private int row;
    private int col;
    private SeatTier tier;
    private SeatStatus status;
    private String heldBy;
    private Instant holdExpiresAt;

    public Seat() {}

    public Seat(int row, int col, SeatTier tier, SeatStatus status) {
        this.row = row;
        this.col = col;
        this.tier = tier;
        this.status = status;
    }

    /** Returns the seat code, e.g. "A1", "B5". Row 0 = 'A', col 0 = 1. */
    @JsonIgnore
    public String getCode() {
        return String.valueOf((char) ('A' + row)) + (col + 1);
    }

    /**
     * Parses a seat code like "A1" or "C10" into a [row, col] pair.
     *
     * @return int array of [rowIndex, colIndex]
     * @throws IllegalArgumentException if the code is malformed
     */
    public static int[] parseSeatCode(String code) {
        if (code == null || code.length() < 2) {
            throw new IllegalArgumentException("Invalid seat code: " + code);
        }
        code = code.toUpperCase();
        char rowChar = code.charAt(0);
        if (rowChar < 'A' || rowChar > 'Z') {
            throw new IllegalArgumentException("Invalid row letter in seat code: " + code);
        }
        int rowIndex = rowChar - 'A';
        try {
            int colNumber = Integer.parseInt(code.substring(1));
            if (colNumber < 1) {
                throw new IllegalArgumentException("Column must be >= 1 in seat code: " + code);
            }
            return new int[]{rowIndex, colNumber - 1};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid column in seat code: " + code);
        }
    }

    /**
     * Transition AVAILABLE -> HELD.
     *
     * @param userId   the user placing the hold
     * @param expiresAt when the hold expires
     * @throws IllegalStateException if the seat is not AVAILABLE
     */
    public void hold(String userId, Instant expiresAt) {
        if (status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "Cannot hold seat " + getCode() + ": status is " + status);
        }
        this.status = SeatStatus.HELD;
        this.heldBy = userId;
        this.holdExpiresAt = expiresAt;
    }

    /**
     * Transition HELD -> BOOKED. Enforces that the caller matches heldBy
     * and the hold has not expired.
     *
     * @param userId the user confirming the booking
     * @throws IllegalStateException if the seat is not HELD, hold is expired,
     *                               or userId doesn't match
     */
    public void book(String userId) {
        if (status != SeatStatus.HELD) {
            throw new IllegalStateException(
                    "Cannot book seat " + getCode() + ": status is " + status);
        }
        if (!userId.equals(this.heldBy)) {
            throw new IllegalStateException(
                    "Cannot book seat " + getCode() + ": held by different user");
        }
        if (holdExpiresAt != null && Instant.now().isAfter(holdExpiresAt)) {
            throw new IllegalStateException(
                    "Cannot book seat " + getCode() + ": hold has expired");
        }
        this.status = SeatStatus.BOOKED;
        this.heldBy = null;
        this.holdExpiresAt = null;
    }

    /**
     * Release a held seat back to AVAILABLE.
     * Used on cancellation, refund, or hold expiry.
     *
     * @throws IllegalStateException if the seat is not HELD or BOOKED
     */
    public void release() {
        if (status != SeatStatus.HELD && status != SeatStatus.BOOKED) {
            throw new IllegalStateException(
                    "Cannot release seat " + getCode() + ": status is " + status);
        }
        this.status = SeatStatus.AVAILABLE;
        this.heldBy = null;
        this.holdExpiresAt = null;
    }

    /** Check if this hold has expired. Returns false if not held. */
    @JsonIgnore
    public boolean isHoldExpired() {
        return status == SeatStatus.HELD
                && holdExpiresAt != null
                && Instant.now().isAfter(holdExpiresAt);
    }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }

    public SeatTier getTier() { return tier; }
    public void setTier(SeatTier tier) { this.tier = tier; }

    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }

    public String getHeldBy() { return heldBy; }
    public void setHeldBy(String heldBy) { this.heldBy = heldBy; }

    public Instant getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(Instant holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }
}
