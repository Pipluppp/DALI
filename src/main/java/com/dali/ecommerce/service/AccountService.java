package com.dali.ecommerce.service;

import com.dali.ecommerce.model.Account;

public interface AccountService {
    Account registerNewUser(Account account) throws Exception;

    void changeUserPassword(String email, String currentPassword, String newPassword);


}