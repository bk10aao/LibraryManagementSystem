package com.library;

import com.library.exceptions.BookAlreadyBorrowedException;
import com.library.exceptions.InvalidParameterException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookTest {

    @Test
    public void givenBook_withNullTitle_andNullAuthor_throwsInvalidArgumentException() {
        assertThrows(InvalidParameterException.class, () -> new Book(null, null));
    }

    @Test
    public void givenBook_withBlankTitle_throwsInvalidArgumentException() {
        assertThrows(InvalidParameterException.class, () -> new Book("", "Jon Skeet"));
    }

    @Test
    public void givenBook_withNullTitle_throwsInvalidArgumentException() {
        assertThrows(InvalidParameterException.class, () -> new Book(null, "Jon Skeet"));
    }

    @Test
    public void givenBook_withNullAuthor_throwsInvalidArgumentException() {
        assertThrows(InvalidParameterException.class, () -> new Book("C# In Depth", null));
    }

    @Test
    public void givenBook_withBlankAuthor_throwsInvalidArgumentException() {
        assertThrows(InvalidParameterException.class, () -> new Book("C# In Depth", ""));
    }

    @Test
    public void givenBook_withAuthor_JonSkeet_andTitle_CSharpInDepth_constructsCorrectBook() throws InvalidParameterException {
        Book book = new Book("C# In Depth", "Jon Skeet");
        assertEquals("C# In Depth", book.getTitle());
        assertEquals("Jon Skeet", book.getAuthor());
        assertFalse(book::isBorrowed);
    }

    @Test
    public void givenBook_onIsBorrowed_returnsFalse() throws InvalidParameterException, BookAlreadyBorrowedException {
        Book book = new Book("C# In Depth", "Jon Skeet");
        assertEquals("C# In Depth", book.getTitle());
        assertEquals("Jon Skeet", book.getAuthor());
        assertFalse(book.isBorrowed());
        book.borrowBook();
        assertTrue(book::isBorrowed);
    }

    @Test
    public void givenBook_alreadyBorrowed_onBorrowBook_throwsAlreadyBorrowedException() throws InvalidParameterException, BookAlreadyBorrowedException {
        Book book = new Book("C# In Depth", "Jon Skeet");
        assertEquals("C# In Depth", book.getTitle());
        assertEquals("Jon Skeet", book.getAuthor());
        assertFalse(book.isBorrowed());
        book.borrowBook();
        assertTrue(book.isBorrowed());
        assertThrows(BookAlreadyBorrowedException.class, book::borrowBook);
    }

    @Test
    public void givenBook_notBorrowed_onToString_returnsCorrectString() throws InvalidParameterException {
        Book book = new Book("C# In Depth", "Jon Skeet");
        assertEquals("Title: C# In Depth Author: Jon Skeet Available", book.toString());
    }

    @Test
    public void givenBook_borrowed_onToString_returnsCorrectString() throws InvalidParameterException {
        Book book = new Book("C# In Depth", "Jon Skeet");
        book.setBorrowed(true);
        assertEquals("Title: C# In Depth Author: Jon Skeet Borrowed", book.toString());
    }
}