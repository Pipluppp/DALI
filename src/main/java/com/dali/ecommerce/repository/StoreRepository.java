package com.dali.ecommerce.repository;

import com.dali.ecommerce.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Integer> {

    // Reverted to a simple list-based search
    List<Store> findByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(String name, String location);
}