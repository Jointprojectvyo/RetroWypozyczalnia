package com.example.retroshopmanager;

public class MovieItem {
    private int id;
    private String title;
    private String director;
    private int releaseYear;
    private int quantity;

    public MovieItem(int id, String title, String director, int releaseYear, int quantity) {
        this.id = id;
        this.title = title;
        this.director = director;
        this.releaseYear = releaseYear;
        this.quantity = quantity;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDirector() { return director; }
    public int getReleaseYear() { return releaseYear; }
    public int getQuantity() { return quantity; }

    public void setTitle(String title) { this.title = title; }
    public void setDirector(String director) { this.director = director; }
    public void setReleaseYear(int releaseYear) { this.releaseYear = releaseYear; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
