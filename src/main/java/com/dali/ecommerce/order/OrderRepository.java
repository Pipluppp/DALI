package com.dali.ecommerce.order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByAccountAccountIdOrderByCreatedAtDesc(Integer accountId);

    @Query("SELECT o FROM Order o WHERE " +
            "CAST(o.orderId as string) LIKE concat('%', :query, '%') OR " +
            "LOWER(o.account.firstName) LIKE LOWER(concat('%', :query, '%')) OR " +
            "LOWER(o.account.lastName) LIKE LOWER(concat('%', :query, '%')) " +
            "ORDER BY o.createdAt DESC")
    List<Order> searchOrders(@Param("query") String query);
}