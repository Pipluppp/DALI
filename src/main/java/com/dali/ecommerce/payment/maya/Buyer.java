package com.dali.ecommerce.payment.maya;

public class Buyer {
    private String firstName;
    private String lastName;
    private Contact contact;

    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Contact getContact() { return contact; }
    public void setContact(Contact contact) { this.contact = contact; }
}