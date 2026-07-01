package com.example.retroshopmanager;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InputValidatorTest {
    @Test
    void acceptsValidMovie() {
        assertNull(InputValidator.validateMovie("Matrix", "Lana Wachowski", "1999", "3"));
    }

    @Test
    void rejectsMovieWithoutTitle() {
        assertEquals("Tytuł i reżyser są wymagane.",
                InputValidator.validateMovie(" ", "Ridley Scott", "1982", "2"));
    }

    @Test
    void rejectsFutureMovieYear() {
        assertEquals("Rok wydania musi mieścić się w zakresie 1888-" + Year.now().getValue() + ".",
                InputValidator.validateMovie("Film", "Reżyser", String.valueOf(Year.now().getValue() + 1), "1"));
    }

    @Test
    void rejectsNegativeMovieQuantity() {
        assertEquals("Stan musi być liczbą od 0 do 10000.",
                InputValidator.validateMovie("Film", "Reżyser", "2000", "-1"));
    }

    @Test
    void acceptsValidCustomer() {
        assertNull(InputValidator.validateCustomer("Anna", "Kowalska", "+48 123 456 789", "anna@example.com"));
    }

    @Test
    void rejectsInvalidCustomerEmail() {
        assertEquals("Podaj poprawny adres e-mail, np. klient@example.com.",
                InputValidator.validateCustomer("Anna", "Kowalska", "123456789", "anna@"));
    }

    @Test
    void rejectsInvalidCustomerPhone() {
        assertEquals("Podaj poprawny numer telefonu (7-20 znaków).",
                InputValidator.validateCustomer("Anna", "Kowalska", "abc", ""));
    }

    @Test
    void acceptsValidRental() {
        LocalDate today = LocalDate.of(2026, 7, 1);
        assertNull(InputValidator.validateRental(customer(), product(1), today.plusDays(14), today));
    }

    @Test
    void rejectsRentalWithoutCustomer() {
        LocalDate today = LocalDate.of(2026, 7, 1);
        assertEquals("Wybierz klienta.",
                InputValidator.validateRental(null, product(1), today.plusDays(1), today));
    }

    @Test
    void rejectsUnavailableRentalProduct() {
        LocalDate today = LocalDate.of(2026, 7, 1);
        assertEquals("Wybierz dostępny towar.",
                InputValidator.validateRental(customer(), product(0), today.plusDays(1), today));
    }

    @Test
    void rejectsPastRentalDueDate() {
        LocalDate today = LocalDate.of(2026, 7, 1);
        assertEquals("Termin zwrotu nie może być datą z przeszłości.",
                InputValidator.validateRental(customer(), product(1), today.minusDays(1), today));
    }

    private CustomerItem customer() {
        return new CustomerItem(1, "Anna", "Kowalska", "123456789", "anna@example.com");
    }

    private ProductChoice product(int quantity) {
        return new ProductChoice("Film", 1, "Matrix", quantity);
    }
}
