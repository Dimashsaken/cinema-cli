package com.cinebook.domain;

import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.domain.enums.SeatTier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** A cinema hall with a fixed grid of seats and designated VIP rows. */
public class Hall {

    private String hallId;
    private int rows;
    private int cols;
    private Set<Integer> vipRows;

    public Hall() {}

    public Hall(String hallId, int rows, int cols, Set<Integer> vipRows) {
        this.hallId = hallId;
        this.rows = rows;
        this.cols = cols;
        this.vipRows = vipRows;
    }

    /**
     * Generate a fresh seat grid for this hall. All seats start as AVAILABLE.
     * Rows in {@code vipRows} get tier VIP, others get REGULAR.
     */
    public List<List<Seat>> generateSeatGrid() {
        List<List<Seat>> grid = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            List<Seat> row = new ArrayList<>();
            SeatTier tier = (vipRows != null && vipRows.contains(r))
                    ? SeatTier.VIP : SeatTier.REGULAR;
            for (int c = 0; c < cols; c++) {
                row.add(new Seat(r, c, tier, SeatStatus.AVAILABLE));
            }
            grid.add(row);
        }
        return grid;
    }

    public String getHallId() { return hallId; }
    public void setHallId(String hallId) { this.hallId = hallId; }

    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }

    public int getCols() { return cols; }
    public void setCols(int cols) { this.cols = cols; }

    public Set<Integer> getVipRows() { return vipRows; }
    public void setVipRows(Set<Integer> vipRows) { this.vipRows = vipRows; }
}
