package com.dali.ecommerce.service;

import com.dali.ecommerce.model.CartItem;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface CartService {

    void addToCart(Integer productId, int quantity, Authentication authentication, HttpSession session);

    List<CartItem> getCartItems(Authentication authentication, HttpSession session);

    void updateQuantity(Integer productId, int quantity, Authentication authentication, HttpSession session);

    void removeFromCart(Integer productId, Authentication authentication, HttpSession session);

    void clearCart(Authentication authentication, HttpSession session);

    double getCartTotal(List<CartItem> cartItems);

    int getCartItemCount(Authentication authentication, HttpSession session);

    void mergeSessionCartWithDbCart(HttpSession session, String username);
}