package com.cinebook.cli;

import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;
import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.domain.enums.SeatTier;

import java.util.List;

/** Renders a showtime's seat grid as ASCII art for the terminal. */
public class AsciiSeatRenderer {

    /**
     * Render the seat map for a showtime.
     * Legend: [ ] available  [X] booked  [H] held  [V] VIP available
     */
    public String render(Showtime showtime) {
        StringBuilder sb = new StringBuilder();
        sb.append("        SCREEN THIS WAY\n");
        sb.append("        ");
        for (int i = 0; i < showtime.getColCount(); i++) {
            sb.append("---");
        }
        sb.append("\n");

        // Column headers
        sb.append("    ");
        for (int c = 0; c < showtime.getColCount(); c++) {
            sb.append(String.format("%3d", c + 1));
        }
        sb.append("\n");

        // Seat rows
        for (int r = 0; r < showtime.getRowCount(); r++) {
            char rowLetter = (char) ('A' + r);
            sb.append(String.format(" %c  ", rowLetter));
            List<Seat> row = showtime.getSeats().get(r);
            for (Seat seat : row) {
                sb.append(renderSeat(seat));
            }
            sb.append("\n");
        }

        sb.append("\n");
        sb.append(" Legend: [ ] available  [X] booked  [H] held  [V] VIP available\n");
        return sb.toString();
    }

    private String renderSeat(Seat seat) {
        if (seat.getStatus() == SeatStatus.BOOKED) return "[X]";
        if (seat.getStatus() == SeatStatus.HELD) return "[H]";
        if (seat.getTier() == SeatTier.VIP) return "[V]";
        return "[ ]";
    }
}
