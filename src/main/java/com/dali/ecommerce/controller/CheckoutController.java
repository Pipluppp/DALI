package com.dali.ecommerce.controller;

import com.dali.ecommerce.model.Account;
import com.dali.ecommerce.model.Address;
import com.dali.ecommerce.model.CartItem;
import com.dali.ecommerce.model.Order;
import com.dali.ecommerce.repository.AccountRepository;
import com.dali.ecommerce.service.CartService;
import com.dali.ecommerce.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/checkout")
@SessionAttributes("checkoutDetails")
public class CheckoutController {

    private final AccountRepository accountRepository;
    private final CartService cartService;
    private final OrderService orderService;

    public CheckoutController(AccountRepository accountRepository, CartService cartService, OrderService orderService) {
        this.accountRepository = accountRepository;
        this.cartService = cartService;
        this.orderService = orderService;
    }

    @ModelAttribute("checkoutDetails")
    public Map<String, Object> checkoutDetails() {
        return new HashMap<>();
    }

    private void populateCheckoutModel(Model model, Authentication authentication, HttpSession session) {
        List<CartItem> cartItems = cartService.getCartItems(authentication, session);
        double subtotal = cartService.getCartTotal(cartItems);
        double shipping = cartItems.isEmpty() ? 0 : 50.0;
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shipping", shipping);
        model.addAttribute("total", subtotal + shipping);
    }

    @GetMapping
    public String checkout(Authentication authentication, HttpSession session, Model model) {
        List<CartItem> cartItems = cartService.getCartItems(authentication, session);
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }
        return "redirect:/checkout/address";
    }

    @GetMapping("/address")
    public String selectAddress(Model model, Authentication authentication, HttpSession session) {
        Account account = accountRepository.findByEmail(authentication.getName()).orElseThrow();
        model.addAttribute("addresses", account.getAddresses());
        model.addAttribute("step", "address");
        populateCheckoutModel(model, authentication, session);
        return "checkout";
    }

    @PostMapping("/address")
    public String saveAddress(@RequestParam("addressId") Integer addressId,
                              @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails) {
        checkoutDetails.put("addressId", addressId);
        return "redirect:/checkout/shipping";
    }

    @GetMapping("/shipping")
    public String selectDelivery(Model model, Authentication authentication, HttpSession session,
                                 @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails) {
        if (checkoutDetails.get("addressId") == null) {
            return "redirect:/checkout/address";
        }
        model.addAttribute("step", "shipping");
        populateCheckoutModel(model, authentication, session);
        return "checkout";
    }

    @PostMapping("/shipping")
    public String saveDelivery(@RequestParam("deliveryMethod") String deliveryMethod,
                               @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails) {
        checkoutDetails.put("deliveryMethod", deliveryMethod);
        return "redirect:/checkout/payment";
    }

    @GetMapping("/payment")
    public String selectPayment(Model model, Authentication authentication, HttpSession session,
                                @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails) {
        if (checkoutDetails.get("deliveryMethod") == null) {
            return "redirect:/checkout/shipping";
        }
        model.addAttribute("step", "payment");
        populateCheckoutModel(model, authentication, session);
        return "checkout";
    }

    @PostMapping("/payment")
    public String savePayment(@RequestParam("paymentMethod") String paymentMethod,
                              @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails) {
        checkoutDetails.put("paymentMethod", paymentMethod);
        return "redirect:/checkout/finish";
    }

    @GetMapping("/finish")
    public String finishOrder(Authentication authentication,
                              @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails,
                              SessionStatus status,
                              RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.createOrder(authentication.getName(), checkoutDetails);
            status.setComplete(); // Clears the @SessionAttributes
            redirectAttributes.addFlashAttribute("orderId", order.getOrderId());
            return "redirect:/checkout/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not place order: " + e.getMessage());
            return "redirect:/checkout/payment";
        }
    }

    @GetMapping("/success")
    public String orderSuccess(Model model) {
        if (!model.containsAttribute("orderId")) {
            return "redirect:/";
        }
        return "order-success";
    }
}