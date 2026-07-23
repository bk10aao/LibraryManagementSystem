package com.library;

import com.library.exceptions.AlreadyBorrowedException;
import com.library.exceptions.InvalidParameterException;
import lombok.Getter;
import lombok.Setter;

import static com.library.constants.ErrorMessages.AUTHOR_CANNOT_BE_BLANK;
import static com.library.constants.ErrorMessages.AUTHOR_CANNOT_BE_NULL;
import static com.library.constants.ErrorMessages.BOOK_ALREADY_BORROWED;
import static com.library.constants.ErrorMessages.TITLE_CANNOT_BE_BLANK;
import static com.library.constants.ErrorMessages.TITLE_CANNOT_BE_NULL;

@Getter
public class Book {

    private final String author;
    private final String title;

    @Setter
    private boolean isBorrowed;

    public Book(String title, String author) throws InvalidParameterException {
        validateInput(author, AUTHOR_CANNOT_BE_BLANK, AUTHOR_CANNOT_BE_NULL);
        validateInput(title, TITLE_CANNOT_BE_BLANK, TITLE_CANNOT_BE_NULL);
        this.author = author;
        this.title = title;
    }

    public void borrowBook() throws AlreadyBorrowedException {
        if(this.isBorrowed)
            throw new AlreadyBorrowedException(BOOK_ALREADY_BORROWED);
        this.isBorrowed = true;
    }

    @Override
    public String toString() {
        return "Title: " + this.title + " Author: " + this.author + " " + (isBorrowed ? "Borrowed" : "Available");
    }

    private static void validateInput(final String input, final String notBlank, final String notNull) throws InvalidParameterException {
        if (input == null)
            throw new InvalidParameterException(notNull);
        if (input.isBlank())
            throw new InvalidParameterException(notBlank);
    }
}
