package com.library.exceptions;

/**
 * Exception thrown when bo books have been borrowed.
 */
public class NoBooksBorrowedException extends Exception {
    /**
     * Constructs a new AuthorNotFoundException with the specified message.
     *
     * @param message the detail message
     */
    public NoBooksBorrowedException(final String message) {
        super(message);
    }
}
