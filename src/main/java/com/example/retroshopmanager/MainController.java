package com.example.retroshopmanager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class MainController {
    @FXML
    private BorderPane mainBorderPane;

    @FXML
    public void initialize() {
        loadPage("StartView.fxml");
    }

    @FXML
    private void showStartView() {
        loadPage("StartView.fxml");
    }

    @FXML
    private void showMusicView() {
        loadPage("MusicView.fxml");
    }

    @FXML
    private void showMoviesView() {
        loadPage("MovieView.fxml");
    }

    @FXML
    private void showBooksView() {
        loadPage("BookView.fxml");
    }

    @FXML
    private void showCustomersView() {
        loadPage("CustomerView.fxml");
    }

    @FXML
    private void showRentalsView() {
        loadPage("RentalView.fxml");
    }

    private void loadPage(String page) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(page));
            mainBorderPane.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Nie udalo sie zaladowac pliku: " + page);
        }
    }
}
