package com.example.retroshopmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
    @FXML private TableColumn<MusicItem, String> genreColumn;
    @FXML private ComboBox<String> filterComboBox;

    private ObservableList<MusicItem> musicMasterList = FXCollections.observableArrayList();
    private FilteredList<MusicItem> filteredMusicList;
    private final ObservableList<String> genresList = FXCollections.observableArrayList("Rock", "Pop", "Jazz", "Inne");

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
        artistColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("artist"));
        yearColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("releaseYear"));
        genreColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("genre"));

        filterComboBox.setItems(FXCollections.observableArrayList("Wszystkie", "Rock", "Pop", "Jazz", "Inne"));
        filterComboBox.getSelectionModel().selectFirst();

        loadMusicFromDatabase();

        filteredMusicList = new FilteredList<>(musicMasterList, p -> true);
        musicTable.setItems(filteredMusicList);
    }

    private void loadMusicFromDatabase() {
        musicMasterList.clear();
        String sql = "SELECT * FROM music";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                musicMasterList.add(new MusicItem(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getInt("release_year"),
                        rs.getString("genre")
                ));
            }

            if (musicMasterList.isEmpty()) {
                insertDefaultMusic();
                loadMusicFromDatabase();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertDefaultMusic() {
        String sql = "INSERT INTO music(title, artist, release_year, genre) VALUES(?,?,?,?)";
        String[][] defaultAlbums = {
                {"Thriller", "Michael Jackson", "1982", "Pop"},
                {"A Kind of Magic", "Queen", "1986", "Rock"},
                {"The Dark Side of the Moon", "Pink Floyd", "1973", "Rock"}
        };

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String[] album : defaultAlbums) {
                pstmt.setString(1, album[0]);
                pstmt.setString(2, album[1]);
                pstmt.setInt(3, Integer.parseInt(album[2]));
                pstmt.setString(4, album[3]);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFilterAction() {
        String selectedFilter = filterComboBox.getValue();
        if (filteredMusicList == null) return;

        filteredMusicList.setPredicate(music -> {
            if (selectedFilter == null || selectedFilter.equals("Wszystkie")) {
                return true;
            }
            return selectedFilter.equals(music.getGenre());
        });
    }

    @FXML
    private void handleAddAction() {
        showMusicDialog(null);
    }

    @FXML
    private void handleEditAction() {
        MusicItem selectedItem = musicTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            showMusicDialog(selectedItem);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Proszę zaznaczyć płytę do edycji.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleDeleteAction() {
        MusicItem selectedItem = musicTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Usunąć: " + selectedItem.getTitle() + "?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                String sql = "DELETE FROM music WHERE id = ?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, selectedItem.getId());
                    pstmt.executeUpdate();
                    musicMasterList.remove(selectedItem);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Proszę zaznaczyć płytę do usunięcia.");
            alert.showAndWait();
        }
    }

    private void showMusicDialog(MusicItem item) {
        Dialog<MusicItem> dialog = new Dialog<>();
        dialog.setTitle(item == null ? "Dodaj nową płytę" : "Edytuj płytę");

        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField titleField = new TextField();
        TextField artistField = new TextField();
        TextField yearField = new TextField();

        ComboBox<String> genreBox = new ComboBox<>(genresList);
        genreBox.getSelectionModel().selectFirst();

        if (item != null) {
            titleField.setText(item.getTitle());
            artistField.setText(item.getArtist());
            yearField.setText(String.valueOf(item.getReleaseYear()));
            genreBox.getSelectionModel().select(item.getGenre());
        }

        grid.add(new Label("Tytuł:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Wykonawca:"), 0, 1);
        grid.add(artistField, 1, 1);
        grid.add(new Label("Rok wydania:"), 0, 2);
        grid.add(yearField, 1, 2);
        grid.add(new Label("Gatunek:"), 0, 3);
        grid.add(genreBox, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    if (titleField.getText().trim().isEmpty() || artistField.getText().trim().isEmpty() || yearField.getText().trim().isEmpty()) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Wszystkie pola muszą być wypełnione!");
                        alert.showAndWait();
                        return null;
                    }
                    int year = Integer.parseInt(yearField.getText());
                    String genre = genreBox.getValue();

                    if (item == null) {
                        String sql = "INSERT INTO music(title, artist, release_year, genre) VALUES(?,?,?,?)";
                        try (Connection conn = DatabaseManager.getConnection();
                             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                            pstmt.setString(1, titleField.getText());
                            pstmt.setString(2, artistField.getText());
                            pstmt.setInt(3, year);
                            pstmt.setString(4, genre);
                            pstmt.executeUpdate();

                            ResultSet keys = pstmt.getGeneratedKeys();
                            int newId = keys.next() ? keys.getInt(1) : 0;
                            return new MusicItem(newId, titleField.getText(), artistField.getText(), year, genre);
                        }
                    } else {
                        String sql = "UPDATE music SET title=?, artist=?, release_year=?, genre=? WHERE id=?";
                        try (Connection conn = DatabaseManager.getConnection();
                             PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.setString(1, titleField.getText());
                            pstmt.setString(2, artistField.getText());
                            pstmt.setInt(3, year);
                            pstmt.setString(4, genre);
                            pstmt.setInt(5, item.getId());
                            pstmt.executeUpdate();

                            return new MusicItem(item.getId(), titleField.getText(), artistField.getText(), year, genre);
                        }
                    }
                } catch (NumberFormatException e) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Rok wydania musi być liczbą!");
                    error.showAndWait();
                    return null;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        });

        Optional<MusicItem> result = dialog.showAndWait();
        result.ifPresent(newRecord -> {
            if (item == null) {
                musicMasterList.add(newRecord);
            } else {
                item.setTitle(newRecord.getTitle());
                item.setArtist(newRecord.getArtist());
                item.setReleaseYear(newRecord.getReleaseYear());
                item.setGenre(newRecord.getGenre());
                musicTable.refresh();
            }
        });
    }
}