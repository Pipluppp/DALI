package com.dali.ecommerce.account;

import com.dali.ecommerce.address.Address;
import com.dali.ecommerce.cart.CartItem;
import com.dali.ecommerce.order.Order;
import jakarta.persistence.*;
import java.util.List;
import jakarta.validation.constraints.*;



@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Integer accountId;

    @Column(name = "account_first_name")
    private String firstName;

    @Column(name = "account_last_name")
    private String lastName;

    @Email(message = "Please enter a valid email address")
    @NotBlank(message = "Email is required")
    @Column(name = "account_email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Pattern(
            regexp = "^(\\+63|0)9\\d{9}$",
            message = "Phone number must start with +63 or 0 followed by 10 digits (e.g. +639123456789 or 09123456789)"
    )
    @Column(name = "phone_number")
    private String phoneNumber;

    @OneToMany(mappedBy = "account", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @OrderBy("isDefault DESC, addressId ASC")
    private List<Address> addresses;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<Order> orders;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    // Getters and Setters
    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public List<Address> getAddresses() { return addresses; }
    public void setAddresses(List<Address> addresses) { this.addresses = addresses; }
    public List<CartItem> getCartItems() { return cartItems; }
    public void setCartItems(List<CartItem> cartItems) { this.cartItems = cartItems; }
    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }
    public String getResetPasswordToken() {return resetPasswordToken; }
    public void setResetPasswordToken(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}