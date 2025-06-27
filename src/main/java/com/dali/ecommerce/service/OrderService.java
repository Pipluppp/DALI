package com.dali.ecommerce.service;

import com.dali.ecommerce.model.Order;
import com.dali.ecommerce.model.ShippingStatus;

import java.util.Map;

public interface OrderService {
    Order createOrder(String username, Map<String, Object> checkoutDetails);
    Order createPendingOrder(String username, Map<String, Object> checkoutDetails);
    void setPaymentTransactionId(Integer orderId, String transactionId);
    void processSuccessfulPayment(Integer orderId, String mayaCheckoutId);
    void confirmPaymentOnSuccessRedirect(Integer orderId); // New method for success redirect
    void processStockForPaidOrder(Integer orderId);
    void failOrderPayment(Integer orderId);
    Order findOrderById(Integer orderId);
    void updateShippingStatus(Integer orderId, ShippingStatus newStatus);
    void cancelOrder(Integer orderId, String username);
}