package com.dali.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @GetMapping("/login")
    public String showAdminLoginPage() {
        return "admin-login";
    }

    @GetMapping("/inventory")
    public String showInventoryPage() {
        return "admin-inventory";
    }

    @GetMapping("/orders")
    public String showOrdersPage() {
        return "admin-orders";
    }
}