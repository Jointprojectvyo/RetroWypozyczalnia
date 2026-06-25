package com.example.retroshopmanager;

public class MusicItem {
    private int id;
    private String title;
    private String artist;
    private int releaseYear;

    public MusicItem(int id, String title, String artist, int releaseYear) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.releaseYear = releaseYear;
    }

    // Gettery (do wyświetlania w tabeli)
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public int getReleaseYear() { return releaseYear; }

    // Settery (niezbędne do funkcji Edytuj)
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setReleaseYear(int releaseYear) { this.releaseYear = releaseYear; }
}