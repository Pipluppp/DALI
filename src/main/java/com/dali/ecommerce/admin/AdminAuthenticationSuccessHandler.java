package com.dali.ecommerce.admin;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component("adminAuthenticationSuccessHandler")
public class AdminAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AdminAccountRepository adminAccountRepository;

    public AdminAuthenticationSuccessHandler(AdminAccountRepository adminAccountRepository) {
        this.adminAccountRepository = adminAccountRepository;
        super.setDefaultTargetUrl("/admin");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        String username = authentication.getName();
        adminAccountRepository.findByEmail(username).ifPresent(admin -> {
            HttpSession session = request.getSession();
            session.setAttribute("storeName", "DALI Carmona Warehouse Hub");
        });

        super.onAuthenticationSuccess(request, response, authentication);
    }
}