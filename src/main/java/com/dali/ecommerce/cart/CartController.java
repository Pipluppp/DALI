package com.dali.ecommerce.cart;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String viewCart(Model model, Authentication authentication, HttpSession session) {
        List<CartItem> cartItems = cartService.getCartItems(authentication, session);
        double subtotal = cartService.getCartTotal(cartItems);

        double shipping = 0.0;
        double total = subtotal;

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shipping", shipping);
        model.addAttribute("total", total);

        return "cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("productId") Integer productId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            Authentication authentication,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        cartService.addToCart(productId, quantity, authentication, session);
        redirectAttributes.addFlashAttribute("successMessage", "Item added to cart!");
        return "redirect:/shop"; // Or could redirect back to the product page
    }

    @PostMapping("/cart/update")
    public String updateCartItem(@RequestParam("productId") Integer productId,
                                 @RequestParam("quantity") int quantity,
                                 Authentication authentication,
                                 HttpSession session) {
        cartService.updateQuantity(productId, quantity, authentication, session);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeCartItem(@RequestParam("productId") Integer productId,
                                 Authentication authentication,
                                 HttpSession session) {
        cartService.removeFromCart(productId, authentication, session);
        return "redirect:/cart";
    }
}