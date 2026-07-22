package com.library;

import com.library.exceptions.AlreadyBorrowedException;
import com.library.exceptions.AuthorNotFoundException;
import com.library.exceptions.BookNotFoundException;
import com.library.exceptions.InvalidParameterException;
import com.library.exceptions.NoBooksAvailableException;
import com.library.exceptions.NoBooksBorrowedException;
import com.library.exceptions.NotBorrowedException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static com.library.constants.DatabaseQueries.ADD_BOOK_QUERY;
import static com.library.constants.DatabaseQueries.CREATE_BOOKS_TABLE;
import static com.library.constants.DatabaseQueries.SEARCH_FOR_BOOK_Query;
import static com.library.constants.DatabaseQueries.SELECT_ALL_AVAILABLE_BOOKS_QUERY;
import static com.library.constants.DatabaseQueries.SELECT_ALL_BOOKS_QUERY;
import static com.library.constants.DatabaseQueries.SELECT_ALL_BORROWED_BOOKS_QUERY;
import static com.library.constants.DatabaseQueries.SELECT_BOOKS_BY_AUTHOR_QUERY;
import static com.library.constants.DatabaseQueries.UPDATE_BORROWED_STATUS;

import static com.library.constants.ErrorMessages.BOOK_ALREADY_BORROWED;
import static com.library.constants.ErrorMessages.BOOK_NOT_BORROWED;
import static com.library.constants.ErrorMessages.BOOK_NOT_FOUND;
import static com.library.constants.ErrorMessages.NO_BOOKS_AVAILABLE;
import static com.library.constants.ErrorMessages.NO_BOOKS_BORROWED;

public class Library {

    private final Connection connection;

    public Library(final Connection connection) {
        this.connection = connection;
        try(Statement statement = connection.createStatement()) {
            statement.execute(CREATE_BOOKS_TABLE);
        } catch (SQLException e) {
            System.out.println("Error in connecting to the library database: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a book to the library.
     *
     * @param book the book to add.
     * @return true if successful, otherwise false.
     * @throws InvalidParameterException if the book is null.
     */
    public boolean addBook(final Book book) throws InvalidParameterException {
        if(book == null)
            throw new InvalidParameterException("Book must not be null.");
        try(var preparedStatement = connection.prepareStatement(ADD_BOOK_QUERY)) {
            preparedStatement.setString(1, book.getTitle());
            preparedStatement.setString(2, book.getAuthor());
            preparedStatement.setBoolean(3, book.isBorrowed());
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error adding book to the library: " + e.getMessage());
            return false;
        }
    }

    /**
     * Borrows a book by its title.
     *
     * @param title the title of the book to borrow.
     * @return true if the book is successfully borrowed.
     * @throws AlreadyBorrowedException if book is already borrowed.
     * @throws InvalidParameterException if title is null or is blank.
     * @throws BookNotFoundException if book is not found.
     */
    public boolean borrowBook(final String title) throws BookNotFoundException, AlreadyBorrowedException, InvalidParameterException {
        validateTitle(title);
        Book book = searchBook(title.trim()).stream()
                .filter(b -> !b.isBorrowed())
                .findFirst()
                .orElseThrow(() -> new AlreadyBorrowedException(BOOK_ALREADY_BORROWED));
        try(PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_BORROWED_STATUS)) {
            preparedStatement.setBoolean(1, true);
            preparedStatement.setString(2, book.getTitle());
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error borrowing book from library: " + e.getMessage());
            return false;
        }
    }

    public List<Book> getBooksByAuthor(String author) throws InvalidParameterException, AuthorNotFoundException, SQLException {
        validateAuthor(author);
        List<Book> books = new ArrayList<>();
        try(PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BOOKS_BY_AUTHOR_QUERY)) {
            preparedStatement.setString(1, author.trim());
            try(ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    books.add(mapBook(resultSet));
                }
            }
        }
        if(books.isEmpty())
            throw new AuthorNotFoundException("No books found for author: " + author.trim());
        return books;
    }

