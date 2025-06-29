package com.dali.ecommerce.product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByCategory(String category);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByNameContainingIgnoreCaseAndCategory(String name, String category);

    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category ASC")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT p.subcategory FROM Product p WHERE p.category = :category AND p.subcategory IS NOT NULL AND p.subcategory <> '' ORDER BY p.subcategory")
    List<String> findSubcategoriesByCategory(@Param("category") String category);

    List<Product> findByCategoryAndSubcategory(String category, String subcategory);
}