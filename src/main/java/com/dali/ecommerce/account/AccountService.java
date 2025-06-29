package com.dali.ecommerce.account;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.regex.Pattern;

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

        // --- START OF PASSWORD VALIDATION LOGIC ---
        String rawPassword = account.getPasswordHash(); // Get the raw password before it's hashed
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }

        // This regex pattern checks for the presence of at least one special character
        Pattern specialCharPattern = Pattern.compile("[!@#$%^&*]");
        if (!specialCharPattern.matcher(rawPassword).find()) {
            throw new IllegalArgumentException("Password must contain at least one special character (e.g., !@#$%^&*).");
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

    public void resetUserPassword(String token, String newPassword) {
        // Find the user by their reset token
        Account account = findByResetPasswordToken(token);
        if (account == null) {
            throw new UsernameNotFoundException("Invalid password reset token.");
        }

        validatePassword(newPassword);


        account.setPasswordHash(passwordEncoder.encode(newPassword));
        account.setResetPasswordToken(null);

        accountRepository.save(account);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }
        Pattern specialCharPattern = Pattern.compile("[!@#$%^&*]");
        if (!specialCharPattern.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one special character (e.g., !@#$%^&*).");
        }
    }

    public void changeUserPassword(String email, String currentPassword, String newPassword) {
        // Find the user in the database
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));


        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new RuntimeException("Incorrect current password.");
        }


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