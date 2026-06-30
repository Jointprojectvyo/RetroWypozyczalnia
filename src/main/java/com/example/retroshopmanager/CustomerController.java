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
import java.util.Optional;

public class CustomerController {
    @FXML private TableView<CustomerItem> customerTable;
    @FXML private TableColumn<CustomerItem, Integer> idColumn;
    @FXML private TableColumn<CustomerItem, String> firstNameColumn;
    @FXML private TableColumn<CustomerItem, String> lastNameColumn;
    @FXML private TableColumn<CustomerItem, String> phoneColumn;
    @FXML private TableColumn<CustomerItem, String> emailColumn;

    // Glowna kolekcja klientow przechowywana w pamieci interfejsu.
    private final ObservableList<CustomerItem> customerList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        loadCustomersFromDatabase();
        customerTable.setItems(customerList);
    }

    private void loadCustomersFromDatabase() {
        customerList.clear();
        String sql = "SELECT * FROM customers ORDER BY last_name, first_name";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                customerList.add(new CustomerItem(
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
    }

    @FXML
    private void handleAddAction() {
        showCustomerDialog(null);
    }

    @FXML
    private void handleEditAction() {
        CustomerItem selectedItem = customerTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showWarning("Prosze zaznaczyc klienta do edycji.");
            return;
        }
        showCustomerDialog(selectedItem);
    }

    @FXML
    private void handleDeleteAction() {
        CustomerItem selectedItem = customerTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showWarning("Prosze zaznaczyc klienta do usuniecia.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Czy na pewno chcesz usunac klienta: " + selectedItem.getFullName() + "?");
        alert.setHeaderText("Potwierdzenie usuniecia");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM customers WHERE id = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, selectedItem.getId());
                pstmt.executeUpdate();
                customerList.remove(selectedItem);
            } catch (SQLException e) {
                showError("Nie udalo sie usunac klienta. Sprawdz, czy nie ma wypozyczen.");
                e.printStackTrace();
            }
        }
    }

    private void showCustomerDialog(CustomerItem item) {
        Dialog<CustomerItem> dialog = new Dialog<>();
        dialog.setTitle(item == null ? "Dodaj klienta" : "Edytuj klienta");

        ButtonType saveButtonType = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField firstNameField = new TextField();
        TextField lastNameField = new TextField();
        TextField phoneField = new TextField();
        TextField emailField = new TextField();

        if (item != null) {
            firstNameField.setText(item.getFirstName());
            lastNameField.setText(item.getLastName());
            phoneField.setText(item.getPhone());
            emailField.setText(item.getEmail());
        }

        grid.add(new Label("Imie:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Nazwisko:"), 0, 1);
        grid.add(lastNameField, 1, 1);
        grid.add(new Label("Telefon:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("E-mail:"), 0, 3);
        grid.add(emailField, 1, 3);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton != saveButtonType) {
                return null;
            }
            if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank()) {
                showError("Imie i nazwisko sa wymagane.");
                return null;
            }

            if (item == null) {
                return insertCustomer(firstNameField.getText(), lastNameField.getText(), phoneField.getText(), emailField.getText());
            }

            updateCustomer(item, firstNameField.getText(), lastNameField.getText(), phoneField.getText(), emailField.getText());
            return item;
        });

        Optional<CustomerItem> result = dialog.showAndWait();
        result.ifPresent(savedItem -> {
            if (!customerList.contains(savedItem)) {
                customerList.add(savedItem);
            }
            customerTable.refresh();
        });
    }

    private CustomerItem insertCustomer(String firstName, String lastName, String phone, String email) {
        String sql = "INSERT INTO customers(id, first_name, last_name, phone, email) VALUES(?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection()) {
            int nextId = findNextAvailableCustomerId(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, nextId);
                pstmt.setString(2, firstName);
                pstmt.setString(3, lastName);
                pstmt.setString(4, phone);
                pstmt.setString(5, email);
                pstmt.executeUpdate();
            }
            return new CustomerItem(nextId, firstName, lastName, phone, email);
        } catch (SQLException e) {
            showError("Nie udalo sie zapisac klienta.");
            e.printStackTrace();
            return null;
        }
    }

    private int findNextAvailableCustomerId(Connection conn) throws SQLException {
        String sql = "SELECT id FROM customers ORDER BY id";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            int expectedId = 1;
            while (rs.next()) {
                int currentId = rs.getInt("id");
                if (currentId != expectedId) {
                    return expectedId;
                }
                expectedId++;
            }
            return expectedId;
        }
    }

    private void updateCustomer(CustomerItem item, String firstName, String lastName, String phone, String email) {
        String sql = "UPDATE customers SET first_name = ?, last_name = ?, phone = ?, email = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, phone);
            pstmt.setString(4, email);
            pstmt.setInt(5, item.getId());
            pstmt.executeUpdate();

            item.setFirstName(firstName);
            item.setLastName(lastName);
            item.setPhone(phone);
            item.setEmail(email);
        } catch (SQLException e) {
            showError("Nie udalo sie zaktualizowac klienta.");
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
