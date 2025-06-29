package com.dali.ecommerce.order;

import com.dali.ecommerce.payment.maya.MayaService;
import com.dali.ecommerce.payment.maya.CheckoutResponse;
import com.dali.ecommerce.account.Account;
import com.dali.ecommerce.address.Address;
import com.dali.ecommerce.cart.CartItem;
import com.dali.ecommerce.account.AccountRepository;
import com.dali.ecommerce.cart.CartService;
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

import com.dali.ecommerce.address.AddressRepository;
import com.dali.ecommerce.store.StoreRepository;
import com.dali.ecommerce.shipping.ShippingService;
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
    private final MayaService mayaService;
    private final StoreRepository storeRepository;

    public CheckoutController(AccountRepository accountRepository, CartService cartService, OrderService orderService, ShippingService shippingService, AddressRepository addressRepository, MayaService mayaService, StoreRepository storeRepository) {
        this.accountRepository = accountRepository;
        this.cartService = cartService;
        this.orderService = orderService;
        this.shippingService = shippingService;
        this.addressRepository = addressRepository;
        this.mayaService = mayaService;
        this.storeRepository = storeRepository;
    }

    @ModelAttribute("checkoutDetails")
    public Map<String, Object> checkoutDetails() {
        return new HashMap<>();
    }

    private void populateCheckoutModel(Model model, Authentication authentication, HttpSession session, Map<String, Object> checkoutDetails) {
        List<CartItem> cartItems = cartService.getCartItems(authentication, session);
        if (cartItems.isEmpty()) {
            model.addAttribute("isCartEmpty", true);
            return;
        }
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
        populateCheckoutModel(model, authentication, session, checkoutDetails);
        if (model.containsAttribute("isCartEmpty")) {
            return "redirect:/cart";
        }
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
        populateCheckoutModel(model, authentication, session, checkoutDetails);
        if (model.containsAttribute("isCartEmpty")) {
            return "redirect:/cart";
        }

        Address customerAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected address not found"));

        String defaultDeliveryMethod = "Standard Delivery";
        double calculatedShipping = shippingService.calculateShippingFee(customerAddress, defaultDeliveryMethod);

        checkoutDetails.put("shippingFee", calculatedShipping);
        checkoutDetails.putIfAbsent("deliveryMethod", defaultDeliveryMethod);

        model.addAttribute("step", "shipping");
        model.addAttribute("priorityFeeAddition", ShippingService.PRIORITY_FEE_ADDITION);
        populateCheckoutModel(model, authentication, session, checkoutDetails);

        model.addAttribute("warehouseLat", shippingService.getWarehouseLat());
        model.addAttribute("warehouseLon", shippingService.getWarehouseLon());
        model.addAttribute("customerAddress", customerAddress);

        // For pickup option, load all stores
        model.addAttribute("stores", storeRepository.findAll());

        return "checkout";
    }

    @PostMapping("/shipping")
    public String saveDelivery(@RequestParam("deliveryMethod") String deliveryMethod, @RequestParam(name="storeId", required = false) Integer storeId,
                               @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails,
                               RedirectAttributes redirectAttributes) {

        if ("Pickup Delivery".equalsIgnoreCase(deliveryMethod)) {
            if (storeId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please select a pickup store.");
                return "redirect:/checkout/shipping";
            }
            checkoutDetails.put("storeId", storeId);
            checkoutDetails.put("shippingFee", 0.0);
        } else {
            checkoutDetails.remove("storeId");
            Integer addressId = (Integer) checkoutDetails.get("addressId");
            if (addressId == null) {
                return "redirect:/checkout/address";
            }
            Address customerAddress = addressRepository.findById(addressId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected address not found"));
            double finalShippingFee = shippingService.calculateShippingFee(customerAddress, deliveryMethod);
            checkoutDetails.put("shippingFee", finalShippingFee);
        }

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
        populateCheckoutModel(model, authentication, session, checkoutDetails);
        if (model.containsAttribute("isCartEmpty")) {
            return "redirect:/cart";
        }
        return "checkout";
    }

    @PostMapping("/payment")
    public String processAndPlaceOrder(@RequestParam("paymentMethod") String paymentMethod,
                                       @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails,
                                       Authentication authentication,
                                       SessionStatus sessionStatus,
                                       RedirectAttributes redirectAttributes) {

        checkoutDetails.put("paymentMethod", paymentMethod);
        String username = authentication.getName();

        if ("Cash on delivery (COD)".equals(paymentMethod)) {
            try {
                Order order = orderService.createOrder(username, checkoutDetails);
                sessionStatus.setComplete(); // Clear checkoutDetails from session for COD
                redirectAttributes.addFlashAttribute("orderId", order.getOrderId());
                return "redirect:/checkout/success";
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Could not place order: " + e.getMessage());
                return "redirect:/checkout/payment";
            }
        } else if ("Maya".equals(paymentMethod) || "Credit/Debit Card".equals(paymentMethod)) {
            try {
                Order pendingOrder = orderService.createPendingOrder(username, checkoutDetails);
                CheckoutResponse mayaResponse = mayaService.createCheckout(pendingOrder);
                // Save the transaction ID immediately after creation
                orderService.setPaymentTransactionId(pendingOrder.getOrderId(), mayaResponse.getCheckoutId());
                return "redirect:" + mayaResponse.getRedirectUrl();
            } catch (Exception e) {
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("errorMessage", "Could not connect to payment gateway. Please try again or select another payment method.");
                return "redirect:/checkout/payment";
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid payment method selected.");
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

    @PostMapping("/recalculate")
    public String recalculateShipping(@RequestParam("deliveryMethod") String deliveryMethod,
                                      Authentication authentication,
                                      HttpSession session,
                                      @ModelAttribute("checkoutDetails") Map<String, Object> checkoutDetails,
                                      Model model) {
        Integer addressId = (Integer) checkoutDetails.get("addressId");

        // For pickup, the fee is always 0.
        if ("Pickup Delivery".equalsIgnoreCase(deliveryMethod)) {
            checkoutDetails.put("shippingFee", 0.0);
        } else {
            // For other methods, calculate based on address
            if (addressId == null) {
                // Fallback if address isn't set, though it should be
                populateCheckoutModel(model, authentication, session, checkoutDetails);
                return "fragments/checkout-summary-update";
            }
            Address customerAddress = addressRepository.findById(addressId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected address not found"));
            double finalShippingFee = shippingService.calculateShippingFee(customerAddress, deliveryMethod);
            checkoutDetails.put("shippingFee", finalShippingFee);
        }

        checkoutDetails.put("deliveryMethod", deliveryMethod);
        populateCheckoutModel(model, authentication, session, checkoutDetails);

        return "fragments/checkout-summary-update";
    }
}