    /**
     * Returns a book by its title.
     *
     * @param title the title of the book to return.
     * @return true if the book is successfully returned, otherwise false.
     * @throws InvalidParameterException if title is null or is blank.
     * @throws BookNotFoundException if no book exists with title.
     * @throws NotBorrowedException if the book is no borrowed.
     */
    public boolean returnBook(final String title) throws BookNotFoundException, NotBorrowedException, InvalidParameterException {
        validateTitle(title);
        boolean isBorrowed = searchBook(title.trim()).stream().anyMatch(Book::isBorrowed);
        if (!isBorrowed)
            throw new NotBorrowedException(BOOK_NOT_BORROWED);
        try(var preparedStatement = connection.prepareStatement(UPDATE_BORROWED_STATUS)) {
            preparedStatement.setBoolean(1, false);
            preparedStatement.setString(2, title.trim());
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error returning book from library: " + e.getMessage());
            return false;
        }
    }

    /**
     * Searches for a book by its title.
     *
     * @param title the title of the book to search for.
     * @return the book if found, otherwise throws BookNotFoundException.
     * @throws InvalidParameterException if title is null, empty or blank.
     */
    public List<Book> searchBook(final String title) throws BookNotFoundException, InvalidParameterException {
        validateTitle(title);
        List<Book> foundBooks = new ArrayList<>();
        try(PreparedStatement preparedStatement = connection.prepareStatement(SEARCH_FOR_BOOK_Query)) {
            preparedStatement.setString(1, title.trim());
            try(ResultSet resultSet = preparedStatement.executeQuery()) {
                while(resultSet.next()) {
                    foundBooks.add(mapBook(resultSet));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error searching for book in the library: " + e.getMessage());
        }
        if (foundBooks.isEmpty())
            throw new BookNotFoundException(BOOK_NOT_FOUND);
        return foundBooks;
    }

    /**
     * Gets a list of all books.
     *
     * @return a list of all books in library, borrowed or not.
     */
    public List<Book> viewAllBooks() throws InvalidParameterException {
        List<Book> books = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_BOOKS_QUERY);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                books.add(mapBook(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Invalid argument when reading from database: " + e.getMessage(), e);
        }
        return books;
    }

    /**
     * Gets a list of available books.
     *
     * @return a list of books that are not borrowed.
     */
    public List<Book> viewAvailableBooks() throws NoBooksAvailableException, InvalidParameterException {
        List<Book> books = new ArrayList<>();
        try(PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_AVAILABLE_BOOKS_QUERY);
            ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                books.add(mapBook(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Invalid argument when reading from database: " + e.getMessage(), e);
        }
        if(books.isEmpty())
            throw new NoBooksAvailableException(NO_BOOKS_AVAILABLE);
        return books;
    }

    public List<Book> viewBorrowedBooks() throws InvalidParameterException, NoBooksBorrowedException {
        List<Book> books = new ArrayList<>();
        try(PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_BORROWED_BOOKS_QUERY);
            ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                books.add(mapBook(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Invalid argument when reading from database: " + e.getMessage(), e);
        }
        if(books.isEmpty())
            throw new NoBooksBorrowedException(NO_BOOKS_BORROWED);
        return books;
    }

    private static void validateAuthor(final String author) throws InvalidParameterException {
        if(author == null)
            throw new InvalidParameterException("Author must not be null.");
        if(author.trim().isBlank())
            throw new InvalidParameterException("Author must not be blank.");
    }

    private static void validateTitle(final String title) throws InvalidParameterException {
        if(title == null)
            throw new InvalidParameterException("Title must not be null.");
        if(title.trim().isBlank())
            throw new InvalidParameterException("Title must not be blank.");
    }

    private Book mapBook(final ResultSet resultSet) throws SQLException, InvalidParameterException {
        final Book book = new Book(resultSet.getString("title"), resultSet.getString("author"));
        book.setBorrowed(resultSet.getBoolean("is_borrowed"));
        return book;
    }
}
