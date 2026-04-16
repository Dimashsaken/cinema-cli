package com.cinebook.exception;

/** Root checked exception for all CineBook domain errors. */
public class CineBookException extends Exception {

    public CineBookException(String message) {
        super(message);
    }

    public CineBookException(String message, Throwable cause) {
        super(message, cause);
    }
}
