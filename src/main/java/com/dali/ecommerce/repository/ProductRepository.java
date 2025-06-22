package com.dali.ecommerce.repository;

import com.dali.ecommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Used for category filtering without search
    List<Product> findByCategory(String category);

    // Used for searching across all categories
    List<Product> findByNameContainingIgnoreCase(String name);

    // Used for searching within a specific category
    List<Product> findByNameContainingIgnoreCaseAndCategory(String name, String category);

    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category ASC")
    List<String> findDistinctCategories();
}