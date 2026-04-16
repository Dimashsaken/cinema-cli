package com.cinebook.exception;

/** Thrown when a hold has expired before confirmation. */
public class HoldExpiredException extends CineBookException {

    public HoldExpiredException(String message) {
        super(message);
    }
}
