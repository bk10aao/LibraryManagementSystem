package com.library;

import com.library.exceptions.AlreadyBorrowedException;
import com.library.exceptions.AuthorNotFoundException;
import com.library.exceptions.BookNotFoundException;
import com.library.exceptions.InvalidParameterException;
import com.library.exceptions.NoBooksAvailableException;
import com.library.exceptions.NoBooksBorrowedException;
import com.library.exceptions.NotBorrowedException;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.InputMismatchException;
import java.util.Scanner;

import static com.library.constants.MenuMessages.ALL_BOOKS;
import static com.library.constants.MenuMessages.ALL_BORROWED_BOOKS;
import static com.library.constants.MenuMessages.AVAILABLE_BOOKS;
import static com.library.constants.MenuMessages.BOOKS_BY_AUTHOR;
import static com.library.constants.MenuMessages.BOOK_ADDED;
import static com.library.constants.MenuMessages.BOOK_NOT_ADDED;
import static com.library.constants.MenuMessages.BORROWED;
import static com.library.constants.MenuMessages.ENTER_AUTHOR;
import static com.library.constants.MenuMessages.ENTER_TITLE;
import static com.library.constants.MenuMessages.EXIT;
import static com.library.constants.MenuMessages.INVALID;
import static com.library.constants.MenuMessages.RETURNED;
import static com.library.constants.MenuMessages.TITLE_AUTHOR_SEARCH;
import static com.library.constants.MenuMessages.TITLE_BORROW;
import static com.library.constants.MenuMessages.TITLE_RETURN;
import static com.library.constants.MenuMessages.WELCOME;
import static com.library.constants.MenuOptions.ADD_A_BOOK_OPTION;
import static com.library.constants.MenuOptions.BORROW_BOOK_OPTION;
import static com.library.constants.MenuOptions.EXIT_OPTION;
import static com.library.constants.MenuOptions.GET_AUTHOR_BOOKS;
import static com.library.constants.MenuOptions.LIST_ALL_BOOKS;
import static com.library.constants.MenuOptions.LIST_AVAILABLE_BOOKS;
import static com.library.constants.MenuOptions.LIST_BORROWED_BOOKS;
import static com.library.constants.MenuOptions.MENU;
import static com.library.constants.MenuOptions.RETURN_BOOK_OPTION;
import static com.library.constants.MenuOptions.SEARCH_BOOK_OPTION;
import static com.library.constants.MenuOptions.SORT_BY_AUTHOR;
import static com.library.constants.MenuOptions.SORT_BY_TITLE;
import static com.library.constants.MenuOptions.SORT_MENU;


public class LibraryApplication {

    private final Library library;

    public LibraryApplication(final Library library) {
        this.library = library;
    }

    public void run() throws SQLException, InvalidParameterException {
        boolean running = true;
        System.out.println(WELCOME);
        Scanner scanner = new Scanner(System.in);
        try {
            while (running) {
                System.out.println(MENU);
                int choice = validateChoices(scanner, EXIT_OPTION);
                scanner.nextLine();
                switch (choice) {
                    case ADD_A_BOOK_OPTION:
                        addBook(scanner);
                        break;
                    case LIST_ALL_BOOKS:
                        listAllBooks(scanner);
                        break;
                    case LIST_AVAILABLE_BOOKS:
                        listAvailableBooks(scanner);
                        break;
                    case SEARCH_BOOK_OPTION:
                        searchForBook(scanner);
                        break;
                    case BORROW_BOOK_OPTION:
                        borrowBook(scanner);
                        break;
                    case RETURN_BOOK_OPTION:
                        returnBook(scanner);
                        break;
                    case GET_AUTHOR_BOOKS:
                        getAuthorBooks(scanner);
                        break;
                    case LIST_BORROWED_BOOKS:
                        listBorrowedBooks(scanner);
                        break;
                    case EXIT_OPTION:
                        running = false;
                        System.out.println(EXIT);
                        break;
                }
            }
        } finally {
            scanner.close();
        }
    }

    private void addBook(final Scanner scanner) {
        System.out.print(ENTER_TITLE);
        String title = scanner.nextLine();
        System.out.print(ENTER_AUTHOR);
        String author = scanner.nextLine();
        boolean success = false;
        try {
            success = library.addBook(new Book(title, author));
        } catch (InvalidParameterException e) {
            System.out.println(e.getMessage());
        }
        System.out.println(success ? BOOK_ADDED : BOOK_NOT_ADDED);
    }

