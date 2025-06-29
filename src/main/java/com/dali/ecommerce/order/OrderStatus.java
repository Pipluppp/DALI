package com.dali.ecommerce.order;

public enum OrderStatus {
    PENDING_PAYMENT("Awaiting payment.", "processing"),
    PROCESSING("Order is being processed.", "processing"),
    PREPARING_FOR_SHIPMENT("Your order is being prepared for shipment.", "shipped"), // Using 'shipped' for CSS class consistency
    SHIPPED("Your order has been shipped.", "shipped"),
    DELIVERED("Your order has been delivered.", "delivered"),
    CANCELLED("Your order has been cancelled.", "cancelled"),
    DELIVERY_FAILED("The delivery attempt was unsuccessful.", "cancelled"); // Using 'cancelled' for CSS class for red color

    private final String description;
    private final String cssClass;

    OrderStatus(String description, String cssClass) {
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