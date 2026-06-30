package com.example.retroshopmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Year;
import java.util.Optional;

public class MovieController {
    @FXML private TableView<MovieItem> movieTable;
    @FXML private TableColumn<MovieItem, Integer> idColumn;
    @FXML private TableColumn<MovieItem, String> titleColumn;
    @FXML private TableColumn<MovieItem, String> directorColumn;
    @FXML private TableColumn<MovieItem, Integer> yearColumn;
    @FXML private TableColumn<MovieItem, Integer> quantityColumn;

    // Lista w pamieci pozwala szybko odswiezac tabele po zmianach w bazie.
    private final ObservableList<MovieItem> movieList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        directorColumn.setCellValueFactory(new PropertyValueFactory<>("director"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("releaseYear"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        loadMoviesFromDatabase();
        movieTable.setItems(movieList);
    }

    private void loadMoviesFromDatabase() {
        movieList.clear();
        String sql = "SELECT * FROM movies ORDER BY title";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                movieList.add(new MovieItem(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("director"),
                        rs.getInt("release_year"),
                        rs.getInt("quantity")
                ));
            }

            if (movieList.isEmpty()) {
                insertDefaultMovies();
                loadMoviesFromDatabase();
            }
        } catch (SQLException e) {
            showError("Nie udalo sie pobrac filmow z bazy danych.");
            e.printStackTrace();
        }
    }

    private void insertDefaultMovies() {
        String sql = "INSERT INTO movies(title, director, release_year, quantity) VALUES(?,?,?,?)";
        String[][] movies = {
                {"Blade Runner", "Ridley Scott", "1982", "3"},
                {"Casablanca", "Michael Curtiz", "1942", "2"},
                {"Matrix", "Lana Wachowski, Lilly Wachowski", "1999", "4"}
        };

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String[] movie : movies) {
                pstmt.setString(1, movie[0]);
                pstmt.setString(2, movie[1]);
                pstmt.setInt(3, Integer.parseInt(movie[2]));
                pstmt.setInt(4, Integer.parseInt(movie[3]));
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddAction() {
        showMovieDialog(null);
    }

    @FXML
    private void handleEditAction() {
        MovieItem selectedItem = movieTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showWarning("Prosze zaznaczyc film do edycji.");
            return;
        }
        showMovieDialog(selectedItem);
    }

    @FXML
    private void handleDeleteAction() {
        MovieItem selectedItem = movieTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showWarning("Prosze zaznaczyc film do usuniecia.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Czy na pewno chcesz usunac film: " + selectedItem.getTitle() + "?");
        alert.setHeaderText("Potwierdzenie usuniecia");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM movies WHERE id = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, selectedItem.getId());
                pstmt.executeUpdate();
                movieList.remove(selectedItem);
            } catch (SQLException e) {
                showError("Nie udalo sie usunac filmu.");
                e.printStackTrace();
            }
        }
    }

    private void showMovieDialog(MovieItem item) {
        Dialog<MovieItem> dialog = new Dialog<>();
        dialog.setTitle(item == null ? "Dodaj film" : "Edytuj film");

        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField titleField = new TextField();
        TextField directorField = new TextField();
        TextField yearField = new TextField();
        TextField quantityField = new TextField();

        if (item != null) {
            titleField.setText(item.getTitle());
            directorField.setText(item.getDirector());
            yearField.setText(String.valueOf(item.getReleaseYear()));
            quantityField.setText(String.valueOf(item.getQuantity()));
        }

        grid.add(new Label("Tytul:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Rezyser:"), 0, 1);
        grid.add(directorField, 1, 1);
        grid.add(new Label("Rok wydania:"), 0, 2);
        grid.add(yearField, 1, 2);
        grid.add(new Label("Stan:"), 0, 3);
        grid.add(quantityField, 1, 3);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton != saveButtonType) {
                return null;
            }

            try {
                int year = Integer.parseInt(yearField.getText());
                int quantity = Integer.parseInt(quantityField.getText());
                if (titleField.getText().isBlank() || directorField.getText().isBlank()) {
                    showError("Tytul i rezyser nie moga byc puste.");
                    return null;
                }
                if (year > Year.now().getValue() || quantity < 0) {
                    showError("Rok nie moze byc przyszly, a stan nie moze byc ujemny.");
                    return null;
                }

                if (item == null) {
                    return insertMovie(titleField.getText(), directorField.getText(), year, quantity);
                }

                updateMovie(item, titleField.getText(), directorField.getText(), year, quantity);
                return item;
            } catch (NumberFormatException e) {
                showError("Rok i stan musza byc liczbami.");
                return null;
            }
        });

        Optional<MovieItem> result = dialog.showAndWait();
        result.ifPresent(savedItem -> {
            if (!movieList.contains(savedItem)) {
                movieList.add(savedItem);
            }
            movieTable.refresh();
        });
    }

    private MovieItem insertMovie(String title, String director, int year, int quantity) {
        String sql = "INSERT INTO movies(title, director, release_year, quantity) VALUES(?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.setString(2, director);
            pstmt.setInt(3, year);
            pstmt.setInt(4, quantity);
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            int id = keys.next() ? keys.getInt(1) : 0;
            return new MovieItem(id, title, director, year, quantity);
        } catch (SQLException e) {
            showError("Nie udalo sie zapisac filmu.");
            e.printStackTrace();
            return null;
        }
    }

    private void updateMovie(MovieItem item, String title, String director, int year, int quantity) {
        String sql = "UPDATE movies SET title = ?, director = ?, release_year = ?, quantity = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, director);
            pstmt.setInt(3, year);
            pstmt.setInt(4, quantity);
            pstmt.setInt(5, item.getId());
            pstmt.executeUpdate();

            item.setTitle(title);
            item.setDirector(director);
            item.setReleaseYear(year);
            item.setQuantity(quantity);
        } catch (SQLException e) {
            showError("Nie udalo sie zaktualizowac filmu.");
            e.printStackTrace();
        }
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText("Brak zaznaczenia");
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Blad");
        alert.showAndWait();
    }
}
