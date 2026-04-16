package com.cinebook.service;

import com.cinebook.domain.Booking;
import com.cinebook.domain.Seat;
import com.cinebook.domain.Showtime;
import com.cinebook.domain.enums.BookingStatus;
import com.cinebook.domain.enums.SeatStatus;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.ShowtimeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Generates reports for admin dashboards. */
public class ReportService {

    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;

    public ReportService(ShowtimeRepository showtimeRepository,
                         BookingRepository bookingRepository) {
        this.showtimeRepository = showtimeRepository;
        this.bookingRepository = bookingRepository;
    }

    /** Occupancy report for a showtime. */
    public String occupancyReport(String showtimeId) {
        Showtime st = showtimeRepository.findById(showtimeId).orElse(null);
        if (st == null) return "Showtime not found.";

        int total = 0, booked = 0, held = 0, available = 0;
        for (var row : st.getSeats()) {
            for (Seat seat : row) {
                total++;
                switch (seat.getStatus()) {
                    case BOOKED -> booked++;
                    case HELD -> held++;
                    case AVAILABLE -> available++;
                }
            }
        }

        BigDecimal occupancy = total > 0
                ? BigDecimal.valueOf(booked).multiply(BigDecimal.valueOf(100))
                  .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return String.format(
                "Showtime: %s\nTotal: %d | Booked: %d | Held: %d | Available: %d\nOccupancy: %s%%",
                showtimeId, total, booked, held, available, occupancy.toPlainString());
    }

    /** Revenue report across all confirmed bookings. */
    public String revenueReport() {
        List<Booking> all = bookingRepository.findAll();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int confirmedCount = 0;
        int refundedCount = 0;

        for (Booking b : all) {
            if (b.getStatus() == BookingStatus.CONFIRMED) {
                totalRevenue = totalRevenue.add(b.getTotalPrice());
                confirmedCount++;
            } else if (b.getStatus() == BookingStatus.REFUNDED) {
                refundedCount++;
            }
        }

        return String.format(
                "Revenue Report\nConfirmed: %d | Refunded: %d\nTotal Revenue: $%s",
                confirmedCount, refundedCount, totalRevenue.toPlainString());
    }

    /** Export bookings as CSV string. */
    public String exportBookingsCsv() {
        List<Booking> all = bookingRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("bookingId,userId,showtimeId,seats,totalPrice,status,createdAt,version\n");
        for (Booking b : all) {
            sb.append(String.join(",",
                    b.getBookingId(),
                    b.getUserId(),
                    b.getShowtimeId(),
                    "\"" + String.join(";", b.getSeats()) + "\"",
                    b.getTotalPrice().toPlainString(),
                    b.getStatus().name(),
                    b.getCreatedAt().toString(),
                    String.valueOf(b.getVersion())
            )).append("\n");
        }
        return sb.toString();
    }
}
