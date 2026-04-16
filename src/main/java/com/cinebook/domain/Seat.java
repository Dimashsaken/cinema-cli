package com.cinebook.domain;

import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.domain.enums.SeatTier;
import java.time.Instant;

/** A single seat within a showtime's seat grid. */
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
    public String getCode() {
        return String.valueOf((char) ('A' + row)) + (col + 1);
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
