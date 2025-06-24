package com.dali.ecommerce.service;

import com.dali.ecommerce.model.Order;

import java.util.Map;

public interface OrderService {
    Order createOrder(String username, Map<String, Object> checkoutDetails);
}