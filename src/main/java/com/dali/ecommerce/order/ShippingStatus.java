package com.dali.ecommerce.order;

public enum ShippingStatus {
    PROCESSING("Order is being processed.", "processing"),
    PREPARING_FOR_SHIPMENT("Your order is being prepared for shipment.", "processing"),
    IN_TRANSIT("Your order has been shipped.", "shipped"),
    DELIVERED("Your order has been delivered / is ready for pickup.", "delivered"),
    COLLECTED("Your order has been collected by the customer.", "delivered"),
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