package com.cinebook.exception;

/** Thrown when a payment is rejected or fails. */
public class PaymentFailedException extends CineBookException {

    public PaymentFailedException(String message) {
        super(message);
    }
}
