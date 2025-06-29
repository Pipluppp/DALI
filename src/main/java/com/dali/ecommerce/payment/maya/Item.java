package com.dali.ecommerce.payment.maya;

public class Item {
    private String name;
    private String quantity;
    private String code;
    private String description;
    private TotalAmount totalAmount;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TotalAmount getTotalAmount() { return totalAmount; }
    public void setTotalAmount(TotalAmount totalAmount) { this.totalAmount = totalAmount; }
}