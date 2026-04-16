package com.cinebook.domain;

import com.cinebook.domain.enums.SeatTier;
import java.math.BigDecimal;

/** An individual ticket within a booking, one per seat. */
public class Ticket {

    private String seatCode;
    private SeatTier tier;
    private BigDecimal price;

    public Ticket() {}

    public Ticket(String seatCode, SeatTier tier, BigDecimal price) {
        this.seatCode = seatCode;
        this.tier = tier;
        this.price = price;
    }

    public String getSeatCode() { return seatCode; }
    public void setSeatCode(String seatCode) { this.seatCode = seatCode; }

    public SeatTier getTier() { return tier; }
    public void setTier(SeatTier tier) { this.tier = tier; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
