package com.dali.ecommerce.location;

import java.util.List;

public interface LocationService {
    List<Province> getAllProvinces();
    List<City> getCitiesByProvinceId(Integer provinceId);
    List<Barangay> getBarangaysByCityId(Integer cityId);
}