package com.dali.ecommerce.cart;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CartMergingHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final CartService cartService;

    public CartMergingHandler(CartService cartService) {
        this.cartService = cartService;
        // Set the default URL to redirect to after a successful login if no saved request is found.
        super.setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        // Our custom logic: Merge session cart with the user's DB cart.
        cartService.mergeSessionCartWithDbCart(request.getSession(), authentication.getName());

        // Let the parent class handle the redirection. It will redirect to the original
        // requested URL if one was saved, or to the default target URL ("/") otherwise.
        // This is a much more robust and standard way to handle login redirection.
        super.onAuthenticationSuccess(request, response, authentication);
    }
}