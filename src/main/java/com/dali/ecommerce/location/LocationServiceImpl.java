package com.dali.ecommerce.location;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationServiceImpl implements LocationService {
    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;
    private final BarangayRepository barangayRepository;

    public LocationServiceImpl(ProvinceRepository provinceRepository, CityRepository cityRepository, BarangayRepository barangayRepository) {
        this.provinceRepository = provinceRepository;
        this.cityRepository = cityRepository;
        this.barangayRepository = barangayRepository;
    }

    @Override
    public List<Province> getAllProvinces() {
        return provinceRepository.findAllByOrderByName();
    }

    @Override
    public List<City> getCitiesByProvinceId(Integer provinceId) {
        return cityRepository.findByProvinceIdOrderByName(provinceId);
    }

    @Override
    public List<Barangay> getBarangaysByCityId(Integer cityId) {
        return barangayRepository.findByCityIdOrderByName(cityId);
    }
}