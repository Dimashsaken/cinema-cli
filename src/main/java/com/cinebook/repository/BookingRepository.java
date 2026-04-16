package com.cinebook.repository;

import com.cinebook.domain.Booking;
import java.util.List;

/** Repository for bookings. Backed by CSV append log. */
public interface BookingRepository extends Repository<Booking> {

    /** Find all bookings for a given user. */
    List<Booking> findByUserId(String userId);
}
