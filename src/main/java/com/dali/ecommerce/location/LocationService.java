package com.dali.ecommerce.location;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LocationService {
    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;
    private final BarangayRepository barangayRepository;

    public LocationService(ProvinceRepository provinceRepository, CityRepository cityRepository, BarangayRepository barangayRepository) {
        this.provinceRepository = provinceRepository;
        this.cityRepository = cityRepository;
        this.barangayRepository = barangayRepository;
    }

    public List<Province> getAllProvinces() {
        return provinceRepository.findAllByOrderByName();
    }

    public List<City> getCitiesByProvinceId(Integer provinceId) {
        return cityRepository.findByProvinceIdOrderByName(provinceId);
    }

    public List<Barangay> getBarangaysByCityId(Integer cityId) {
        return barangayRepository.findByCityIdOrderByName(cityId);
    }
}