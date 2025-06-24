package com.dali.ecommerce.repository;

import com.dali.ecommerce.model.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminAccountRepository extends JpaRepository<AdminAccount, Integer> {
    Optional<AdminAccount> findByEmail(String email);
}