package com.library;

import com.library.exceptions.AlreadyBorrowedException;
import com.library.exceptions.AuthorNotFoundException;
import com.library.exceptions.BookNotFoundException;
import com.library.exceptions.InvalidParameterException;
import com.library.exceptions.NoBooksAvailableException;
import com.library.exceptions.NoBooksBorrowedException;

import com.library.exceptions.NotBorrowedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static com.library.constants.DatabaseQueries.ADD_BOOK_QUERY;
import static com.library.constants.DatabaseQueries.SEARCH_FOR_BOOK_Query;
import static com.library.constants.DatabaseQueries.SELECT_ALL_AVAILABLE_BOOKS_QUERY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class LibraryTest {

    private Library library;

    @Mock
    private Connection mockConnection;

    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private PreparedStatement mockPreparedStatement;

    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream standardOut = System.out;

    private Book book1;
    private Book book2;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws SQLException, InvalidParameterException {
        closeable = openMocks(this);
        System.setOut(new PrintStream(outputStreamCaptor));

        lenient().when(mockConnection.createStatement()).thenReturn(mockStatement);
        lenient().when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        lenient().when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        lenient().when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        book1 = new Book("C# In Depth", "Jon Skeet");
        book2 = new Book("Clean Code", "Robert C. Martin");
    }

    @AfterEach
    public void tearDown() throws Exception {
        System.setOut(standardOut);
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    public void givenLibrary_initializedNotEmpty_returnsCorrectBook() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("C# In Depth");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false);

        library = new Library(mockConnection);
        List<Book> books = library.viewAllBooks();

        verify(mockConnection, times(1)).createStatement();
        verify(mockPreparedStatement).executeQuery();
        verify(mockResultSet, times(2)).next();
        verify(mockResultSet).getString("title");
        verify(mockResultSet).getString("author");
        verify(mockResultSet).getBoolean("is_borrowed");

        assertEquals(1, books.size());
        assertEquals("C# In Depth", books.getFirst().getTitle());
        assertEquals("Jon Skeet", books.getFirst().getAuthor());
        assertFalse(books.getFirst().isBorrowed());
    }

    @Test
    public void givenLibrary_initializedEmpty_returnsEmptyDatabase() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        library = new Library(mockConnection);
        List<Book> books = library.viewAllBooks();

        verify(mockConnection, times(1)).createStatement();
        verify(mockStatement).execute(anyString());
        verify(mockPreparedStatement).executeQuery();
        verify(mockResultSet, times(1)).next();
        verify(mockResultSet, never()).getString(anyString());
        verify(mockResultSet, never()).getBoolean(anyString());
        assertEquals(0, books.size());
    }

    @Test
    public void givenLibrary_throwsRunTimeException_whenDatabaseConnectionErrors() throws SQLException {
        when(mockConnection.createStatement()).thenThrow(new SQLException("Test message."));
        RuntimeException exception = assertThrows(RuntimeException.class, () -> new Library(mockConnection));
        assertEquals("java.sql.SQLException: Test message.", exception.getMessage());
        assertEquals("Error in connecting to the library database: Test message.", outputStreamCaptor.toString().trim());
    }

    @Test
    public void givenLibraryConstructor_throwsRuntimeException_whenReadingInvalidBookDataFromDatabase() throws SQLException {
        library = new Library(mockConnection);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> library.viewAllBooks());
    }

    @Test
    public void givenLibraryConstructor_returnsBookBorrowed_whenInstantiatingDataBase() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("C# In Depth");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(true);

        library = new Library(mockConnection);

        List<Book> books = library.viewAllBooks();
        verify(mockConnection, times(1)).createStatement();
        verify(mockConnection, times(1)).prepareStatement(anyString());
        verify(mockPreparedStatement, times(1)).executeQuery();
        verify(mockResultSet, times(2)).next();
        verify(mockResultSet).getString("title");
        verify(mockResultSet).getString("author");
        verify(mockResultSet).getBoolean("is_borrowed");
        assertEquals(1, books.size());

        Book book = books.getFirst();
        assertEquals("C# In Depth", book.getTitle());
        assertEquals("Jon Skeet", book.getAuthor());
        assertTrue(book.isBorrowed());
    }

    @Test
    void givenAddBook_persistsToDatabase_withCorrectValues_andNotBorrowed() throws SQLException, InvalidParameterException {
        Book book = new Book("C# In Depth", "Jon Skeet");
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        library = new Library(mockConnection);

        boolean result = library.addBook(book);

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "C# In Depth");
        verify(mockPreparedStatement).setString(2, "Jon Skeet");
        verify(mockPreparedStatement).setBoolean(3, false);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void givenAddBook_returnsFalse_whenSqlExceptionThrown() throws SQLException, InvalidParameterException {
        Book book = new Book("Brave New World", "Aldous Huxley");
        library = new Library(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Test message."));

        boolean result = library.addBook(book);

        assertFalse(result);
        verify(mockPreparedStatement, never()).setString(1, "Brave New World");
        verify(mockPreparedStatement, never()).setString(2, "Aldous Huxley");
        verify(mockPreparedStatement, never()).setBoolean(3, false);
        verify(mockPreparedStatement, never()).executeUpdate();
        assertEquals("Error adding book to the library: Test message.", outputStreamCaptor.toString().trim());
    }

    @Test
    void givenAddBook_whenBookIsNull_throwsInvalidParameterException() {
        library = new Library(mockConnection);
        assertThrows(InvalidParameterException.class, () -> library.addBook(null));
    }

    @Test
    void givenTwoBooks_onViewBooks_returnsCorrectList() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("title")).thenReturn("C# In Depth", "Clean Code");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet", "Robert C. Martin");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false, false);

        library = new Library(mockConnection);

        List<Book> availableBooks = library.viewAllBooks();
        assertEquals(2, availableBooks.size());
        Book first = availableBooks.getFirst();
        assertEquals(book1.getTitle(), first.getTitle());
        assertEquals(book1.getAuthor(), first.getAuthor());

        Book second = availableBooks.get(1);
        assertEquals(book2.getTitle(), second.getTitle());
        assertEquals(book2.getAuthor(), second.getAuthor());
        verify(mockPreparedStatement).executeQuery();
        verify(mockResultSet, times(3)).next();
    }

    @Test
    void givenBookThatDoesNotExist_onSearchForBook_throwsBookNotFoundException() {
        library = new Library(mockConnection);
        assertThrows(BookNotFoundException.class, () -> library.searchBook("Nonexistent Book"));
    }

    @Test
    public void whenSearchingForBook_withNullValue_throwsInvalidParameterException() {
        library = new Library(mockConnection);
        assertThrows(InvalidParameterException.class, () -> library.searchBook(null));
    }

    @Test
    void whenSearchingForBook_withEmptyString_throwsInvalidParameterException() {
        library = new Library(mockConnection);
        assertThrows(InvalidParameterException.class, () -> library.searchBook(""));
    }

    @Test
    void whenSearchingForBook_withBlankString_throwsInvalidParameterException() {
        library = new Library(mockConnection);
        assertThrows(InvalidParameterException.class, () -> library.searchBook("    "));
    }

    @Test
    void givenBookExists_onSearchForBook_returnsCorrectBook() throws BookNotFoundException, InvalidParameterException, SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("C# In Depth");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false);

        library = new Library(mockConnection);

        List<Book> foundBooks = library.searchBook("C# In Depth");
        assertNotNull(foundBooks);
        assertEquals(1, foundBooks.size());

        Book foundBook = foundBooks.getFirst();
        assertEquals("C# In Depth", foundBook.getTitle());
        assertEquals("Jon Skeet", foundBook.getAuthor());

        verify(mockPreparedStatement).setString(1, "C# In Depth");
        verify(mockPreparedStatement).executeQuery();
        verify(mockResultSet, times(2)).next();
    }

    @Test
    void givenDatabaseError_onSearchBook_throwsRuntimeException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Database connection failed"));
        library = new Library(mockConnection);
        assertThrows(RuntimeException.class, () -> library.searchBook("Clean Code"));
    }

    @Test
    void givenSQLExceptionOnExecuteQuery_onViewAllBooks_throwsRuntimeExceptionWithOriginalCause() throws SQLException {
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Table 'books' does not exist"));
        library = new Library(mockConnection);
        assertThrows(RuntimeException.class, () -> library.viewAllBooks());
    }

    @Test
    void whenSearchingForBooksByAuthor_withNull_throwsInvalidArgumentException() {
        library = new Library(mockConnection);
        assertThrows(InvalidParameterException.class, () -> library.getBooksByAuthor(null));
    }

    @Test
    void whenSearchingForBooksByAuthor_withEmptyString_throwsInvalidArgumentException() {
        library = new Library(mockConnection);
        assertThrows(InvalidParameterException.class, () -> library.getBooksByAuthor(""));
    }

    @Test
    void whenSearchingForBooksByAuthor_withBlankString_throwsInvalidArgumentException() {
        library = new Library(mockConnection);
        assertThrows(InvalidParameterException.class, () -> library.getBooksByAuthor("     "));
    }

    @Test
    void whenSearchingForBooksByAuthor_whereAuthorHasNoBooks_throwsAuthorNotFoundException() {
        library = new Library(mockConnection);
        assertThrows(AuthorNotFoundException.class, () -> library.getBooksByAuthor("Bob"));
    }

    @Test
    void whenSearchingForBooksByAuthor_whereAuthorOneBooks_returnsCorrectBook() throws InvalidParameterException, AuthorNotFoundException, SQLException {
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false);

        library = new Library(mockConnection);
        Book book = new Book("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", "Jon Skeet");
        library.addBook(book);

        List<Book> authorBooks = library.getBooksByAuthor("Jon Skeet");
        assertEquals(1, authorBooks.size());
        Book returnedBook = authorBooks.getFirst();
        assertEquals("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", returnedBook.getTitle());
        assertEquals("Jon Skeet", returnedBook.getAuthor());
    }

    @Test
    void whenSearchingForBooksByAuthor_whereAuthorHasTwoBooks_returnsCorrectBooks_sortedAlphabetically() throws InvalidParameterException, AuthorNotFoundException, SQLException {
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("title")).thenReturn("C# In Depth", "Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false);

        library = new Library(mockConnection);
        Book book = new Book("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", "Jon Skeet");
        Book bookTwo = new Book("C# In Depth", "Jon Skeet");
        library.addBook(book);
        library.addBook(bookTwo);

        List<Book> authorBooks = library.getBooksByAuthor("Jon Skeet");
        assertEquals(2, authorBooks.size());

        Book returnBook = authorBooks.getFirst();
        assertEquals("C# In Depth", returnBook.getTitle());
        assertEquals("Jon Skeet", returnBook.getAuthor());

        Book returnBookTwo = authorBooks.get(1);
        assertEquals("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", returnBookTwo.getTitle());
        assertEquals("Jon Skeet", returnBookTwo.getAuthor());
    }

    @Test
    void whenSearchingForBooksByAuthor_whereAuthorTwoBooks_AndThereIsASecondAuthor_returnsCorrectBooks_sortedAlphabetically() throws InvalidParameterException, AuthorNotFoundException, SQLException {
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockResultSet.next()).thenReturn(
                true, true, false, // Jon Skeet's 2 books
                true, false        // Robert C. Martin's 1 book
        );
        when(mockResultSet.getString("title")).thenReturn(
                "C# In Depth",
                "Software Mistakes and Tradeoffs: How to Make Good Programming Decisions",
                "Clean Code"
        );

        when(mockResultSet.getString("author")).thenReturn(
                "Jon Skeet",
                "Jon Skeet",
                "Robert C. Martin"
        );

        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false);

        library = new Library(mockConnection);
        Book book = new Book("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", "Jon Skeet");
        Book bookTwo = new Book("C# In Depth", "Jon Skeet");
        Book bookThree = new Book("Clean Code", "Robert C. Martin");

        library.addBook(book);
        library.addBook(bookTwo);
        library.addBook(bookThree);

        List<Book> authorBooksOne = library.getBooksByAuthor("Jon Skeet");
        assertEquals(2, authorBooksOne.size());

        Book returnBookTwo = authorBooksOne.getFirst();
        assertEquals("C# In Depth", returnBookTwo.getTitle());
        assertEquals("Jon Skeet", returnBookTwo.getAuthor());

        Book returnBook = authorBooksOne.get(1);
        assertEquals("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", returnBook.getTitle());
        assertEquals("Jon Skeet", returnBook.getAuthor());

        List<Book> authorBooksTwo = library.getBooksByAuthor("Robert C. Martin");
        assertEquals(1, authorBooksTwo.size());

        Book returnedBookThree = authorBooksTwo.getFirst();
        assertEquals("Clean Code", returnedBookThree.getTitle());
        assertEquals("Robert C. Martin", returnedBookThree.getAuthor());
    }

    @Test
    void whenRequestingListOfBorrowedBooks_withNoBorrowedBooks_throwsNoBooksFoundException() {
        library = new Library(mockConnection);
        assertThrows(NoBooksBorrowedException.class, () -> library.viewBorrowedBooks());
    }

    @Test
    void givenTwoBooks_withOneBorrowed_returnsCorrectlyBorrowedBooks() throws Exception {
        library = new Library(mockConnection);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        ResultSet searchRs = mock(ResultSet.class);
        when(searchRs.next()).thenReturn(true, false);
        when(searchRs.getString("title")).thenReturn("Clean Code");
        when(searchRs.getString("author")).thenReturn("Robert C. Martin");
        when(searchRs.getBoolean("is_borrowed")).thenReturn(false);

        ResultSet borrowedRs = mock(ResultSet.class);
        when(borrowedRs.next()).thenReturn(true, false);
        when(borrowedRs.getString("title")).thenReturn("Clean Code");
        when(borrowedRs.getString("author")).thenReturn("Robert C. Martin");
        when(borrowedRs.getBoolean("is_borrowed")).thenReturn(true);

        ResultSet allRs = mock(ResultSet.class);
        when(allRs.next()).thenReturn(true, true, false);
        when(allRs.getString("title")).thenReturn("Clean Code", "The Pragmatic Programmer");
        when(allRs.getString("author")).thenReturn("Robert C. Martin", "Andrew Hunt");
        when(allRs.getBoolean("is_borrowed")).thenReturn(true, false);

        when(mockPreparedStatement.executeQuery()).thenReturn(searchRs, borrowedRs, allRs);

        assertTrue(library.borrowBook("Clean Code"));

        List<Book> borrowedBooks = library.viewBorrowedBooks();
        List<Book> allBooks = library.viewAllBooks();

        assertEquals(1, borrowedBooks.size());
        assertEquals(2, allBooks.size());
        assertEquals("Clean Code", borrowedBooks.getFirst().getTitle());
    }

    @Test
    void givenBook_alreadyBorrowed_throwsAlreadyBorrowedException() throws SQLException {
        when(mockConnection.prepareStatement(SEARCH_FOR_BOOK_Query)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("Clean Code");
        when(mockResultSet.getString("author")).thenReturn("Robert C. Martin");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(true);

        library = new Library(mockConnection);
        assertThrows(AlreadyBorrowedException.class, () -> library.borrowBook("Clean Code"));
    }

    @Test
    void testReturnBook_WhenBookDoesNotExist_throwsBookNotFoundException() throws Exception {
        library = new Library(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        ResultSet searchRs = mock(ResultSet.class);
        when(searchRs.next()).thenReturn(false);
        when(mockPreparedStatement.executeQuery()).thenReturn(searchRs);
        assertThrows(BookNotFoundException.class, () -> library.returnBook("Nonexistent Book"));
    }

    @Test
    void whenReturningBook_thatIsNotBorrowed_throwsNotBorrowedException() throws Exception {
        library = new Library(mockConnection);

        ResultSet searchRs = mock(ResultSet.class);
        when(searchRs.next()).thenReturn(true, false);
        when(searchRs.getString("title")).thenReturn("1984");
        when(searchRs.getString("author")).thenReturn("George Orwell");
        when(searchRs.getBoolean("is_borrowed")).thenReturn(false);

        doReturn(searchRs).when(mockPreparedStatement).executeQuery();
    }

    @Test
    void whenReturningBook_returnsFalse_whenSqlExceptionIsThrown() throws InvalidParameterException, SQLException, NotBorrowedException, BookNotFoundException {
        library = new Library(mockConnection);

        ResultSet searchRs = mock(ResultSet.class);
        when(searchRs.next()).thenReturn(true, false);
        when(searchRs.getString("title")).thenReturn("1984");
        when(searchRs.getString("author")).thenReturn("George Orwell");
        when(searchRs.getBoolean("is_borrowed")).thenReturn(true);

        doReturn(searchRs).when(mockPreparedStatement).executeQuery();

        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Database failure"));

        boolean result = library.returnBook("1984");

        assertFalse(result);
        assertEquals("Error returning book from library: Database failure", outputStreamCaptor.toString().trim());
    }

    @Test
    void whenReturningBook_thatDoesNotExist_throwsBookNotFoundException() {
        library = new Library(mockConnection);
        assertThrows(BookNotFoundException.class, () -> library.returnBook("Nonexistent Book"));
    }

    @Test
    void whenReturningBook_withNullValue_throwsInvalidParameterException() {
        library = new Library(mockConnection);
        assertThrows(InvalidParameterException.class, () -> library.returnBook(null));
    }

    @Test
    void whenReturningBook_withEmptyTitle_throwsInvalidParameterException() {
        library = new Library(mockConnection);
        assertThrows(InvalidParameterException.class, () -> library.returnBook(""));
    }

    @Test
    void whenReturningBook_withBlankString_throwsInvalidParameterException() {
        library = new Library(mockConnection);
        assertThrows(InvalidParameterException.class, () -> library.returnBook("    "));
    }

    @Test
    void testReturnBook_WhenBookIsBorrowed() throws Exception {
        library = new Library(mockConnection);

        ResultSet searchRs = mock(ResultSet.class);
        when(searchRs.next()).thenReturn(true, false);
        when(searchRs.getString("title")).thenReturn("1984");
        when(searchRs.getString("author")).thenReturn("George Orwell");
        when(searchRs.getBoolean("is_borrowed")).thenReturn(true);

        doReturn(searchRs).when(mockPreparedStatement).executeQuery();
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean result = library.returnBook("1984");

        assertTrue(result);
    }

    @Test
    void whenBorrowingBook_thatDoesNotExist_throwsBookNotFoundException() {
        library = new Library(mockConnection);
        assertThrows(BookNotFoundException.class, () -> library.borrowBook("Nonexistent Book"));
    }

    @Test
    void givenBorrowBook_returnsFalse_whenSqlExceptionThrown() throws Exception {
        library = new Library(mockConnection);

        ResultSet searchRs = mock(ResultSet.class);
        when(searchRs.next()).thenReturn(true, false);
        when(searchRs.getString("title")).thenReturn("Brave New World");
        when(searchRs.getString("author")).thenReturn("Aldous Huxley");
        when(searchRs.getBoolean("is_borrowed")).thenReturn(false);

        doReturn(searchRs).when(mockPreparedStatement).executeQuery();

        when(mockPreparedStatement.executeUpdate()).thenThrow(new SQLException("Test message."));

        boolean result = library.borrowBook("Brave New World");

        assertFalse(result);
        assertEquals("Error borrowing book from library: Test message.", outputStreamCaptor.toString().trim());
    }

    @Test
    void givenDatabaseError_onViewBorrowedBooks_throwsRuntimeException() throws SQLException {
        library = new Library(mockConnection);
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Database query failed"));
        assertThrows(RuntimeException.class, () -> library.viewBorrowedBooks());
    }

    @Test
    void testReturnBook_whenBookExists_butIsNotBorrowed_throwsNotBorrowedException() throws Exception {
        library = new Library(mockConnection);

        ResultSet searchRs = mock(ResultSet.class);
        when(searchRs.next()).thenReturn(true, false);
        when(searchRs.getString("title")).thenReturn("1984");
        when(searchRs.getString("author")).thenReturn("George Orwell");
        when(searchRs.getBoolean("is_borrowed")).thenReturn(false);

        doReturn(searchRs).when(mockPreparedStatement).executeQuery();

        assertThrows(NotBorrowedException.class, () -> library.returnBook("1984"));
        verify(mockPreparedStatement, never()).executeUpdate();
    }

    @Test
    void testViewAvailableBooks() throws SQLException, InvalidParameterException, NoBooksAvailableException {
        Book book1 = new Book("1984", "George Orwell");
        Book book2 = new Book("The Catcher in the Rye", "J.D. Salinger");

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockConnection.prepareStatement(ADD_BOOK_QUERY)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        library = new Library(mockConnection);
        library.addBook(book1);
        library.addBook(book2);

        when(mockConnection.prepareStatement(SELECT_ALL_AVAILABLE_BOOKS_QUERY)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("title")).thenReturn("1984", "The Catcher in the Rye");
        when(mockResultSet.getString("author")).thenReturn("George Orwell", "J.D. Salinger");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false, false);

        List<Book> availableBooks = library.viewAvailableBooks();

        assertEquals(2, availableBooks.size());
        assertEquals("1984", availableBooks.get(0).getTitle());
        assertEquals("The Catcher in the Rye", availableBooks.get(1).getTitle());
        assertFalse(availableBooks.get(0).isBorrowed());
        assertFalse(availableBooks.get(1).isBorrowed());
    }

    @Test
    void testViewAllBooks() throws SQLException, AlreadyBorrowedException, BookNotFoundException, InvalidParameterException, NoBooksAvailableException {
        Book book1 = new Book("1984", "George Orwell");
        Book book2 = new Book("The Catcher in the Rye", "J.D. Salinger");

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        library = new Library(mockConnection);

        library.addBook(book1);
        library.addBook(book2);

        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("1984");
        when(mockResultSet.getString("author")).thenReturn("George Orwell");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false);

        library.borrowBook("1984");

        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("The Catcher in the Rye");
        when(mockResultSet.getString("author")).thenReturn("J.D. Salinger");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false);

        List<Book> availableBooks = library.viewAvailableBooks();

        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("title")).thenReturn("1984", "The Catcher in the Rye");
        when(mockResultSet.getString("author")).thenReturn("George Orwell", "J.D. Salinger");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(true, false);

        List<Book> allBooks = library.viewAllBooks();

        assertEquals(1, availableBooks.size());
        assertEquals(2, allBooks.size());
    }

    @Test
    public void givenLibrary_withTwoIdenticalBooks_onBorrow_returnsOneBorrowedAndOneNot() throws SQLException, InvalidParameterException, AlreadyBorrowedException, BookNotFoundException, NoBooksBorrowedException, NoBooksAvailableException {
        library = new Library(mockConnection);
        Book book = new Book("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", "Jon Skeet");
        Book book2 = new Book("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", "Jon Skeet");

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        library.addBook(book);

        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false);

        library.borrowBook("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");

        library.addBook(book2);

        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(true);

        List<Book> borrowedBooks = library.viewBorrowedBooks();
        assertEquals(1, borrowedBooks.size());
        Book borrowedBook = borrowedBooks.getFirst();
        assertEquals("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", borrowedBook.getTitle());

        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false);

        List<Book> availableBooks = library.viewAvailableBooks();
        assertEquals("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", availableBooks.getFirst().getTitle());
    }

    @Test
    public void givenLibrary_withTwoIdenticalBooks_onBorrowingBoth_thenReturning_updatesCorrectly() throws SQLException, InvalidParameterException, AlreadyBorrowedException, BookNotFoundException, NoBooksBorrowedException, NotBorrowedException, NoBooksAvailableException {
        library = new Library(mockConnection);
        Book book1 = new Book("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", "Jon Skeet");
        Book book2 = new Book("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", "Jon Skeet");

        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        library.addBook(book1);

        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false);

        library.borrowBook("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");

        library.addBook(book2);

        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("title")).thenReturn("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", "Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet", "Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(true, false);

        library.borrowBook("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");

        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("title")).thenReturn("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", "Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet", "Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(true, true);

        List<Book> borrowedBooks = library.viewBorrowedBooks();
        assertEquals(2, borrowedBooks.size());
        Book borrowedBookOne = borrowedBooks.get(0);
        assertEquals("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", borrowedBookOne.getTitle());
        Book borrowedBookTwo = borrowedBooks.get(1);
        assertEquals("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", borrowedBookTwo.getTitle());

        when(mockResultSet.next()).thenReturn(false);
        assertThrows(NoBooksAvailableException.class, () -> library.viewAvailableBooks());

        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("title")).thenReturn("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", "Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet", "Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(true, true);

        assertTrue(library.returnBook(book1.getTitle()));

        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false);

        List<Book> availableBooks = library.viewAvailableBooks();
        assertEquals(1, availableBooks.size());
        assertEquals("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", availableBooks.getFirst().getTitle());

        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("title")).thenReturn("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(true);

        borrowedBooks = library.viewBorrowedBooks();
        assertEquals(1, borrowedBooks.size());
        assertEquals("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", borrowedBooks.getFirst().getTitle());

        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("title")).thenReturn("Software Mistakes and Tradeoffs: How to Make Good Programming Decisions", "Software Mistakes and Tradeoffs: How to Make Good Programming Decisions");
        when(mockResultSet.getString("author")).thenReturn("Jon Skeet", "Jon Skeet");
        when(mockResultSet.getBoolean("is_borrowed")).thenReturn(false, true);

        assertTrue(library.returnBook(book2.getTitle()));

        when(mockResultSet.next()).thenReturn(false);
        assertThrows(NoBooksBorrowedException.class, () -> library.viewBorrowedBooks());
    }

    @Test
    public void givenDatabaseError_whenViewAvailableBooks_thenThrowsRuntimeException() throws SQLException {
        library = new Library(mockConnection);

        when(mockConnection.prepareStatement(SELECT_ALL_AVAILABLE_BOOKS_QUERY)).thenThrow(new SQLException("Database connection error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> library.viewAvailableBooks());

        assertTrue(exception.getMessage().contains("Invalid argument when reading from database"));
        assertNotNull(exception.getCause());
    }
}