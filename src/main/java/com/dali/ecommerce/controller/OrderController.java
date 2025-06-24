package com.dali.ecommerce.controller;

import com.dali.ecommerce.model.Order;
import com.dali.ecommerce.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/order/{id}")
    public String orderDetail(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        Order order = orderService.findOrderById(id);

        // Security check: ensure the logged-in user owns this order
        if (authentication == null || !order.getAccount().getEmail().equals(authentication.getName())) {
            // Redirect to profile or an access-denied page if the user is not the owner
            return "redirect:/profile?error=access_denied";
        }

        model.addAttribute("order", order);
        return "order-detail";
    }
}