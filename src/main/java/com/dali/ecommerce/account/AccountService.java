package com.dali.ecommerce.account;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

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

    @Transactional
    public void updateUserProfile(String email, Account profileUpdates) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Update only the allowed fields to prevent malicious data injection
        account.setFirstName(profileUpdates.getFirstName());
        account.setLastName(profileUpdates.getLastName());
        account.setPhoneNumber(profileUpdates.getPhoneNumber());

        accountRepository.save(account);
    }

    public Account findByEmail(String email) {
        return accountRepository.findByEmail(email).orElse(null);
    }

    public Account findByResetPasswordToken(String token) {
        return accountRepository.findByResetPasswordToken(token).orElse(null);
    }

    public void createPasswordResetTokenForUser(Account account, String token) {
        account.setResetPasswordToken(token);
        accountRepository.save(account);
    }

    public void save(Account account) {
        accountRepository.save(account);
    }
}