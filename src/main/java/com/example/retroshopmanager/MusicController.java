package com.example.retroshopmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class MusicController {

    @FXML private TableView<MusicItem> musicTable;
    @FXML private TableColumn<MusicItem, Integer> idColumn;
    @FXML private TableColumn<MusicItem, String> titleColumn;
    @FXML private TableColumn<MusicItem, String> artistColumn;
    @FXML private TableColumn<MusicItem, Integer> yearColumn;

    // Lista przechowująca płyty w pamięci (zgodnie z założeniami projektu)
    private ObservableList<MusicItem> musicList;
    // Zmienna do symulowania ID bazy danych
    private int nextId = 4;

    @FXML
    public void initialize() {
        // Powiązanie kolumn z polami w klasie MusicItem
        idColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
        artistColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("artist"));
        yearColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("releaseYear"));

        // Przykładowe dane startowe
        musicList = FXCollections.observableArrayList(
                new MusicItem(1, "The Dark Side of the Moon", "Pink Floyd", 1973),
                new MusicItem(2, "Abbey Road", "The Beatles", 1969),
                new MusicItem(3, "Rumours", "Fleetwood Mac", 1977)
        );

        musicTable.setItems(musicList);
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
                // Usuwamy z pamięci (w przyszłości tu dacie zapytanie SQL DELETE)
                musicList.remove(selectedItem);
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

        // Konwersja kliknięcia "Zapisz" na nowy obiekt MusicItem
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    int year = Integer.parseInt(yearField.getText());
                    // W przyszłości ID będzie generowała baza danych. Na razie robimy to ręcznie.
                    int id = (item == null) ? nextId++ : item.getId();
                    return new MusicItem(id, titleField.getText(), artistField.getText(), year);
                } catch (NumberFormatException e) {
                    // Zabezpieczenie, jeśli ktoś wpisze litery zamiast roku
                    Alert error = new Alert(Alert.AlertType.ERROR, "Rok wydania musi być liczbą!");
                    error.showAndWait();
                    return null;
                }
            }
            return null;
        });

        // Oczekiwanie na akcję użytkownika (kliknięcie w oknie)
        Optional<MusicItem> result = dialog.showAndWait();

        result.ifPresent(newRecord -> {
            if (item == null) {
                // DODAWANIE: dodajemy nową płytę do listy
                musicList.add(newRecord);
            } else {
                // EDYCJA: podmieniamy dane w istniejącej płycie
                item.setTitle(newRecord.getTitle());
                item.setArtist(newRecord.getArtist());
                item.setReleaseYear(newRecord.getReleaseYear());
                musicTable.refresh(); // Odświeżenie widoku tabeli
            }
        });
    }
}