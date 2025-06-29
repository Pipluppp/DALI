package com.dali.ecommerce.cart;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CartUIAdvice {

    private final CartService cartService;

    public CartUIAdvice(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("cartItemCount")
    public int getCartItemCount(HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // A more robust check to distinguish a real user from an anonymous one.
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return cartService.getCartItemCount(authentication, session);
        }

        // For anonymous users, calculate cart count from session.
        return cartService.getCartItemCount(null, session);
    }
}
