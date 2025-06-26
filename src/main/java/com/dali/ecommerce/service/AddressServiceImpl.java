package com.dali.ecommerce.service;

import com.dali.ecommerce.location.Barangay;
import com.dali.ecommerce.location.City;
import com.dali.ecommerce.location.Province;
import com.dali.ecommerce.location.BarangayRepository;
import com.dali.ecommerce.location.CityRepository;
import com.dali.ecommerce.location.ProvinceRepository;
import com.dali.ecommerce.model.Account;
import com.dali.ecommerce.model.Address;
import com.dali.ecommerce.repository.AddressRepository;
import org.springframework.stereotype.Service;

@Service
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;
    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;
    private final BarangayRepository barangayRepository;


    public AddressServiceImpl(AddressRepository addressRepository, ProvinceRepository provinceRepository, CityRepository cityRepository, BarangayRepository barangayRepository) {
        this.addressRepository = addressRepository;
        this.provinceRepository = provinceRepository;
        this.cityRepository = cityRepository;
        this.barangayRepository = barangayRepository;
    }

    @Override
    public Address addAddress(Account account, Address address, Integer provinceId, Integer cityId, Integer barangayId) {
        Province province = provinceRepository.findById(provinceId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Province ID"));
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid City ID"));
        Barangay barangay = barangayRepository.findById(barangayId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Barangay ID"));

        address.setAccount(account);
        address.setProvince(province);
        address.setCity(city);
        address.setBarangay(barangay);

        // If this is the first address, make it the default
        if (account.getAddresses() == null || account.getAddresses().isEmpty()) {
            address.setDefault(true);
        }

        return addressRepository.save(address);
    }
}