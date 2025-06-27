package com.dali.ecommerce.model;

public enum ShippingStatus {
    PROCESSING("Order is being processed.", "processing"),
    PREPARING_FOR_SHIPMENT("Your order is being prepared for shipment.", "processing"),
    IN_TRANSIT("Your order has been shipped.", "shipped"),
    DELIVERED("Your order has been delivered.", "delivered"),
    CANCELLED("Your order has been cancelled.", "cancelled"),
    DELIVERY_FAILED("The delivery attempt was unsuccessful.", "cancelled");

    private final String description;
    private final String cssClass;

    ShippingStatus(String description, String cssClass) {
        this.description = description;
        this.cssClass = cssClass;
    }

    public String getDescription() {
        return description;
    }

    public String getCssClass() {
        return cssClass;
    }
}