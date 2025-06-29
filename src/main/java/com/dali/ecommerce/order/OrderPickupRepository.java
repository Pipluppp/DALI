package com.dali.ecommerce.order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderPickupRepository extends JpaRepository<OrderPickup, Integer> {
}