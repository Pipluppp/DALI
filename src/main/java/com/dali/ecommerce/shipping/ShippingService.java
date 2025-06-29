package com.dali.ecommerce.shipping;

import com.dali.ecommerce.address.Address;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class ShippingService {

    public static final double BASE_FEE = 50.0; // Base fee in PHP
    public static final double FEE_PER_KM = 10;   // Fee per kilometer in PHP
    public static final double FREE_SHIPPING_DISTANCE_KM = 0; // Free shipping for pickup
    public static final double PRIORITY_FEE_ADDITION = 150.0;

    private final DistanceService distanceService;

    // Inject warehouse coordinates from application.properties
    private final double warehouseLat;
    private final double warehouseLon;

    public ShippingService(DistanceService distanceService,
                           @Value("${app.warehouse.latitude}") double warehouseLat,
                           @Value("${app.warehouse.longitude}") double warehouseLon) {
        this.distanceService = distanceService;
        this.warehouseLat = warehouseLat;
        this.warehouseLon = warehouseLon;

    }

    public double getWarehouseLat() {
        return warehouseLat;
    }

    public double getWarehouseLon() {
        return warehouseLon;
    }

    public double calculateShippingFee(Address customerAddress, String deliveryMethod) {
        // Handle Pickup immediately
        if ("Pickup Delivery".equalsIgnoreCase(deliveryMethod)) {
            return 0.0;
        }

        if (customerAddress == null || customerAddress.getLatitude() == null || customerAddress.getLongitude() == null) {
            return 300.0; // Default fallback
        }

        double distance = distanceService.calculateDistance(
                warehouseLat,
                warehouseLon,
                customerAddress.getLatitude(),
                customerAddress.getLongitude()
        );

        double baseDistanceFee;
        if (distance <= FREE_SHIPPING_DISTANCE_KM) {
            baseDistanceFee = 0.0;
        } else {
            double distanceFee = distance * FEE_PER_KM;
            baseDistanceFee = BASE_FEE + distanceFee;
        }

        // Add extra fee for priority delivery
        if ("Priority Delivery".equalsIgnoreCase(deliveryMethod)) {
            return baseDistanceFee + PRIORITY_FEE_ADDITION;
        }

        // Return the base distance fee for Standard Delivery
        return baseDistanceFee;
    }
}