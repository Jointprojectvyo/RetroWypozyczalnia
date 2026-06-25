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
    private void showMusicView() {
        loadPage("MusicView.fxml");
    }

    private void loadPage(String page) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(page));
            mainBorderPane.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Nie udało się załadować pliku: " + page);
        }
    }
}