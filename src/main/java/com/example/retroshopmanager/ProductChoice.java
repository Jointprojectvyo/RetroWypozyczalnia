package com.example.retroshopmanager;

public class ProductChoice {
    private final String type;
    private final int id;
    private final String title;
    private final int quantity;

    public ProductChoice(String type, int id, String title, int quantity) {
        this.type = type;
        this.id = id;
        this.title = title;
        this.quantity = quantity;
    }

    public String getType() { return type; }
    public int getId() { return id; }
    public String getTitle() { return title; }
    public int getQuantity() { return quantity; }

    @Override
    public String toString() {
        return type + ": " + title + " (stan: " + quantity + ")";
    }
}
