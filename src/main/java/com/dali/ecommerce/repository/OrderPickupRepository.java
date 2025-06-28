package com.dali.ecommerce.repository;

import com.dali.ecommerce.model.OrderPickup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderPickupRepository extends JpaRepository<OrderPickup, Integer> {
}