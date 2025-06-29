package com.dali.ecommerce.store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Integer> {

    // Updated search method to only search by name
    List<Store> findByNameContainingIgnoreCase(String name);
}