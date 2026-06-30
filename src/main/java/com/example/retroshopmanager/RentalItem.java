package com.example.retroshopmanager;

public class RentalItem {
    private int id;
    private String customerName;
    private String productType;
    private int productId;
    private String productTitle;
    private String rentalDate;
    private String dueDate;
    private String returnDate;
    private double penalty;
    private String status;

    public RentalItem(int id, String customerName, String productType, int productId, String productTitle,
                      String rentalDate, String dueDate, String returnDate, double penalty, String status) {
        this.id = id;
        this.customerName = customerName;
        this.productType = productType;
        this.productId = productId;
        this.productTitle = productTitle;
        this.rentalDate = rentalDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.penalty = penalty;
        this.status = status;
    }

    public int getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getProductType() { return productType; }
    public int getProductId() { return productId; }
    public String getProductTitle() { return productTitle; }
    public String getRentalDate() { return rentalDate; }
    public String getDueDate() { return dueDate; }
    public String getReturnDate() { return returnDate; }
    public double getPenalty() { return penalty; }
    public String getStatus() { return status; }

    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }
    public void setPenalty(double penalty) { this.penalty = penalty; }
    public void setStatus(String status) { this.status = status; }
}
