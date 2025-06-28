package com.dali.ecommerce.service;

import com.dali.ecommerce.model.Account;

public interface AccountService {
    Account registerNewUser(Account account) throws Exception;

    void changeUserPassword(String email, String currentPassword, String newPassword);

    void updateUserProfile(String email, Account profileUpdates);

    Account findByEmail(String email);
    Account findByResetPasswordToken(String token);

    void createPasswordResetTokenForUser(Account account, String token);
    void save(Account account);

}