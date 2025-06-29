package com.dali.ecommerce.shipping;

import org.springframework.stereotype.Service;

@Service
public class DistanceService {

    private static final int EARTH_RADIUS_KM = 6371;

    /*
      Calculates the distance between two points in latitude and longitude.
      Uses the Haversine formula.
      @return Distance in kilometers.
     */

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {

        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formula
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;

    }



}
