package com.cinebook.exception;

/** Thrown when login credentials are invalid. */
public class AuthenticationException extends CineBookException {

    public AuthenticationException(String message) {
        super(message);
    }
}
