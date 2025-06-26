package com.dali.ecommerce.service;

import com.dali.ecommerce.model.Account;
import com.dali.ecommerce.model.Address;

public interface AddressService {
    Address addAddress(Account account, Address address, Integer provinceId, Integer cityId, Integer barangayId);
}