    private void borrowBook(final Scanner scanner) {
        System.out.print(TITLE_BORROW);
        String title = scanner.nextLine();
        try {
            library.borrowBook(title);
            System.out.println(BORROWED);
        } catch (BookNotFoundException | AlreadyBorrowedException | InvalidParameterException e) {
            System.out.println(e.getMessage());
        }
    }

    private void getAuthorBooks(final Scanner scanner) {
        System.out.print(BOOKS_BY_AUTHOR);
        String author = scanner.nextLine();
        try {
            library.getBooksByAuthor(author).stream()
                    .sorted(Comparator.comparing(Book::getTitle))
                    .forEach(System.out::println);
        } catch (InvalidParameterException | AuthorNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private void listAllBooks(final Scanner scanner) {
        System.out.println(SORT_MENU);
        int sortAllChoice = validateChoices(scanner, 2);
        System.out.println(ALL_BOOKS);
        switch (sortAllChoice) {
            case SORT_BY_AUTHOR:
                library.viewAllBooks().stream()
                        .sorted(Comparator.comparing(Book::getAuthor)
                                .thenComparing(Book::getTitle))
                        .forEach(System.out::println);
                break;
            case SORT_BY_TITLE:
                library.viewAllBooks().stream()
                        .sorted(Comparator.comparing(Book::getTitle)
                                .thenComparing(Book::getAuthor))
                        .forEach(System.out::println);
                break;
        }
    }

    private void listAvailableBooks(final Scanner scanner) {
        System.out.println(SORT_MENU);
        int sortAvailableChoice = validateChoices(scanner, 2);
        System.out.println(AVAILABLE_BOOKS);
        switch (sortAvailableChoice) {
            case SORT_BY_AUTHOR:
                try {
                    library.viewAvailableBooks().stream()
                            .sorted(Comparator.comparing(Book::getAuthor)
                                    .thenComparing(Book::getTitle))
                            .forEach(System.out::println);
                } catch (NoBooksAvailableException e) {
                    System.out.println(e.getMessage());
                }
                break;
            case SORT_BY_TITLE:
                try {
                    library.viewAvailableBooks().stream()
                            .sorted(Comparator.comparing(Book::getTitle)
                                    .thenComparing(Book::getAuthor))
                            .forEach(System.out::println);
                } catch (NoBooksAvailableException e) {
                    System.out.println(e.getMessage());
                }
                break;
        }
    }

    private void listBorrowedBooks(final Scanner scanner) {
        System.out.println(SORT_MENU);
        int sortAvailableChoice = validateChoices(scanner, 3);
        System.out.println(ALL_BORROWED_BOOKS);
        switch (sortAvailableChoice) {
            case SORT_BY_AUTHOR:
                try {
                    library.viewBorrowedBooks().stream()
                            .sorted(Comparator.comparing(Book::getAuthor)
                                    .thenComparing(Book::getTitle))
                            .forEach(System.out::println);
                } catch (NoBooksBorrowedException e) {
                    System.out.println(e.getMessage());
                }
                break;
            case SORT_BY_TITLE:
                try {
                    library.viewBorrowedBooks().stream()
                            .sorted(Comparator.comparing(Book::getTitle)
                                    .thenComparing(Book::getAuthor))
                            .forEach(System.out::println);
                } catch (NoBooksBorrowedException e) {
                    System.out.println(e.getMessage());
                }
                break;
        }
    }

    private void returnBook(final Scanner scanner) {
        System.out.print(TITLE_RETURN);
        String title = scanner.nextLine();
        try {
            library.returnBook(title);
            System.out.println(RETURNED);
        } catch (BookNotFoundException | NotBorrowedException | InvalidParameterException e) {
            System.out.println(e.getMessage());
        }
    }

    private void searchForBook(final Scanner scanner) {
        System.out.print(TITLE_AUTHOR_SEARCH);
        String title = scanner.nextLine();
        try {
            library.searchBook(title).stream()
                    .sorted(Comparator.comparing(Book::getTitle)
                            .thenComparing(Book::getAuthor))
                    .forEach(System.out::println);
        } catch (BookNotFoundException | InvalidParameterException e) {
            System.out.println(e.getMessage());
        }
    }

    private int validateChoices(final Scanner scanner, final int max) {
        int choice = 0;
        boolean validChoice = false;
        while (!validChoice) {
            try {
                choice = scanner.nextInt();
                if(choice < 1 || choice > max)
                    throw new InputMismatchException();
                validChoice = true;
            } catch (InputMismatchException inputMismatchException) {
                System.out.println(INVALID);
                if(!scanner.hasNextInt() && scanner.hasNext())
                    scanner.next();
            }
        }
        return choice;
    }
}
