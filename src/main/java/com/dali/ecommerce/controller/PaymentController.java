package com.dali.ecommerce.controller;

import com.dali.ecommerce.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payment/callback")
public class PaymentController {

    private final OrderService orderService;

    public PaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/success")
    public String handleSuccess(@RequestParam("orderId") Integer orderId, SessionStatus sessionStatus, Model model) {
        // The webhook is the source of truth, but we can clear the session here
        // as the checkout flow is complete from the user's perspective.
        sessionStatus.setComplete();
        model.addAttribute("orderId", orderId);
        return "payment-success";
    }

    @GetMapping("/failure")
    public String handleFailure(@RequestParam("orderId") Integer orderId, SessionStatus sessionStatus, RedirectAttributes redirectAttributes) {
        orderService.failOrderPayment(orderId);
        sessionStatus.setComplete();
        redirectAttributes.addFlashAttribute("errorMessage", "Your payment failed. Please try again or select a different payment method.");
        return "redirect:/checkout/payment";
    }

    @GetMapping("/cancel")
    public String handleCancel(@RequestParam("orderId") Integer orderId, SessionStatus sessionStatus, RedirectAttributes redirectAttributes) {
        orderService.failOrderPayment(orderId);
        sessionStatus.setComplete();
        redirectAttributes.addFlashAttribute("errorMessage", "Payment was cancelled. Please feel free to try again.");
        return "redirect:/checkout/payment";
    }
}