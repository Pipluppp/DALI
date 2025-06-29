package com.dali.ecommerce.cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByAccountAccountId(Integer accountId);
    Optional<CartItem> findByAccountAccountIdAndProductId(Integer accountId, Integer productId);
    void deleteByAccountAccountId(Integer accountId);
}