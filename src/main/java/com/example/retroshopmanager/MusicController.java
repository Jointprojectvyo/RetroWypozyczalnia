package com.example.retroshopmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.sql.*;
import java.util.Optional;

public class MusicController {

    @FXML private TableView<MusicItem> musicTable;
    @FXML private TableColumn<MusicItem, Integer> idColumn;
    @FXML private TableColumn<MusicItem, String> titleColumn;
    @FXML private TableColumn<MusicItem, String> artistColumn;
    @FXML private TableColumn<MusicItem, Integer> yearColumn;

    // Lista przechowująca płyty – teraz synchronizowana z bazą danych
    private ObservableList<MusicItem> musicList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Powiązanie kolumn z polami w klasie MusicItem
        idColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
        artistColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("artist"));
        yearColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("releaseYear"));

        // Ładowanie danych z bazy danych SQLite zamiast sztywno wpisanych linii
        loadMusicFromDatabase();

        musicTable.setItems(musicList);
    }

    /**
     * Pobiera wszystkie rekordy z tabeli 'music' i zapisuje je w pamięci RAM (musicList).
     */
    private void loadMusicFromDatabase() {
        musicList.clear();
        String sql = "SELECT * FROM music";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                musicList.add(new MusicItem(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getInt("release_year")
                ));
            }

            // Jeśli baza danych jest całkowicie pusta, wstawiamy domyślne dane startowe
            if (musicList.isEmpty()) {
                insertDefaultMusic();
                loadMusicFromDatabase();
            }
        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Błąd: Nie udało się pobrać muzyki z bazy danych!");
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    /**
     * Zasila bazę danych domyślnymi rekordami muzycznymi przy pierwszym uruchomieniu.
     */
    private void insertDefaultMusic() {
        String sql = "INSERT INTO music(title, artist, release_year) VALUES(?,?,?)";
        String[][] defaultAlbums = {
                {"The Dark Side of the Moon", "Pink Floyd", "1973"},
                {"Abbey Road", "The Beatles", "1969"},
                {"Rumours", "Fleetwood Mac", "1977"}
        };

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String[] album : defaultAlbums) {
                pstmt.setString(1, album[0]);
                pstmt.setString(2, album[1]);
                pstmt.setInt(3, Integer.parseInt(album[2]));
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddAction() {
        // Wywołujemy nasze okienko z pustymi polami (null = nowy element)
        showMusicDialog(null);
    }

    @FXML
    private void handleEditAction() {
        // Pobieramy zaznaczoną płytę z tabeli
        MusicItem selectedItem = musicTable.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            // Wywołujemy okienko wypełnione danymi zaznaczonej płyty
            showMusicDialog(selectedItem);
        } else {
            // Jeśli użytkownik nic nie zaznaczył, pokazujemy ostrzeżenie
            Alert alert = new Alert(Alert.AlertType.WARNING, "Proszę zaznaczyć płytę do edycji w tabeli.");
            alert.setHeaderText("Brak zaznaczenia");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleDeleteAction() {
        MusicItem selectedItem = musicTable.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            // Pytamy o potwierdzenie usunięcia
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Czy na pewno chcesz usunąć: " + selectedItem.getTitle() + "?");
            alert.setHeaderText("Potwierdzenie usunięcia");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Usuwanie pozycji bezpośrednio z bazy danych przy użyciu SQL DELETE
                String sql = "DELETE FROM music WHERE id = ?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, selectedItem.getId());
                    pstmt.executeUpdate();

                    // Usuwamy z pamięci podręcznej UI
                    musicList.remove(selectedItem);
                } catch (SQLException e) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Błąd: Nie udało się usunąć pozycji z bazy danych.");
                    errorAlert.showAndWait();
                    e.printStackTrace();
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Proszę zaznaczyć płytę do usunięcia.");
            alert.setHeaderText("Brak zaznaczenia");
            alert.showAndWait();
        }
    }

    // --- METODA POMOCNICZA: GENEROWANIE OKNA FORMULARZA ---
    private void showMusicDialog(MusicItem item) {
        Dialog<MusicItem> dialog = new Dialog<>();
        dialog.setTitle(item == null ? "Dodaj nową płytę" : "Edytuj płytę");
        dialog.setHeaderText("Wprowadź dane albumu muzycznego");

        // Przyciski Zapisz i Anuluj
        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Układ pól tekstowych
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField titleField = new TextField();
        titleField.setPromptText("Tytuł");
        TextField artistField = new TextField();
        artistField.setPromptText("Wykonawca");
        TextField yearField = new TextField();
        yearField.setPromptText("Rok np. 1980");

        // Jeśli to edycja (item nie jest nullem), wypełniamy pola obecnymi danymi
        if (item != null) {
            titleField.setText(item.getTitle());
            artistField.setText(item.getArtist());
            yearField.setText(String.valueOf(item.getReleaseYear()));
        }

        grid.add(new Label("Tytuł:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Wykonawca:"), 0, 1);
        grid.add(artistField, 1, 1);
        grid.add(new Label("Rok wydania:"), 0, 2);
        grid.add(yearField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Konwersja kliknięcia "Zapisz" na operację bazodanową i walidacja danych
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    // OBSŁUGA WYJĄTKÓW / WALIDACJA: Sprawdzenie pustych pól tekstowych
                    if (titleField.getText().trim().isEmpty() ||
                            artistField.getText().trim().isEmpty() ||
                            yearField.getText().trim().isEmpty()) {

                        Alert emptyAlert = new Alert(Alert.AlertType.ERROR, "Błąd: Wszystkie pola muszą być wypełnione!");
                        emptyAlert.setHeaderText("Błąd walidacji");
                        emptyAlert.showAndWait();
                        return null; // Zwrócenie null przerywa proces i nie zamyka okna modalnego
                    }

                    int year = Integer.parseInt(yearField.getText());

                    if (item == null) {
                        // TRYB: DODAWANIE NOWEGO REKORDU DO BAZY DANYCH
                        String sql = "INSERT INTO music(title, artist, release_year) VALUES(?,?,?)";
                        try (Connection conn = DatabaseManager.getConnection();
                             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                            pstmt.setString(1, titleField.getText());
                            pstmt.setString(2, artistField.getText());
                            pstmt.setInt(3, year);
                            pstmt.executeUpdate();

                            // Pobieramy ID wygenerowane automatycznie przez SQLite
                            ResultSet generatedKeys = pstmt.getGeneratedKeys();
                            int newId = generatedKeys.next() ? generatedKeys.getInt(1) : 0;
                            return new MusicItem(newId, titleField.getText(), artistField.getText(), year);
                        }
                    } else {
                        // TRYB: EDYCJA ISTNIEJĄCEGO REKORDU W BAZIE DANYCH
                        String sql = "UPDATE music SET title=?, artist=?, release_year=? WHERE id=?";
                        try (Connection conn = DatabaseManager.getConnection();
                             PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setString(1, titleField.getText());
                            pstmt.setString(2, artistField.getText());
                            pstmt.setInt(3, year);
                            pstmt.setInt(4, item.getId());
                            pstmt.executeUpdate();

                            // Zwracamy zaktualizowany obiekt modyfikujący listę podręczną
                            return new MusicItem(item.getId(), titleField.getText(), artistField.getText(), year);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Zabezpieczenie, jeśli ktoś wpisze litery zamiast roku
                    Alert error = new Alert(Alert.AlertType.ERROR, "Rok wydania musi być liczbą!");
                    error.setHeaderText("Błąd formatu danych");
                    error.showAndWait();
                    return null;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        });

        // Oczekiwanie na akcję użytkownika (kliknięcie w oknie)
        Optional<MusicItem> result = dialog.showAndWait();

        result.ifPresent(newRecord -> {
            if (item == null) {
                // DODAWANIE: dodajemy nową płytę do listy widoku
                musicList.add(newRecord);
            } else {
                // EDYCJA: podmieniamy dane bezpośrednio w obiekcie tabeli
                item.setTitle(newRecord.getTitle());
                item.setArtist(newRecord.getArtist());
                item.setReleaseYear(newRecord.getReleaseYear());
                musicTable.refresh(); // Odświeżenie widoku tabeli
            }
        });
    }
}