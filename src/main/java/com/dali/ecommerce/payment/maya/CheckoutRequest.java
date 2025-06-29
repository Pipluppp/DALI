package com.dali.ecommerce.payment.maya;

import java.util.List;

public class CheckoutRequest {
    private TotalAmount totalAmount;
    private Buyer buyer;
    private List<Item> items;
    private RedirectUrl redirectUrl;
    private String requestReferenceNumber;

    // Getters and Setters
    public TotalAmount getTotalAmount() { return totalAmount; }
    public void setTotalAmount(TotalAmount totalAmount) { this.totalAmount = totalAmount; }
    public Buyer getBuyer() { return buyer; }
    public void setBuyer(Buyer buyer) { this.buyer = buyer; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public RedirectUrl getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(RedirectUrl redirectUrl) { this.redirectUrl = redirectUrl; }
    public String getRequestReferenceNumber() { return requestReferenceNumber; }
    public void setRequestReferenceNumber(String requestReferenceNumber) { this.requestReferenceNumber = requestReferenceNumber; }
}