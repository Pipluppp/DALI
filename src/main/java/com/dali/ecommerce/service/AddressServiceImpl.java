package com.dali.ecommerce.service;

import com.dali.ecommerce.location.Barangay;
import com.dali.ecommerce.location.City;
import com.dali.ecommerce.location.Province;
import com.dali.ecommerce.location.BarangayRepository;
import com.dali.ecommerce.location.CityRepository;
import com.dali.ecommerce.location.ProvinceRepository;
import com.dali.ecommerce.model.Account;
import com.dali.ecommerce.model.Address;
import com.dali.ecommerce.repository.AccountRepository;
import com.dali.ecommerce.repository.AddressRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;
    private final AccountRepository accountRepository;
    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;
    private final BarangayRepository barangayRepository;


    public AddressServiceImpl(AddressRepository addressRepository, AccountRepository accountRepository, ProvinceRepository provinceRepository, CityRepository cityRepository, BarangayRepository barangayRepository) {
        this.addressRepository = addressRepository;
        this.accountRepository = accountRepository;
        this.provinceRepository = provinceRepository;
        this.cityRepository = cityRepository;
        this.barangayRepository = barangayRepository;
    }

    @Override
    @Transactional
    public Address addAddress(Account account, Address address, Integer provinceId, Integer cityId, Integer barangayId) {
        Province province = provinceRepository.findById(provinceId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Province ID"));
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid City ID"));
        Barangay barangay = barangayRepository.findById(barangayId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Barangay ID"));

        // If the new address is marked as default, unset all others for this user first.
        if (address.isDefault()) {
            addressRepository.unsetAllDefaultsForAccount(account.getAccountId());
        }

        address.setAccount(account);
        address.setProvince(province);
        address.setCity(city);
        address.setBarangay(barangay);

        // If this is the very first address for the user, make it default regardless of checkbox.
        if (account.getAddresses() == null || account.getAddresses().isEmpty()) {
            address.setDefault(true);
        }

        return addressRepository.save(address);
    }

    @Override
    public Address findByIdAndAccount(Integer addressId, String username) {
        Account account = accountRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return addressRepository.findByAddressIdAndAccount(addressId, account)
                .orElseThrow(() -> new AccessDeniedException("Address not found or does not belong to the user"));
    }

    @Override
    @Transactional
    public Address updateAddress(Integer addressId, String username, Address addressDetails, Integer provinceId, Integer cityId, Integer barangayId) {
        // Securely find the address
        Address existingAddress = findByIdAndAccount(addressId, username);

        // If this address is being set as the default, unset all other addresses for the user first.
        if (addressDetails.isDefault()) {
            addressRepository.unsetAllDefaultsForAccount(existingAddress.getAccount().getAccountId());
        }

        // Find location entities
        Province province = provinceRepository.findById(provinceId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Province ID"));
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid City ID"));
        Barangay barangay = barangayRepository.findById(barangayId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Barangay ID"));

        // Update properties from the form
        existingAddress.setPhoneNumber(addressDetails.getPhoneNumber());
        existingAddress.setAdditionalInfo(addressDetails.getAdditionalInfo());
        existingAddress.setLatitude(addressDetails.getLatitude());
        existingAddress.setLongitude(addressDetails.getLongitude());
        existingAddress.setDefault(addressDetails.isDefault());

        // Update location associations
        existingAddress.setProvince(province);
        existingAddress.setCity(city);
        existingAddress.setBarangay(barangay);

        return addressRepository.save(existingAddress);
    }
}