package com.example.retroshopmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        DatabaseManager.initializeDatabase();

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("MainView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 930, 620);
        stage.setTitle("RetroWypożyczalnia");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
