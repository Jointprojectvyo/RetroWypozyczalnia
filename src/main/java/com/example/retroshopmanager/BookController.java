package com.example.retroshopmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.sql.*;
import java.time.Year;
import java.util.Optional;

public class BookController {

    // --- ELEMENTY INTERFEJSU GRAFICZNEGO (FXML) ---
    @FXML private TableView<BookItem> bookTable;
    @FXML private TableColumn<BookItem, Integer> idColumn;
    @FXML private TableColumn<BookItem, String> titleColumn;
    @FXML private TableColumn<BookItem, String> authorColumn;
    @FXML private TableColumn<BookItem, Integer> yearColumn;
    @FXML private TableColumn<BookItem, String> originColumn;
    @FXML private ComboBox<String> filterComboBox;

    // KOLEKCJE W PAMIĘCI PROGRAMU
    // Główna lista przechowująca wszystkie książki pobrane z bazy danych
    private ObservableList<BookItem> bookMasterList = FXCollections.observableArrayList();
    // Lista opakowująca (wrapper), która pozwala na dynamiczne filtrowanie wierszy w TableView
    private FilteredList<BookItem> filteredBookList;

    /**
     * Metoda inicjalizująca kontroler, wywoływana automatycznie po załadowaniu pliku FXML.
     */
    @FXML
    public void initialize() {
        // Łączenie kolumn tabeli z odpowiednimi polami właściwości klasy BookItem
        idColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("author"));
        yearColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("releaseYear"));
        originColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("isForeign"));

        // Konfigurowanie pola kombi (ComboBox) do wyboru kryterium filtrowania
        filterComboBox.setItems(FXCollections.observableArrayList("Wszyscy autorzy", "Polscy autorzy", "Zagraniczni autorzy"));
        filterComboBox.getSelectionModel().selectFirst(); // Domyślne wybranie pierwszej opcji

        // Ładowanie danych z relacyjnej bazy danych SQLite podczas startu zakładki
        loadBooksFromDatabase();

        // Zainicjowanie FilteredList na podstawie naszej głównej listy master
        filteredBookList = new FilteredList<>(bookMasterList, p -> true);
        // Przekazanie filtrowanej listy jako źródła danych dla komponentu TableView
        bookTable.setItems(filteredBookList);
    }

    /**
     * Pobiera wszystkie rekordy z tabeli 'books' i zapisuje je w pamięci RAM (bookMasterList).
     */
    private void loadBooksFromDatabase() {
        // Czyszczenie listy w pamięci przed załadowaniem świeżych danych z bazy
        bookMasterList.clear();

        // Zapytanie SQL pobierające wszystkie rekordy z tabeli książek
        String sql = "SELECT * FROM books";

        // Otwarcie połączenia, utworzenie obiektu Statement i wykonanie zapytania (try-with-resources automatycznie zamknie zasoby)
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // Przetwarzanie wyników zapytania w pętli — wiersz po wierszu
            while (rs.next()) {
                // Tworzenie nowego obiektu BookItem na podstawie danych z bazy i dodawanie go do listy
                bookMasterList.add(new BookItem(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("release_year"),
                        rs.getString("is_foreign")
                ));
            }

            // Jeśli baza danych jest całkowicie pusta (np. pierwsze uruchomienie aplikacji)
            if (bookMasterList.isEmpty()) {
                insertDefaultBooks(); // Wstawianie 7 domyślnych książek demonstracyjnych
                loadBooksFromDatabase(); // Ponowne załadowanie danych po uzupełnieniu bazy
            }

        } catch (SQLException e) {
            // Obsługa ewentualnych błędów związanych z dostępem do bazy danych
            e.printStackTrace();
        }
    }

    /**
     * Zasila bazę danych domyślnymi rekordami (seeding) przy pierwszym uruchomieniu projektu.
     */
    private void insertDefaultBooks() {
        // Szablon zapytania SQL do wstawiania nowego rekordu z użyciem symboli wieloznacznych
        String sql = "INSERT INTO books(title, author, release_year, is_foreign) VALUES(?,?,?,?)";

        // Tablica dwuwymiarowa zawierająca 7 domyślnych pozycji książkowych (dane testowe)
        String[][] defaultBooks = {
                {"Wiedźmin", "Andrzej Sapkowski", "1990", "POLSKI"},
                {"Lalka", "Bolesław Prus", "1890", "POLSKI"},
                {"Pan Tadeusz", "Adam Mickiewicz", "1834", "POLSKI"},
                {"Zbrodnia i kara", "Fiodor Dostojewski", "1866", "ZAGRANICZNY"},
                {"Harry Potter", "J.K. Rowling", "1997", "ZAGRANICZNY"},
                {"Sto lat samotności", "Gabriel García Márquez", "1967", "ZAGRANICZNY"},
                {"Stary człowiek i morze", "Ernest Hemingway", "1952", "ZAGRANICZNY"}
        };

        // Inicjalizacja połączenia oraz przygotowanie sparametryzowanego zapytania SQL (PreparedStatement)
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Iteracja przez każdy element tablicy z danymi domyślnymi
            for (String[] book : defaultBooks) {
                pstmt.setString(1, book[0]); // Przypisanie tytułu książki do pierwszego parametru (?)
                pstmt.setString(2, book[1]); // Przypisanie autora do drugiego parametru (?)
                pstmt.setInt(3, Integer.parseInt(book[2])); // Konwersja String na int i przypisanie roku wydania do trzeciego parametru (?)
                pstmt.setString(4, book[3]); // Przypisanie flagi pochodzenia do czwartego parametru (?)

                // Wykonanie zapytania modyfikującego bazę danych (wstawienie wiersza)
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            // Logowanie błędów w przypadku niepowodzenia operacji zapisu w bazie danych
            e.printStackTrace();
        }
    }

    /**
     * Akcja wywoływana przy zmianie wartości w ComboBox. Filtruje listę książek "na żywo".
     */
    @FXML
    private void handleFilterAction() {
        // Pobranie aktualnie wybranej wartości z komponentu ComboBox
        String selectedFilter = filterComboBox.getValue();

        // Zabezpieczenie przed błędem NullPointerException, jeśli lista filtrowana nie została zainicjalizowana
        if (filteredBookList == null) return;

        // Ustawienie warunku filtrowania (orzecznika/predykatu) dla listy dynamicznej w pamięci programu
        filteredBookList.setPredicate(book -> {
            // Jeśli filtr nie został wybrany lub wybrano opcję "Wszyscy autorzy", wyświetlaj każdy rekord
            if (selectedFilter == null || selectedFilter.equals("Wszyscy autorzy")) {
                return true;
            }
            // Jeśli wybrano "Polscy autorzy", dopasuj tylko te książki, które mają flagę "POLSKI"
            if (selectedFilter.equals("Polscy autorzy") && book.getIsForeign().equals("POLSKI")) {
                return true;
            }
            // W przeciwnym wypadku (gdy wybrano "Zagraniczni autorzy"), dopasuj tylko rekordy z flagą "ZAGRANICZNY"
            return selectedFilter.equals("Zagraniczni autorzy") && book.getIsForeign().equals("ZAGRANICZNY");
        });
    }

    /**
     * Obsługa przycisku "Dodaj". Otwiera pusty formularz.
     */
    @FXML
    private void handleAddAction() {
        // Wywołanie okna dialogowego z wartością null oznacza chęć dodania nowego rekordu
        showBookDialog(null);
    }

    /**
     * Obsługa przycisku "Edytuj". Otwiera formularz z danymi wybranej książki.
     */
    @FXML
    private void handleEditAction() {
        // Pobranie aktualnie zaznaczonego wiersza z tabeli
        BookItem selectedItem = bookTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            // Jeśli obiekt został zaznaczony, przekazujemy go do okna edycji
            showBookDialog(selectedItem);
        } else {
            // W przypadku braku selekcji, wyświetlamy okno ostrzegawcze
            showWarning("Proszę zaznaczyć książkę do edycji.");
        }
    }

    /**
     * Obsługa przycisku "Usuń". Usuwa rekord z bazy danych oraz z widoku tabeli.
     */
    @FXML
    private void handleDeleteAction() {
        // Pobranie zaznaczonego obiektu przeznaczonego do usunięcia
        BookItem selectedItem = bookTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            // Wyświetlenie okna potwierdzenia (Confirmation Alert) w celu uniknięcia przypadkowego usunięcia
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Czy na pewno chcesz usunąć: " + selectedItem.getTitle() + "?");
            Optional<ButtonType> result = alert.showAndWait();

            // Jeżeli użytkownik kliknął przycisk OK, następuje fizyczne usunięcie
            if (result.isPresent() && result.get() == ButtonType.OK) {
                String sql = "DELETE FROM books WHERE id = ?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    // Powiązanie ID usuwanego obiektu z parametrem zapytania SQL
                    pstmt.setInt(1, selectedItem.getId());
                    pstmt.executeUpdate();

                    // Aktualizacja głównej kolekcji w pamięci programu (automatycznie odświeża widok TableView)
                    bookMasterList.remove(selectedItem);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            showWarning("Proszę zaznaczyć książkę do usunięcia.");
        }
    }

    /**
     * Metoda pomocnicza generująca i obsługująca okno modalne formularza (Dodaj/Edytuj).
     */
    private void showBookDialog(BookItem item) {
        // Inicjalizacja modalnego okna dialogowego zwracającego obiekt typu BookItem
        Dialog<BookItem> dialog = new Dialog<>();
        // Dynamiczne ustawianie tytułu okna w zależności od trybu (Dodaj / Edytuj)
        dialog.setTitle(item == null ? "Dodaj nową książkę" : "Edytuj książkę");

        // Definiowanie standardowych przycisków formularza: Zapisz oraz Anuluj
        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Tworzenie siatki (GridPane) do wyrównania etykiet i pól tekstowych formularza
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Inicjalizacja pól tekstowych dla danych wejściowych
        TextField titleField = new TextField();
        TextField authorField = new TextField();
        TextField yearField = new TextField();

        // Tworzenie listy rozwijanej dla flagi pochodzenia autora
        ComboBox<String> originBox = new ComboBox<>(FXCollections.observableArrayList("POLSKI", "ZAGRANICZNY"));
        originBox.getSelectionModel().selectFirst(); // Domyślne zaznaczenie pierwszej opcji

        // Jeśli okno uruchomiono w trybie edycji (item != null), wypełniamy formularz istniejącymi danymi
        if (item != null) {
            titleField.setText(item.getTitle());
            authorField.setText(item.getAuthor());
            yearField.setText(String.valueOf(item.getReleaseYear()));
            originBox.getSelectionModel().select(item.getIsForeign());
        }

        // Układanie komponentów GUI w odpowiednich komórkach siatki (kolumna, wiersz)
        grid.add(new Label("Tytuł:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Autor:"), 0, 1);
        grid.add(authorField, 1, 1);
        grid.add(new Label("Rok wydania:"), 0, 2);
        grid.add(yearField, 1, 2);
        grid.add(new Label("Pochodzenie:"), 0, 3);
        grid.add(originBox, 1, 3);

        // Osadzenie przygotowanej siatki z polami wewnątrz okna dialogowego
        dialog.getDialogPane().setContent(grid);

        // Konwerter wyniku — decyduje co zrobić po kliknięciu przycisku przez użytkownika
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    if (titleField.getText().trim().isEmpty() ||
                            authorField.getText().trim().isEmpty() ||
                            yearField.getText().trim().isEmpty()) {

                        showError("Błąd: Wszystkie pola (Tytuł, Autor, Rok wydania) muszą być wypełnione!");
                        return null; // Blokuje zamknięcie okna i przerywa zapis
                    }
                    // Próba parsowania wprowadzonego roku (Walidacja formatu liczbowego)
                    int year = Integer.parseInt(yearField.getText());
                    int currentYear = Year.now().getValue(); // Pobranie aktualnego roku systemowego

                    // BIZNESOWA WALIDACJA ROKU: Blokowanie wpisania lat przyszłych
                    if (year > currentYear) {
                        showError("Rok wydania nie może być większy niż obecny rok (" + currentYear + ")!");
                        return null; // Przerwanie zapisu, okno dialogowe nie zamyka się
                    }

                    // TRYB: DODAWANIE NOWEGO REKORDU
                    if (item == null) {
                        String sql = "INSERT INTO books(title, author, release_year, is_foreign) VALUES(?,?,?,?)";
                        try (Connection conn = DatabaseManager.getConnection();
                             // Żądanie zwrotu wygenerowanego klucza głównego (id)
                             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                            pstmt.setString(1, titleField.getText());
                            pstmt.setString(2, authorField.getText());
                            pstmt.setInt(3, year);
                            pstmt.setString(4, originBox.getValue());
                            pstmt.executeUpdate();

                            // Pobranie nowo utworzonego identyfikatora ID z bazy danych
                            ResultSet generatedKeys = pstmt.getGeneratedKeys();
                            int newId = generatedKeys.next() ? generatedKeys.getInt(1) : 0;

                            // Zwrócenie w pełni utworzonego nowego obiektu
                            return new BookItem(newId, titleField.getText(), authorField.getText(), year, originBox.getValue());
                        }
                    } else {
                        // TRYB: EDYCJA ISTNIEJĄCEGO REKORDU
                        String sql = "UPDATE books SET title = ?, author = ?, release_year = ?, is_foreign = ? WHERE id = ?";
                        try (Connection conn = DatabaseManager.getConnection();
                             PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setString(1, titleField.getText());
                            pstmt.setString(2, authorField.getText());
                            pstmt.setInt(3, year);
                            pstmt.setString(4, originBox.getValue());
                            pstmt.setInt(5, item.getId());
                            pstmt.executeUpdate();

                            // Synchronizacja zmienionych wartości bezpośrednio w edytowanym obiekcie modelu
                            item.setTitle(titleField.getText());
                            item.setAuthor(authorField.getText());
                            item.setReleaseYear(year);
                            item.setIsForeign(originBox.getValue());
                        }
                    }
                } catch (NumberFormatException e) {
                    // Przechwycenie błędu, jeśli w polu roku wpisano tekst zamiast liczb
                    showError("Rok wydania musi być liczbą!");
                    return null;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return null; // W przypadku kliknięcia "Anuluj" zwracana jest wartość null
        });

        // Otwarcie okna w trybie blokującym interfejs (modalnym) i oczekiwanie na decyzję użytkownika
        Optional<BookItem> result = dialog.showAndWait();
        // Sprawdzenie czy konwerter zwrócił poprawnie utworzony obiekt nowej książki
        result.ifPresent(newRecord -> {
            // Dodanie rekordu do głównej listy, o ile nie został tam dodany wcześniej
            if (!bookMasterList.contains(newRecord)) {
                bookMasterList.add(newRecord);
            }
            // Wymuszenie graficznego przerysowania tabeli w celu wyświetlenia zmian
            bookTable.refresh();
        });
    }

    // Pomocnicza metoda generująca standardowe komunikaty ostrzegawcze (Warning Alert)
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText("Brak zaznaczenia");
        alert.showAndWait();
    }

    // Pomocnicza metoda generująca komunikaty o błędach krytycznych/walidacyjnych (Error Alert)
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Błąd walidacji");
        alert.showAndWait();
    }
}