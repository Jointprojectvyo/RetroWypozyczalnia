package com.example.retroshopmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class RentalController {
    private static final double DAILY_PENALTY = 2.50;

    @FXML private TableView<RentalItem> rentalTable;
    @FXML private TableColumn<RentalItem, Integer> idColumn;
    @FXML private TableColumn<RentalItem, String> customerColumn;
    @FXML private TableColumn<RentalItem, String> typeColumn;
    @FXML private TableColumn<RentalItem, String> titleColumn;
    @FXML private TableColumn<RentalItem, String> rentalDateColumn;
    @FXML private TableColumn<RentalItem, String> dueDateColumn;
    @FXML private TableColumn<RentalItem, String> returnDateColumn;
    @FXML private TableColumn<RentalItem, Double> penaltyColumn;
    @FXML private TableColumn<RentalItem, String> statusColumn;

    // Kolekcja w pamieci ulatwia filtrowanie i odswiezanie dziennika.
    private final ObservableList<RentalItem> rentalList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("productType"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("productTitle"));
        rentalDateColumn.setCellValueFactory(new PropertyValueFactory<>("rentalDate"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        returnDateColumn.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        penaltyColumn.setCellValueFactory(new PropertyValueFactory<>("penalty"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadRentalsFromDatabase();
        rentalTable.setItems(rentalList);
    }

    private void loadRentalsFromDatabase() {
        rentalList.clear();
        String sql = "SELECT r.*, c.first_name, c.last_name FROM rentals r " +
                "JOIN customers c ON c.id = r.customer_id ORDER BY r.id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rentalList.add(new RentalItem(
                        rs.getInt("id"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("product_type"),
                        rs.getInt("product_id"),
                        rs.getString("product_title"),
                        rs.getString("rental_date"),
                        rs.getString("due_date"),
                        rs.getString("return_date"),
                        rs.getDouble("penalty"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            showError("Nie udalo sie pobrac dziennika wypozyczen.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRentAction() {
        ObservableList<CustomerItem> customers = loadCustomers();
        ObservableList<ProductChoice> products = loadAvailableProducts();

        if (customers.isEmpty()) {
            showWarning("Najpierw dodaj klienta w zakladce Klienci.");
            return;
        }
        if (products.isEmpty()) {
            showWarning("Brak dostepnych produktow do wypozyczenia.");
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Nowe wypozyczenie");
        ButtonType saveButtonType = new ButtonType("Wypozycz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        ComboBox<CustomerItem> customerBox = new ComboBox<>(customers);
        ComboBox<ProductChoice> productBox = new ComboBox<>(products);
        DatePicker dueDatePicker = new DatePicker(LocalDate.now().plusDays(14));
        customerBox.getSelectionModel().selectFirst();
        productBox.getSelectionModel().selectFirst();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Klient:"), 0, 0);
        grid.add(customerBox, 1, 0);
        grid.add(new Label("Towar:"), 0, 1);
        grid.add(productBox, 1, 1);
        grid.add(new Label("Termin zwrotu:"), 0, 2);
        grid.add(dueDatePicker, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton != saveButtonType) {
                return false;
            }

            ProductChoice product = productBox.getValue();
            if (product == null || product.getQuantity() <= 0) {
                showError("Wybrany towar nie jest dostepny.");
                return false;
            }
            if (dueDatePicker.getValue() == null || dueDatePicker.getValue().isBefore(LocalDate.now())) {
                showError("Termin zwrotu nie moze byc data z przeszlosci.");
                return false;
            }

            return insertRental(customerBox.getValue(), product, dueDatePicker.getValue());
        });

        Optional<Boolean> result = dialog.showAndWait();
        if (result.orElse(false)) {
            loadRentalsFromDatabase();
        }
    }

    @FXML
    private void handleReturnAction() {
        RentalItem selectedItem = rentalTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showWarning("Prosze zaznaczyc wypozyczenie do zwrotu.");
            return;
        }
        if ("Zwrócone".equals(selectedItem.getStatus()) || "Zwrocone".equals(selectedItem.getStatus())) {
            showWarning("To wypozyczenie zostalo juz zwrocone.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate = LocalDate.parse(selectedItem.getDueDate());
        long lateDays = Math.max(0, ChronoUnit.DAYS.between(dueDate, today));
        double penalty = lateDays * DAILY_PENALTY;

        String sql = "UPDATE rentals SET return_date = ?, penalty = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today.toString());
            pstmt.setDouble(2, penalty);
            pstmt.setString(3, "Zwrocone");
            pstmt.setInt(4, selectedItem.getId());
            pstmt.executeUpdate();

            selectedItem.setReturnDate(today.toString());
            selectedItem.setPenalty(penalty);
            selectedItem.setStatus("Zwrocone");
            rentalTable.refresh();
        } catch (SQLException e) {
            showError("Nie udalo sie zapisac zwrotu.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefreshAction() {
        loadRentalsFromDatabase();
    }

    private boolean insertRental(CustomerItem customer, ProductChoice product, LocalDate dueDate) {
        String sql = "INSERT INTO rentals(customer_id, product_type, product_id, product_title, rental_date, due_date, status) " +
                "VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customer.getId());
            pstmt.setString(2, product.getType());
            pstmt.setInt(3, product.getId());
            pstmt.setString(4, product.getTitle());
            pstmt.setString(5, LocalDate.now().toString());
            pstmt.setString(6, dueDate.toString());
            pstmt.setString(7, "Aktywne");
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            showError("Nie udalo sie zapisac wypozyczenia.");
            e.printStackTrace();
            return false;
        }
    }

    private ObservableList<CustomerItem> loadCustomers() {
        ObservableList<CustomerItem> customers = FXCollections.observableArrayList();
        String sql = "SELECT * FROM customers ORDER BY last_name, first_name";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                customers.add(new CustomerItem(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("phone"),
                        rs.getString("email")
                ));
            }
        } catch (SQLException e) {
            showError("Nie udalo sie pobrac klientow.");
            e.printStackTrace();
        }
        return customers;
    }

    private ObservableList<ProductChoice> loadAvailableProducts() {
        ObservableList<ProductChoice> products = FXCollections.observableArrayList();
        loadBookChoices(products);
        loadMovieChoices(products);
        return products;
    }

    private void loadBookChoices(ObservableList<ProductChoice> products) {
        String sql = "SELECT b.id, b.title, " +
                "(1 - COUNT(r.id)) AS available_count FROM books b " +
                "LEFT JOIN rentals r ON r.product_type = 'Ksiazka' AND r.product_id = b.id AND r.status = 'Aktywne' " +
                "GROUP BY b.id, b.title HAVING available_count > 0";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(new ProductChoice("Ksiazka", rs.getInt("id"), rs.getString("title"), rs.getInt("available_count")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMovieChoices(ObservableList<ProductChoice> products) {
        String sql = "SELECT m.id, m.title, " +
                "(m.quantity - COUNT(r.id)) AS available_count FROM movies m " +
                "LEFT JOIN rentals r ON r.product_type = 'Film' AND r.product_id = m.id AND r.status = 'Aktywne' " +
                "GROUP BY m.id, m.title, m.quantity HAVING available_count > 0";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(new ProductChoice("Film", rs.getInt("id"), rs.getString("title"), rs.getInt("available_count")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText("Uwaga");
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Blad");
        alert.showAndWait();
    }
}
