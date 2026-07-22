package com.library.constants;

public class DatabaseQueries {

    public static final String ADD_BOOK_QUERY = "INSERT INTO books (title, author, is_borrowed) VALUES (?, ?, ?)";

    public static final String CREATE_BOOKS_TABLE = """
                                                        CREATE TABLE IF NOT EXISTS books (
                                                            title TEXT COLLATE NOCASE,
                                                            author TEXT,
                                                            is_borrowed BOOLEAN
                                                        );
                                                        """;
    public static final String SEARCH_FOR_BOOK_Query = "SELECT title, author, is_borrowed FROM books " +
            "                                           WHERE LOWER(title) = LOWER(?)";
    public static final String SELECT_BOOKS_BY_AUTHOR_QUERY = "SELECT title, author, is_borrowed FROM books WHERE author = ? ORDER BY title ASC";
    public static final String SELECT_ALL_BOOKS_QUERY = "SELECT title, author, is_borrowed FROM books";
    public static final String SELECT_ALL_AVAILABLE_BOOKS_QUERY = "SELECT title, author, is_borrowed FROM books WHERE is_borrowed = false";

    public static final String SELECT_ALL_BORROWED_BOOKS_QUERY = "SELECT title, author, is_borrowed FROM books WHERE is_borrowed = true";
    public static final String UPDATE_BORROWED_STATUS = "UPDATE books SET is_borrowed = ? WHERE title = ? COLLATE NOCASE";


}
