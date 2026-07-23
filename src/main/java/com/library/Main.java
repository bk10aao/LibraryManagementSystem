package com.library;

import com.library.exceptions.InvalidParameterException;
import com.library.exceptions.NoBooksAvailableException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static com.library.constants.DatabaseQueries.CREATE_BOOKS_TABLE;

public class Main {

    public static void main(final String[] args) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:library.db")) {
            System.out.println("Connected to SQLite successfully!");
            createTableIfNotExists(connection);
            Library library = new Library(connection);
            LibraryApplication libraryApplication = new LibraryApplication(library);
            libraryApplication.run();
        } catch (SQLException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        } catch (InvalidParameterException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createTableIfNotExists(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_BOOKS_TABLE);
        }
    }
}
