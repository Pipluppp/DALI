package com.dali.ecommerce.service;

import com.dali.ecommerce.model.Account;
import com.dali.ecommerce.model.Address;
import com.dali.ecommerce.repository.AddressRepository;
import org.springframework.stereotype.Service;

@Service
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;

    public AddressServiceImpl(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    public void addAddress(Account account, Address address) {
        address.setAccount(account);
        // If this is the first address, make it the default
        if (account.getAddresses() == null || account.getAddresses().isEmpty()) {
            address.setDefault(true);
        }
        addressRepository.save(address);
    }
}