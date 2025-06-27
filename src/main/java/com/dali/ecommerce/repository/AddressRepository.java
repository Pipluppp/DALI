package com.dali.ecommerce.repository;

import com.dali.ecommerce.model.Account;
import com.dali.ecommerce.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {
    Optional<Address> findByAddressIdAndAccount(Integer addressId, Account account);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.account.accountId = :accountId")
    void unsetAllDefaultsForAccount(@Param("accountId") Integer accountId);
}