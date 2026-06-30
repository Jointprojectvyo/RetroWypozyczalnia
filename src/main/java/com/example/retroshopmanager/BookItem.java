package com.example.retroshopmanager;

/**
 * Klasa modelowa reprezentująca pojedynczy obiekt książki w systemie.
 * Wykorzystuje standardowe właściwości (properties) niezbędne do poprawnego
 * mapowania danych w komponencie TableView biblioteki JavaFX.
 */
public class BookItem {
    // POLA PRYWATNE (STRUKTURA REKORDU)
    private int id;             // Unikalny identyfikator książki (Klucz główny z bazy danych)
    private String title;       // Tytuł książki
    private String author;      // Imię i nazwisko autora
    private int releaseYear;    // Rok wydania pozycji
    private String isForeign;   // Flaga pochodzenia autora ("POLSKI" lub "ZAGRANICZNY")

    /**
     * Konstruktor pełny, używany do tworzenia obiektów na podstawie danych pobranych z bazy.
     */
    public BookItem(int id, String title, String author, int releaseYear, String isForeign) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.releaseYear = releaseYear;
        this.isForeign = isForeign;
    }

    // METODY DOSTĘPOWE (GETTERY)
    // Wymagane przez PropertyValueFactory w TableView do odczytu danych i wyświetlenia ich w kolumnach

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getReleaseYear() { return releaseYear; }
    public String getIsForeign() { return isForeign; }

    // METODY MODYFIKUJĄCE (SETTERY)
    // Używane podczas edycji istniejącego obiektu w pamięci programu po zatwierdzeniu formularza

    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setReleaseYear(int releaseYear) { this.releaseYear = releaseYear; }
    public void setIsForeign(String isForeign) { this.isForeign = isForeign; }
}