package com.dali.ecommerce.repository;

import com.dali.ecommerce.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByAccountAccountIdOrderByCreatedAtDesc(Integer accountId);
}