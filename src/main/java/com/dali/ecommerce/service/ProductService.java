package com.dali.ecommerce.service;

public interface ProductService {
    void updateProductStock(Integer productId, Integer newQuantity);
}