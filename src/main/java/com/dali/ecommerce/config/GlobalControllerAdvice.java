package com.dali.ecommerce.config;

import com.dali.ecommerce.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final CartService cartService;

    public GlobalControllerAdvice(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("cartItemCount")
    public int getCartItemCount(HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return cartService.getCartItemCount(authentication, session);
        }
        return cartService.getCartItemCount(null, session);
    }
}