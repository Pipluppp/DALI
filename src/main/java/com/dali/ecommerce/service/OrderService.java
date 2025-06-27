package com.dali.ecommerce.service;

import com.dali.ecommerce.model.Order;
import com.dali.ecommerce.model.OrderStatus;

import java.util.Map;

public interface OrderService {
    Order createOrder(String username, Map<String, Object> checkoutDetails);
    Order findOrderById(Integer orderId);
    void updateOrderStatus(Integer orderId, OrderStatus newStatus);
    void cancelOrder(Integer orderId, String username);
}