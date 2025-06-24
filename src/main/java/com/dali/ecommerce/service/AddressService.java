package com.dali.ecommerce.service;

import com.dali.ecommerce.model.Account;
import com.dali.ecommerce.model.Address;

public interface AddressService {
    void addAddress(Account account, Address address);
}