package com.dali.ecommerce.address;

import com.dali.ecommerce.account.Account;
import com.dali.ecommerce.location.Barangay;
import com.dali.ecommerce.location.City;
import com.dali.ecommerce.location.Province;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Integer addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id")
    private Province province;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id")
    private City city;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "barangay_id")
    private Barangay barangay;

    @Column(name = "additional_info")
    private String additionalInfo;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_default")
    private boolean isDefault;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public Province getProvince() { return province; }
    public void setProvince(Province province) { this.province = province; }
    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }
    public Barangay getBarangay() { return barangay; }
    public void setBarangay(Barangay barangay) { this.barangay = barangay; }
    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public String getFullAddress() {
        return Stream.of(
                        additionalInfo,
                        barangay != null ? barangay.getName() : null,
                        city != null ? city.getName() : null,
                        province != null ? province.getName() : null)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(", "));
    }
}