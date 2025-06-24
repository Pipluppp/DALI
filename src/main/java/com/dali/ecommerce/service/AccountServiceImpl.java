package com.dali.ecommerce.service;

import com.dali.ecommerce.model.Account;
import com.dali.ecommerce.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Account registerNewUser(Account account) throws Exception {
        if (account.getEmail() == null || account.getEmail().isBlank()) {
            throw new Exception("Email cannot be empty.");
        }
        String trimmedEmail = account.getEmail().trim();
        if (accountRepository.findByEmail(trimmedEmail).isPresent()) {
            throw new Exception("There is already an account with that email address: " + trimmedEmail);
        }
        account.setEmail(trimmedEmail);
        // Encode the raw password before saving
        account.setPasswordHash(passwordEncoder.encode(account.getPasswordHash()));
        return accountRepository.save(account);
    }
}