package com.dali.ecommerce.repository;

import com.dali.ecommerce.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Integer> {

    // Updated search method to only search by name
    List<Store> findByNameContainingIgnoreCase(String name);
}