package com.cinebook.exception;

/** Thrown when a requested seat is already held or booked. */
public class SeatUnavailableException extends CineBookException {

    public SeatUnavailableException(String message) {
        super(message);
    }
}
