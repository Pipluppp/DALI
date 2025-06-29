package com.dali.ecommerce.payment.maya;

public class CheckoutResponse {
    private String checkoutId;
    private String redirectUrl;

    // Getters and Setters
    public String getCheckoutId() { return checkoutId; }
    public void setCheckoutId(String checkoutId) { this.checkoutId = checkoutId; }
    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
}