module com.example.retroshopmanager {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.retroshopmanager to javafx.fxml;
    exports com.example.retroshopmanager;
}