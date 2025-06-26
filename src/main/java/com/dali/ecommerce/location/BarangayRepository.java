package com.dali.ecommerce.location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BarangayRepository extends JpaRepository<Barangay, Integer> {
    List<Barangay> findByCityIdOrderByName(Integer cityId);
}