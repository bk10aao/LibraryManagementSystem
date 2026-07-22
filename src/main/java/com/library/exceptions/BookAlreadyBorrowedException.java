package com.library.exceptions;

/**
 * Exception thrown when attempting to borrow already borrowed book.
 */
public class BookAlreadyBorrowedException extends Exception {

    /**
     * Constructs a new BookAlreadyBorrowedException with the specified message.
     *
     * @param message the detail message
     */
    public BookAlreadyBorrowedException(final String message) {
        super(message);
    }
}
