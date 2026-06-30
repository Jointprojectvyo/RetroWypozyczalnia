package com.example.retroshopmanager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // Nazwa pliku bazy danych, który pojawi się w folderze projektu
    private static final String URL = "jdbc:sqlite:retroshop.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // Metoda inicjalizacji bazy danych (tworzenie tabeli)
    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Tworzenie tabeli książek
            String createBooksTable = "CREATE TABLE IF NOT EXISTS books (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL," +
                    "author TEXT NOT NULL," +
                    "release_year INTEGER NOT NULL," +
                    "is_foreign TEXT NOT NULL" + // "POLSKI" або "ZAGRANICZNY"
                    ");";
            stmt.execute(createBooksTable);


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}