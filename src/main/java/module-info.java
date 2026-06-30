module com.example.retroshopmanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql; // Umożliwia pracę z bazami danych SQL

    opens com.example.retroshopmanager to javafx.fxml;
    exports com.example.retroshopmanager;
}