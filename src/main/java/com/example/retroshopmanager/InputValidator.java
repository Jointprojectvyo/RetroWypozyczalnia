package com.example.retroshopmanager;

import java.time.LocalDate;
import java.time.Year;

final class InputValidator {
    private InputValidator() {
    }

    static String validateMovie(String title, String director, String yearText, String quantityText) {
        if (title == null || title.isBlank() || director == null || director.isBlank()) {
            return "Tytuł i reżyser są wymagane.";
        }
        if (title.trim().length() > 120 || director.trim().length() > 120) {
            return "Tytuł i reżyser mogą mieć maksymalnie 120 znaków.";
        }
        try {
            int year = Integer.parseInt(yearText.trim());
            int quantity = Integer.parseInt(quantityText.trim());
            if (year < 1888 || year > Year.now().getValue()) {
                return "Rok wydania musi mieścić się w zakresie 1888-" + Year.now().getValue() + ".";
            }
            if (quantity < 0 || quantity > 10000) {
                return "Stan musi być liczbą od 0 do 10000.";
            }
        } catch (NumberFormatException | NullPointerException e) {
            return "Rok wydania i stan muszą być liczbami całkowitymi.";
        }
        return null;
    }

    static String validateCustomer(String firstName, String lastName, String phone, String email) {
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            return "Imię i nazwisko są wymagane.";
        }
        if (!firstName.trim().matches("[\\p{L} .'-]{2,50}")
                || !lastName.trim().matches("[\\p{L} .'-]{2,50}")) {
            return "Imię i nazwisko powinny mieć 2-50 liter.";
        }
        if (phone != null && !phone.isBlank() && !phone.trim().matches("[+0-9 ()-]{7,20}")) {
            return "Podaj poprawny numer telefonu (7-20 znaków).";
        }
        if (email != null && !email.isBlank()
                && !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return "Podaj poprawny adres e-mail, np. klient@example.com.";
        }
        return null;
    }

    static String validateRental(CustomerItem customer, ProductChoice product, LocalDate dueDate, LocalDate today) {
        if (customer == null) {
            return "Wybierz klienta.";
        }
        if (product == null || product.getQuantity() <= 0) {
            return "Wybierz dostępny towar.";
        }
        if (dueDate == null) {
            return "Wybierz termin zwrotu.";
        }
        if (dueDate.isBefore(today)) {
            return "Termin zwrotu nie może być datą z przeszłości.";
        }
        return null;
    }
}
