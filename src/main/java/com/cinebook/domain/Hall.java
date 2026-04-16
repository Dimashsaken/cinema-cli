package com.cinebook.domain;

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

    public String getHallId() { return hallId; }
    public void setHallId(String hallId) { this.hallId = hallId; }

    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }

    public int getCols() { return cols; }
    public void setCols(int cols) { this.cols = cols; }

    public Set<Integer> getVipRows() { return vipRows; }
    public void setVipRows(Set<Integer> vipRows) { this.vipRows = vipRows; }
}
