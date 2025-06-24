package com.dali.ecommerce.config;

import com.dali.ecommerce.service.CartService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final CartService cartService;

    public CustomAuthenticationSuccessHandler(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        // Merge session cart with DB cart
        cartService.mergeSessionCartWithDbCart(request.getSession(), authentication.getName());

        // Check if the original destination was the cart or checkout
        String targetUrl = getTargetUrlParameter();
        if (targetUrl != null && (request.getRequestURI().contains("/cart") || request.getRequestURI().contains("/checkout"))) {
            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, "/cart");
            return;
        }

        // Default behavior (e.g., redirect to home or saved request)
        super.onAuthenticationSuccess(request, response, authentication);
    }
}