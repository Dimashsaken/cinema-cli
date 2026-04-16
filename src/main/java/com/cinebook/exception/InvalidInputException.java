package com.cinebook.exception;

/** Thrown when user input is malformed (bad seat code, invalid menu choice, etc.). */
public class InvalidInputException extends CineBookException {

    public InvalidInputException(String message) {
        super(message);
    }
}
