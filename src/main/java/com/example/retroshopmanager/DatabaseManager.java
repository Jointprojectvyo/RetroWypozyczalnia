package com.example.retroshopmanager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // Nazwa pliku bazy danych, ktory pojawi sie w folderze projektu.
    private static final String URL = "jdbc:sqlite:retroshop.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // Metoda inicjalizacji bazy danych (tworzenie tabel).
    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            String createBooksTable = "CREATE TABLE IF NOT EXISTS books (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL," +
                    "author TEXT NOT NULL," +
                    "release_year INTEGER NOT NULL," +
                    "is_foreign TEXT NOT NULL" +
                    ");";
            stmt.execute(createBooksTable);

            String createMoviesTable = "CREATE TABLE IF NOT EXISTS movies (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL," +
                    "director TEXT NOT NULL," +
                    "release_year INTEGER NOT NULL," +
                    "quantity INTEGER NOT NULL DEFAULT 0" +
                    ");";
            stmt.execute(createMoviesTable);

            String createMusicTable = "CREATE TABLE IF NOT EXISTS music (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL," +
                    "artist TEXT NOT NULL," +
                    "release_year INTEGER NOT NULL" +
                    ");";
            stmt.execute(createMusicTable);

            String createCustomersTable = "CREATE TABLE IF NOT EXISTS customers (" +
                    "id INTEGER PRIMARY KEY," +
                    "first_name TEXT NOT NULL," +
                    "last_name TEXT NOT NULL," +
                    "phone TEXT," +
                    "email TEXT" +
                    ");";
            stmt.execute(createCustomersTable);

            String createRentalsTable = "CREATE TABLE IF NOT EXISTS rentals (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "customer_id INTEGER NOT NULL," +
                    "product_type TEXT NOT NULL," +
                    "product_id INTEGER NOT NULL," +
                    "product_title TEXT NOT NULL," +
                    "rental_date TEXT NOT NULL," +
                    "due_date TEXT NOT NULL," +
                    "return_date TEXT," +
                    "penalty REAL NOT NULL DEFAULT 0," +
                    "status TEXT NOT NULL," +
                    "FOREIGN KEY(customer_id) REFERENCES customers(id)" +
                    ");";
            stmt.execute(createRentalsTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
