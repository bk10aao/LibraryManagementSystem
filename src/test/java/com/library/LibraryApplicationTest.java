package com.library;

import com.library.exceptions.AlreadyBorrowedException;
import com.library.exceptions.AuthorNotFoundException;
import com.library.exceptions.BookNotFoundException;
import com.library.exceptions.InvalidParameterException;
import com.library.exceptions.NoBooksAvailableException;
import com.library.exceptions.NoBooksBorrowedException;
import com.library.exceptions.NotBorrowedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;

import static com.library.constants.ErrorMessages.BOOK_ALREADY_BORROWED;
import static com.library.constants.ErrorMessages.BOOK_NOT_BORROWED;
import static com.library.constants.ErrorMessages.BOOK_NOT_FOUND;
import static com.library.constants.ErrorMessages.NO_BOOKS_AVAILABLE;
import static com.library.constants.MenuMessages.ALL_BOOKS;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class LibraryApplicationTest {

    @Mock
    private Library mockLibrary;

    private LibraryApplication libraryApplication;

    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    private Book book1;
    private Book book2;
    private Book book3;

    private static final int INVALID_CHOICE = -1;
    private static final String MOCK_TITLE_1 = "Mock Title 1";
    private static final String MOCK_AUTHOR_1 = "Mock Author 1";

    @BeforeEach
    void setUp() throws BookNotFoundException, AlreadyBorrowedException, NotBorrowedException, InvalidParameterException, NoBooksAvailableException {
        openMocks(this);
        book1 = new Book(MOCK_TITLE_1, "Mock Author 2");
        book2 = new Book("Mock Title 2", "Mock Author 3");
        book3 = new Book("Mock Title 3", MOCK_AUTHOR_1);
        when(mockLibrary.addBook(any(Book.class))).thenReturn(true);
        when(mockLibrary.searchBook(anyString())).thenReturn(List.of(book1, book2, book3));
        when(mockLibrary.borrowBook(anyString())).thenReturn(true);
        when(mockLibrary.returnBook(anyString())).thenReturn(true);
        libraryApplication = new LibraryApplication(mockLibrary);
        when(mockLibrary.viewAvailableBooks()).thenReturn(List.of(book1, book3));
        when(mockLibrary.viewAllBooks()).thenReturn(List.of(book1, book2, book3));
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Test
    void givenInvalidMenuInput_returnsCorrectMenuOptions() throws InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                INVALID_CHOICE,
                EXIT_OPTION
        ));
        libraryApplication.run();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(INVALID));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void givenNonNumberMenuInput_returnsCorrectMenuOptions() throws InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                "abc",
                EXIT_OPTION
        ));
        libraryApplication.run();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(INVALID));
        assertTrue(output.contains(EXIT));
    }

    private InputStream buildTestInput(Object... inputs) {
        String[] stringInputs = java.util.Arrays.stream(inputs)
                .map(String::valueOf)
                .toArray(String[]::new);
        String joinedInput = String.join("\n", stringInputs) + "\n";
        return new ByteArrayInputStream(joinedInput.getBytes());
    }

    @Test
    void whenAddingBook_returnsCorrectMenuOptions() throws InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                ADD_A_BOOK_OPTION,
                MOCK_TITLE_1,
                MOCK_AUTHOR_1,
                EXIT_OPTION
        ));
        libraryApplication.run();
        verify(mockLibrary).addBook(any(Book.class));
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(ENTER_TITLE));
        assertTrue(output.contains(ENTER_AUTHOR));
        assertTrue(output.contains(BOOK_ADDED));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenAddingBook_onFail_returnsCorrectMenuOptions() throws InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                ADD_A_BOOK_OPTION,
                MOCK_TITLE_1,
                MOCK_AUTHOR_1,
                EXIT_OPTION
        ));
        when(mockLibrary.addBook(any())).thenReturn(false);
        libraryApplication.run();
        verify(mockLibrary).addBook(any(Book.class));
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(ENTER_TITLE));
        assertTrue(output.contains(ENTER_AUTHOR));
        assertTrue(output.contains(BOOK_NOT_ADDED));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenAddingBook_withEmptyTitle_ThrowsInvalidParameterException() throws InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                ADD_A_BOOK_OPTION,
                " ",
                MOCK_AUTHOR_1,
                EXIT_OPTION
        ));
        when(mockLibrary.addBook(any(Book.class))).thenThrow(new InvalidParameterException("Title cannot be blank."));
        libraryApplication.run();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(ENTER_TITLE));
        assertTrue(output.contains(ENTER_AUTHOR));
        assertTrue(output.contains("Title cannot be blank."));
        assertTrue(output.contains(BOOK_NOT_ADDED));
    }

    @Test
    void whenBorrowingBook_returnsCorrectMenuOptions() throws BookNotFoundException, AlreadyBorrowedException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                BORROW_BOOK_OPTION,
                MOCK_TITLE_1,
                EXIT_OPTION
        ));
        libraryApplication.run();
        verify(mockLibrary).borrowBook(MOCK_TITLE_1);
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(TITLE_BORROW));
        assertTrue(output.contains(BORROWED));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenBorrowingBook_thatIsAlreadyBorrowed_throwsAlreadyBorrowedException_returnsCorrectMenuOptions() throws BookNotFoundException, AlreadyBorrowedException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                BORROW_BOOK_OPTION,
                MOCK_TITLE_1,
                EXIT_OPTION
        ));
        when(mockLibrary.borrowBook(anyString())).thenThrow(new AlreadyBorrowedException(BOOK_ALREADY_BORROWED));
        libraryApplication.run();
        verify(mockLibrary).borrowBook(MOCK_TITLE_1);
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(TITLE_BORROW));
        assertTrue(output.contains(BOOK_ALREADY_BORROWED));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenBorrowingBook_withBlankTitle_throwsInvalidParameterException_returnsCorrectMenuOptions() throws InvalidParameterException, AlreadyBorrowedException, BookNotFoundException, SQLException {
        System.setIn(buildTestInput(
                BORROW_BOOK_OPTION,
                " ",
                EXIT_OPTION
        ));
        when(mockLibrary.borrowBook(" ")).thenThrow(new InvalidParameterException("Title must not be blank."));
        libraryApplication.run();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(TITLE_BORROW));
        assertTrue(output.contains("Title must not be blank."));
    }

    @Test
    void whenViewingAllBooks_withInvalidInputForSorted_throwsInvalidParameterException_returnsCorrectMenuOptions() throws InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                LIST_ALL_BOOKS,
                INVALID_CHOICE,
                SORT_BY_TITLE,
                EXIT_OPTION
        ));
        libraryApplication.run();
        verify(mockLibrary).viewAllBooks();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(SORT_MENU));
        assertTrue(output.contains(INVALID));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenViewingAllBooks_sortedByAuthor_returnsCorrectOutput() throws InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                LIST_ALL_BOOKS,
                SORT_BY_AUTHOR,
                EXIT_OPTION
        ));
        libraryApplication.run();
        verify(mockLibrary).viewAllBooks();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(SORT_MENU));
        assertTrue(output.contains(ALL_BOOKS));
        assertTrue(output.contains(book3.toString() + "\n" + book1.toString() + "\n" + book2.toString()));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenViewingAllBooks_sortedByTitle_returnsCorrectOutput() throws InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                LIST_ALL_BOOKS,
                SORT_BY_TITLE,
                EXIT_OPTION
        ));
        libraryApplication.run();
        verify(mockLibrary).viewAllBooks();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(SORT_MENU));
        assertTrue(output.contains(ALL_BOOKS));
        assertTrue(output.contains(book1.toString() + "\n" + book2.toString() + "\n" + book3.toString()));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenViewingAllAvailableBooks_withInvalidInputForSorted_throwsInvalidParameterException_returnsCorrectMenuOptions() throws InvalidParameterException, SQLException, NoBooksAvailableException {
        System.setIn(buildTestInput(
                LIST_AVAILABLE_BOOKS,
                INVALID_CHOICE,
                SORT_BY_AUTHOR,
                EXIT_OPTION
        ));
        libraryApplication.run();
        verify(mockLibrary).viewAvailableBooks();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(SORT_MENU));
        assertTrue(output.contains(INVALID));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenViewingAllAvailableBooks_sortedByAuthor_returnsCorrectOutput() throws NoBooksAvailableException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                LIST_AVAILABLE_BOOKS,
                SORT_BY_AUTHOR,
                EXIT_OPTION
        ));
        libraryApplication.run();
        verify(mockLibrary).viewAvailableBooks();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(SORT_MENU));
        assertTrue(output.contains(AVAILABLE_BOOKS));
        assertTrue(output.contains(book3.toString() + "\n" + book1.toString()));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenViewingAllAvailableBooks_sortedByTitle_returnsCorrectOutput() throws InvalidParameterException, SQLException, NoBooksAvailableException {
        System.setIn(buildTestInput(
                LIST_AVAILABLE_BOOKS,
                SORT_BY_TITLE,
                EXIT_OPTION
        ));
        libraryApplication.run();
        verify(mockLibrary).viewAvailableBooks();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(SORT_MENU));
        assertTrue(output.contains(AVAILABLE_BOOKS));
        assertTrue(output.contains(book1.toString() + "\n" + book3.toString()));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenViewingAvailableBooks_sortedByAuthor_whenNoBooksAvailable_returnsCorrectOutput() throws NoBooksAvailableException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                LIST_AVAILABLE_BOOKS,
                SORT_BY_AUTHOR,
                EXIT_OPTION
        ));
        when(mockLibrary.viewAvailableBooks()).thenThrow(new NoBooksAvailableException(NO_BOOKS_AVAILABLE));
        libraryApplication.run();
        verify(mockLibrary).viewAvailableBooks();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(SORT_MENU));
        assertTrue(output.contains(AVAILABLE_BOOKS));
        assertTrue(output.contains(NO_BOOKS_AVAILABLE));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenViewingAvailableBooks_sortedByTitle_whenNoBooksAvailable_returnsCorrectOutput() throws NoBooksAvailableException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                LIST_AVAILABLE_BOOKS,
                SORT_BY_TITLE,
                EXIT_OPTION
        ));
        when(mockLibrary.viewAvailableBooks()).thenThrow(new NoBooksAvailableException(NO_BOOKS_AVAILABLE));
        libraryApplication.run();
        verify(mockLibrary).viewAvailableBooks();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(SORT_MENU));
        assertTrue(output.contains(AVAILABLE_BOOKS));
        assertTrue(output.contains(NO_BOOKS_AVAILABLE));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenSearchingForBook_returnsCorrectOutput() throws BookNotFoundException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                SEARCH_BOOK_OPTION,
                MOCK_TITLE_1,
                EXIT_OPTION
        ));
        libraryApplication.run();
        verify(mockLibrary).searchBook(MOCK_TITLE_1);
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(TITLE_AUTHOR_SEARCH));
        assertTrue(output.contains(book1.toString()));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenSearchingForBook_thatDoesNotExists_throwsBookNotFoundException_andCorrectOutput() throws BookNotFoundException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                SEARCH_BOOK_OPTION,
                MOCK_TITLE_1,
                EXIT_OPTION
        ));
        when(mockLibrary.searchBook(anyString())).thenThrow(new BookNotFoundException(BOOK_NOT_FOUND));
        libraryApplication.run();
        verify(mockLibrary).searchBook(MOCK_TITLE_1);
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(TITLE_AUTHOR_SEARCH));
        assertTrue(output.contains(BOOK_NOT_FOUND));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenReturningBook_returnsCorrectOutput() throws BookNotFoundException, NotBorrowedException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                RETURN_BOOK_OPTION,
                MOCK_TITLE_1,
                EXIT_OPTION
        ));
        libraryApplication.run();
        verify(mockLibrary).returnBook(MOCK_TITLE_1);
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(TITLE_RETURN));
        assertTrue(output.contains(RETURNED));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenReturningBook_thatIsNotBorrowed_throwsNotBorrowedException_withCorrectOutput() throws BookNotFoundException, NotBorrowedException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                RETURN_BOOK_OPTION,
                MOCK_TITLE_1,
                EXIT_OPTION
        ));
        when(mockLibrary.returnBook(anyString())).thenThrow(new NotBorrowedException(BOOK_NOT_BORROWED));
        libraryApplication.run();
        verify(mockLibrary).returnBook(MOCK_TITLE_1);
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(TITLE_RETURN));
        assertTrue(output.contains(BOOK_NOT_BORROWED));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenGettingBooksByAuthor_returnsCorrectOutput() throws InvalidParameterException, AuthorNotFoundException, SQLException {
        System.setIn(buildTestInput(
                GET_AUTHOR_BOOKS,
                MOCK_AUTHOR_1,
                EXIT_OPTION
        ));
        when(mockLibrary.getBooksByAuthor(MOCK_AUTHOR_1)).thenReturn(List.of(book3));
        libraryApplication.run();
        verify(mockLibrary).getBooksByAuthor(MOCK_AUTHOR_1);
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(BOOKS_BY_AUTHOR));
        assertTrue(output.contains(book3.toString()));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenGettingBooksByAuthor_throwsInvalidArgumentException_returnsCorrectOutput() throws AuthorNotFoundException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                GET_AUTHOR_BOOKS,
                " ",
                EXIT_OPTION
        ));
        when(mockLibrary.getBooksByAuthor(" ")).thenThrow(new InvalidParameterException("Author must not be blank."));
        libraryApplication.run();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(BOOKS_BY_AUTHOR));
        assertTrue(output.contains("Author must not be blank."));
    }

    @Test
    void whenViewingBorrowedBooks_sortedInvalid_throwsException_returnsCorrectOutput() throws NoBooksBorrowedException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                LIST_BORROWED_BOOKS,
                INVALID_CHOICE,
                SORT_BY_TITLE,
                EXIT_OPTION
        ));
        libraryApplication.run();
        verify(mockLibrary).viewBorrowedBooks();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains(WELCOME));
        assertTrue(output.contains(MENU));
        assertTrue(output.contains(SORT_MENU));
        assertTrue(output.contains(INVALID));
        assertTrue(output.contains(EXIT));
    }

    @Test
    void whenViewingBorrowedBooks_sortedByAuthor_returnsCorrectOutput() throws InvalidParameterException, NoBooksBorrowedException, SQLException {
        System.setIn(buildTestInput(
                LIST_BORROWED_BOOKS,
                SORT_BY_AUTHOR,
                EXIT_OPTION
        ));

        Book first = new Book("B Title", "Mock Author 2");
        Book second = new Book("A Title", MOCK_AUTHOR_1);
        when(mockLibrary.viewBorrowedBooks()).thenReturn(List.of(first, second));
        libraryApplication.run();
        verify(mockLibrary).viewBorrowedBooks();
        String output = outputStreamCaptor.toString().trim();
        int firstBookIndex= output.indexOf(first.toString());
        int secondBookIndex = output.indexOf(second.toString());
        assertTrue(firstBookIndex >= 0);
        assertTrue(secondBookIndex >= 0);
        assertTrue(secondBookIndex < firstBookIndex);
    }

    @Test
    void whenViewingBorrowedBooks_sortedByTitle_returnsCorrectOutput() throws InvalidParameterException, NoBooksBorrowedException, SQLException {
        System.setIn(buildTestInput(
                LIST_BORROWED_BOOKS,
                SORT_BY_TITLE,
                EXIT_OPTION
        ));
        Book first = new Book("A Title", "Mock Author 2");
        Book second = new Book("B Title", MOCK_AUTHOR_1);
        when(mockLibrary.viewBorrowedBooks()).thenReturn(List.of(first, second));
        libraryApplication.run();
        verify(mockLibrary).viewBorrowedBooks();
        String output = outputStreamCaptor.toString().trim();
        int firstBookIndex= output.indexOf(first.toString());
        int secondBookIndex = output.indexOf(second.toString());
        assertTrue(firstBookIndex >= 0);
        assertTrue(secondBookIndex >= 0);
        assertTrue(secondBookIndex > firstBookIndex);
    }

    @Test
    void whenViewingBorrowedBooks_SortedByTitle_whenNotAddedInOrder_returnsCorrectOutput() throws InvalidParameterException, NoBooksBorrowedException, SQLException {
        System.setIn(buildTestInput(
                LIST_BORROWED_BOOKS,
                SORT_BY_TITLE,
                EXIT_OPTION
        ));
        Book first = new Book("B Title", "Mock Author 2");
        Book second = new Book("A Title", MOCK_AUTHOR_1);
        when(mockLibrary.viewBorrowedBooks()).thenReturn(List.of(first, second));
        libraryApplication.run();
        verify(mockLibrary).viewBorrowedBooks();
        String output = outputStreamCaptor.toString().trim();
        int firstBookIndex= output.indexOf(first.toString());
        int secondBookIndex = output.indexOf(second.toString());
        assertTrue(firstBookIndex >= 0);
        assertTrue(secondBookIndex >= 0);
        assertTrue(secondBookIndex < firstBookIndex);
    }

    @Test
    void whenViewingBorrowedBooks_sortedByAuthor_ThrowsInvalidArgumentException() throws NoBooksBorrowedException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                LIST_BORROWED_BOOKS,
                SORT_BY_AUTHOR,
                EXIT_OPTION
        ));
        when(mockLibrary.viewBorrowedBooks()).thenThrow(new NoBooksBorrowedException("No Books Have been borrowed"));
        libraryApplication.run();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("No Books Have been borrowed"));
    }

    @Test
    void whenViewingBorrowedBooks_sortedByTitle_ThrowsInvalidArgumentException() throws NoBooksBorrowedException, InvalidParameterException, SQLException {
        System.setIn(buildTestInput(
                LIST_BORROWED_BOOKS,
                SORT_BY_TITLE,
                EXIT_OPTION
        ));
        when(mockLibrary.viewBorrowedBooks()).thenThrow(new NoBooksBorrowedException("No Books Have been borrowed"));
        libraryApplication.run();
        String output = outputStreamCaptor.toString().trim();
        assertTrue(output.contains("No Books Have been borrowed"));
    }
}
