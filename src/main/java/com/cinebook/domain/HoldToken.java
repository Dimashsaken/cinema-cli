package com.cinebook.domain;

import java.time.Instant;
import java.util.List;

/** Temporary hold on seats, expires after a configured TTL. */
public class HoldToken {

    private String tokenId;
    private String showtimeId;
    private String userId;
    private List<String> seats;
    private Instant expiresAt;

    public HoldToken() {}

    public HoldToken(String tokenId, String showtimeId, String userId,
                     List<String> seats, Instant expiresAt) {
        this.tokenId = tokenId;
        this.showtimeId = showtimeId;
        this.userId = userId;
        this.seats = seats;
        this.expiresAt = expiresAt;
    }

    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public String getShowtimeId() { return showtimeId; }
    public void setShowtimeId(String showtimeId) { this.showtimeId = showtimeId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<String> getSeats() { return seats; }
    public void setSeats(List<String> seats) { this.seats = seats; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
