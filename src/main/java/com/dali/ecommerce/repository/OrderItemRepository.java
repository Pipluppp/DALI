// DALI/src/main/java/com/dali/ecommerce/repository/OrderItemRepository.java
package com.dali.ecommerce.repository;

import com.dali.ecommerce.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
}