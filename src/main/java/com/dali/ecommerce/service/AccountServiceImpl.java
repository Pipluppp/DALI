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

    @Override
public void changeUserPassword(String email, String currentPassword, String newPassword) {
    // Find the user in the database
    Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

    // 1. Check if the provided current password matches the one in the database
    if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
        throw new RuntimeException("Incorrect current password.");
    }

    // 2. Validate the new password against your rules
    String passwordPattern = "^(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
    if (!newPassword.matches(passwordPattern)) {
        throw new RuntimeException("New password does not meet security requirements (8+ chars, 1 special symbol).");
    }

    // 3. If all checks pass, encode and save the new password
    account.setPasswordHash(passwordEncoder.encode(newPassword));
    accountRepository.save(account);
}
}