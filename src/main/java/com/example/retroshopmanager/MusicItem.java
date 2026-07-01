package com.example.retroshopmanager;

public class MusicItem {
    private int id;
    private String title;
    private String artist;
    private int releaseYear;
    private String genre;

    public MusicItem(int id, String title, String artist, int releaseYear, String genre) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.releaseYear = releaseYear;
        this.genre = genre;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public int getReleaseYear() { return releaseYear; }
    public String getGenre() { return genre; }

    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setReleaseYear(int releaseYear) { this.releaseYear = releaseYear; }
    public void setGenre(String genre) { this.genre = genre; }
}