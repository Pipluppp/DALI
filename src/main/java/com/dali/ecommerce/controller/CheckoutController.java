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

import com.dali.ecommerce.repository.AddressRepository;
import com.dali.ecommerce.service.ShippingService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping("/checkout")
@SessionAttributes("checkoutDetails")
public class CheckoutController {

    private final AccountRepository accountRepository;
    private final CartService cartService;
    private final OrderService orderService;
    private final ShippingService shippingService;
    private final AddressRepository addressRepository;

    public CheckoutController(AccountRepository accountRepository, CartService cartService, OrderService orderService, ShippingService shippingService, AddressRepository addressRepository) {
        this.accountRepository = accountRepository;
        this.cartService = cartService;
        this.orderService = orderService;
        this.shippingService = shippingService;
        this.addressRepository = addressRepository;
    }

    @ModelAttribute("checkoutDetails")
    public Map<String, Object> checkoutDetails() {
        return new HashMap<>();
    }

    private void populateCheckoutModel(Model model, Authentication authentication, HttpSession session, Map<String, Object> checkoutDetails) {
        List<CartItem> cartItems = cartService.getCartItems(authentication, session);
        double subtotal = cartService.getCartTotal(cartItems);
        double shipping = ((Number) checkoutDetails.getOrDefault("shippingFee", 0.0)).doubleValue();

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
    public String selectAddress(Model model, Authentication authentication, HttpSession session, @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails) {
        Account account = accountRepository.findByEmail(authentication.getName()).orElseThrow();
        model.addAttribute("account", account);
        model.addAttribute("addresses", account.getAddresses());
        model.addAttribute("step", "address");
        populateCheckoutModel(model, authentication, session,  checkoutDetails);
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
        Integer addressId = (Integer) checkoutDetails.get("addressId");
        if (addressId == null) {
            return "redirect:/checkout/address";
        }

        // 1. Fetch the selected address from the database
        Address customerAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected address not found"));

        // 2. FIX: Calculate the initial shipping fee using the default "Standard Delivery" method.
        // This provides the base fee that the JavaScript on the frontend will use.
        String defaultDeliveryMethod = "Standard Delivery";
        double calculatedShipping = shippingService.calculateShippingFee(customerAddress, defaultDeliveryMethod);

        // 3. Store the calculated base fee in the session
        checkoutDetails.put("shippingFee", calculatedShipping);

        // Optional but good practice: set a default delivery method in the session
        checkoutDetails.putIfAbsent("deliveryMethod", defaultDeliveryMethod);

        model.addAttribute("step", "shipping");

        model.addAttribute("priorityFeeAddition", ShippingService.PRIORITY_FEE_ADDITION);

        // 4. Populate the model with the new, correct shipping fee
        populateCheckoutModel(model, authentication, session, checkoutDetails);

        // Expose warehouse and user coordinates to the template for the map modal
        model.addAttribute("warehouseLat", shippingService.getWarehouseLat());
        model.addAttribute("warehouseLon", shippingService.getWarehouseLon());
        model.addAttribute("customerAddress", customerAddress);

        return "checkout";
    }

    @PostMapping("/shipping")
    public String saveDelivery(@RequestParam("deliveryMethod") String deliveryMethod,
                               @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails) {

        Integer addressId = (Integer) checkoutDetails.get("addressId");
        if (addressId == null) {
            return "redirect:/checkout/address"; // Should not happen, but good practice
        }

        // Fetch the selected address again
        Address customerAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected address not found"));

        // Recalculate the fee with the chosen delivery method
        double finalShippingFee = shippingService.calculateShippingFee(customerAddress, deliveryMethod);

        // Update the session details
        checkoutDetails.put("deliveryMethod", deliveryMethod);
        checkoutDetails.put("shippingFee", finalShippingFee); // <-- IMPORTANT

        return "redirect:/checkout/payment";
    }

    @GetMapping("/payment")
    public String selectPayment(Model model, Authentication authentication, HttpSession session,
                                @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails) {
        if (checkoutDetails.get("deliveryMethod") == null) {
            return "redirect:/checkout/shipping";
        }
        model.addAttribute("step", "payment");
        populateCheckoutModel(model, authentication, session, checkoutDetails);
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

    @GetMapping("/address/form")
    public String getAddressForm(Model model) {
        model.addAttribute("address", new Address());
        return "fragments/address-form :: address-form";
    }

    @GetMapping("/address/link")
    public String getAddAddressLink() {
        return "fragments/add-address-link :: add-address-link";
    }

    @PostMapping("/recalculate")
    public String recalculateShipping(@RequestParam("deliveryMethod") String deliveryMethod,
                                      Authentication authentication,
                                      HttpSession session,
                                      @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails,
                                      Model model) {
        Integer addressId = (Integer) checkoutDetails.get("addressId");
        if (addressId == null) {
            populateCheckoutModel(model, authentication, session, checkoutDetails);
            // In case of an error, we still need to provide the fragment
            return "fragments/checkout-summary-update";
        }

        Address customerAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected address not found"));

        // Recalculate the fee with the chosen delivery method
        double finalShippingFee = shippingService.calculateShippingFee(customerAddress, deliveryMethod);

        // Update the session details
        checkoutDetails.put("shippingFee", finalShippingFee);
        checkoutDetails.put("deliveryMethod", deliveryMethod);

        // Repopulate the model with the new values
        populateCheckoutModel(model, authentication, session, checkoutDetails);

        // Return the new fragment dedicated to OOB swaps
        return "fragments/checkout-summary-update";
    }
}